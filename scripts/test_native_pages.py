import struct
import tempfile
import unittest
import zipfile
from pathlib import Path
from check_native_pages import inspect_elf, inspect_archive

def elf(alignment=16384, offset=0, virtual=0):
    data=bytearray(120);data[:6]=b'\x7fELF\x02\x01'
    struct.pack_into('<Q',data,32,64);struct.pack_into('<HH',data,54,56,1)
    struct.pack_into('<IIQQQQQQ',data,64,1,5,offset,virtual,0,100,100,alignment)
    return bytes(data)

class NativeTests(unittest.TestCase):
    def test_16k_segments_pass(self):
        self.assertTrue(inspect_elf(elf())[0]['compatible'])
    def test_4k_segments_fail(self):
        self.assertFalse(inspect_elf(elf(4096))[0]['compatible'])
    def test_offset_must_match_virtual_address(self):
        self.assertFalse(inspect_elf(elf(offset=4096))[0]['compatible'])
    def test_truncated_binary_rejected(self):
        with self.assertRaises(ValueError):inspect_elf(elf()[:70])
    def test_real_zip_reports_offending_library(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'fixture.apk'
            with zipfile.ZipFile(path,'w') as archive:
                archive.writestr('lib/arm64-v8a/libok.so',elf())
                archive.writestr('lib/x86_64/libbad.so',elf(4096))
            report=inspect_archive(path)
            self.assertFalse(report['passed'])
            self.assertEqual(['lib/x86_64/libbad.so'],report['failures'])

if __name__ == '__main__':unittest.main()
