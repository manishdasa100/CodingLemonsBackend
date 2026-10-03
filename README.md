# CodingLemons

**A production-grade competitive-programming platform** — think LeetCode, but where I built the hard parts myself: a from-scratch code-execution sandbox and a distributed, horizontally-scalable submission pipeline.

> 🔗 **Live demo:** _[coding-lemons.example.com](https://your-live-url-here)_ &nbsp;·&nbsp; 🎥 **3-min walkthrough:** _[watch the demo](https://your-demo-video-here)_
>
> _Try it: sign in with GitHub/Google → open any problem → write a solution → submit and watch it run in an isolated sandbox._

<!-- TODO before sharing: replace the two links above, and drop a demo GIF right here. A 5-second GIF of writing code → submitting → getting a verdict is worth more than any paragraph below. -->

![screenshot placeholder](docs/demo.gif)

---

## Why this project is interesting

Most "LeetCode clone" projects stop at CRUD over a problem table and shell out to a third-party judge. The two genuinely hard problems on a platform like this are **running untrusted code safely** and **doing it under load without falling over**. This project solves both directly.

### 1. A code-execution sandbox built from scratch

User-submitted code is arbitrary, hostile input. It cannot be trusted with the network, the filesystem, other users' submissions, or the host. So execution runs inside a **custom Python sandbox layered on [nsjail](https://github.com/google/nsjail)** that enforces isolation with Linux primitives:

- **Namespace isolation** (PID/mount/network/IPC/user) — each run gets its own world; **no network namespace** means user code has zero network access.
- **cgroups + seccomp-bpf** — hard CPU-time, wall-clock, memory, process-count and file-size limits, with the syscall surface filtered down.
- **Parallel execution governed by a semaphore** — a worker runs N concurrent sandboxed jobs sized to the box's vCPUs, so one traffic spike can't fork-bomb the machine.

The security boundary is architectural, not just per-process: **workers run on a physically separate host from the database and Redis** (see the deployment topology below). A sandbox escape lands an attacker on a disposable, network-restricted worker — not next to user data.

### 2. A distributed submission pipeline that scales horizontally

Submissions don't run inline on the request thread. The backend enqueues work and returns immediately; workers consume it asynchronously:

```
Client ──submit──▶ Spring Boot ──enqueue──▶ Redis Streams ──▶ nsjail workers (N)
   ▲                    │                    (consumer group)        │
   └──── poll result ◀──┴───────────── result backplane ◀───────────┘
```

- **Redis Streams consumer groups** load-balance jobs across every worker process and every worker node automatically — adding capacity is just starting more consumers on the same group, zero coordination.
- **Idempotent submissions** — Redis-backed dedup rejects double-submits of identical code.
- **Decoupling by design** — the backend and workers never talk directly, only through the stream. That's what makes the execution tier independently scalable from the API tier.

> This design is the result of iteration, not a first draft — the queue went **RabbitMQ → AWS SQS → Redis Streams** as I learned the real constraints (per-command cost of polling, latency on the hot path, operational simplicity). That evolution is in the git history.

---

## Feature set

| Area | What's there |
|---|---|
| **Problem solving** | Problem set with difficulty tiers & topics, per-language driver code, hidden/sample test cases, run vs. submit, detailed per-testcase verdicts |
| **Auth** | Email/password + **OAuth2 (Google & GitHub)**, JWT with a custom auth filter, role-based access (user/admin) |
| **Engagement** | **Timezone-aware daily streaks**, achievement **badges** (rule-based evaluation), **Problem of the Day** (scheduled + cached), likes |
| **Study plans** | Curated learning tracks with activate/deactivate/reset and per-user progress tracking |
| **Profiles** | Public profiles, submission history & stats, language-specific solve counts, work experience, ranks |
| **Admin** | Problem/test-case/driver-code management, company & badge & topic administration, publish workflow |
| **Ops** | Prometheus metrics, Grafana dashboards, ELK log pipeline, health indicators, request/latency instrumentation |

---

## Tech stack

**Backend** — Java 17 · Spring Boot 3.1 (Web + WebFlux + Security + OAuth2 + Validation) · MongoDB · Redis (cache, Streams queue, dedup) · JWT (jjwt) · ModelMapper · Jasypt (encrypted config)

**Execution tier** — Python · nsjail · Linux namespaces / cgroups / seccomp

**Storage & infra** — MongoDB Atlas · Cloudflare R2 (S3-compatible object storage) · Docker / Docker Compose

**Observability** — Micrometer + Prometheus · Grafana · Alertmanager · Logstash / Filebeat (ELK) · GeoIP2

**Frontend** — React (separate repo) _[link it here]_

---

## Architecture

Designed to run cheaply (~€13–19/mo) with a hard security boundary and no hyperscaler lock-in. Full reasoning in **[DEPLOYMENT.md](DEPLOYMENT.md)**.

```
                        Cloudflare (DNS · TLS · CDN · DDoS · Tunnel)
                                        │
              ┌─────────────────────────┴──────────────────────────┐
              ▼                                                     ▼
     Cloudflare Pages                              Cloudflare Tunnel (no open inbound port)
     React (static)                                                │
                                                                   ▼
   ╔══════════════════ CORE NODE — trusted data plane ══════════════════╗
   ║   Spring Boot  ◀────────▶  Redis (AOF, private-IP only)            ║
   ╚═══════│═══════════════════════════════│══════════════════════════════╝
           │ Atlas (TLS)                   │ private network · Redis Streams
           ▼                               ▼
     MongoDB Atlas                ╔═══════════════════════════════════╗
     Cloudflare R2                ║ WORKER NODE(S) — untrusted plane   ║
                                  ║  Python + nsjail · N processes     ║
                                  ║  consume stream · isolated code    ║
                                  ╚═══════════════════════════════════╝
```

**The one idea to take away:** untrusted user code never shares a host with user data. Workers are outbound-only, network-namespaced away, and independently scalable.

---

## API surface (selection)

```
POST /api/v1/auth/register            Register
POST /api/v1/auth/login               Login → JWT
GET  /api/v1/problemset/all           Paginated problem set
GET  /api/v1/problem/{id}             Problem detail
POST /api/v1/submission/submit        Enqueue a submission
GET  /api/v1/submission/check/{id}    Poll verdict
GET  /api/v1/problem/today            Problem of the Day
GET  /api/v1/user/streak              Current streak
POST /api/v1/studyPlan/set            Activate a study plan
GET  /api/v1/me                       Current user
```
Admin operations live under `/api/v1/admin/**` (role-gated).

---

## Running locally

**Prerequisites:** JDK 17, Docker + Docker Compose, a MongoDB URI, a Redis instance. Sandbox workers require a **Linux host with a real kernel** (namespaces/cgroups/seccomp) — they won't run on macOS/Windows directly; use a Linux VM.

```bash
# 1. Configure secrets (encrypted via Jasypt) — see application.properties
export JASYPT_ENCRYPTOR_PASSWORD=your-master-key

# 2. Run the backend
./gradlew bootRun

# 3. Bring up the observability stack (optional)
docker compose -f docker-compose.monitoring.yml up -d
```

Deployment is fully documented in **[DEPLOYMENT.md](DEPLOYMENT.md)**; monitoring setup in **[MONITORING-SETUP.md](MONITORING-SETUP.md)**.

---

## Roadmap

- [ ] **AI code review** — LLM-powered feedback on a user's submission (complexity, edge cases, idiomatic suggestions), with streaming responses and per-user rate/cost limits.
- [ ] Autoscaler for worker nodes driven by `XLEN` / `XPENDING`.
- [ ] Contest mode with live leaderboards.

---

## About

Built solo over ~a year. The parts I'm proudest of aren't the feature count — they're the **sandbox** and the **execution pipeline**, because those are where the real engineering is. Everything else exists to give those two systems something real to serve.

_Frontend repo: [link] · Sandbox worker repo: [link]_
