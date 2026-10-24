"""Recreate native punctuation fixtures. Run with sherpa-onnx==1.13.8 installed on Linux."""
import sherpa_onnx,time,json,base64,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1];assets=root/'app/src/main/assets/punctuation'
p=sherpa_onnx.OnlinePunctuation(sherpa_onnx.OnlinePunctuationConfig(model_config=sherpa_onnx.OnlinePunctuationModelConfig(cnn_bilstm=str(assets/'model.int8.onnx'),bpe_vocab=str(assets/'bpe.vocab'),num_threads=1)))
texts=[
'we need to find another way out the guards are coming down the stairs and we do not have much time i will meet you at the station bring the car around and wait by the gate do not leave without me',
'we went to the station and the guard told us to wait but the train had already left i have been waiting for you for a long time and now we need to leave because the police are coming']
inputs=set()
for text in texts:
 w=text.split()
 for a in range(len(w)):
  for b in range(a+1,min(len(w),a+96)+1):inputs.add(' '.join(w[a:b]))
start=time.perf_counter();rows=[];durations=[]
for text in sorted(inputs):
 t=time.perf_counter();prediction=p.add_punctuation_with_case(text);durations.append((time.perf_counter()-t)*1000)
 rows.append(base64.b64encode(text.encode()).decode()+'\t'+base64.b64encode(prediction.encode()).decode())
resources=root/'app/src/test/resources';resources.mkdir(exist_ok=True)
(resources/'native-punctuation-0_23.tsv').write_text('\n'.join(rows)+'\n')
report={'scope':'Linux x86_64 Sherpa 1.13.8 CPU punctuation inference; outputs replayed by Kotlin stream tests. Not Android ASR or Turkish translation accuracy.','model_sha256':hashlib.sha256((assets/'model.int8.onnx').read_bytes()).hexdigest(),'vocab_sha256':hashlib.sha256((assets/'bpe.vocab').read_bytes()).hexdigest(),'native_predictions':len(rows),'elapsed_seconds':round(time.perf_counter()-start,3),'median_inference_ms':round(sorted(durations)[len(durations)//2],3),'max_inference_ms':round(max(durations),3),'transcripts':texts}
(root/'verification/punctuation-native-0_23.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
