#!/usr/bin/env bash
# run-push-matrix.sh 의 보충 측정 (#216).
#  1) 1회차의 c0, c1-10, c1-20 재실행: 첫 측정은 이전 프로브가 쓴 응원 조합과 겹쳐 전부 409였다(푸시 경로가 돌지 않음).
#  2) 차가운 풀 측정(cold-*): 포화 시나리오를 직전 시나리오의 유휴 스레드(keepAlive 60초)가 사라진 뒤에 시작한다.
#     직전 시나리오가 풀을 8개로 늘려 놓으면 다음 시나리오는 큐가 차기를 기다리지 않고 8개 스레드로 시작하므로 용량이 달라진다.
#     시작 시 풀 스레드 수는 report의 scenario.pool_size_at_start 에 기록된다.
# 사전 조건과 환경 변수는 run-push-matrix.sh 와 같다.
set -uo pipefail
cd "$(dirname "$0")/../.."
run() { scripts/perf/run-push-scenario.py "$@"; }

scripts/perf/run-app-loadtest.sh stop >/dev/null 2>&1 || true
FAKE_FCM=1 scripts/perf/run-app-loadtest.sh start >/dev/null || { echo "앱 시작 실패"; exit 1; }
echo "### 워밍업 (결과 폐기)"
run --name warmup2 --rate 10 --duration 20s --fcm-delay-ms 300 > /dev/null

echo "##### 1회차 재실행"
run --name "c0-r1"    --rate 10 --duration 30s --fcm-delay-ms 0
run --name "c1-10-r1" --rate 10 --duration 30s --fcm-delay-ms 300
run --name "c1-20-r1" --rate 20 --duration 30s --fcm-delay-ms 300

echo "##### 차가운 풀"
for r in 1 2 3; do
  sleep 70   # 유휴 스레드(keepAlive 60초)가 사라질 때까지 기다린다
  run --name "cold-c1-30-r$r" --rate 30  --duration 30s --fcm-delay-ms 300
  sleep 70
  run --name "cold-c2-12-r$r" --rate 12  --duration 30s --fcm-delay-ms 1000
  sleep 70
  run --name "cold-c3-r$r"    --rate 100 --duration 20s --fcm-delay-ms 300
done
scripts/perf/run-app-loadtest.sh stop
echo "### 전체 완료"
