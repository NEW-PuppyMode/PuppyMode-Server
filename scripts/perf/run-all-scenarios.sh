#!/usr/bin/env bash
# 신고 알림 부하 시나리오 전체 실행 (#214). 사전 조건은 run-scenario.py 설명 참고.
#   - 가짜 웹훅 서버(fake-webhook.py)와 앱(run-app-loadtest.sh start)이 실행 중이어야 한다.
# 순서: 워밍업(결과 폐기) -> S0/S1 3회 교차 반복(노이즈 범위 확보) -> S2 2회 -> 타임아웃 -> 500 -> 시크릿 불일치
# 소요 시간 약 15분. 결과는 scripts/perf/out/report-*.json
set -uo pipefail
cd "$(dirname "$0")/../.."
run() { scripts/perf/run-scenario.py "$@"; }

echo "### 워밍업 (결과 폐기: JIT·커넥션 풀 워밍업)"
run --name warmup --rate 10 --duration 20s > /dev/null
for r in 1 2 3; do
  run --name "s0-r$r" --rate 10 --duration 30s                      # S0 기준선 (웹훅 즉시 응답)
  run --name "s1-r$r" --rate 10 --duration 30s --delay-ms 4000      # S1 느린 웹훅(4초)
done
run --name s2-r1 --rate 50 --duration 20s --delay-ms 4000           # S2 폭주
run --name s2-r2 --rate 50 --duration 20s --delay-ms 4000
run --name s1t   --rate 10 --duration 20s --delay-ms 8000           # 타임아웃 초과(앱 타임아웃 5초)
run --name s3    --rate 10 --duration 20s --status 500              # S3 웹훅 500
run --name s4    --rate 10 --duration 20s --expected-secret wrong-secret   # S4 시크릿 불일치
echo "### 전체 완료"
