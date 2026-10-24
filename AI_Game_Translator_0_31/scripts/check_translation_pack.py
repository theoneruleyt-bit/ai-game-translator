"""Validate the bundled optional OPUS-MT model and authored conversation pack."""
from pathlib import Path
import hashlib
import re

assets = Path('app/src/main/assets')
for name, expected in {
    'model.tflite': '6bab379f1f20f8ba927a1494603dcaca25e1882d5f83c2efecfb71060d7ac723',
    'pieces.tsv': 'bb621d6effa7923e159c2238b8e9f7d0be789b9721a7d7c6365efe9ac3f07913',
}.items():
    data = (assets / 'translation-en-tr' / name).read_bytes()
    assert hashlib.sha256(data).hexdigest() == expected, f'Translation asset mismatch: {name}'
rows = [s for s in (assets / 'conversation-v2-en-tr.tsv').read_text().splitlines() if s and not s.startswith('#')]
assert len(rows) == 517
keys = [re.sub(r'\s+', ' ', s.split('\t')[0].lower().replace('’', "'").strip().rstrip('.,!?;:')) for s in rows]
assert len(keys) == len(set(keys))
assert all(len(s.split('\t')) == 2 and s.split('\t')[1].strip() for s in rows)
print('OPUS-MT model, tokenizer assets and 517 whole-phrase variants: PASS')
