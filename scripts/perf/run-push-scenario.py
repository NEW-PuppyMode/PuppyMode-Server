#!/usr/bin/env python3
"""응원 푸시 경로 부하 시나리오 한 번을 실행하고 결과를 대조한다. (#216)

하는 일
  1) 가짜 FCM 설정(지연/느린 비율/실패 비율)을 제어 파일에 쓴다 (앱 재시작 없이 반영)
  2) 응원 API(POST /friends/{userId}/cheers)에 k6로 일정한 도착률의 부하를 준다 (cheer-load-test.js)
  3) 푸시가 모두 처리될 때까지 대기(drain)
  4) k6 결과 + 앱 메트릭(/actuator/prometheus) + 앱 로그를 모아 "회계 대조"를 출력한다

  회계 식: 응원 201 = 가짜 FCM 호출(이벤트 1건당 1회) + 푸시 풀 거부(queue_full) + 설명되지 않는 건
          설명되지 않는 건이 0이 아니면 이벤트가 어디선가 사라진 것이다 (예외, 조기 종료 등).
  도달률(앱 관점) = 가짜 FCM 호출 / 응원 201.  토큰 단위 성공률 = (FCM이 받은 토큰 - 실패 주입) / 받은 토큰.

사전 조건 (모두 로컬)
  - 앱이 FAKE_FCM=1 로 떠 있어야 한다: FAKE_FCM=1 scripts/perf/run-app-loadtest.sh start
  - scripts/perf/loadtest-data.sh setup 으로 유저·친구 관계·음주 기록·FCM 토큰이 준비돼 있어야 한다
  - 앱 포트는 PERF_APP_PORT 환경 변수(기본 8080)

응원 조합은 UNIQUE(보내는 사람, 받는 사람, 날짜)라, 이전 실행이 쓴 조합 수를 scripts/perf/out/push-offset 에 기록하고
다음 실행은 그 번호부터 이어서 쓴다. (loadtest-data.sh reset 으로 응원을 지우면 이 파일도 지운다)

예시
  scripts/perf/run-push-scenario.py --name c0 --rate 10 --duration 30s --fcm-delay-ms 0
  scripts/perf/run-push-scenario.py --name c1-r20 --rate 20 --duration 30s --fcm-delay-ms 300
  scripts/perf/run-push-scenario.py --name c4 --rate 10 --duration 30s --fcm-delay-ms 300 --slow-pct 5 --slow-delay-ms 4000
"""
import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "scripts/perf/out")
APP = "http://localhost:" + os.environ.get("PERF_APP_PORT", "8080")
APP_LOG = os.path.join(OUT, "app.log")
CONTROL = os.path.join(OUT, "fcm-control.json")
OFFSET_FILE = os.path.join(OUT, "push-offset")
FAKE_TOKEN_MARK = "loadtest-fake-fcm-token"
POOL = 'name="notificationExecutor"'


def scrape():
    """앱의 /actuator/prometheus를 {(metric, labels): value} 로 읽는다."""
    with open(os.path.join(OUT, "prometheus-token"), encoding="utf-8") as f:
        token = f.read().strip()
    req = urllib.request.Request(APP + "/actuator/prometheus", headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req, timeout=5) as resp:
        text = resp.read().decode()
    metrics = {}
    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue
        m = re.match(r"^([a-zA-Z_:][a-zA-Z0-9_:]*)(\{[^}]*\})?\s+(\S+)$", line)
        if m:
            try:
                metrics[(m.group(1), m.group(2) or "")] = float(m.group(3))
            except ValueError:
                pass
    return metrics


def pick(metrics, name, label_contains=""):
    return sum(v for (n, l), v in metrics.items() if n == name and label_contains in l)


def snapshot():
    m = scrape()
    return {
        "fake_calls": pick(m, "fake_fcm_calls_total"),
        "fake_tokens": pick(m, "fake_fcm_tokens_total"),
        "fake_failed_tokens": pick(m, "fake_fcm_failed_tokens_total"),
        "rejected": pick(m, "notification_dropped_total", POOL),
        "send_count": {r: pick(m, "notification_send_seconds_count", f'result="{r}"') for r in ("success", "failure", "error")},
        "send_sum": {r: pick(m, "notification_send_seconds_sum", f'result="{r}"') for r in ("success", "failure", "error")},
    }


