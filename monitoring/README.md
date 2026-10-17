# Monitoring

Metrics, logs, alert rules and the dashboard live in **Grafana Cloud** (free tier). One **Grafana Alloy**
agent per node ships to it. This folder is the source of truth for everything that lives there.

| Path | What |
|---|---|
| [`alerts.yml`](alerts.yml) | The 13 alert rules, in Prometheus format |
| [`alerts.test.yml`](alerts.test.yml) | promtool unit tests: each alert fires when it should, and stays quiet when it shouldn't |
| [`dashboards/`](dashboards/) | The overview dashboard, exported from Grafana |
| [`runbook.md`](runbook.md) | What to do when each alert fires |
| [`../deploy/alloy-config.alloy`](../deploy/alloy-config.alloy) | Core-node agent: app, Redis and host metrics; container logs |
| [`../deploy/alloy-worker.alloy`](../deploy/alloy-worker.alloy) | Worker-node agent: host metrics; the systemd journal |
| [`../deploy/alloy-local.alloy`](../deploy/alloy-local.alloy) | Dev box only: tails `logs/` instead of container stdout |

## Rules this setup keeps

- **Every series is on a dashboard or an alert.** Anything else is switched off at the source in
  `application.properties` (`management.metrics.enable.*`). Grafana Cloud free allows 10,000 active series.
- **Labels stay bounded.** `requestId`, `user` and `uri` are JSON fields in the logs, never Loki labels.
- **ERROR means "an operator should look".** Client mistakes are WARN at most; the access line already
  records every status.
- **No PII leaves the box.** Alloy masks anything email-shaped; never log one on purpose.

## Custom metrics

| Metric | Labels | Meaning |
|---|---|---|
| `submissions_dispatched_total` | executor, task | Jobs accepted, credited to the executor that took them |
| `submissions_completed_total` | executor, task, status | Verdicts delivered, counted once per job |
| `submissions_failed_total` | — | Jobs that ended FAILED; the reason is in the log line |
| `submissions_turnaround_seconds` | executor, task | Queued → verdict, p95, in whole seconds |
| `user_registrations_total`, `user_logins_total` | provider | Sign-ups and sign-ins; logins per day ≈ daily active users |
| `ai_hint_tokens` | model, type | LLM tokens spent |
| `async_task_failures_total` | task | `@Async` listener failures — that work is lost, not retried |

Everything else comes from Spring Boot (`http_server_requests_*`, `jvm_*`, `logback_events_total`,
`mongodb_driver_*`), the Redis exporter inside Alloy (`redis_*`) and the unix exporter (`node_*`).

## Changing things

```bash
# local$  repo root, Git Bash. Every check: promtool, the dashboard JSON, and the live app if it's up.
scripts/check-observability.sh
```

**Alert rules.** Edit `alerts.yml`, add or adjust a case in `alerts.test.yml`, run the check, then sync.
mimirtool wants a `namespace:` key that promtool rejects, so it is added on the way out:

```bash
# local$  with GC_GRAFANA_URL and GC_SA_TOKEN exported (see .env.example)
T=$(mktemp -d) && (echo "namespace: codinglemon"; cat monitoring/alerts.yml) > "$T/rules.yml"
MSYS_NO_PATHCONV=1 docker run --rm -v "$(cygpath -w "$T" 2>/dev/null || echo "$T"):/r:ro" \
  grafana/mimirtool:3.2.1 rules sync --concurrency=1 /r/rules.yml \
  --address="$GC_GRAFANA_URL/api/convert/" --id=1 --auth-token="$GC_SA_TOKEN" \
  --extra-headers="X-Grafana-Alerting-Datasource-UID=grafanacloud-prom"
```

This loads them as Grafana-managed rules, so they notify through Grafana's own Alertmanager, where the contact
points are. Rules in Grafana Cloud's hosted ruler would notify an Alertmanager this stack can't configure (Obs-7
step 6).

`sync` is idempotent and deletes groups you removed from the file.

**The dashboard.** Edit it in Grafana, then Export → Export as code, with the **Classic** model under Advanced
options (`mimirtool analyze` reads that format) and *Share dashboard with another instance* off (the data source
variables already make it portable). Download, save over `dashboards/codinglemon-overview.json`, and commit.
The JSON in git is what gets restored if the stack is lost.

## Testing

| Layer | Proves | How |
|---|---|---|
| Unit (JUnit) | requestId/MDC/access line, prod JSON shape, no email in logs, status codes, submission metrics counted right | `./gradlew test` |
| Alert logic | each alert fires and stays quiet on synthetic data | `promtool test rules` (in the check script) |
| Local smoke | actuator surface, metric names, JSON log file, series count | `scripts/check-observability.sh` with the app running |
| End to end | data reaches Grafana Cloud; alerts actually notify | the local harness and the drills below |

**Local harness.** Runs the production Alloy config against the app on your machine:

```bash
# local$  app running (dev profile); GC_* set in the root .env
docker compose -f docker-compose.dev.yml --profile observability up -d alloy
# Alloy's UI: http://localhost:12345
```

Stop it once production ships to the same stack — there is no `env` label, so local data would mix into
production's panels and alerts.

## Alert drills

An alert you haven't seen fire is a guess. Each drill: trigger it, wait for the notification, undo it,
and wait for the resolved notification. `R` is `docker exec redis_container redis-cli -a cheeselings`
locally, or the `docker compose … exec redis redis-cli` form in the runbook on the node.

| Alert | Drill | Drilled |
|---|---|---|
| ApiDown | stop the app for 4+ minutes | |
| BackendRestarting | restart the app three times, a minute or two apart | |
| RedisUnhealthy, HighErrorRatio, ErrorLogBurst | stop Redis and send authenticated requests to `/api/v1/problem/1/metadata` for 7 minutes | |
| RedisMemoryHigh | unit-tested only | |
| ResultsDeadLettered | `R XADD stream:execution-results-dlq '*' body drill`, then `R DEL stream:execution-results-dlq` | |
| SubmissionQueueBacklog | `XADD` 51 entries to `stream:pending-submissions`, wait 11 minutes, `DEL` it | |
| NsjailNotServing | fires whenever no worker heartbeat exists; refresh `executor:nsjail:heartbeat` by hand to resolve it | |
| SubmissionsFailing, SlowResponses, DiskFilling | unit-tested only | |
| WorkerEgressSpike | unit-tested only until the worker exists (Day 21) | |