#!/usr/bin/env python3
"""신고 알림 부하 시나리오 한 번을 실행하고 결과를 대조한다. (#214)

하는 일
  1) 가짜 웹훅 서버 설정(지연/응답 코드/시크릿 검증)과 집계 초기화
  2) 테스트 유저의 신고 데이터 삭제(UNIQUE 충돌 방지)
  3) k6 실행 (scripts/perf/complaint-load-test.js)
  4) 알림이 모두 처리될 때까지 대기(drain)
  5) k6 결과 + 가짜 서버 집계 + 앱 로그 + (가능하면) Prometheus 피크값을 모아 "회계 대조"를 출력

  회계 식: 신고 201 건수 = 가짜 서버 도착 건수 + 앱이 폐기한 건수(경고 로그)
          어긋나면 알림이 설명되지 않게 사라진 것이다.

사전 조건: 앱(run-app-loadtest.sh start)과 가짜 웹훅 서버(fake-webhook.py)가 실행 중이어야 한다.

예시
  scripts/perf/run-scenario.py --name s0 --rate 10 --duration 30s
  scripts/perf/run-scenario.py --name s1 --rate 10 --duration 30s --delay-ms 4000
  scripts/perf/run-scenario.py --name s2 --rate 50 --duration 20s --delay-ms 4000
  scripts/perf/run-scenario.py --name s3 --rate 10 --duration 20s --status 500
  scripts/perf/run-scenario.py --name s4 --rate 10 --duration 20s --expected-secret wrong-secret
"""
import argparse
import json
import os
import subprocess
import sys
import time
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "scripts/perf/out")
FAKE = "http://127.0.0.1:9999"
APP = "http://localhost:8080"
PROM = "http://127.0.0.1:9090"
APP_LOG = os.path.join(OUT, "app.log")
DUMMY_SECRET = "loadtest-dummy"


def call(method, url, body=None, timeout=5, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={"Content-Type": "application/json", **(headers or {})})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read() or b"{}")


def parse_seconds(text):
    return float(text[:-1]) * (60 if text.endswith("m") else 1) if text[-1] in "sm" else float(text)


def app_dropped():
    """앱의 notification_dropped_total{name="complaintExecutor"} 현재 값. 읽지 못하면 None."""
    try:
        with open(os.path.join(OUT, "prometheus-token"), encoding="utf-8") as f:
            token = f.read().strip()
        req = urllib.request.Request(APP + "/actuator/prometheus", headers={"Authorization": "Bearer " + token})
        with urllib.request.urlopen(req, timeout=5) as resp:
            for line in resp.read().decode().splitlines():
                if line.startswith("notification_dropped_total{") and 'name="complaintExecutor"' in line:
                    return float(line.rsplit(" ", 1)[1])
        return None
    except Exception:
        return None