class PeakSampler(threading.Thread):
    """부하 중 0.5초마다 풀·DB 풀·가짜 FCM 상태를 읽어 최대값을 기록한다. (Prometheus 서버 없이도 피크를 얻는다)"""
    WANTED = [
        ("peak_active_threads", "executor_active_threads", POOL),
        ("peak_queued_tasks", "executor_queued_tasks", POOL),
        ("peak_pool_size", "executor_pool_size_threads", POOL),
        ("peak_fake_fcm_inflight", "fake_fcm_inflight", ""),
        ("peak_hikari_active", "hikaricp_connections_active", ""),
        ("peak_hikari_pending", "hikaricp_connections_pending", ""),
    ]

    def __init__(self):
        super().__init__(daemon=True)
        self.stop_flag = threading.Event()
        self.peaks = {key: None for key, _, _ in self.WANTED}
        self.last = {}

    def run(self):
        while not self.stop_flag.is_set():
            try:
                m = scrape()
                for key, name, label in self.WANTED:
                    value = pick(m, name, label)
                    self.last[key] = value
                    if self.peaks[key] is None or value > self.peaks[key]:
                        self.peaks[key] = value
            except Exception:
                pass
            self.stop_flag.wait(0.5)


def write_control(args):
    body = {"delayMs": args.fcm_delay_ms, "slowPct": args.slow_pct, "slowDelayMs": args.slow_delay_ms,
            "failurePct": args.failure_pct, "failureCode": args.failure_code}
    with open(CONTROL, "w", encoding="utf-8") as f:
        json.dump(body, f)
    # 앱은 파일 변경 시각이 바뀌면 다시 읽는다. 이전 실행과 같은 시각이 되지 않도록 시각을 앞으로 보낸다.
    t = max(time.time(), os.path.getmtime(CONTROL) + 1) if os.path.exists(CONTROL) else time.time()
    os.utime(CONTROL, (t, t + 1))
    time.sleep(0.3)


