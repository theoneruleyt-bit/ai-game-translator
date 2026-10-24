"""Fail if the bundled upstream native engine changed or lacks 16 KB alignment."""
import hashlib,zipfile
from pathlib import Path
from check_native_pages import inspect_elf
p=Path('app/libs/sherpa-onnx-static-1.13.8.aar')
assert hashlib.sha256(p.read_bytes()).hexdigest()=='b22c3fc1b6a45666d28892bb2f7694beeb77a8362d7ebd77c1a5431ec9435471'
with zipfile.ZipFile(p) as archive:
    assert archive.testzip() is None
    for abi in ['arm64-v8a','x86_64']:
        files=[n for n in archive.namelist() if f'/{abi}/' in n and n.endswith('.so')]
        assert files,abi
        for name in files:assert all(row['compatible'] for row in inspect_elf(archive.read(name))),name
print('Pinned Sherpa AAR checksum and 64-bit 16 KB alignment: PASS')

vad=Path('app/src/main/assets/silero_vad.onnx')
assert hashlib.sha256(vad.read_bytes()).hexdigest()=='9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6'
print('Bundled Silero VAD checksum: PASS')
