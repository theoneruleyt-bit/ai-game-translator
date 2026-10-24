from pathlib import Path
import numpy as np,sherpa_onnx,wave,re,json,time,urllib.request
import argparse,hashlib
parser=argparse.ArgumentParser(description="Limited reproducible host ASR comparison, not a game or translation benchmark")
parser.add_argument('--model-root',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
r=args.model_root;project=Path(__file__).resolve().parent.parent
manifest=json.loads((project/'verification/asr-inputs-0_27.json').read_text())
for part in manifest:
 if part['file']=='README.md':continue
 dest=r/part['family']/part['file'] if part['family']!='public' else r/part['file']
 if not dest.exists():
  dest.parent.mkdir(parents=True,exist_ok=True)
  data=urllib.request.urlopen(part['url'],timeout=120).read()
  assert len(data)==part['bytes'] and hashlib.sha256(data).hexdigest()==part['sha256']
  dest.write_bytes(data)
 data=dest.read_bytes()
 assert len(data)==part['bytes'] and hashlib.sha256(data).hexdigest()==part['sha256']
f=r/'jfk.wav'
if not f.exists():f.write_bytes(urllib.request.urlopen('https://raw.githubusercontent.com/ggml-org/whisper.cpp/master/samples/jfk.wav',timeout=45).read())
def read(p):
 with wave.open(str(p)) as f:
  assert f.getframerate()==16000 and f.getnchannels()==1
  return np.frombuffer(f.readframes(f.getnframes()),np.int16).astype(np.float32)/32768
refs={a.split(' ',1)[0]:a.split(' ',1)[1] for a in (r/'light/test_wavs/trans.txt').read_text().strip().splitlines()}
cases=[('literary0',read(r/'light/test_wavs/0.wav'),refs['0.wav']),('literary1',read(r/'light/test_wavs/1.wav'),refs['1.wav']),('public_speech_jfk',read(r/'jfk.wav'),'And so my fellow Americans ask not what your country can do for you ask what you can do for your country')]
for name,audio,ref in list(cases):
 cases.append((name+'_quiet',audio*.1,ref))
 noise=np.random.default_rng(17).normal(0,np.sqrt(np.mean(audio**2))*.16,len(audio)).astype(np.float32)
 cases.append((name+'_noise',audio+noise,ref))
cases += [('silence',np.zeros(160000,np.float32),''),('noise',np.random.default_rng(1).normal(0,.01,160000).astype(np.float32),''),('repeat_real',np.concatenate([cases[0][1],np.zeros(80000,np.float32),cases[0][1]]),refs['0.wav']+' '+refs['0.wav'])]
def words(s):return re.findall(r"[A-Z]+(?:'[A-Z]+)?",s.upper())
def edits(a,b):
 row=list(range(len(b)+1))
 for i,x in enumerate(a,1):
  new=[i]
  for j,y in enumerate(b,1):new.append(min(new[-1]+1,row[j]+1,row[j-1]+(x!=y)))
  row=new
 return row[-1]
rows=[]
for profile in ['light','giga']:
 folder=r/profile
 names=['encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx','decoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx','joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx'] if profile=='light' else ['encoder-epoch-99-avg-1.int8.onnx','decoder-epoch-99-avg-1.onnx','joiner-epoch-99-avg-1.int8.onnx']
 model=sherpa_onnx.OnlineRecognizer.from_transducer(tokens=str(folder/'tokens.txt'),encoder=str(folder/names[0]),decoder=str(folder/names[1]),joiner=str(folder/names[2]),num_threads=2,sample_rate=16000,feature_dim=80,enable_endpoint_detection=True,rule1_min_trailing_silence=2.4,rule2_min_trailing_silence=.7,rule3_min_utterance_length=99999,decoding_method='modified_beam_search',max_active_paths=4)
 for name,audio,ref in cases:
  cfg=sherpa_onnx.VadModelConfig();cfg.silero_vad.model=str(project/'app/src/main/assets/silero_vad.onnx');cfg.silero_vad.threshold=.5;cfg.silero_vad.min_silence_duration=.25;cfg.silero_vad.min_speech_duration=.064;cfg.silero_vad.max_speech_duration=60;cfg.silero_vad.window_size=512;cfg.sample_rate=16000;cfg.num_threads=1
  vad=sherpa_onnx.VoiceActivityDetector(cfg,buffer_size_in_seconds=60)
  stream=model.create_stream();data=np.concatenate([audio,np.zeros(160000,np.float32)]);buffer=np.zeros(0,np.float32);prefix=np.zeros(0,np.float32);active=False;last_speech=None;trusted='';parts=[];started=time.perf_counter()
  for i in range(0,len(data),800):
   frame=data[i:i+800];at=(i+len(frame))/16;buffer=np.concatenate([buffer,frame]);speech=False
   while len(buffer)>=512:
    vad.accept_waveform(buffer[:512]);buffer=buffer[512:];speech=speech or vad.is_speech_detected()
    while not vad.empty():vad.pop()
   speech=speech and np.mean(frame.astype(np.float64)**2)>1e-8
   if speech:last_speech=at
   if not active:
    prefix=np.concatenate([prefix,frame])[-6400:]
    if not speech:continue
    active=True;feed=prefix;prefix=np.zeros(0,np.float32)
   else:feed=frame
   stream.accept_waveform(16000,feed)
   while model.is_ready(stream):model.decode_stream(stream)
   text=model.get_result(stream).strip()
   if last_speech is not None and at-last_speech<=700:trusted=text
   if model.is_endpoint(stream) or last_speech is not None and at-last_speech>=1100:
    if trusted:parts.append(trusted)
    model.reset(stream);active=False;last_speech=None;trusted='';prefix=np.zeros(0,np.float32)
  hyp=' '.join(parts);duration=len(audio)/16000
  row={'profile':profile,'case':name,'reference':ref,'hypothesis':hyp,'reference_words':len(words(ref)),'word_errors':edits(words(ref),words(hyp)),'utterances':len(parts),'audio_seconds':duration,'host_processing_seconds':round(time.perf_counter()-started,3)}
  rows.append(row);print(json.dumps(row),flush=True)
args.output.write_text(json.dumps(rows,indent=2))
