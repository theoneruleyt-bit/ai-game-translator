"""Rebuild the bundled token table from pinned OPUS-MT files. Requires sentencepiece==0.2.1."""
from pathlib import Path
import base64
import hashlib
import json
import tempfile
import urllib.request
import sentencepiece as spm
from sentencepiece import sentencepiece_model_pb2

repo = 'https://huggingface.co/Helsinki-NLP/opus-mt_tiny_eng-tur/resolve/daadff27d04f77a1f37b43ab4983ee50468ea7d7/'
checksums = {
    'source.spm': 'fe55e84b94d17b424eafba19d1e4815a90b23cd52138b9cdb1072d1b65c9795c',
    'vocab.json': '5ad08bcc77230030648df7079b1523db693030fb9a206d83effbc66f2944f268',
}
with tempfile.TemporaryDirectory() as directory:
    stage = Path(directory)
    for name, expected in checksums.items():
        with urllib.request.urlopen(repo + name, timeout=60) as response:
            data = response.read()
        assert hashlib.sha256(data).hexdigest() == expected
        (stage / name).write_bytes(data)
    proto = sentencepiece_model_pb2.ModelProto()
    proto.ParseFromString((stage / 'source.spm').read_bytes())
    vocab = json.loads((stage / 'vocab.json').read_text())
    rows = [f'{vocab[p.piece]}\t{p.score!r}\t' + base64.b64encode(p.piece.encode()).decode()
            for p in proto.pieces if p.type in (1, 2)]
    data = ('\n'.join(rows) + '\n').encode()
    assert hashlib.sha256(data).hexdigest() == 'bb621d6effa7923e159c2238b8e9f7d0be789b9721a7d7c6365efe9ac3f07913'
    output = Path(__file__).resolve().parents[1] / 'app/src/main/assets/translation-en-tr/pieces.tsv'
    output.write_bytes(data)
    print('Token table reproduced with pinned checksum')
