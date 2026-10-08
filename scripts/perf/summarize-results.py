#!/usr/bin/env python3
"""scripts/perf/out/report-*.json 을 모아 표와 합격 기준 판정을 출력한다. (#214)"""
import glob
import json
import os
import statistics

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")


def load(prefix):
    reports = []
    for path in sorted(glob.glob(os.path.join(OUT, f"report-{prefix}*.json"))):
        name = os.path.basename(path)[len("report-"):-len(".json")]
        if name == prefix or name.startswith(prefix + "-r"):
            with open(path, encoding="utf-8") as f:
                reports.append(json.load(f))
    return reports


def row(r):
    a, w, l, p = r["api"], r["webhook_server"], r["app_log"], r["prometheus_peaks"]
    lat = a["latency_ms"]
    acc = r["accounting"]
    return (f"| {r['scenario']['name']} | {a['requests']} | {a['created_201']} | "
            f"{lat['med']} | {lat['p95']} | {lat['p99']} | {lat['max']} | "
            f"{w['received']} | {l['dropped_warnings']} | {l['send_failed_warnings']} | "
            f"{p['peak_active_threads']} / {p['peak_queued_tasks']} | {l['error_lines']} | {l['url_or_secret_leaks']} | {acc['unexplained']} |")


names = ["s0", "s1", "s2", "s1t", "s3", "s4"]
print("| 시나리오 | 요청 | 201 | p50(ms) | p95(ms) | p99(ms) | max(ms) | 웹훅 도착 | 폐기 | 전송실패 경고 | 피크 스레드/큐 | ERROR | URL·시크릿 노출 | 설명 안 되는 건 |")
print("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
for n in names:
    for r in load(n):
        print(row(r))

s0, s1 = load("s0"), load("s1")
p95_s0 = [r["api"]["latency_ms"]["p95"] for r in s0]
p95_s1 = [r["api"]["latency_ms"]["p95"] for r in s1]
print()
print(f"S0 p95 3회: {p95_s0}  평균 {statistics.mean(p95_s0):.2f}  범위 {min(p95_s0)}~{max(p95_s0)}")
print(f"S1 p95 3회: {p95_s1}  평균 {statistics.mean(p95_s1):.2f}  범위 {min(p95_s1)}~{max(p95_s1)}")
diff = statistics.mean(p95_s1) - statistics.mean(p95_s0)
print(f"S1 - S0 평균 p95 차이: {diff:+.2f}ms ({diff / statistics.mean(p95_s0) * 100:+.1f}%)")
print(f"S0 내 회차 간 변동(최대-최소): {max(p95_s0) - min(p95_s0):.2f}ms")

print()
for n in ("s2", "s1t", "s3", "s4"):
    for r in load(n):
        w, l, a = r["webhook_server"], r["app_log"], r["accounting"]
        print(f"[{r['scenario']['name']}] 도착 {w['received']} 응답코드 {w['by_status']} 끊김 {w['client_aborted']} "
              f"동시 처리 최대 {w['max_in_flight']} | 시크릿 ok/불일치/없음 {w['secret_ok']}/{w['secret_mismatch']}/{w['secret_missing']} "
              f"| 전송실패 코드 {l['send_failed_by_http']} | 대기 {r['drain_seconds']}s | 회계 {a['created_201']}={a['webhook_received']}+{a['dropped']}")

print()
for n in names:
    for r in load(n):
        m, l = r.get("dropped_metric"), r["app_log"]["dropped_warnings"]
        if m is None:
            continue
        print(f"[{r['scenario']['name']}] 폐기 메트릭 {m} / 로그 {l} -> {'일치' if m == l else '불일치'}")
