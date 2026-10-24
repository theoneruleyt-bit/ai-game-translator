from pathlib import Path
import sys,wave,time,json,re,hashlib
import numpy as np,sherpa_onnx
import argparse
parser=argparse.ArgumentParser(description="Small pinned-model ASR regression; not a general accuracy benchmark")
parser.add_argument('--model-dir',required=True,type=Path,help='Model files plus upstream test_wavs directory')
parser.add_argument('--output',type=Path)
args=parser.parse_args()
root=args.model_dir;project=Path(__file__).resolve().parent.parent
source=(project/'app/src/main/java/com/optima/gametranslator/LocalSpeechModels.kt').read_text().split('val light=',1)[1].split('val all=',1)[0]
parts=re.findall(r'SpeechModelPart\("([^"]+)",(\d+)L,"([a-f0-9]+)"\)',source)
assert len(parts)==4
for name,size,sha in parts:
 data=(root/name).read_bytes();assert len(data)==int(size);assert hashlib.sha256(data).hexdigest()==sha
refs={x.split(' ',1)[0]:x.split(' ',1)[1] for x in (root/'test_wavs/trans.txt').read_text().strip().splitlines()}
def words(x):return re.findall(r"[A-Z]+(?:'[A-Z]+)?",x.upper())
def errors(a,b):
 d=list(range(len(b)+1))
 for i,x in enumerate(a,1):
  n=[i]
  for j,y in enumerate(b,1):n.append(min(n[-1]+1,d[j]+1,d[j-1]+(x!=y)))
  d=n
 return d[-1]
r=sherpa_onnx.OnlineRecognizer.from_transducer(tokens=str(root/'tokens.txt'),encoder=str(root/'encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx'),decoder=str(root/'decoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx'),joiner=str(root/'joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx'),num_threads=2,sample_rate=16000,feature_dim=80,enable_endpoint_detection=True,rule1_min_trailing_silence=2.4,rule2_min_trailing_silence=1.05,rule3_min_utterance_length=99999,decoding_method='modified_beam_search',max_active_paths=4)
def read(name):
 with wave.open(str(root/'test_wavs'/name)) as w:
  assert w.getframerate()==16000
  return np.frombuffer(w.readframes(w.getnframes()),dtype=np.int16).astype(np.float32)/32768
cases=[('quiet_speech',read('0.wav')*.1,refs['0.wav']),('noise_only',np.random.default_rng(42).normal(0,.01,80000).astype(np.float32),''),('silence',np.zeros(160000,dtype=np.float32),''),('sample0',read('0.wav'),refs['0.wav']),('sample1',read('1.wav'),refs['1.wav']),('repeated_with_pause',np.concatenate([read('0.wav'),np.zeros(4800,dtype=np.float32),read('0.wav')]),refs['0.wav']+' '+refs['0.wav'])]
# Restart after a real endpoint, not only the short in-turn pause above.
cases += [
 ('speech_after_long_silence',np.concatenate([np.zeros(80000,dtype=np.float32),read('0.wav')]),refs['0.wav']),
 ('repeat_after_endpoint',np.concatenate([read('0.wav'),np.zeros(80000,dtype=np.float32),read('0.wav')]),refs['0.wav']+' '+refs['0.wav']),
 ('impulse_noise',np.where(np.arange(160000)%16000<160,np.random.default_rng(7).normal(0,.1,160000),0).astype(np.float32),''),
]
reports=[]
for name,audio,ref in cases:
 for frame_size in [800,1920]:
  config=sherpa_onnx.VadModelConfig();config.silero_vad.model=str(project/'app/src/main/assets/silero_vad.onnx')
  config.silero_vad.threshold=.5;config.silero_vad.min_silence_duration=.25;config.silero_vad.min_speech_duration=.064;config.silero_vad.max_speech_duration=60;config.silero_vad.window_size=512;config.sample_rate=16000;config.num_threads=1
  vad=sherpa_onnx.VoiceActivityDetector(config,buffer_size_in_seconds=60)
  vad_buffer=np.zeros(0,dtype=np.float32);speech_seen=False;active=False;prefix=np.zeros(0,dtype=np.float32)
  last_speech=None;trusted="";silence_words_rejected=0
  stream=r.create_stream();data=np.concatenate([audio,np.zeros(160000,dtype=np.float32)]);parts=[];start=time.perf_counter()
  for i in range(0,len(data),frame_size):
   frame=data[i:i+frame_size]
   vad_buffer=np.concatenate([vad_buffer,frame])
   current_speech=False
   while len(vad_buffer)>=512:
    vad.accept_waveform(vad_buffer[:512]);vad_buffer=vad_buffer[512:]
    current_speech=current_speech or vad.is_speech_detected()
    while not vad.empty():vad.pop()
   current_speech=current_speech and bool(len(frame) and np.mean(frame.astype(np.float64)**2)>1e-8)
   now=(i+len(frame))/16
   if current_speech:last_speech=now
   speech_seen=speech_seen or current_speech
   if not active:
    prefix=np.concatenate([prefix,frame])[-6400:]
    if current_speech:
     active=True;feed=prefix;prefix=np.zeros(0,dtype=np.float32)
    else:feed=np.zeros(0,dtype=np.float32)
   else:feed=frame
   if len(feed):stream.accept_waveform(16000,feed)
   while r.is_ready(stream):r.decode_stream(stream)
   hypothesis=r.get_result(stream).strip()
   if last_speech is not None and now-last_speech<=700:trusted=hypothesis
   elif hypothesis and hypothesis!=trusted:silence_words_rejected+=1
   if r.is_endpoint(stream) or last_speech is not None and now-last_speech>=1100:
    t=trusted
    if t and speech_seen:parts.append(t)
    speech_seen=False;active=False;prefix=np.zeros(0,dtype=np.float32);last_speech=None;trusted="";r.reset(stream)
  stream.input_finished()
  while r.is_ready(stream):r.decode_stream(stream)
  t=trusted
  if t and speech_seen:parts.append(t)
  row={'case':name,'frame_ms':frame_size/16,'seconds':round(len(audio)/16000,3),'word_errors':errors(words(ref),words(' '.join(parts))),'reference_words':len(words(ref)),'parts':len(parts),'text':' '.join(parts),'silence_hypotheses_rejected':silence_words_rejected,'decode_seconds':round(time.perf_counter()-start,3)}
  reports.append(row);print(json.dumps(row),flush=True)
(args.output or project/'AUDIO_REGRESSION_GATE_0_25.json').write_text(json.dumps(reports,indent=2))
sys.exit(1 if any(row['word_errors'] for row in reports) else 0)