def prom_peak(query, at_ts=None):
    try:
        qs = urllib.parse.urlencode({"query": query, **({"time": at_ts} if at_ts else {})})
        result = call("GET", f"{PROM}/api/v1/query?{qs}")["data"]["result"]
        return float(result[0]["value"][1]) if result else None
    except Exception:
        return None


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--name", required=True)
    ap.add_argument("--rate", type=int, default=10, help="초당 신고 요청 수")
    ap.add_argument("--duration", default="30s")
    ap.add_argument("--delay-ms", type=int, default=0, help="가짜 웹훅 응답 지연")
    ap.add_argument("--status", type=int, default=200, help="가짜 웹훅이 돌려줄 응답 코드")
    ap.add_argument("--expected-secret", default=DUMMY_SECRET, help="가짜 서버가 기대하는 시크릿(앱은 더미 값을 보낸다). 다르게 주면 403")
    ap.add_argument("--drain-max", type=int, default=120, help="알림 처리 완료를 기다리는 최대 초")
    ap.add_argument("--no-reset", action="store_true", help="신고 데이터 삭제를 건너뜀(이전 요청 수만큼 START_OFFSET 필요)")
    args = ap.parse_args()

    # --- 사전 점검
    for label, url in (("가짜 웹훅 서버", FAKE + "/stats"), ("앱", APP + "/actuator/health")):
        try:
            call("GET", url)
        except Exception:
            sys.exit(f"[중단] {label}에 연결할 수 없습니다: {url}")
    if not os.path.exists(os.path.join(OUT, "tokens.json")):
        sys.exit("[중단] scripts/perf/out/tokens.json 이 없습니다. scripts/perf/loadtest-data.sh setup 을 먼저 실행하세요.")

    # --- 1) 가짜 서버 설정, 2) 데이터 정리
    call("POST", FAKE + "/config", {"delay_ms": args.delay_ms, "status": args.status, "expected_secret": args.expected_secret})
    call("POST", FAKE + "/reset", {})
    if not args.no_reset:
        subprocess.run([os.path.join(ROOT, "scripts/perf/loadtest-data.sh"), "reset"], cwd=ROOT, check=True, stdout=subprocess.DEVNULL)

    log_offset = os.path.getsize(APP_LOG) if os.path.exists(APP_LOG) else 0
    metric_before = app_dropped()
    started = time.time()

    # --- 3) k6
    env = {**os.environ, "NAME": args.name, "RATE": str(args.rate), "DURATION": args.duration}
    k6 = subprocess.run(["k6", "run", "--quiet", "scripts/perf/complaint-load-test.js"], cwd=ROOT, env=env,
                        capture_output=True, text=True)
    k6_end = time.time()
    summary_lines = [l for l in k6.stdout.splitlines() if l.startswith("[k6:")]
    print("\n".join(summary_lines) or k6.stdout[-800:])
    if k6.returncode != 0:
        print(k6.stderr[-800:])

    # --- 4) drain: 도착이 8초(앱 타임아웃 5초보다 길게) 동안 없고 처리 중인 요청이 없을 때까지
    quiet_for = 8
    while time.time() - k6_end < args.drain_max:
        s = call("GET", FAKE + "/stats")["stats"]
        last = s["last_at"] or started
        if s["in_flight"] == 0 and time.time() - last > quiet_for:
            break
        time.sleep(1)
    ended = time.time()

    # --- 5) 수집
    fake = call("GET", FAKE + "/stats")
    fs = fake["stats"]
    with open(os.path.join(OUT, f"result-{args.name}.json"), encoding="utf-8") as f:
        result = json.load(f)

    new_log = ""
    if os.path.exists(APP_LOG):
        with open(APP_LOG, encoding="utf-8", errors="ignore") as f:
            f.seek(log_offset)
            new_log = f.read()
    lines = new_log.splitlines()
    dropped = sum("작업 큐가 가득 차" in l for l in lines)
    metric_after = app_dropped()
    dropped_metric = int(metric_after - metric_before) if metric_before is not None and metric_after is not None else None
    send_failed = sum("[신고 Slack 알림] 전송 실패" in l for l in lines)
    errors = sum(" ERROR " in l for l in lines)
    leaked = sum(("localhost:9999" in l) or (args.expected_secret and args.expected_secret in l) or (DUMMY_SECRET in l) for l in lines)
    failed_codes = {}
    for l in lines:
        if "[신고 Slack 알림] 전송 실패: HTTP" in l:
            code = l.rsplit("HTTP", 1)[1].strip()
            failed_codes[code] = failed_codes.get(code, 0) + 1

    window = int(ended - started) + 5
    peaks = {
        "peak_active_threads": prom_peak(f'max_over_time(executor_active_threads{{name="complaintExecutor"}}[{window}s])', ended),
        "peak_queued_tasks": prom_peak(f'max_over_time(executor_queued_tasks{{name="complaintExecutor"}}[{window}s])', ended),
        "server_p95_ms": None,
    }
    p95 = prom_peak(f'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{{uri="/complaints"}}[{window}s])))', ended)
    peaks["server_p95_ms"] = round(p95 * 1000, 1) if p95 is not None else None

    created = result["created_201"]
    explained = fs["received"] + dropped
    report = {
        "scenario": {"name": args.name, "rate_per_sec": args.rate, "duration": args.duration,
                     "webhook_delay_ms": args.delay_ms, "webhook_status": args.status,
                     "secret_matches": args.expected_secret == DUMMY_SECRET},
        "api": result,
        "webhook_server": fs,
        "app_log": {"dropped_warnings": dropped, "send_failed_warnings": send_failed, "send_failed_by_http": failed_codes,
                    "error_lines": errors, "url_or_secret_leaks": leaked},
        "prometheus_peaks": peaks,
        "dropped_metric": dropped_metric,
        "accounting": {"created_201": created, "webhook_received": fs["received"], "dropped": dropped,
                       "explained": explained, "unexplained": created - explained},
        "drain_seconds": round(ended - k6_end, 1),
    }
    with open(os.path.join(OUT, f"report-{args.name}.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    # --- 출력
    lat = result["latency_ms"]
    print(f"""
== 시나리오 {args.name}  (초당 {args.rate}건 x {args.duration}, 웹훅 지연 {args.delay_ms}ms, 응답 {args.status}, 시크릿 {'일치' if report['scenario']['secret_matches'] else '불일치'})
  신고 API   : 요청 {result['requests']} / 201={created} 409={result['conflict_409']} 기타={result['other_status']} / k6 dropped={result['dropped_iterations']}
               p50 {lat['med']}ms  p95 {lat['p95']}ms  p99 {lat['p99']}ms  max {lat['max']}ms   (서버측 p95: {peaks['server_p95_ms']}ms)
  알림 풀    : 피크 활성 스레드 {peaks['peak_active_threads']}, 피크 큐 깊이 {peaks['peak_queued_tasks']}  (core 1 / max 2 / queue 20)
  웹훅 서버  : 도착 {fs['received']} / 응답코드 {fs['by_status']} / 클라이언트 끊김 {fs['client_aborted']} / 동시 처리 최대 {fs['max_in_flight']}
               시크릿 ok={fs['secret_ok']} 불일치={fs['secret_mismatch']} 없음={fs['secret_missing']} / 구조화 필드 포함 {fs['with_structured_fields']}
  앱 로그    : 폐기 경고 {dropped} / 전송 실패 경고 {send_failed} {failed_codes or ''} / ERROR {errors} / URL·시크릿 노출 {leaked}
  폐기 메트릭: notification_dropped_total 증가분 {dropped_metric} / 로그 폐기 경고 {dropped}  → {'일치' if dropped_metric == dropped else ('측정 불가' if dropped_metric is None else '불일치')}
  회계 대조  : 신고 201 {created} = 웹훅 도착 {fs['received']} + 폐기 {dropped} ({explained})  → 설명되지 않는 건수 {created - explained}
  알림 처리 완료까지 추가 대기 {report['drain_seconds']}s   (결과: scripts/perf/out/report-{args.name}.json)
""")


if __name__ == "__main__":
    main()
