"""Optional host experiment, not CI, not a quality certification or phone latency test.

python -m pip install litert-lm==0.16.1 sacrebleu==2.6.0
python scripts/evaluate_context_translation.py --model /path/Hy-MT2-1.8B_int8.litertlm --output results.json
The 1.83 GB model is NOT included in the APK/source ZIP. No account or inference API is used.
"""
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import re
import time


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--model',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--threads',type=int,default=2)
    args=parser.parse_args()
    root=Path(__file__).resolve().parents[1]
    pin=json.loads((root/'qa/context-translation/model-pin.json').read_text())
    if args.model.stat().st_size!=pin['bytes']:raise SystemExit('Model length mismatch')
    sha=hashlib.sha256()
    with args.model.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''):sha.update(chunk)
    if sha.hexdigest()!=pin['sha256']:raise SystemExit('Model SHA-256 mismatch')
    source=(root/'app/src/main/java/com/optima/gametranslator/ContextTranslationPolicy.kt').read_text()
    instructions={k:re.search('const val '+k+'="([^"]+)"',source).group(1) for k in ['INSTRUCTION','CONTEXT_INSTRUCTION']}
    cases=json.loads((root/'qa/context-translation/authored-cases.json').read_text())
    import litert_lm
    from sacrebleu.metrics import CHRF
    cache=args.output.resolve().parent/'context-host-cache';cache.mkdir(parents=True,exist_ok=True)
    with litert_lm.Engine(str(args.model),backend=litert_lm.Backend.CPU(thread_count=args.threads),max_num_tokens=1024,cache_dir=str(cache)) as engine:
        for case in cases:
            bg='\n'.join(s[:240] for s in case['background'][-2:])
            prompt=(('[Background Information]\n'+bg+'\n'+instructions['CONTEXT_INSTRUCTION']+'\n[Source Text]\n') if bg else instructions['INSTRUCTION']+'\n\n[Source Text]\n')+case['source']
            with engine.create_conversation(sampler_config=litert_lm.SamplerConfig(top_k=1,top_p=1,temperature=0),max_output_tokens=160) as conversation:
                start=time.monotonic();response=conversation.send_message(prompt)
                case.update(output=''.join(p.get('text','') for p in response.get('content',[]) if p.get('type')=='text'),host_ms=round((time.monotonic()-start)*1000,2))
                print(json.dumps(case,ensure_ascii=False),flush=True)
    result={'scope':'40 authored typed-text probes; the 10 follow-ups were added after candidate selection. No blind human study, microphone, ML Kit comparison or phone latency. chrF++ is text overlap, not semantic accuracy.','runtime':importlib.metadata.version('litert-lm'),'threads':args.threads,'model':pin,'instructions':instructions,'chrf2pp':CHRF(word_order=2).corpus_score([r['output'] for r in cases],[[r['reference'] for r in cases]]).score,'rows':cases}
    args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2))


if __name__=='__main__':main()
