#!/usr/bin/env bash
# 부하 테스트용 옵션으로 앱을 실행/종료한다. (#214)
#   scripts/perf/run-app-loadtest.sh start   가짜 웹훅(:9999)이 떠 있어야 시작한다
#   scripts/perf/run-app-loadtest.sh stop
#   scripts/perf/run-app-loadtest.sh status
#
# 이 스크립트가 고정하는 안전장치와 측정 옵션
#   - 웹훅 URL은 가짜 서버(http://localhost:9999/hook), 시크릿은 더미 값. 실제 n8n 주소는 쓰지 않는다.
#   - FIREBASE_CREDENTIALS_BASE64는 비운다. (FCM 초기화 스킵, 푸시 호출 없음)
#   - 로그 레벨을 WARN으로 올리고 Hibernate SQL 출력을 끈다. (요청마다 찍히는 로그가 응답시간에 끼어들지 않도록)
#   - /actuator/prometheus 노출 + HTTP 히스토그램(p95/p99) 활성화
# 앱 로그는 scripts/perf/out/app.log 에 쌓인다.
set -euo pipefail
cd "$(dirname "$0")/../.."

LOG=scripts/perf/out/app.log
FAKE_URL="http://localhost:9999/hook"
FAKE_SECRET="loadtest-dummy"   # 더미 값. 실제 시크릿을 쓰지 않는다.
: "${JAVA_HOME:=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
export JAVA_HOME

listening_pid() { lsof -nP -iTCP:"$1" -sTCP:LISTEN -t 2>/dev/null | head -1 || true; }

case "${1:-}" in
  start)
    [ -z "$(listening_pid 8080)" ] || { echo "8080 포트가 이미 사용 중입니다. 먼저 stop 하세요." >&2; exit 1; }
    [ -n "$(listening_pid 9999)" ] || { echo "가짜 웹훅 서버(:9999)가 없습니다. 먼저 'python3 scripts/perf/fake-webhook.py'를 실행하세요." >&2; exit 1; }
    mkdir -p scripts/perf/out
    COMPLAINT_SLACK_WEBHOOK_URL="$FAKE_URL" COMPLAINT_SLACK_WEBHOOK_SECRET="$FAKE_SECRET" \
    SCHEDULER_ALERT_WEBHOOK_URL="$FAKE_URL" SCHEDULER_ALERT_WEBHOOK_SECRET="$FAKE_SECRET" \
    FIREBASE_CREDENTIALS_BASE64="" \
      nohup ./gradlew bootRun --args="--management.endpoints.web.exposure.include=health,prometheus \
--management.metrics.distribution.percentiles-histogram.http.server.requests=true \
--logging.level.root=WARN --spring.jpa.properties.hibernate.show_sql=false" > "$LOG" 2>&1 &
    for _ in $(seq 1 60); do
      if curl -fs localhost:8080/actuator/health >/dev/null 2>&1; then echo "앱 실행 완료 (로그: $LOG)"; exit 0; fi
      sleep 3
    done
    echo "앱이 시간 안에 뜨지 않았습니다. 로그를 확인하세요: $LOG" >&2; exit 1 ;;
  stop)
    pid="$(listening_pid 8080)"
    if [ -n "$pid" ]; then kill "$pid"; sleep 3; echo "앱 종료"; else echo "실행 중인 앱이 없습니다."; fi ;;
  status)
    [ -n "$(listening_pid 8080)" ] && echo "앱 실행 중 (8080)" || echo "앱 중지됨" ;;
  *) echo "사용법: $0 {start|stop|status}" >&2; exit 2 ;;
esac
