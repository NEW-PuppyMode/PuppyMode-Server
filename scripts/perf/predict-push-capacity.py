#!/usr/bin/env python3
"""푸시 풀(notificationExecutor: core 2 / max 8 / queue 100)의 수용량을 예측한다. (#216, [예측])

ThreadPoolExecutor 규칙: 실행 중 스레드 < core면 새 스레드, 아니면 큐(100)에 넣고, 큐가 가득 차야 max(8)까지 스레드를 늘리며,
그래도 모두 차 있으면 거부(AbortPolicy). 한 건의 처리 시간 L = FCM 지연 + 리스너의 DB 조회 3번(L_DB).

두 가지로 예측한다.
  1) 파 단위 산식: 수용 ≈ 초기 용량(8 + 100 = 108) + 스레드 수(8) x ⌊시간 ÷ L⌋       (거칠다)
  2) 이산 사건 시뮬레이션: 규칙을 그대로 따라 도착(일정 간격)과 완료를 재생한다

사용법: python3 scripts/perf/predict-push-capacity.py [DB 조회 시간 ms, 기본 5]
출력의 "수용"은 푸시 풀이 받아 준 건수, "거부"는 AbortPolicy로 버려진 건수다. 실측과 비교한다.
"""
import heapq
import math
import sys

CORE, MAX, QUEUE = 2, 8, 100
L_DB = (float(sys.argv[1]) if len(sys.argv) > 1 else 5.0) / 1000


def simulate(rate, duration, delay, slow_pct=0, slow_delay=0.0, initial_threads=0):
    """도착 k/rate (k < rate*duration). 완료 이벤트는 힙으로 처리. 스레드는 큐가 비면 놀고 다음 도착이 오면 재사용한다.

    initial_threads: 시작 시점에 이미 만들어져 있는 유휴 스레드 수. 0이면 "차가운 풀"(core 2에서 시작),
    8이면 직전 시나리오가 늘린 스레드가 keepAlive(60초) 안에 남아 있는 "데워진 풀"이다.
    유휴 스레드는 큐에서 바로 작업을 가져가므로, 큐가 차기를 기다리지 않고 처음부터 그 수만큼 일한다."""
    arrivals = [k / rate for k in range(int(rate * duration))]
    running = 0           # 작업 중인 스레드 수
    threads = initial_threads   # 만들어진 스레드 수 (유휴 포함, 60초 keepAlive 안이라 줄지 않는다고 본다)
    queue = 0
    done = []             # 완료 시각 힙
    started = 0           # 서비스를 시작한 호출 수 (느린 호출 판정은 호출 순번 기준)
    accepted = rejected = 0

    def service(now):
        nonlocal started
        slow = slow_pct > 0 and (started % 100) < slow_pct
        started += 1
        heapq.heappush(done, now + (slow_delay if slow else delay) + L_DB)

    for t in arrivals:
        while done and done[0] <= t:          # 먼저 끝난 작업 처리: 큐에서 다음 작업을 꺼내거나 스레드가 논다
            end = heapq.heappop(done)
            if queue > 0:
                queue -= 1
                service(end)
            else:
                running -= 1
        if running < threads:                 # 유휴 스레드가 있으면 그 스레드가 바로 가져간다
            running += 1; accepted += 1; service(t)
        elif threads < CORE:
            threads += 1; running += 1; accepted += 1; service(t)
        elif queue < QUEUE:
            queue += 1; accepted += 1
        elif threads < MAX:
            threads += 1; running += 1; accepted += 1; service(t)
        else:
            rejected += 1
    return accepted, rejected


def wave(rate, duration, delay):
    total = int(rate * duration)
    return min(total, 108 + MAX * int(duration // (delay + L_DB))) if delay > 0 else total


def row(label, rate, duration, delay, slow_pct=0, slow_delay=0.0):
    total = int(rate * duration)
    acc, rej = simulate(rate, duration, delay, slow_pct, slow_delay)
    w = wave(rate, duration, delay)
    print(f"| {label} | {rate}/s x {duration}s = {total} | {int(delay * 1000)}ms{f' (+{slow_pct}%={int(slow_delay * 1000)}ms)' if slow_pct else ''} | {w} | {acc} | {rej} | {acc / total * 100:.1f}% |")


print(f"푸시 풀 수용량 예측 [예측]  (core {CORE} / max {MAX} / queue {QUEUE}, 리스너 DB 조회 {L_DB * 1000:.0f}ms 가정)\n")
print("| 시나리오 | 부하 | FCM 지연 | 파 단위 산식 수용 | 시뮬레이션 수용 | 시뮬레이션 거부 | 예측 도달률 |")
print("|---|---|---|---|---|---|---|")
row("C0", 10, 30, 0.0)
for r in (10, 20, 30, 40):
    row(f"C1 {r}/s", r, 30, 0.3)
for r in (4, 8, 12):
    row(f"C2 {r}/s", r, 30, 1.0)
row("C3", 100, 20, 0.3)
for r in (10, 20):
    row(f"C4 {r}/s", r, 30, 0.3, 5, 4.0)
