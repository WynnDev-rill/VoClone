#!/usr/bin/env python3
"""Verify APK signature, identity, exact ABI and Android 16/16 KiB native alignment."""
import argparse, hashlib, os, pathlib, re, subprocess, zipfile
p=argparse.ArgumentParser();p.add_argument('apk');p.add_argument('--debug',action='store_true');args=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/workspace/android-sdk')))
tools=sdk/'build-tools/36.0.0';apk=pathlib.Path(args.apk).resolve()
cert=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
signer=re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)',cert).group(1)
if not args.debug:assert signer==(root/'docs/signing-sha256.txt').read_text().strip(), 'Wrong owner signing key'
subprocess.run([str(tools/'zipalign'),'-c','-P','16','4',str(apk)],check=True,stdout=subprocess.DEVNULL)
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
build=(root/'app/build.gradle.kts').read_text()
version=re.search(r'versionName = "([^"]+)"',build).group(1);code=int(re.search(r'versionCode = (\d+)',build).group(1))
assert f"package: name='id.wynn.voclone' versionCode='{code}' versionName='{version}'" in badging
assert "sdkVersion:'29'" in badging and "targetSdkVersion:'36'" in badging
readelf=next((sdk/'ndk/27.1.12297006/toolchains/llvm/prebuilt').glob('*/bin/llvm-readelf'))
import tempfile
with zipfile.ZipFile(apk) as z,tempfile.TemporaryDirectory() as temp:
 libs=[n for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')]
 assert {n.split('/')[1] for n in libs}=={'arm64-v8a'},libs
 assert {pathlib.Path(n).name for n in libs}=={'libvc_omni.so','libvc_omni_i8mm.so','libvc_whisper.so','libvc_mp3.so','libc++_shared.so','libandroidx.graphics.path.so'}
 assert not any(n.endswith(('.p12','.jks','.keystore','.gguf','.onnx')) for n in z.namelist())
 for name in libs:
  f=pathlib.Path(temp)/pathlib.Path(name).name;f.write_bytes(z.read(name))
  program=subprocess.check_output([str(readelf),'-lW',str(f)],text=True)
  aligns=[int(line.split()[-1],16) for line in program.splitlines() if line.strip().startswith('LOAD')]
  assert aligns and all(v>=0x4000 for v in aligns),(name,aligns)
print('Verified:',apk.name,'bytes='+str(apk.stat().st_size),'sha256='+hashlib.file_digest(apk.open('rb'),'sha256').hexdigest(),'signer='+signer,'ARM64; 16 KiB ZIP + ELF aligned')
