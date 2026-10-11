#!/usr/bin/env python3
"""응원 푸시 경로 측정 결과(report-c*.json, report-f1*.json)를 모아 표와 예측 대조를 출력한다. (#216)

예측은 scripts/perf/predict-push-capacity.py 의 이산 사건 시뮬레이션과 파 단위 산식을 그대로 쓴다.
도달률(앱 관점) = 가짜 FCM 호출 / 응원 201.  거부 = 푸시 풀이 가득 차 AbortPolicy로 버린 건수.
"""
import glob
import importlib.util
import io
import contextlib
import json
import os
import re
import statistics

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

spec = importlib.util.spec_from_file_location("predict", os.path.join(HERE, "predict-push-capacity.py"))
predict = importlib.util.module_from_spec(spec)
with contextlib.redirect_stdout(io.StringIO()):
    spec.loader.exec_module(predict)


def groups():
    result = {}
    for path in sorted(glob.glob(os.path.join(OUT, "report-*.json"))):
        name = os.path.basename(path)[len("report-"):-len(".json")]
        m = re.match(r"^((?:cold-)?(?:c[0-4]|f1)(?:-\d+)?)-r(\d+)$", name)
        if m:
            with open(path, encoding="utf-8") as f:
                result.setdefault(m.group(1), []).append(json.load(f))
    def order(item):
        key = item[0]
        cold = key.startswith("cold-")
        base = key[5:] if cold else key
        num = int(base.split("-")[1]) if "-" in base else 0
        return (cold, base[:2], num)
    return dict(sorted(result.items(), key=order))


def fmt(values, digits=0):
    values = [v for v in values if v is not None]
    if not values:
        return "-"
    mean = statistics.mean(values)
    if len(values) == 1 or min(values) == max(values):
        return f"{mean:.{digits}f}"
    return f"{mean:.{digits}f} ({min(values):.{digits}f}~{max(values):.{digits}f})"


def predicted(r, initial_threads=0):
    s = r["scenario"]
    duration = predict_seconds(s["duration"])
    acc, rej = predict.simulate(s["rate_per_sec"], duration, s["fcm_delay_ms"] / 1000,
                                s["slow_pct"], s["slow_delay_ms"] / 1000, initial_threads)
    return acc, rej, predict.wave(s["rate_per_sec"], duration, s["fcm_delay_ms"] / 1000)


def predict_seconds(text):
    return float(text[:-1]) * (60 if text.endswith("m") else 1) if text[-1] in "sm" else float(text)


data = groups()
if not data:
    raise SystemExit("report-c*.json 이 없습니다. scripts/perf/run-push-matrix.sh 를 먼저 실행하세요.")

