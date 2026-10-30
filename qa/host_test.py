#!/usr/bin/env python3
"""Real native inference smoke test; never substitutes silence/static audio for model output."""
import hashlib, json, pathlib, resource, subprocess, time, wave, struct, math, os
root=pathlib.Path(__file__).resolve().parents[1]
models=pathlib.Path(os.environ.get('VOCLONE_TEST_MODELS','/workspace/test-models'))
cli=root/'native/build/omni-host/omnivoice-tts'
out=root/'qa/host';out.mkdir(exist_ok=True)
results=[]
for lang in ('en','id','ja'):
 target=out/f'generated-{lang}.wav'
 command=[str(cli),'--model',str(models/'omnivoice-base-Q4_K_M.gguf'),'--codec',str(models/'omnivoice-tokenizer-Q4_K_M.gguf'),'--ref-wav',str(out/'reference.wav'),'--ref-text',str(out/'reference.txt'),'--lang',lang,'--no-fa','--seed','42','-o',str(target)]
 start=time.monotonic()
 with (out/f'{lang}.log').open('w') as log,(out/f'input-{lang}.txt').open() as text:
  process=subprocess.run(command,stdin=text,stdout=log,stderr=subprocess.STDOUT,timeout=900)
 entry=dict(language=lang,exit_code=process.returncode,seconds=round(time.monotonic()-start,2),max_rss_kib=resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss)
 if process.returncode==0:
  with wave.open(str(target)) as wav:
   raw=wav.readframes(wav.getnframes());samples=struct.unpack('<'+'h'*(len(raw)//2),raw)
   entry.update(rate=wav.getframerate(),channels=wav.getnchannels(),duration=len(samples)/wav.getframerate(),rms=math.sqrt(sum(x*x for x in samples)/len(samples))/32768,sha256=hashlib.sha256(target.read_bytes()).hexdigest(),bytes=target.stat().st_size)
   assert entry['rate']==24000 and entry['channels']==1 and entry['duration']>.1 and entry['rms']>.0001
 entry['quality_verified']=False
 results.append(entry);(out/'results.json').write_text(json.dumps(results,indent=2)+'\n')
 print(json.dumps(entry),flush=True)
 if process.returncode:raise SystemExit(process.returncode)
