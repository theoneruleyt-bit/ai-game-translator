import base64
import tempfile
import unittest
from pathlib import Path
from professional import Budget, PilotError, validate, GoogleProvider

class ProfessionalTest(unittest.TestCase):
    def test_invalid_language_and_mood(self):
        for path,body in [('/v1/translate',dict(text='Hi',source='en',target='fr')),('/v1/speak',dict(text='Hi',language='en',style='male',mood='unknown'))]:
            with self.assertRaises(PilotError): validate(path,body)
    def test_bytes_and_invalid_rate(self):
        with self.assertRaises(PilotError): validate('/v1/translate',dict(text='ı'*2500,source='en',target='tr'))
        for rate in [float('nan'),float('inf'),True,2]:
            with self.assertRaises(PilotError): validate('/v1/speak',dict(text='Hi',language='en',style='male',rate=rate))
    def test_persistent_budget(self):
        with tempfile.TemporaryDirectory() as d:
            p=str(Path(d)/'budget.db');Budget(p,10).reserve(6)
            with self.assertRaises(PilotError): Budget(p,10).reserve(5)
            Budget(p,10).reserve(4)
            with self.assertRaises(PilotError): Budget(p,10).reserve(1)
    def test_llm_translation_request(self):
        p=GoogleProvider.__new__(GoogleProvider);p.project='test-project';requests=[]
        p.post=lambda url,body: (requests.append(body) or {'translations':[{'translatedText':'Merhaba'}]})
        self.assertEqual(p.translate(dict(text='Hello',source='en',target='tr')),{'text':'Merhaba'})
        self.assertTrue(requests[0]['model'].endswith('/translation-llm'))
        self.assertEqual(requests[0]['mimeType'],'text/plain')
    def test_expression_separate_from_text(self):
        p=GoogleProvider.__new__(GoogleProvider);p.tts_model='gemini-2.5-flash-tts';requests=[]
        p.post=lambda url,body: (requests.append(body) or {'audioContent':base64.b64encode(b'audio').decode()})
        p.speak(dict(text='Dur!',language='tr',style='male',mood='tense',rate=1.4))
        self.assertEqual(requests[0]['input']['text'],'Dur!')
        self.assertIn('urgent',requests[0]['input']['prompt'])
        self.assertEqual(requests[0]['voice']['name'],'Puck')
    def test_wavenet_has_no_fake_expression(self):
        p=GoogleProvider.__new__(GoogleProvider);p.tts_model='wavenet';requests=[]
        p.post=lambda url,body: (requests.append(body) or {'audioContent':base64.b64encode(b'audio').decode()})
        p.speak(dict(text='Hi',language='en',style='female',mood='tense',rate=1.2))
        self.assertNotIn('prompt',requests[0]['input'])
        self.assertEqual(requests[0]['audioConfig']['speakingRate'],1.2)
    def test_http_authorization_and_budget_before_provider(self):
        import json
        import threading
        from http.client import HTTPConnection
        from professional import make_handler
        from app import BoundedServer
        class Provider:
            calls=0
            def translate(self,body): self.calls+=1;return {'text':'Merhaba'}
        with tempfile.TemporaryDirectory() as d:
            p=Provider();server=BoundedServer(('127.0.0.1',0),make_handler('x'*32,Budget(str(Path(d)/'b.db'),2),p))
            thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
            try:
                for token,status in [('bad',401),('x'*32,429)]:
                    client=HTTPConnection('127.0.0.1',server.server_port,timeout=3)
                    client.request('POST','/v1/translate',json.dumps(dict(text='Hello',source='en',target='tr')),{'Authorization':'Bearer '+token})
                    response=client.getresponse();self.assertEqual(response.status,status);response.read();client.close()
                self.assertEqual(p.calls,0)
            finally: server.shutdown();server.server_close();thread.join()
