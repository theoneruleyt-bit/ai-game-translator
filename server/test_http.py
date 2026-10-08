import http.client
import os
import tempfile
import threading
import unittest
from app import BoundedServer, make_handler
from core import Ledger

class HttpBoundaryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.folder = tempfile.TemporaryDirectory()
        cls.server = BoundedServer(('127.0.0.1', 0), make_handler(Ledger(os.path.join(cls.folder.name, 'db')), 'test.apps.googleusercontent.com'))
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown();cls.server.server_close();cls.thread.join();cls.folder.cleanup()
    def send(self, method, path, body=None, headers=None):
        con = http.client.HTTPConnection('127.0.0.1', self.server.server_port, timeout=2)
        try:
            con.request(method,path,body,headers or {})
            response=con.getresponse();data=response.read()
            return response.status,data,response.getheader('Cache-Control')
        finally:
            con.close()
    def test_health_is_non_private(self):
        status,body,cache=self.send('GET','/health')
        self.assertEqual(200,status);self.assertEqual(b'{"ok": true}',body);self.assertEqual('no-store',cache)
    def test_identity_required_before_entitlement(self):
        self.assertEqual(401,self.send('POST','/v1/state','{}')[0])
    def test_oversized_request_is_rejected(self):
        self.assertEqual(413,self.send('POST','/v1/state',None,{'Content-Length':'65537'})[0])
    def test_unknown_endpoint_has_no_mutation(self):
        self.assertEqual(404,self.send('POST','/v1/unlock','{}')[0])
    def test_malformed_body_is_rejected_without_google_call(self):
        self.assertEqual(400,self.send('POST','/v1/state','{invalid',{'Authorization':'Bearer test'})[0])

if __name__ == '__main__':
    unittest.main()
