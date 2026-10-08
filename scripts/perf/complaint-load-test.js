// 신고 API(POST /complaints) 부하 테스트 (#214).
// 신고 접수 후 비동기로 나가는 알림(가짜 웹훅)이 신고 API 응답에 영향을 주는지 본다.
//
// 사전 준비 (모두 로컬):
//   scripts/perf/loadtest-data.sh setup            # 테스트 유저 + scripts/perf/out/tokens.json
//   python3 scripts/perf/fake-webhook.py            # 가짜 웹훅 서버(:9999)
//   scripts/perf/run-app-loadtest.sh start          # 앱을 부하 테스트용 옵션으로 실행
// 시나리오 한 번 실행은 scripts/perf/run-scenario.py 가 위 단계를 묶어서 처리한다.
//
// 직접 실행 (저장소 루트에서):
//   NAME=s0 RATE=10 DURATION=30s k6 run scripts/perf/complaint-load-test.js
//
// 환경 변수:
//   NAME(결과 파일 이름), RATE(초당 요청 수), DURATION, BASE_URL, PRE_VUS, MAX_VUS,
//   START_OFFSET(조합 시작 번호. 재실행 시 reset 없이 이어서 쓰려면 이전 요청 수만큼 지정)
//
// 신고는 (신고자, 대상, 사유)가 UNIQUE라 같은 조합을 다시 보내면 409다.
// 유저 N명 x 대상 N-1명 x 사유 4개를 번호(iteration)로부터 결정적으로 계산해 중복 없이 소진한다.
// 성공 응답은 201이다.
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

// OTHER는 직접 입력(detail)이 필요해서 제외한다.
const REASONS = ['INAPPROPRIATE_NICKNAME', 'INAPPROPRIATE_PUPPY_NAME', 'ABUSE_HARASSMENT', 'SPAM_AD'];

const tokens = new SharedArray('tokens', () => JSON.parse(open('./out/tokens.json')).tokens);

const created = new Counter('complaint_created_201');
const conflict = new Counter('complaint_conflict_409');
const unauthorized = new Counter('complaint_unauthorized_401');
const otherStatus = new Counter('complaint_other_status');

// 기대 응답은 201만 성공으로 센다. (http_req_failed에 409/401/5xx가 실패로 잡힌다)
http.setResponseCallback(http.expectedStatuses(201));

export const options = {
    scenarios: {
        complaints: {
            executor: 'constant-arrival-rate', // 서버 응답 속도와 무관하게 일정한 도착률을 유지한다
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Number(__ENV.PRE_VUS || 20),
            maxVUs: Number(__ENV.MAX_VUS || 200),
        },
    },
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
    const n = tokens.length;
    const total = REASONS.length * n * (n - 1);
    const i = START_OFFSET + exec.scenario.iterationInTest;
    if (i >= total) {
        throw new Error(`신고 조합을 모두 사용했습니다 (${total}개). 유저 수를 늘리거나 reset 후 다시 실행하세요.`);
    }

    const reasonIdx = i % REASONS.length;
    const rest = Math.floor(i / REASONS.length);
    const reporterIdx = rest % n;
    const offset = (Math.floor(rest / n) % (n - 1)) + 1; // 1..n-1 이라 신고자 != 대상
    const targetIdx = (reporterIdx + offset) % n;

    const res = http.post(
        `${BASE_URL}/complaints`,
        JSON.stringify({ userId: tokens[targetIdx].userId, reason: REASONS[reasonIdx] }),
        {
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${tokens[reporterIdx].token}`,
            },
            tags: { name: 'POST /complaints' },
        },
    );

    if (res.status === 201) created.add(1);
    else if (res.status === 409) conflict.add(1);
    else if (res.status === 401) unauthorized.add(1);
    else otherStatus.add(1);

    check(res, { '201 접수': (r) => r.status === 201 });
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
        created_201: count(data, 'complaint_created_201'),
        conflict_409: count(data, 'complaint_conflict_409'),
        unauthorized_401: count(data, 'complaint_unauthorized_401'),
        other_status: count(data, 'complaint_other_status'),
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
