"""Run behind an HTTPS reverse proxy. ADC credentials stay on the server."""
import json
import os
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import quote
from core import ApiError, Ledger, Service, account_hash, PACKAGE, SUBS

class GooglePlay:
    def __init__(self):
        import google.auth
        from google.auth.transport.requests import AuthorizedSession
        credentials, _ = google.auth.default(scopes=['https://www.googleapis.com/auth/androidpublisher'])
        self.session = AuthorizedSession(credentials)
    def get(self, product, token):
        prefix = f'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}/purchases/'
        path = f'subscriptionsv2/tokens/{quote(token, safe="")}' if product == SUBS else f'products/{product}/tokens/{quote(token, safe="")}'
        response = self.session.get(prefix+path, timeout=5)
        if response.status_code in {404, 410}:
            return None
        if response.status_code != 200:
            raise ApiError(503, 'play_verification_unavailable')
        return response.json()
    def acknowledge(self, product, token):
        kind = 'subscriptions' if product == SUBS else 'products'
        url = f'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}/purchases/{kind}/{product}/tokens/{quote(token, safe="")}:acknowledge'
        response = self.session.post(url, json={}, timeout=5)
        if response.status_code not in {200, 204}:
            raise ApiError(503, 'acknowledgement_failed')

class BoundedServer(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, *args, **kwargs):
        self.slots = threading.BoundedSemaphore(32)
        super().__init__(*args, **kwargs)
    def process_request(self, request, address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, address)
        except Exception:
            self.slots.release()
            raise
    def process_request_thread(self, request, address):
        try:
            super().process_request_thread(request, address)
        finally:
            self.slots.release()

def make_handler(ledger, client_id):
    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup()
            self.connection.settimeout(15)
        def log_message(self, *_):
            pass  # No body, token, identity or authorization data in request logs.
        def reply(self, status, data):
            body = json.dumps(data).encode('utf-8')
            self.send_response(status)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Content-Length', str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        def do_GET(self):
            self.reply(200 if self.path == '/health' else 404, {'ok': self.path == '/health'})
        def do_POST(self):
            try:
                if self.path not in {'/v1/state', '/v1/usage'}:
                    raise ApiError(404, 'not_found')
                length = int(self.headers.get('Content-Length', '0'))
                if not 0 < length <= 65536 or self.headers.get('Transfer-Encoding'):
                    raise ApiError(413, 'invalid_body_size')
                auth = self.headers.get('Authorization', '')
                if not auth.startswith('Bearer ') or len(auth) > 8192:
                    raise ApiError(401, 'sign_in_required')
                body = json.loads(self.rfile.read(length))
                if not isinstance(body, dict):
                    raise ApiError(400, 'invalid_body')
                from google.oauth2 import id_token
                from google.auth.transport.requests import Request
                class IdentityRequest(Request):
                    def __call__(self, *args, **kwargs):
                        kwargs['timeout'] = 5
                        return super().__call__(*args, **kwargs)
                try:
                    claims = id_token.verify_oauth2_token(auth[7:], IdentityRequest(), client_id)
                    subject = claims.get('sub')
                    if not isinstance(subject, str) or not subject:
                        raise ValueError('missing_subject')
                except ValueError:
                    raise ApiError(401, 'invalid_identity') from None
                account = account_hash(subject)
                if self.path == '/v1/state':
                    self.reply(200, Service(ledger, GooglePlay()).state(account, body.get('purchases', [])))
                else:
                    used = ledger.consume(account, body.get('session'), body.get('elapsedMillis'))
                    self.reply(200, {'trialUsedMillis': used})
            except ApiError as error:
                self.reply(error.status, {'error': error.code})
            except (ValueError, UnicodeDecodeError):
                self.reply(400, {'error': 'invalid_request'})
            except Exception:
                self.reply(503, {'error': 'service_unavailable'})
    return Handler

if __name__ == '__main__':
    client_id = os.environ.get('GOOGLE_WEB_CLIENT_ID', '')
    if not client_id.endswith('.apps.googleusercontent.com'):
        raise SystemExit('Configure GOOGLE_WEB_CLIENT_ID before starting.')
    path = os.environ.get('LEDGER_PATH', './data/trial.sqlite3')
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    server = BoundedServer(('127.0.0.1', int(os.environ.get('PORT', '8080'))), make_handler(Ledger(path), client_id))
    server.serve_forever()
