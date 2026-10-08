# 로컬 부하 테스트용 가짜 웹훅 서버 (#214). 실제 n8n/Slack 대신 신고·스케줄러 알림을 받는다.
#
# - 127.0.0.1에만 바인딩한다. 외부로 아무것도 보내지 않는다.
# - 지연(delay_ms)과 응답 코드(status)를 실행 중에 바꿀 수 있어 "느린 웹훅", "403/500"을 재현한다.
# - X-Webhook-Secret 헤더를 검증한다(n8n Header Auth 모방). 시크릿은 더미 값이다.
# - 본문과 시크릿 값은 로그에 남기지 않는다.
#
# 사용법:
#   python3 scripts/perf/fake-webhook.py                      # 기본: 127.0.0.1:9999, 시크릿 loadtest-dummy
#   FAKE_WEBHOOK_PORT=9999 FAKE_WEBHOOK_SECRET=loadtest-dummy python3 scripts/perf/fake-webhook.py
#
# 제어:
#   POST /config  {"delay_ms": 4000, "status": 200, "expected_secret": "loadtest-dummy"}
#                 expected_secret을 ""로 두면 시크릿 검증을 끈다
#   POST /reset   집계 초기화
#   GET  /stats   수신 집계
#   POST /hook    웹훅 수신 (앱이 호출하는 주소)
import json
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = "127.0.0.1"
PORT = int(os.environ.get("FAKE_WEBHOOK_PORT", "9999"))

lock = threading.Lock()
config = {
    "delay_ms": 0,
    "status": 200,
    "expected_secret": os.environ.get("FAKE_WEBHOOK_SECRET", "loadtest-dummy"),
}


def fresh_stats():
    return {
        "received": 0,              # 도착한 요청 수 (지연 전에 집계)
        "by_status": {},            # 실제로 돌려준 응답 코드별 건수
        "secret_ok": 0,
        "secret_missing": 0,
        "secret_mismatch": 0,
        "with_structured_fields": 0,  # occurredAt 등 구조화 필드가 함께 온 건수
        "text_only": 0,
        "client_aborted": 0,        # 응답을 쓰기 전에 클라이언트가 끊은 건수 (앱 타임아웃)
        "in_flight": 0,
        "max_in_flight": 0,
        "first_at": None,
        "last_at": None,
    }


stats = fresh_stats()


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):  # 접근 로그를 남기지 않는다
        pass

    def _json(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        try:
            return json.loads(raw) if raw else {}
        except ValueError:
            return None

    def do_GET(self):
        if self.path == "/stats":
            with lock:
                self._json(200, {"config": {k: v for k, v in config.items() if k != "expected_secret"},
                                 "secret_check": bool(config["expected_secret"]), "stats": stats})
        else:
            self._json(404, {"error": "not found"})

    def do_POST(self):
        global stats
        if self.path == "/config":
            body = self._read_json() or {}
            with lock:
                for key in ("delay_ms", "status", "expected_secret"):
                    if key in body:
                        config[key] = body[key]
            self._json(200, {"ok": True})
        elif self.path == "/reset":
            self._read_json()
            with lock:
                stats = fresh_stats()
            self._json(200, {"ok": True})
        elif self.path == "/hook":
            self._hook()
        else:
            self._json(404, {"error": "not found"})

    def _hook(self):
        body = self._read_json()
        secret = self.headers.get("X-Webhook-Secret")
        with lock:
            expected = config["expected_secret"]
            delay = config["delay_ms"] / 1000.0
            configured_status = int(config["status"])
            now = time.time()
            stats["received"] += 1
            stats["first_at"] = stats["first_at"] or now
            stats["last_at"] = now
            stats["in_flight"] += 1
            stats["max_in_flight"] = max(stats["max_in_flight"], stats["in_flight"])
            if expected:
                if secret is None:
                    stats["secret_missing"] += 1
                elif secret != expected:
                    stats["secret_mismatch"] += 1
                else:
                    stats["secret_ok"] += 1
            if isinstance(body, dict) and "occurredAt" in body and ("complaintId" in body or "job" in body):
                stats["with_structured_fields"] += 1
            else:
                stats["text_only"] += 1
            secret_valid = (not expected) or secret == expected

        if delay > 0:
            time.sleep(delay)

        status = configured_status if secret_valid else 403  # 시크릿이 틀리면 n8n처럼 403
        try:
            self._json(status, {"message": "Workflow was started" if status < 400 else "error"})
            aborted = False
        except (BrokenPipeError, ConnectionResetError):
            aborted = True

        with lock:
            stats["in_flight"] -= 1
            if aborted:
                stats["client_aborted"] += 1
            else:
                key = str(status)
                stats["by_status"][key] = stats["by_status"].get(key, 0) + 1


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    server.daemon_threads = True
    print(f"가짜 웹훅 서버 시작: http://{HOST}:{PORT}/hook  (시크릿 검증 {'on' if config['expected_secret'] else 'off'})", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
