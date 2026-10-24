"""Run bundled LiteRT model on typed dialogue. Needs ai-edge-litert, numpy, sentencepiece."""
import argparse
import hashlib
import json
from pathlib import Path
import tempfile
import time
import urllib.request
import importlib.metadata
import numpy as np
import sentencepiece as spm
from ai_edge_litert.interpreter import Interpreter

parser = argparse.ArgumentParser()
parser.add_argument('--report', required=True)
parser.add_argument('--tokenizer-dir', help='Directory with original source.spm and vocab.json; otherwise fetch pinned files')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
model = root / 'app/src/main/assets/translation-en-tr/model.tflite'
assert hashlib.sha256(model.read_bytes()).hexdigest() == '6bab379f1f20f8ba927a1494603dcaca25e1882d5f83c2efecfb71060d7ac723'
checks = {'source.spm': 'fe55e84b94d17b424eafba19d1e4815a90b23cd52138b9cdb1072d1b65c9795c',
          'vocab.json': '5ad08bcc77230030648df7079b1523db693030fb9a206d83effbc66f2944f268'}
with tempfile.TemporaryDirectory() as directory:
    stage = Path(args.tokenizer_dir or directory)
    for name, sha in checks.items():
        if not args.tokenizer_dir:
            uri = 'https://huggingface.co/Helsinki-NLP/opus-mt_tiny_eng-tur/resolve/daadff27d04f77a1f37b43ab4983ee50468ea7d7/' + name
            with urllib.request.urlopen(uri, timeout=60) as response:
                (stage / name).write_bytes(response.read())
        assert hashlib.sha256((stage / name).read_bytes()).hexdigest() == sha
    tokenizer = spm.SentencePieceProcessor(model_file=str(stage / 'source.spm'))
    vocab = json.loads((stage / 'vocab.json').read_text())
    inverse = {idx: piece for piece, idx in vocab.items()}
    interpreter = Interpreter(model_path=str(model), num_threads=2)
    encode = interpreter.get_signature_runner('encode')
    prefill = interpreter.get_signature_runner('prefill')
    decode = interpreter.get_signature_runner('decode')
    def translate(text):
        ids = [vocab.get(piece, 1) for piece in tokenizer.encode(text, out_type=str)] + [0]
        assert len(ids) <= 256 and 1 not in ids
        tokens = np.zeros((1, 256), np.int32)
        tokens[0, :len(ids)] = ids
        mask = np.full((256,), -1e9, np.float32)
        mask[:len(ids)] = 0
        hidden = encode(input_ids=tokens, input_pos=np.arange(256, dtype=np.int32), pad_mask=mask)['output_0']
        cache = {k: np.zeros(v['shape'], v['dtype']) for k, v in prefill.get_input_details().items() if 'cache' in k}
        token = 32000
        result = []
        for step in range(128):
            output = (prefill if step == 0 else decode)(decoder_input_ids=np.array([[token]], np.int32),
                decoder_input_pos=np.array([step], np.int32), encoder_hidden_states=hidden, pad_mask=mask, **cache)
            scores = output['logits'][0, 0].copy()
            scores[32000] = -np.inf
            token = int(scores.argmax())
            cache = {k: v for k, v in output.items() if 'cache' in k}
            if token == 0:
                assert 1 not in result
                return ''.join(inverse[i] for i in result).replace('▁', ' ').strip()
            result.append(token)
        raise RuntimeError('No EOS: refusing truncated target')
    examples = json.loads((root / 'verification/translation-observations-0_24.json').read_text())['tiny']
    outputs = []
    for example in examples:
        start = time.perf_counter()
        target = translate(example['source'])
        outputs.append({'source': example['source'], 'target': target, 'elapsed_ms': round((time.perf_counter()-start)*1000, 3)})
    report = {'scope': 'Host CPU typed-text inference; not Android or an accuracy score.',
              'runtime': importlib.metadata.version('ai-edge-litert'), 'outputs': outputs}
    Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('Completed', len(outputs), 'typed-text model inferences')
