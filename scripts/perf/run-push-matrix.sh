#!/usr/bin/env bash
# 응원 푸시 경로의 용량 곡선을 교차 3회 반복 측정한다. (#216)
#   scripts/perf/run-push-matrix.sh              # 기본 3회 교차
#   ROUNDS=1 scripts/perf/run-push-matrix.sh     # 빠른 확인
#
# 사전 조건: 가짜 웹훅 서버(python3 scripts/perf/fake-webhook.py), loadtest-data.sh setup, 앱이 쓸 prometheus-token
# 환경 변수: JAVA_HOME, DB_URL, SPRING_DATA_REDIS_PORT, PERF_APP_PORT 를 호출 전에 지정한다. (개발 DB·다른 앱과 격리)
# 앱은 가짜 FCM(FAKE_FCM=1)으로 한 번만 재시작하고, 그 직후 워밍업 한 번은 결과를 버린다.
# 가짜 FCM의 지연·실패는 실행 중에 제어 파일로 바꾸므로 시나리오마다 재시작하지 않는다.
# 회차 순서는 시나리오 단위로 섞지 않고 "1회차 전체 -> 2회차 전체 -> 3회차 전체"로 돌려 실행 순서에 따른 드리프트가 한쪽에 쏠리지 않게 한다.
# 결과: scripts/perf/out/report-<이름>.json  →  python3 scripts/perf/summarize-push.py
set -uo pipefail
cd "$(dirname "$0")/../.."
ROUNDS="${ROUNDS:-3}"
run() { scripts/perf/run-push-scenario.py "$@"; }

scripts/perf/run-app-loadtest.sh stop >/dev/null 2>&1 || true
rm -f scripts/perf/out/push-offset
FAKE_FCM=1 scripts/perf/run-app-loadtest.sh start >/dev/null || { echo "앱 시작 실패"; exit 1; }
echo "### 워밍업 (결과 폐기: JIT·커넥션 풀 워밍업)"
run --name warmup --rate 10 --duration 20s --fcm-delay-ms 300 > /dev/null

for r in $(seq 1 "$ROUNDS"); do
  echo "##### 회차 $r / $ROUNDS"
  run --name "c0-r$r" --rate 10 --duration 30s --fcm-delay-ms 0                                   # C0 기준선
  for rate in 10 20 30 40; do                                                                    # C1 FCM 300ms, 도착률 증가
    run --name "c1-$rate-r$r" --rate "$rate" --duration 30s --fcm-delay-ms 300
  done
  for rate in 4 8 12; do                                                                         # C2 FCM 1,000ms
    run --name "c2-$rate-r$r" --rate "$rate" --duration 30s --fcm-delay-ms 1000
  done
  run --name "c3-r$r" --rate 100 --duration 20s --fcm-delay-ms 300                               # C3 과부하(풀 포화)
  for rate in 10 20; do                                                                          # C4 평소 300ms + 5%는 4초
    run --name "c4-$rate-r$r" --rate "$rate" --duration 30s --fcm-delay-ms 300 --slow-pct 5 --slow-delay-ms 4000
  done
  if [ "$r" = "1" ]; then                                                                        # F1 일부 토큰 실패(로그 안전성)
    run --name "f1-r$r" --rate 10 --duration 20s --fcm-delay-ms 300 --failure-pct 20
  fi
done
scripts/perf/run-app-loadtest.sh stop
echo "### 전체 완료"
