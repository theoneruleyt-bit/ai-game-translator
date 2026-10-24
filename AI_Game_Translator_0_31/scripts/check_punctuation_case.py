from pathlib import Path
import sherpa_onnx,json,hashlib
p=Path(__file__).resolve().parents[1];a=p/'app/src/main/assets/punctuation'
m=sherpa_onnx.OnlinePunctuation(sherpa_onnx.OnlinePunctuationConfig(model_config=sherpa_onnx.OnlinePunctuationModelConfig(cnn_bilstm=str(a/'model.int8.onnx'),bpe_vocab=str(a/'bpe.vocab'),num_threads=1)))
texts=[('written_game_dialogue','we need to find another way out the guards are coming down the stairs and we do not have much time'),('written_narration','we went to the station and the guard told us to wait but the train had already left i have been waiting for you for a long time and now we need to leave because the police are coming')]
for row in json.loads((p/'verification/asr-model-comparison-0_27.json').read_text()):
 if row['profile']=='giga' and row['case'] in ['literary0','literary1','public_speech_jfk']:texts.append(('actual_asr_'+row['case'],row['hypothesis']))
rows=[]
for name,text in texts:
 raw=text.upper();before=m.add_punctuation_with_case(raw);prepared=raw.lower();after=m.add_punctuation_with_case(prepared)
 rows.append({'case':name,'raw_asr_shape':raw,'old_native_output':before,'prepared_input':prepared,'fixed_native_output':after})
report={'scope':'Actual Linux Sherpa-ONNX 1.13.8 punctuation inference. 3 transcripts from real native ASR in 0.27; 2 written dialogue/narration samples. Not Turkish translation quality, Android latency or microphone testing.','model_sha256':hashlib.sha256((a/'model.int8.onnx').read_bytes()).hexdigest(),'cases':rows}
(p/'verification/punctuation-case-0_28.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps([{'case':r['case'],'before':r['old_native_output'],'after':r['fixed_native_output']} for r in rows]))