print("## 결과 (회차 평균과 (최소~최대))\n")
print("| 시나리오 | 부하 | FCM 지연 | 회차 | 응원 201 | FCM 호출(수용) | 거부 | 도달률(%) | 응원 API p50/p95(ms) | 피크 스레드/큐 | 피크 DB 사용/대기 | ERROR | 토큰·JWT 노출 | 설명 안 되는 건 |")
print("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
total = {"runs": 0, "cheers": 0, "unexplained": 0, "errors": 0, "leaks": 0, "non201": 0}
for key, rs in data.items():
    s = rs[0]["scenario"]
    dist = f" +{s['slow_pct']}%={s['slow_delay_ms']}ms" if s["slow_pct"] else ""
    fail = f" 실패{s['failure_pct']}%" if s["failure_pct"] else ""
    print(f"| {key} | {s['rate_per_sec']}/s x {s['duration']} | {s['fcm_delay_ms']}ms{dist}{fail} | {len(rs)} | "
          f"{fmt([r['api']['created_201'] for r in rs])} | {fmt([r['fake_fcm']['calls'] for r in rs])} | "
          f"{fmt([r['push_pool']['rejected_metric'] for r in rs])} | {fmt([r['reach']['pct'] for r in rs], 1)} | "
          f"{fmt([r['api']['latency_ms']['med'] for r in rs], 1)} / {fmt([r['api']['latency_ms']['p95'] for r in rs], 1)} | "
          f"{fmt([r['push_pool']['peaks']['peak_active_threads'] for r in rs])} / {fmt([r['push_pool']['peaks']['peak_queued_tasks'] for r in rs])} | "
          f"{fmt([r['push_pool']['peaks']['peak_hikari_active'] for r in rs])} / {fmt([r['push_pool']['peaks']['peak_hikari_pending'] for r in rs])} | "
          f"{sum(r['app_log']['error_lines'] for r in rs)} | {sum(r['app_log']['fcm_token_leaks'] + r['app_log']['jwt_leaks'] for r in rs)} | "
          f"{sum(r['accounting']['unexplained'] for r in rs)} |")
    for r in rs:
        total["runs"] += 1
        total["cheers"] += r["api"]["created_201"]
        total["unexplained"] += r["accounting"]["unexplained"]
        total["errors"] += r["app_log"]["error_lines"]
        total["leaks"] += r["app_log"]["fcm_token_leaks"] + r["app_log"]["jwt_leaks"]
        total["non201"] += r["api"]["requests"] - r["api"]["created_201"]

print(f"\n전체 {total['runs']}개 실행: 응원 요청 {total['cheers'] + total['non201']}건 중 201 {total['cheers']}건 (201이 아닌 응답 {total['non201']}건), "
      f"설명되지 않는 건수 {total['unexplained']}, 토큰·JWT 로그 노출 {total['leaks']}, ERROR 로그 {total['errors']}줄")

print("\n## 예측 대 실측 (수용 = 푸시 풀이 받아 준 건수)\n")
print("차가운 풀 = 시작 시 core 2개만 있는 풀, 데워진 풀 = 직전 시나리오가 늘린 유휴 스레드 8개(keepAlive 60초)가 남아 있는 풀.")
print("`시작 풀`은 실행이 기록한 시작 시 풀 스레드 수다. 기록이 없는 실행(첫 매트릭스)은 -로 표시한다.\n")
print("| 시나리오 | 시작 풀 | 파 단위 산식 | 시뮬레이션(차가운) | 시뮬레이션(데워진) | 실측 수용 | 차가운 대비 | 데워진 대비 | 시뮬(차가운) 거부 | 시뮬(데워진) 거부 | 실측 거부 |")
print("|---|---|---|---|---|---|---|---|---|---|---|")
for key, rs in data.items():
    if key.startswith("f1"):
        continue
    cold = [predicted(r, 0) for r in rs]
    warm = [predicted(r, 8) for r in rs]
    acc = [r["fake_fcm"]["calls"] for r in rs]
    rej = [r["push_pool"]["rejected_metric"] for r in rs]
    starts = [r["scenario"].get("pool_size_at_start") for r in rs]
    start_txt = fmt(starts) if any(v is not None for v in starts) else "-"

    def vs(sim):
        base = statistics.mean(p[0] for p in sim)
        return f"{(statistics.mean(acc) - base) / base * 100:+.1f}%" if base else "-"
    print(f"| {key} | {start_txt} | {fmt([p[2] for p in cold])} | {fmt([p[0] for p in cold])} | {fmt([p[0] for p in warm])} | {fmt(acc)} | "
          f"{vs(cold)} | {vs(warm)} | {fmt([p[1] for p in cold])} | {fmt([p[1] for p in warm])} | {fmt(rej)} |")

print("\n## 포화(거부) 시 실패 방식 — 거부 건수와 ERROR 로그")
for key, rs in data.items():
    rejected = [r["push_pool"]["rejected_metric"] for r in rs]
    if max(rejected) == 0:
        continue
    r = rs[0]
    print(f"- {key}: 거부 {fmt(rejected)}건 → ERROR {fmt([x['app_log']['error_lines'] for x in rs])}줄 "
          f"(로거 {r['app_log']['error_by_logger']}, 예외 {r['app_log']['exceptions']}), 응원 API 201 비율 "
          f"{fmt([x['api']['created_201'] / x['api']['requests'] * 100 for x in rs], 1)}%")

print("\n## 전송 타이머 (notification_send_seconds{name=\"push\"}) — FCM 호출 구간만")
for key, rs in data.items():
    means = [r["push_pool"]["send_mean_ms"] for r in rs]
    results = {}
    for r in rs:
        for res, n in r["push_pool"]["send_timer"].items():
            results[res] = results.get(res, 0) + n
    print(f"- {key}: 평균 {fmt(means, 1)}ms, 결과별 건수(합) {results}")
