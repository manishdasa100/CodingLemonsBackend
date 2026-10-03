# CodingLemons — Deployment Architecture

Final deployment plan for the entire application: low-cost, scalable, and free of
AWS/GCP/Azure managed services. MongoDB stays on MongoDB Atlas (free tier) and object
storage on Cloudflare R2.

> **Key shift:** Redis is no longer just a cache. It is now load-bearing infrastructure —
> the work **queue** (Redis Streams), the **result** backplane, **dedup**, and
> **pending-likes**. The architecture is built around that and around the one hard
> constraint of the stack: *untrusted user code must never share a host with your data.*

---

## Guiding principles

1. **Redis is critical now** → it must be durable (AOF), private (never public), and
   low-latency to both the backend and the workers.
2. **Security boundary** → nsjail workers run untrusted code. They get their own host,
   isolated from Redis/backend/data. A sandbox escape should land an attacker on a
   disposable worker box, not next to your database.
3. **nsjail needs a real kernel** (namespaces / cgroups / seccomp) → rules out serverless
   container platforms. Use KVM VPSs (Hetzner, Contabo, Vultr). Avoid OpenVZ/LXC "VPS"
   providers — nsjail won't have the kernel features it needs.
4. **No Kubernetes.** At this scale it is pure cost and complexity. Docker Compose per
   node is the right tool.

---

## Where Redis lives: self-hosted on a private network (not Upstash)

Upstash (serverless Redis, not a hyperscaler, free tier) looks tempting but is the **wrong
fit** here, for two concrete reasons:

- **Per-command billing punishes queues.** Workers run `XREADGROUP ... BLOCK` in a
  continuous loop. Every poll is a billed command even when it returns nothing. Always-on
  consumers generate millions of commands/month → the free tier evaporates and
  pay-per-request gets expensive.
- **Cross-provider latency on the hot path.** The submission queue is latency-sensitive.
  Redis on Upstash + workers on a VPS means every queue op crosses the public internet.

**Decision: self-host Redis on the same private network as the backend and workers.** Zero
per-command cost, sub-millisecond latency, full control over persistence.

---

## Final topology

```
                          ┌─────────────────────────┐
        Users ───────────▶│   Cloudflare (free)     │  DNS, TLS, CDN, DDoS, WAF
                          └───────────┬─────────────┘
                          ┌───────────┴───────────┐
                          ▼                        ▼
              ┌───────────────────┐   ┌─────────────────────────┐
              │ Cloudflare Pages  │   │  Cloudflare Tunnel       │
              │ React (static)    │   │  (cloudflared, no open   │
              └───────────────────┘   │   inbound port)          │
                                      └────────────┬─────────────┘
                                                   ▼
   ╔═══════════════════════════════════════════════════════════════╗
   ║  CORE NODE  (Hetzner CX22, ~€4.5/mo)   — trusted data plane    ║
   ║   ┌─────────────────────┐     ┌──────────────────────────┐    ║
   ║   │ Spring Boot (Docker)│◀───▶│ Redis (Docker, AOF on)   │    ║
   ║   └─────────┬───────────┘     │ bound to private IP only │    ║
   ║             │                 └────────────┬─────────────┘    ║
   ╚═════════════│══════════════════════════════│══════════════════╝
                 │ Atlas (TLS)                  │  private network (10.x)
                 ▼                              ▼   Redis Streams
        ┌──────────────────┐      ╔═══════════════════════════════════╗
        │ MongoDB Atlas M0 │      ║ WORKER NODE(S) (Hetzner CPX, KVM) ║
        │ (free)           │      ║  Python + nsjail, N processes     ║
        └──────────────────┘      ║  consume stream consumer group    ║
                                  ║  — untrusted code, isolated —     ║
        ┌──────────────────┐      ╚═══════════════════════════════════╝
        │ Cloudflare R2    │◀─────────── backend (S3 SDK)
        └──────────────────┘
```

### Component summary

| Component | Hosting | Cost | Notes |
|---|---|---|---|
| React frontend | Cloudflare Pages | $0 | Global CDN, custom domain via CF DNS |
| Spring Boot + Redis | Hetzner CX22 (core node) | ~€4.5 | Co-located; talk over localhost / private net |
| Python nsjail workers | Hetzner CPX21/31 (worker node) | ~€8–14 | Separate box; sized for CPU |
| MongoDB | Atlas M0 | $0 | Already done; over TLS |
| Object storage | Cloudflare R2 | ~$0 | Already migrated |
| DNS / TLS / CDN / DDoS | Cloudflare | $0 | Tunnel hides origin entirely |
| **Total** | | **~€13–19/mo** | Grows only by adding worker nodes |

