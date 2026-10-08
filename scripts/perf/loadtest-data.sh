#!/usr/bin/env bash
# 로컬 부하 테스트용 유저·토큰 관리 (#214)
#   scripts/perf/loadtest-data.sh setup     테스트 유저 생성(멱등) + scripts/perf/out/tokens.json 발급
#   scripts/perf/loadtest-data.sh reset     테스트 유저 관련 신고 데이터만 삭제 (시나리오 재실행 전)
#   scripts/perf/loadtest-data.sh cleanup   신고 데이터 + 테스트 유저 + 토큰 파일 삭제
# LOADTEST_USERS=200 (기본, 2~2000) 으로 유저 수를 바꿀 수 있다.
# 활성 DB가 localhost가 아니면 도구가 중단한다.
set -euo pipefail

mode="${1:-}"
case "$mode" in
  setup|reset|cleanup) ;;
  *) echo "사용법: $0 {setup|reset|cleanup}" >&2; exit 2 ;;
esac

cd "$(dirname "$0")/../.."
: "${JAVA_HOME:=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
export JAVA_HOME

# cleanTest: 환경 변수만 바뀌면 Gradle이 테스트를 UP-TO-DATE로 건너뛰므로 매번 다시 실행한다.
out="$(mktemp)"
status=0
LOADTEST_MODE="$mode" ./gradlew cleanTest test --tests 'com.umc.puppymode2.loadtest.LoadTestDataSetup' -i >"$out" 2>&1 || status=$?
grep -E "\[loadtest\]|BUILD|FAILED|localhost DB" "$out" || true
rm -f "$out"
exit "$status"