def parse_seconds(text):
    return float(text[:-1]) * (60 if text.endswith("m") else 1) if text[-1] in "sm" else float(text)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--name", required=True)
    ap.add_argument("--rate", type=int, default=10, help="초당 응원 요청 수")
    ap.add_argument("--duration", default="30s")
    ap.add_argument("--fcm-delay-ms", type=int, default=0, help="가짜 FCM 응답 지연")
    ap.add_argument("--slow-pct", type=int, default=0, help="이 비율(%)의 호출만 --slow-delay-ms만큼 느리다 (호출 순번 기준)")
    ap.add_argument("--slow-delay-ms", type=int, default=0)
    ap.add_argument("--failure-pct", type=int, default=0, help="이 비율(%)의 토큰을 실패시킨다")
    ap.add_argument("--failure-code", default="UNAVAILABLE", help="실패 코드. 토큰 삭제를 일으키는 UNREGISTERED·INVALID_ARGUMENT는 쓰지 않는다")
    ap.add_argument("--drain-max", type=int, default=180, help="푸시 처리 완료를 기다리는 최대 초")
    args = ap.parse_args()

    if args.failure_code in ("UNREGISTERED", "INVALID_ARGUMENT"):
        sys.exit("[중단] 이 실패 코드는 FCM 토큰 삭제를 일으켜 테스트 데이터가 지워집니다.")
    try:
        urllib.request.urlopen(APP + "/actuator/health", timeout=5).read()
        before = snapshot()
    except Exception:
        sys.exit(f"[중단] 앱({APP}) 또는 가짜 FCM 메트릭에 접근할 수 없습니다. FAKE_FCM=1 로 앱을 띄웠는지 확인하세요.")
    if not os.path.exists(os.path.join(OUT, "tokens.json")):
        sys.exit("[중단] scripts/perf/out/tokens.json 이 없습니다. scripts/perf/loadtest-data.sh setup 을 먼저 실행하세요.")
    if not any(k[0] == "fake_fcm_calls_total" for k in scrape()):
        sys.exit("[중단] fake_fcm_calls_total 메트릭이 없습니다. 가짜 FCM이 꺼져 있습니다. 실제 FCM으로 부하를 주지 않도록 중단합니다.")

    start_offset = int(open(OFFSET_FILE).read().strip()) if os.path.exists(OFFSET_FILE) else 0
    write_control(args)

    log_offset = os.path.getsize(APP_LOG) if os.path.exists(APP_LOG) else 0
    # 시작 시점에 이미 만들어져 있는 푸시 풀 스레드 수. 직전 시나리오가 늘린 스레드(유휴, keepAlive 60초)가 남아 있으면
    # 큐가 차기를 기다리지 않고 바로 일하므로 용량이 달라진다. 모든 실행에 기록해 초기 조건을 구분한다.
    pool_size_at_start = pick(scrape(), "executor_pool_size_threads", POOL)
    before = snapshot()
    sampler = PeakSampler()
    sampler.start()
    started = time.time()

    env = {**os.environ, "BASE_URL": APP, "NAME": args.name, "RATE": str(args.rate), "DURATION": args.duration,
           "START_OFFSET": str(start_offset)}
    k6 = subprocess.run(["k6", "run", "--quiet", "scripts/perf/cheer-load-test.js"], cwd=ROOT, env=env,
                        capture_output=True, text=True)
    k6_end = time.time()
    summary_lines = [l for l in k6.stdout.splitlines() if l.startswith("[k6:")]
    print("\n".join(summary_lines) or k6.stdout[-800:])
    if k6.returncode != 0:
        print(k6.stderr[-800:])
    with open(os.path.join(OUT, f"result-{args.name}.json"), encoding="utf-8") as f:
        result = json.load(f)
    with open(OFFSET_FILE, "w") as f:
        f.write(str(start_offset + result["requests"]))

    # drain: 풀이 비고(활성 0, 큐 0), 가짜 FCM에 진행 중인 호출이 없고, 호출 수가 3초 동안 늘지 않을 때까지
    last_calls, stable_since = None, time.time()
    while time.time() - k6_end < args.drain_max:
        time.sleep(1)
        try:
            m = scrape()
        except Exception:
            continue
        calls = pick(m, "fake_fcm_calls_total")
        idle = pick(m, "executor_active_threads", POOL) == 0 and pick(m, "executor_queued_tasks", POOL) == 0 \
            and pick(m, "fake_fcm_inflight") == 0
        if calls != last_calls:
            last_calls, stable_since = calls, time.time()
        if idle and time.time() - stable_since >= 3:
            break
    ended = time.time()
    sampler.stop_flag.set()
    time.sleep(0.6)

    after = snapshot()
    created = result["created_201"]
    calls = int(after["fake_calls"] - before["fake_calls"])
    tokens = int(after["fake_tokens"] - before["fake_tokens"])
    failed_tokens = int(after["fake_failed_tokens"] - before["fake_failed_tokens"])
    rejected = int(after["rejected"] - before["rejected"])
    send = {r: int(after["send_count"][r] - before["send_count"][r]) for r in ("success", "failure", "error")}
    send_total = sum(send.values())
    send_mean_ms = round(sum(after["send_sum"][r] - before["send_sum"][r] for r in send) / send_total * 1000, 1) if send_total else None

    new_log = ""
    if os.path.exists(APP_LOG):
        with open(APP_LOG, encoding="utf-8", errors="ignore") as f:
            f.seek(log_offset)
            new_log = f.read()
    lines = new_log.splitlines()
    error_lines = [l for l in lines if " ERROR " in l]
    rejected_logs = sum("TaskRejectedException" in l or "RejectedExecutionException" in l for l in lines)
    token_leaks = sum(FAKE_TOKEN_MARK in l for l in lines)
    jwt_leaks = sum(bool(re.search(r"eyJ[A-Za-z0-9_-]{10,}\.", l)) for l in lines)
    # ERROR 로그의 종류별 건수 (메시지에 값이 섞일 수 있어 예외 클래스 이름과 로거만 센다)
    kinds = {}
    for l in error_lines:
        logger = re.search(r"\s---\s+\[[^\]]*\]\s+(\S+)\s+:", l)
        kinds[logger.group(1) if logger else "?"] = kinds.get(logger.group(1) if logger else "?", 0) + 1
    exceptions = {}
    for l in lines:
        m = re.match(r"^(?:Caused by: )?([a-zA-Z0-9_.$]+(?:Exception|Error))\b", l)
        if m:
            exceptions[m.group(1).rsplit(".", 1)[-1]] = exceptions.get(m.group(1).rsplit(".", 1)[-1], 0) + 1

    explained = calls + rejected
    report = {
        "scenario": {"name": args.name, "rate_per_sec": args.rate, "duration": args.duration,
                     "fcm_delay_ms": args.fcm_delay_ms, "slow_pct": args.slow_pct, "slow_delay_ms": args.slow_delay_ms,
                     "failure_pct": args.failure_pct, "start_offset": start_offset,
                     "pool_size_at_start": int(pool_size_at_start)},
        "api": result,
        "fake_fcm": {"calls": calls, "tokens": tokens, "failed_tokens": failed_tokens},
        "push_pool": {"rejected_metric": rejected, "rejected_logs": rejected_logs, "send_timer": send,
                      "send_mean_ms": send_mean_ms, "peaks": sampler.peaks},
        "app_log": {"error_lines": len(error_lines), "error_by_logger": kinds, "exceptions": exceptions,
                    "fcm_token_leaks": token_leaks, "jwt_leaks": jwt_leaks},
        "accounting": {"cheers_201": created, "fake_fcm_calls": calls, "rejected": rejected, "explained": explained,
                       "unexplained": created - explained},
        "reach": {"delivered_events": calls, "pct": round(calls / created * 100, 1) if created else None,
                  "token_success_pct": round((tokens - failed_tokens) / tokens * 100, 1) if tokens else None},
        "drain_seconds": round(ended - k6_end, 1),
    }
    with open(os.path.join(OUT, f"report-{args.name}.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    lat = result["latency_ms"]
    p = sampler.peaks
    dist = f", 느린 호출 {args.slow_pct}%={args.slow_delay_ms}ms" if args.slow_pct else ""
    fail = f", 실패 주입 {args.failure_pct}%" if args.failure_pct else ""
    print(f"""
== 시나리오 {args.name}  (초당 {args.rate}건 x {args.duration}, 가짜 FCM 지연 {args.fcm_delay_ms}ms{dist}{fail}, 시작 시 풀 스레드 {int(pool_size_at_start)}개)
  응원 API   : 요청 {result['requests']} / 201={created} 409={result['conflict_409']} 401={result['unauthorized_401']} 기타={result['other_status']} / k6 dropped={result['dropped_iterations']}
               p50 {lat['med']}ms  p95 {lat['p95']}ms  p99 {lat['p99']}ms  max {lat['max']}ms
  푸시 풀    : 피크 활성 스레드 {p['peak_active_threads']}, 피크 풀 크기 {p['peak_pool_size']}, 피크 큐 깊이 {p['peak_queued_tasks']}  (core 2 / max 8 / queue 100)
               거부(메트릭) {rejected} / 거부 로그 {rejected_logs} → {'일치' if rejected == rejected_logs else '다름(스택 한 건이 여러 줄이면 정상일 수 있음)'}
  가짜 FCM   : 호출 {calls} / 토큰 {tokens} / 실패 주입 {failed_tokens} / 피크 진행 중 호출 {p['peak_fake_fcm_inflight']}
  전송 타이머: {send} 평균 {send_mean_ms}ms (FCM 호출 구간만. 리스너의 DB 조회 3번은 제외)
  DB 풀      : 피크 사용 {p['peak_hikari_active']} / 피크 대기 {p['peak_hikari_pending']}  (Hikari 최대 10)
  앱 로그    : ERROR {len(error_lines)} {kinds or ''} / 예외 {exceptions or ''}
               토큰 노출 {token_leaks} / JWT 노출 {jwt_leaks}
  회계 대조  : 응원 201 {created} = 가짜 FCM 호출 {calls} + 거부 {rejected} ({explained})  → 설명되지 않는 건수 {created - explained}
  도달률     : 이벤트 {calls}/{created} = {report['reach']['pct']}%  (토큰 단위 성공률 {report['reach']['token_success_pct']}%)
  푸시 처리 완료까지 추가 대기 {report['drain_seconds']}s   (결과: scripts/perf/out/report-{args.name}.json)
""")


if __name__ == "__main__":
    main()
