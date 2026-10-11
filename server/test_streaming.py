import tempfile,unittest
from pathlib import Path
from streaming import validate_frame,bridge
from professional import Budget,PilotError
class StreamingTest(unittest.TestCase):
    def test_pcm_boundaries(self):
        self.assertEqual(validate_frame(bytes(1600)),1600)
        for frame in [b'',b'x',bytes(4000),'text']:
            with self.assertRaises(ValueError):validate_frame(frame)
    def test_audio_budget_persists(self):
        with tempfile.TemporaryDirectory() as d:
            p=str(Path(d)/'speech.db');Budget(p,32000).reserve(16000);Budget(p,32000).reserve(16000)
            with self.assertRaises(PilotError):Budget(p,32000).reserve(1600)
    def test_unauthorized_cannot_open_provider(self):
        class Request:
            path='/v1/listen';headers={'Authorization':'Bearer wrong'}
        class Socket:
            request=Request();messages=[];closed=False
            def send(self,message):self.messages.append(message)
            def close(self):self.closed=True
        class Client:
            def streaming_recognize(self,**kwargs):raise AssertionError('Provider accessed')
        ws=Socket();bridge(ws,'secret',None,Client(),'project','us',None)
        self.assertTrue(ws.closed);self.assertIn('unauthorized',ws.messages[0])
