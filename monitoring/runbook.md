# Runbook

One section per alert in [`alerts.yml`](alerts.yml); each alert's `runbook_url` links here.
Logs: Grafana Cloud → Explore → Loki. Metrics: Explore → Prometheus. Don't SSH in to read logs.

| You want | Query |
|---|---|
| One request, start to finish | `{service="codinglemon-backend"} \| json \| requestId="<X-Request-ID>"` |
| Everything one user did | `{service="codinglemon-backend"} \| json \| user="<username>"` |
| One submission | `{service="codinglemon-backend"} \|= "<jobId>"` |
| Recent errors | `{service="codinglemon-backend", level="ERROR"}` |
| Failed requests | `{service="codinglemon-backend"} \| json \| logger_name="web.access" \| status >= 500` |

Redis commands below run on the core node as
`docker compose -f ~/codinglemon/deploy/docker-compose.prod.yml exec redis redis-cli -a "$REDIS_PASSWORD" <command>`
— or locally as `docker exec redis_container redis-cli -a cheeselings <command>`.

## ApiDown
**Means:** `up` is 0, or no metrics have arrived from the core node for 3 minutes. The backend is down, or Alloy is.
1. App or agent? If the `core` host panels are blank too, it's Alloy or the whole node.
2. `docker compose ps`; `docker compose logs backend --tail 100`.
3. Deployed recently? Roll back (Day 18).
4. Can't reach Atlas? Check the Atlas IP allowlist — the node's public IP can change on stop/start.
5. Alloy: `docker compose logs alloy --tail 50`, and its UI through `ssh -L 12345:localhost:12345 cl-core`.