---

## Networking & security

- **Hetzner private network** (free) connects core ↔ worker nodes. All Redis traffic stays
  on `10.x`.
- **Redis lockdown**: `bind` to the private IP + `127.0.0.1` only, `protected-mode yes`,
  `requirepass <strong>`, and a Hetzner Cloud Firewall (free) allowing the Redis port
  *only* from the worker node's private IP. Redis is never reachable from the internet.
- **Backend exposure**: use **Cloudflare Tunnel** (`cloudflared` on the core node). The
  backend has **no open public port** — `cloudflared` dials out to Cloudflare. Free, and it
  hides the origin IP completely (big DDoS win).
- **Workers need no public IP** — outbound only. Inside nsjail, run executions with **no
  network namespace** so user code itself has zero network access.
- **Component communication** is decoupled:
  - frontend → backend (HTTPS / CORS to the Pages domain)
  - backend → Redis (private network)
  - backend → Atlas (TLS)
  - backend → R2 (S3 SDK)
  - workers ↔ Redis (private network)
  - backend and workers never talk directly — only through the stream. That decoupling is
    what makes workers independently scalable.

---

## Redis production config (important nuance)

Because Redis now holds the queue **and** results, eviction policy matters. The default LRU
policies could silently evict a queued job or an unread result under memory pressure — i.e.
data loss.

```conf
appendonly yes
appendfsync everysec          # ≤1s loss on crash
maxmemory <~70% of box RAM>
maxmemory-policy noeviction   # fail loudly instead of dropping jobs/results
save 900 1                    # periodic RDB snapshot
```

`noeviction` means that if Redis fills up, writes error out (alerting you to scale) rather
than quietly losing submissions. The cache footprint is modest (problem lists, POTD, like
counts), so this is safe.

**Scale-up refinement:** when traffic grows, split into two Redis instances — one for cache
(`allkeys-lru`, disposable) and one for the durable queue/results (`noeviction`, AOF). Start
with one.

**Backups:** a daily cron dumping `BGSAVE` output to R2 is near-free insurance. Nothing
financial or irreplaceable lives in Redis — worst case on total loss is that users resubmit
and the cache rebuilds.

---

## Scaling path

- **Phase 1 (launch):** one worker node, N processes ≈ vCPU count. The Redis Streams
  consumer group load-balances across them automatically. Vertically resize the worker box
  when CPU saturates.
- **Phase 2 (elastic):** add more worker nodes — they just join the **same consumer group**,
  zero coordination needed. A small autoscaler script polls `XLEN` / `XPENDING` and uses the
  Hetzner API to add/destroy worker nodes by demand. For spiky bursts with scale-to-zero
  economics, Fly.io Machines (Firecracker microVMs, nsjail-capable) are a good complement —
  pay per-second only while executing.
- **Phase 3 (HA, only if needed):** add a Redis replica + Sentinel for failover. Not needed
  at launch.

---

## Deployment mechanics

- **Per-node Docker Compose.** Core node: `backend` + `redis` + `cloudflared`. Worker node:
  the `worker` service (scale with `deploy.replicas`, `docker compose up --scale worker=N`,
  or a systemd template).
- **CI/CD, free:** GitHub Actions builds images → pushes to **GitHub Container Registry**
  (free) → SSH into the node and `docker compose pull && docker compose up -d`. No paid CI.
- **Secrets:** `.env` files (gitignored) or Docker secrets; keep using Jasypt for the
  encrypted values already in the properties files.

---

## What to do next (ordered)

1. Provision the **core node**, deploy Redis (locked-down config above) + backend via
   Compose, point `spring.data.redis.host` at the private IP / localhost.
2. Put **Cloudflare Tunnel** in front of the backend; move DNS to Cloudflare; deploy the
   React build to **Pages**.
3. Provision the **worker node** on the private network; deploy the Python nsjail worker
   consuming the stream consumer group.
4. Enable Redis **AOF + daily R2 backup**.
5. Once stable, add the **autoscaler** for Phase 2.

> **The one thing to internalize:** keeping Redis self-hosted and private — co-located with
> the backend, reachable by workers only over the private network — is what makes this both
> the cheapest *and* the lowest-latency option, while the separate worker node keeps
> untrusted execution away from your data. Everything else (Pages, R2, Atlas, Cloudflare) is
> already free or near-free and slots in cleanly.
