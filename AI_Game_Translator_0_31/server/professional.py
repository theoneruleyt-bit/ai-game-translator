"""Private pilot translation/TTS gateway. Bind locally, terminate TLS at the proxy.
Not a public billing/auth system. No provider calls occur during unit tests.
"""
import base64
import hmac
import json
import math
import os
import re
import sqlite3
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler
from app import BoundedServer

class PilotError(Exception):
    def __init__(self, status, code):
        self.status, self.code = status, code

class Budget:
    """Persistent global daily character budget; failures still consume reservations."""
    def __init__(self, path, limit=20000):
        self.path, self.limit = path, limit
        with sqlite3.connect(path) as db:
            db.execute('CREATE TABLE IF NOT EXISTS quota(day TEXT PRIMARY KEY, used INTEGER NOT NULL)')
    def reserve(self, count):
        day = datetime.now(timezone.utc).date().isoformat()
        with sqlite3.connect(self.path, timeout=5) as db:
            db.execute('BEGIN IMMEDIATE')
            row = db.execute('SELECT used FROM quota WHERE day=?', (day,)).fetchone()
            used = row[0] if row else 0
            if used + count > self.limit:
                raise PilotError(429, 'daily_budget_exhausted')
            db.execute('INSERT INTO quota VALUES(?,?) ON CONFLICT(day) DO UPDATE SET used=excluded.used', (day, used+count))

MOODS = {'natural': 'Use natural conversational delivery. Express emotion only when clearly implied by the text; otherwise remain neutral.',
         'calm': 'Use calm, reassuring delivery with restrained emotion.',
         'energetic': 'Use lively, excited delivery without shouting.',
         'tense': 'Use tense, urgent delivery without adding screams or sound effects.',
         'curious': 'Use curious, questioning delivery.'}

def validate(path, body):
    if not isinstance(body, dict):
        raise PilotError(400, 'invalid_body')
    text = body.get('text')
    if not isinstance(text, str) or not text.strip() or len(text) > 3000 or len(text.encode('utf-8')) > 4000:
        raise PilotError(400, 'invalid_text')
    if path == '/v1/translate':
        if (body.get('source'), body.get('target')) not in {('en', 'tr'), ('tr', 'en')}:
            raise PilotError(400, 'unsupported_direction')
    elif path == '/v1/speak':
        if body.get('language') not in {'tr', 'en'} or body.get('style') not in {'female', 'male'} or body.get('mood', 'natural') not in MOODS:
            raise PilotError(400, 'unsupported_voice')
        rate = body.get('rate', 1)
        if isinstance(rate, bool) or not isinstance(rate, (float, int)) or not math.isfinite(rate) or not .7 <= rate <= 1.5:
            raise PilotError(400, 'invalid_rate')
    else:
        raise PilotError(404, 'not_found')
    return text

