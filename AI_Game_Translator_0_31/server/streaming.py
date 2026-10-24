"""Private PCM WebSocket bridge to STT V2. ADC credentials stay on the server."""
import hmac,json,os,queue,threading,time,uuid
from professional import Budget,PilotError

def validate_frame(frame):
    if not isinstance(frame,bytes) or not frame or len(frame)>3200 or len(frame)%2:raise ValueError('invalid_pcm')
    return len(frame)

def bridge(ws,token,budget,client,project,region,slots):
    if ws.request.path!='/v1/listen' or not hmac.compare_digest(ws.request.headers.get('Authorization','').encode(),('Bearer '+token).encode()):
        ws.send(json.dumps({'type':'error','reason':'unauthorized'}));ws.close();return
    if not slots.acquire(blocking=False):
        ws.send(json.dumps({'type':'error','reason':'quota'}));ws.close();return
    done=threading.Event();audio=queue.Queue(maxsize=40);reason=[];receiver=None;call=None
    try:
        from google.cloud.speech_v2.types import cloud_speech as types
        language=json.loads(ws.recv(timeout=5)).get('language')
        if language not in {'en-US','tr-TR'}:raise ValueError('unsupported_language')
        config=types.RecognitionConfig(explicit_decoding_config=types.ExplicitDecodingConfig(
            encoding=types.ExplicitDecodingConfig.AudioEncoding.LINEAR16,sample_rate_hertz=16000,audio_channel_count=1),language_codes=[language],model='chirp_3')
        streaming=types.StreamingRecognitionConfig(config=config,streaming_features=types.StreamingRecognitionFeatures(interim_results=True))
        started=time.monotonic()
        def receive():
            try:
                while not done.is_set() and time.monotonic()-started<240:
                    try:frame=ws.recv(timeout=1)
                    except TimeoutError:continue
                    budget.reserve(validate_frame(frame));audio.put(frame,timeout=1)
            except PilotError:reason.append('quota')
            except queue.Full:reason.append('capture')
            except Exception:reason.append('connection')
            finally:done.set()
        def requests():
            yield types.StreamingRecognizeRequest(recognizer=f'projects/{project}/locations/{region}/recognizers/_',streaming_config=streaming)
            while not done.is_set() or not audio.empty():
                try:frame=audio.get(timeout=.25)
                except queue.Empty:continue
                yield types.StreamingRecognizeRequest(audio=frame)
        receiver=threading.Thread(target=receive,daemon=True);receiver.start()
        ws.send(json.dumps({'type':'ready'}))
        call=client.streaming_recognize(requests=requests(),timeout=250)
        session=uuid.uuid4().hex;segment=0
        for response in call:
            for result in response.results:
                if not result.alternatives:continue
                text=result.alternatives[0].transcript.strip()
                if not text:continue
                ws.send(json.dumps({'type':'result','id':f'{session}:{segment}','text':text,'final':result.is_final}))
                if result.is_final:segment+=1
        ws.send(json.dumps({'type':'error','reason':reason[0]}) if reason else json.dumps({'type':'rotate'}))
    except Exception:
        try:ws.send(json.dumps({'type':'error','reason':'service'}))
        except Exception:pass
    finally:
        done.set()
        if call is not None and hasattr(call,'cancel'):call.cancel()
        ws.close()
        if receiver is not None:receiver.join(timeout=2)
        slots.release()

if __name__=='__main__':
    from websockets.sync.server import serve
    from google.cloud.speech_v2 import SpeechClient
    from google.api_core.client_options import ClientOptions
    import re
    token=os.environ.get('PILOT_TOKEN','');project=os.environ.get('GOOGLE_CLOUD_PROJECT','');region=os.environ.get('SPEECH_REGION','us')
    if len(token)<32 or not re.fullmatch(r'[a-z][a-z0-9-]{4,62}',project) or not re.fullmatch(r'[a-z0-9-]+',region):raise SystemExit('Set PILOT_TOKEN, GOOGLE_CLOUD_PROJECT and supported SPEECH_REGION')
    client=SpeechClient(client_options=ClientOptions(api_endpoint=f'{region}-speech.googleapis.com'))
    budget=Budget(os.environ.get('SPEECH_BUDGET_DB','speech-budget.sqlite'),int(os.environ.get('SPEECH_DAILY_SECONDS','600'))*32000)
    slots=threading.BoundedSemaphore(4)
    with serve(lambda ws:bridge(ws,token,budget,client,project,region,slots),'127.0.0.1',8082,max_size=4096,max_queue=8,compression=None,open_timeout=5,close_timeout=3) as server:server.serve_forever()