## BackendRestarting
**Means:** the app restarted 3 times or more in 30 minutes. Docker brings it back each time it dies (`restart: unless-stopped`), so `up` recovers before [ApiDown](#apidown) can fire — but users hit an outage every few minutes.
1. Why it exits: `docker inspect $(docker compose ps -q backend) --format '{{.State.ExitCode}} oom={{.State.OOMKilled}} restarts={{.RestartCount}}'`.
2. `137 oom=true` → the 2 GB `mem_limit` killed the JVM. Dashboard → "JVM heap": climbing to max before each restart is a leak; flat means the heap plus everything else outgrew the limit.
3. Exit `1` → it dies during startup, usually after a config or secret change. `docker compose logs backend --tail 200` shows the last lines before each exit.
4. Deployed recently? Roll back (Day 18).

## HighErrorRatio
**Means:** over 5% of requests returned 5xx or 424 for 5 minutes.
1. Which endpoints: `sum by (uri, status) (rate(http_server_requests_seconds_count{status=~"5..|424"}[5m]))`.
2. Why: `{service="codinglemon-backend", level="ERROR"}` — read the stack traces.
3. Mostly 424 → no executor would take submissions. See [NsjailNotServing](#nsjailnotserving), then check Judge0 (quota, 401s) in the logs.
4. Mostly 503 → Redis unreachable. See [RedisUnhealthy](#redisunhealthy).

## SlowResponses
**Means:** one endpoint's p95 has been over 2s for 10 minutes. LLM calls, uploads and admin routes are excluded.
1. The `uri` is in the alert.
2. Mongo slow? Dashboard → "Mongo command latency". Atlas M0 throttles; check its metrics page.
3. Heap near max on the dashboard → GC pressure.
4. `/api/v1/submission/submit` slow → it's dispatching to Judge0 synchronously. See [NsjailNotServing](#nsjailnotserving).

## ErrorLogBurst
**Means:** more than 10 ERROR lines in 10 minutes, usually from code with no HTTP status to show it: stream consumers, schedulers, `@Async` listeners.
1. `{service="codinglemon-backend", level="ERROR"}`, grouped by `logger_name`.
2. "Async task … failed" → a listener (badges, streaks, scoring) lost its work. It is not retried.
3. "Execution result … failed to process" → see [ResultsDeadLettered](#resultsdeadlettered); it may be on its way there.
4. A new ERROR that is really a client mistake → make it WARN. ERROR has to keep meaning "look at this".

## RedisUnhealthy
**Means:** Redis is down, or its last RDB save or AOF write failed — after which it refuses writes.
1. `INFO persistence` → `rdb_last_bgsave_status`, `aof_last_write_status`. Then `docker compose logs redis --tail 50`.
2. "Can't save in background: fork: Cannot allocate memory" → memory overcommit. Check `vm.overcommit_memory` on the host; Redis warns about it at startup.
3. Disk full → see [DiskFilling](#diskfilling).
4. Redis down → `docker compose up -d redis`. Queued jobs survive in the AOF.

## RedisMemoryHigh
**Means:** over 80% of `maxmemory`. Under `noeviction`, at 100% every write fails, result delivery included.
1. `INFO memory`, then `redis-cli --bigkeys`.
2. `XLEN` each stream. A `stream:pending-submissions` that only grows means the worker acks without `XDEL`.
3. Short term: raise `maxmemory` in `deploy/redis.conf` if the box has headroom.
4. Long term: split the cache (`allkeys-lru`) from the queue (`noeviction`) into two instances.

## ResultsDeadLettered
**Means:** a result failed 5 deliveries. Its user has no verdict, and nothing will retry it.
1. `XRANGE stream:execution-results-dlq - + COUNT 10` — read the entries.
2. Take the `job_id`: `HGETALL submission:report:<jobId>`, and search the logs for it.
3. Usually a parse failure: a status value the backend doesn't know (worker/backend contract drift).
4. Fix the cause; the user resubmits.
5. Only once you understand it: `DEL stream:execution-results-dlq`.

## SubmissionQueueBacklog
**Means:** over 50 jobs have waited for the NSJAIL worker for 10 minutes.
1. Is anything consuming? `XINFO GROUPS stream:pending-submissions` — pending, lag, consumers.
2. Heartbeat: `TTL executor:nsjail:heartbeat` should be counting down and resetting.
3. Worker logs: `{node="worker", unit="cl-worker.service"}`.
4. `XLEN` growing while `lag` and `pending` are 0 → the worker acks without deleting. Fix the worker (`XDEL` after `XACK`).

## NsjailNotServing
**Means:** the in-house executor isn't taking work. Submissions run on Judge0's small free quota, and `CALIBRATE` fails outright.
1. Which cause? Heartbeat absent or stale → the worker's loop stopped. Judge0 dispatches with a fresh heartbeat → the IE breaker or an override.
2. `GET executor:override` — Judge0 pinned by hand? `DEL executor:override`.
3. `GET executor:nsjail:consecutive-ie` ≥ 5 → the worker is up but failing every job (toolchain, seccomp, disk). Read its logs.
4. `ssh cl-worker 'systemctl status cl-worker'`, then `sudo systemctl restart cl-worker`.
5. Verify: `TTL executor:nsjail:heartbeat` resets before it reaches 0.

## SubmissionsFailing
**Means:** over 20% of submissions ended FAILED in 15 minutes (at least 3).
1. `{service="codinglemon-backend"} |= "marked FAILED"` — the reasons.
2. "could not be read" → report contract drift (see [ResultsDeadLettered](#resultsdeadlettered) step 3).
3. "No result after …" or "did not return a result in time" → the worker or Judge0 is hanging. See [NsjailNotServing](#nsjailnotserving).
4. "Executor … reported status FAILED" → a worker-side crash. Read the worker logs.

## DiskFilling
**Means:** under 15% free on `/` of the node named in the alert.
1. `df -h /`; `docker system df`.
2. `docker image prune -a` removes old deploy images — keep the previous one for rollback.
3. Redis AOF size: `INFO persistence` → `aof_current_size`. `BGREWRITEAOF` compacts it.
4. Container logs are capped by Day 7's `json-file` options (10m × 3). If they aren't, that's the leak.

## WorkerEgressSpike
**Means:** sustained outbound traffic from a worker that should only talk to Redis and package mirrors. Possibly a sandbox escape or a compromised host.
1. `ssh cl-worker 'ss -tunp; top -b -n1 | head -20'`.
2. If it's unexplained: `sudo systemctl stop cl-worker` **first**, investigate second.
3. The worker is disposable: `terraform taint` and rebuild is a valid response.
4. Afterwards, rotate the Redis password — the worker holds it.