class GoogleProvider:
    def __init__(self, project, tts_model='gemini-2.5-flash-tts'):
        import google.auth
        from google.auth.transport.requests import AuthorizedSession
        credentials, _ = google.auth.default(scopes=['https://www.googleapis.com/auth/cloud-platform'])
        self.session = AuthorizedSession(credentials)
        self.project, self.tts_model = project, tts_model
        self.lock = threading.Lock()
    def post(self, url, body):
        with self.lock:
            response = self.session.post(url, json=body, headers={'x-goog-user-project': self.project}, timeout=(5, 10), allow_redirects=False)
        if response.status_code != 200 or len(response.content) > 4000000:
            raise PilotError(503, 'provider_unavailable')
        return response.json()
    def translate(self, body):
        parent = f'projects/{self.project}/locations/global'
        result = self.post(f'https://translation.googleapis.com/v3/{parent}:translateText', {
            'contents': [body['text']], 'mimeType': 'text/plain',
            'sourceLanguageCode': body['source'], 'targetLanguageCode': body['target'],
            'model': parent+'/models/general/translation-llm'})
        text = result['translations'][0]['translatedText']
        if not isinstance(text, str) or not text.strip() or len(text) > 12000:
            raise PilotError(503, 'invalid_translation')
        return {'text': text}
    def speak(self, body):
        language = 'tr-TR' if body['language'] == 'tr' else 'en-US'
        female = body['style'] == 'female'
        rate = body.get('rate', 1)
        if self.tts_model == 'wavenet':
            voice = ('tr-TR-Wavenet-A' if female else 'tr-TR-Wavenet-B') if language == 'tr-TR' else ('en-US-Wavenet-F' if female else 'en-US-Wavenet-D')
            payload = {'input': {'text': body['text']}, 'voice': {'languageCode': language, 'name': voice},
                       'audioConfig': {'audioEncoding': 'MP3', 'speakingRate': rate}}
        else:
            pace = 'slow' if rate < .95 else 'normal' if rate < 1.1 else 'brisk' if rate < 1.35 else 'very brisk but intelligible'
            prompt = ('Read the supplied text verbatim in its original language, as a natural game dialogue. '
                      'Do not add words, introductions, music, laughter or sound effects. '
                      +MOODS[body.get('mood', 'natural')]+f' Use {pace} pacing.')
            payload = {'input': {'text': body['text'], 'prompt': prompt},
                       'voice': {'languageCode': language, 'name': 'Kore' if female else 'Puck', 'modelName': self.tts_model},
                       'audioConfig': {'audioEncoding': 'MP3'}}
        result = self.post('https://texttospeech.googleapis.com/v1/text:synthesize', payload)
        audio = result.get('audioContent', '')
        decoded = base64.b64decode(audio, validate=True)
        if not decoded or len(decoded) > 3000000:
            raise PilotError(503, 'invalid_audio')
        return {'audio': audio, 'mimeType': 'audio/mpeg'}

def make_handler(token, budget, provider):
    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup(); self.connection.settimeout(15)
        def log_message(self, *_):
            pass
        def reply(self, status, data):
            payload = json.dumps(data).encode('utf-8')
            self.send_response(status)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Content-Length', str(len(payload)))
            self.end_headers(); self.wfile.write(payload)
        def do_POST(self):
            try:
                supplied = self.headers.get('Authorization', '').encode('utf-8')
                if not hmac.compare_digest(supplied, ('Bearer '+token).encode('utf-8')):
                    raise PilotError(401, 'unauthorized')
                size = int(self.headers.get('Content-Length', '0'))
                if size <= 0 or size > 20000 or self.headers.get('Transfer-Encoding'):
                    raise PilotError(413, 'body_too_large')
                body = json.loads(self.rfile.read(size))
                text = validate(self.path, body)
                budget.reserve(len(text))
                result = provider.translate(body) if self.path == '/v1/translate' else provider.speak(body)
                self.reply(200, result)
            except PilotError as error:
                self.reply(error.status, {'error': error.code})
            except (ValueError, TypeError, UnicodeError):
                self.reply(400, {'error': 'invalid_request'})
            except Exception:
                self.reply(503, {'error': 'service_unavailable'})
    return Handler

if __name__ == '__main__':
    token = os.environ.get('PILOT_TOKEN', '')
    project = os.environ.get('GOOGLE_CLOUD_PROJECT', '')
    if len(token) < 32 or not re.fullmatch(r'[a-z][a-z0-9-]{4,62}', project):
        raise SystemExit('Set PILOT_TOKEN (32+ characters) and GOOGLE_CLOUD_PROJECT')
    model = os.environ.get('PILOT_TTS_MODEL', 'gemini-2.5-flash-tts')
    if model not in {'wavenet', 'gemini-2.5-flash-tts', 'gemini-2.5-pro-tts', 'gemini-3.1-flash-tts-preview'}:
        raise SystemExit('Unsupported PILOT_TTS_MODEL')
    budget = Budget(os.environ.get('PILOT_BUDGET_DB', 'pilot-budget.sqlite'), int(os.environ.get('PILOT_DAILY_CHAR_LIMIT', '20000')))
    server = BoundedServer(('127.0.0.1', int(os.environ.get('PILOT_PORT', '8081'))), make_handler(token, budget, GoogleProvider(project, model)))
    server.serve_forever()
