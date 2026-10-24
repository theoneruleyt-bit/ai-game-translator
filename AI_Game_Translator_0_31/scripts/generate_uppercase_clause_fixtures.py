"""Reproduce the uppercase punctuation issue using actual Sherpa-ONNX 1.13.8 inference.
Fixtures are written English, not a microphone or Turkish translation benchmark.
"""
from pathlib import Path
import base64,hashlib,json,time
import sherpa_onnx
root=Path(__file__).resolve().parents[1];assets=root/'app/src/main/assets/punctuation'
p=sherpa_onnx.OnlinePunctuation(sherpa_onnx.OnlinePunctuationConfig(model_config=sherpa_onnx.OnlinePunctuationModelConfig(cnn_bilstm=str(assets/'model.int8.onnx'),bpe_vocab=str(assets/'bpe.vocab'),num_threads=1)))
original=root/'app/src/test/resources/native-punctuation-0_23.tsv'
rows=[];changed=0;started=time.perf_counter()
for row in original.read_text().splitlines():
 raw=base64.b64decode(row.split('\t')[0]).decode().upper()
 result=p.add_punctuation_with_case(raw);changed+=int(result!=raw)
 rows.append(base64.b64encode(raw.encode()).decode()+'\t'+base64.b64encode(result.encode()).decode())
dest=root/'app/src/test/resources/native-uppercase-punctuation-0_28.tsv';dest.write_text('\n'.join(rows)+'\n')
report={'scope':'Actual Linux Sherpa 1.13.8 punctuation predictions for uppercase versions of written 0.23 clause fixtures. Kotlin tests replay original native output versus normalized native output, with transcript consensus and StreamingTurns. Not Android microphone latency or Turkish translation accuracy.','native_predictions':len(rows),'changed_uppercase_inputs':changed,'elapsed_seconds':round(time.perf_counter()-started,3),'input_fixture_sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'fixture_sha256':hashlib.sha256(dest.read_bytes()).hexdigest()}
(root/'verification/native-uppercase-fixtures-0_28.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
