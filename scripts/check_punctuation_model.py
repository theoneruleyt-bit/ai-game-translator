"""Verify the bundled English sentence-boundary model before Android builds."""
from pathlib import Path
import hashlib

FILES = {
    'model.int8.onnx': '9d611f445fe4a46186080fe161be6059d87d72eb88d3a8cb00c1a06e83a6067e',
    'bpe.vocab': 'e118b7ad88c54db562517df49e1cffd4836d166c34fb190fd311d7f34eb238f5',
}
for name, expected in FILES.items():
    data = (Path('app/src/main/assets/punctuation') / name).read_bytes()
    assert hashlib.sha256(data).hexdigest() == expected, f'Punctuation asset mismatch: {name}'
print('Bundled English punctuation model and vocabulary: PASS')
