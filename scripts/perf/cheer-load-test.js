// 응원 API(POST /friends/{userId}/cheers) 부하 테스트 (#216).
// 응원 저장 후 AFTER_COMMIT으로 나가는 푸시(가짜 FCM)가 응원 API 응답에 영향을 주는지, 푸시 풀이 얼마나 버티는지 본다.
//
// 사전 준비 (모두 로컬): scripts/perf/loadtest-data.sh setup  → tokens.json (유저 JWT, 응원 문구 ID, 오늘·어제 날짜)
// 시나리오 한 번 실행은 scripts/perf/run-push-scenario.py 가 위 단계와 가짜 FCM 설정, 회계 대조를 묶어서 처리한다.
//
// 직접 실행 (저장소 루트에서):
//   NAME=c0 RATE=10 DURATION=30s k6 run scripts/perf/cheer-load-test.js
//
// 환경 변수: NAME, RATE(초당 요청 수), DURATION, BASE_URL, PRE_VUS, MAX_VUS,
//   START_OFFSET(조합 시작 번호. 이전 실행이 쓴 조합 수만큼 지정해 UNIQUE 충돌(409)을 피한다)
//
// 응원은 (보내는 사람, 받는 사람, 대상 날짜)가 UNIQUE라 같은 조합을 다시 보내면 409다.
// 유저 N명 x 상대 N-1명 x 날짜 2개(오늘·어제)를 번호(iteration)로부터 결정적으로 계산해 중복 없이 소진한다.
// 번호가 1 늘 때마다 보내는 사람이 바뀌어 같은 유저에게 요청이 몰리지 않는다. 성공 응답은 201이다.
import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const NAME = __ENV.NAME || 'run';
const RATE = Number(__ENV.RATE || 10);
const DURATION = __ENV.DURATION || '30s';
const START_OFFSET = Number(__ENV.START_OFFSET || 0);

const fixture = JSON.parse(open('./out/tokens.json'));
const tokens = new SharedArray('tokens', () => JSON.parse(open('./out/tokens.json')).tokens);
const dates = [fixture.today, fixture.yesterday];
const templateId = fixture.cheerTemplateId;

const created = new Counter('cheer_created_201');
const conflict = new Counter('cheer_conflict_409');
const unauthorized = new Counter('cheer_unauthorized_401');
const otherStatus = new Counter('cheer_other_status');

// 기대 응답은 201만 성공으로 센다.
http.setResponseCallback(http.expectedStatuses(201));

export const options = {
    scenarios: {
        cheers: {
            executor: 'constant-arrival-rate', // 서버 응답 속도와 무관하게 일정한 도착률을 유지한다
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Number(__ENV.PRE_VUS || 20),
            maxVUs: Number(__ENV.MAX_VUS || 300),
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
    const n = tokens.length;
    const total = dates.length * n * (n - 1);
    const i = START_OFFSET + exec.scenario.iterationInTest;
    if (i >= total) {
        throw new Error(`응원 조합을 모두 사용했습니다 (${total}개). reset 후 다시 실행하세요.`);
    }

    const senderIdx = i % n;
    const rest = Math.floor(i / n);
    const offset = (rest % (n - 1)) + 1; // 1..n-1 이라 보내는 사람 != 받는 사람
    const receiverIdx = (senderIdx + offset) % n;
    const date = dates[Math.floor(rest / (n - 1)) % dates.length];

    const res = http.post(
        `${BASE_URL}/friends/${tokens[receiverIdx].userId}/cheers`,
        JSON.stringify({ templateId: templateId, targetDate: date }),
        {
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${tokens[senderIdx].token}`,
            },
            tags: { name: 'POST /friends/{userId}/cheers' },
        },
    );

    if (res.status === 201) created.add(1);
    else if (res.status === 409) conflict.add(1);
    else if (res.status === 401) unauthorized.add(1);
    else otherStatus.add(1);

    check(res, { '201 응원': (r) => r.status === 201 });
}

function count(data, metric) {
    const m = data.metrics[metric];
    return m && m.values && m.values.count !== undefined ? m.values.count : 0;
}

export function handleSummary(data) {
    const d = data.metrics.http_req_duration.values;
    const ms = (v) => (v === undefined ? null : Math.round(v * 100) / 100);
    const result = {
        name: NAME,
        rate_per_sec: RATE,
        duration: DURATION,
        requests: count(data, 'http_reqs'),
        created_201: count(data, 'cheer_created_201'),
        conflict_409: count(data, 'cheer_conflict_409'),
        unauthorized_401: count(data, 'cheer_unauthorized_401'),
        other_status: count(data, 'cheer_other_status'),
        dropped_iterations: count(data, 'dropped_iterations'),
        latency_ms: {
            avg: ms(d.avg), min: ms(d.min), med: ms(d.med),
            p90: ms(d['p(90)']), p95: ms(d['p(95)']), p99: ms(d['p(99)']), max: ms(d.max),
        },
    };
    const line = `[k6:${NAME}] 요청 ${result.requests} | 201=${result.created_201} 409=${result.conflict_409} `
        + `401=${result.unauthorized_401} 기타=${result.other_status} | dropped=${result.dropped_iterations} | `
        + `p50=${result.latency_ms.med}ms p95=${result.latency_ms.p95}ms p99=${result.latency_ms.p99}ms max=${result.latency_ms.max}ms\n`;
    return {
        [`scripts/perf/out/result-${NAME}.json`]: JSON.stringify(result, null, 2),
        stdout: line,
    };
}
