#!/usr/bin/env bash
# Logging & monitoring checks. Run from the repo root (Git Bash on Windows):
#   scripts/check-observability.sh
# The static checks always run. The live checks run only while the app is up on :9090.
set -euo pipefail
shopt -s nullglob

PROMTOOL_IMAGE=prom/prometheus:v3.15.0
ROOT=$(pwd -W 2>/dev/null || pwd)              # a path Docker Desktop understands
# The first Python that actually runs. On Windows, python3 is often the Microsoft Store placeholder:
# it is on PATH, but it only prints "Python was not found".
PY=$(for p in python3 python; do "$p" -c '' >/dev/null 2>&1 && { echo "$p"; break; }; done; true)

fail() { echo "FAIL: $*" >&2; exit 1; }
ok()   { echo "  ok  $*"; }

[ -n "$PY" ] || fail "no working python3 or python on PATH"

echo "== source"
if grep -rnE 'System\.(out|err)\.print|printStackTrace\(' app/src/main/java | grep -vE ':[0-9]+:\s*//'; then
  fail "the lines above write to stdout/stderr directly - use the logger"
fi
ok "no System.out / printStackTrace"

echo "== alert rules"
# MSYS_NO_PATHCONV for docker only: it stops Git Bash rewriting the container paths. Set for the whole
# script, it would also stop curl's -o /dev/null becoming NUL, and every live check would fail.
promtool() { MSYS_NO_PATHCONV=1 docker run --rm -v "$ROOT/monitoring:/m:ro" --entrypoint promtool "$PROMTOOL_IMAGE" "$@"; }
promtool check rules /m/alerts.yml >/dev/null || fail "monitoring/alerts.yml does not parse"
promtool test rules /m/alerts.test.yml >/dev/null || fail "alert unit tests failed - rerun without >/dev/null to see which"
ok "alerts.yml parses and every alert test passes"

echo "== dashboards"
for f in monitoring/dashboards/*.json; do
  "$PY" -m json.tool "$f" >/dev/null || fail "$f is not valid JSON"
  ok "$f parses"
done

if ! curl -sf -o /dev/null localhost:9090/actuator/health/liveness; then
  echo "== app not running on :9090 - live checks skipped"
  echo "OK (static checks)"
  exit 0
fi

echo "== actuator surface"
code() { curl -s -o /dev/null -w '%{http_code}' "$1"; }
[ "$(code localhost:3000/actuator/health)" = 404 ] || fail "actuator still answers on the public port 3000"
for ep in env beans configprops loggers mappings; do
  [ "$(code "localhost:9090/actuator/$ep")" = 404 ] || fail "/actuator/$ep is exposed"
done
[ "$(code localhost:9090/actuator/health/readiness)" = 200 ] || fail "readiness is not UP - is Mongo or Redis down?"
ok "only health/info/metrics/prometheus exposed; readiness UP"

echo "== request correlation"
curl -s -D - -o /dev/null localhost:3000/api/v1/hello | grep -qi '^x-request-id:' || fail "no X-Request-ID header"
ok "X-Request-ID returned"

echo "== metrics"
metrics=$(curl -s localhost:9090/actuator/prometheus)
# Buckets replace the quantile series the p95 panels and SlowResponses read (Obs-1). Drop this guard
# only in the change that moves those to histogram_quantile().
if grep -q '^http_server_requests_seconds_bucket' <<<"$metrics"; then
  fail "HTTP histogram buckets are back - they replace the p95 series the dashboard and SlowResponses read"
fi
for name in http_server_requests_seconds_count logback_events_total jvm_memory_used_bytes process_start_time_seconds \
            mongodb_driver_commands_seconds_count submissions_dispatched_total submissions_failed_total \
            submissions_completed_total user_registrations_total; do
  grep -q "^$name" <<<"$metrics" || fail "metric $name is missing"
done
verdicts=$(grep -c '^submissions_completed_total' <<<"$metrics" || true)
[ "$verdicts" -ge 64 ] || fail "only $verdicts verdict counters - Obs-6 step 4 registers all 64 at zero"
ok "no histogram buckets; key metrics present"
echo "  series: $(grep -vc '^#' <<<"$metrics"). Biggest families:"
awk '!/^#/ { split($1, a, "{"); print a[1] }' <<<"$metrics" | sort | uniq -c | sort -rn | head -8 | sed 's/^/    /'

echo "== log file"
log=logs/codinglemon-backend.log
if [ -s "$log" ]; then
  tail -n 200 "$log" | "$PY" -c "import sys, json; [json.loads(l) for l in sys.stdin if l.strip()]" \
    || fail "$log has a line that is not JSON"
  ok "the last 200 lines of $log are JSON"
else
  echo "  --  no $log (it is written under the dev profile only) - skipped"
fi

echo OK