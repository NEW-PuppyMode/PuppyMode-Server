# Prometheus가 /actuator/prometheus를 수집할 때 쓸 로컬 전용 JWT를 만든다. (#214)
#
# - 로컬 application.yml의 auth.jwt.secret으로 앱과 같은 방식(JJWT: 키 길이에 따라 HS256/384/512)으로 서명한다.
# - 토큰은 scripts/perf/out/prometheus-token 에만 쓰고 화면에 출력하지 않는다. (out/ 은 .gitignore 대상)
# - 시크릿 값은 어디에도 출력하지 않는다.
#
# 사용법: python3 scripts/perf/make-prometheus-token.py [유효시간(시간), 기본 24] [userId, 기본 1]
import base64, hashlib, hmac, json, os, re, sys, time

root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
hours = float(sys.argv[1]) if len(sys.argv) > 1 else 24
user_id = int(sys.argv[2]) if len(sys.argv) > 2 else 1

yml = open(os.path.join(root, 'src/main/resources/application.yml'), encoding='utf-8').read()
m = re.search(r'jwt:\s*\n\s*secret:\s*(\S+)', yml)
if not m:
    raise SystemExit('application.yml에서 auth.jwt.secret을 찾지 못했습니다.')
secret = m.group(1)
key = base64.urlsafe_b64decode(secret + '=' * (-len(secret) % 4))
if len(key) < 32:
    raise SystemExit('JWT 키가 너무 짧습니다.')
alg, digest = {32: ('HS256', hashlib.sha256), 48: ('HS384', hashlib.sha384)}.get(len(key), ('HS512', hashlib.sha512))


def b64(x: bytes) -> bytes:
    return base64.urlsafe_b64encode(x).rstrip(b'=')


now = int(time.time())
head = b64(json.dumps({'typ': 'JWT', 'alg': alg}, separators=(',', ':')).encode())
body = b64(json.dumps({'iat': now, 'exp': now + int(hours * 3600), 'userId': user_id}, separators=(',', ':')).encode())
sig = b64(hmac.new(key, head + b'.' + body, digest).digest())

out_dir = os.path.join(root, 'scripts/perf/out')
os.makedirs(out_dir, exist_ok=True)
path = os.path.join(out_dir, 'prometheus-token')
with open(path, 'wb') as f:
    f.write(head + b'.' + body + b'.' + sig)
os.chmod(path, 0o644)  # 컨테이너(nobody)가 읽을 수 있어야 한다. 로컬 전용 토큰이고 만료가 짧다.
print(f'토큰 저장: scripts/perf/out/prometheus-token (userId={user_id}, {hours}시간 유효, {alg})')
