#!/usr/bin/env bash
# 거부 정책 변경(경고 한 줄 후 폐기) 뒤 포화 시나리오 재측정 (#216). 사전 조건은 run-push-matrix.sh 와 같다.
# 앱은 이미 FAKE_FCM=1 로 떠 있어야 한다. 결과는 after-* 이름으로 저장한다.
set -uo pipefail
cd "$(dirname "$0")/../.."
run() { scripts/perf/run-push-scenario.py "$@"; }
echo "### 워밍업 (결과 폐기)"
run --name after-warmup --rate 10 --duration 20s --fcm-delay-ms 300 > /dev/null
for r in 1 2 3; do
  sleep 70; run --name "after-cold-c1-30-r$r" --rate 30  --duration 30s --fcm-delay-ms 300
  sleep 70; run --name "after-cold-c3-r$r"    --rate 100 --duration 20s --fcm-delay-ms 300
done
echo "### 전체 완료"
