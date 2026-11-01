#!/usr/bin/env python3
"""Publish only VoClone's public feed, after anonymous APK/source hash verification."""
import base64, hashlib, json, os, pathlib, re, subprocess, time, urllib.error, urllib.request, zipfile

REPO='WynnDev-rill/VoClone'
PATH='updates/alpha.json'

def next_feed(current, candidate):
    if current is not None:
        if current['versionCode']>candidate['versionCode']:
            raise ValueError('Refusing to roll back the public update channel')
        if current['versionCode']==candidate['versionCode']:
            if current!=candidate:
                raise ValueError('Published versionCode is immutable; increment it for a new release')
            return False
    return True

def verify_public(url, file):
    expected=hashlib.file_digest(file.open('rb'),'sha256').hexdigest()
    for attempt in range(3):
        try:
            digest=hashlib.sha256();count=0
            with urllib.request.urlopen(url,timeout=30) as response:
                if response.status!=200:raise ValueError('Release asset is not public')
                while True:
                    block=response.read(65536)
                    if not block:break
                    count+=len(block)
                    if count>file.stat().st_size:raise ValueError('Public release asset exceeds expected size')
                    digest.update(block)
            if count!=file.stat().st_size or digest.hexdigest()!=expected:
                raise ValueError('Public release asset hash mismatch')
            return
        except (urllib.error.URLError,TimeoutError):
            if attempt==2:raise
            time.sleep(5)

def main():
    root=pathlib.Path(__file__).resolve().parents[1]
    metadata=json.loads((root/'distribution/alpha.json').read_text())
    version=metadata['versionName'];apk=f'VoClone-{version}-arm64.apk';source=f'VoClone-{version}-source.tar.gz'
    prefix=f'https://github.com/{REPO}/releases/download/v{version}/'
    assert metadata['apkUrl']==prefix+apk and metadata['packageName']=='id.wynn.voclone'
    assert metadata['signerSha256']==(root/'docs/signing-sha256.txt').read_text().strip()
    file=root/'distribution'/apk
    assert metadata['sha256']==hashlib.file_digest(file.open('rb'),'sha256').hexdigest()
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
    assert metadata['sourceCommit']==commit
    with zipfile.ZipFile(file) as archive:
        assert re.search(r'revision: "([a-f0-9]{40})"',archive.read('META-INF/version-control-info.textproto').decode()).group(1)==commit
        assert {name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}=={'arm64-v8a'}
    tools=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
    cert=subprocess.check_output([str(tools/'apksigner'),'verify','--print-certs',str(file)],text=True)
    assert re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)',cert).group(1)==metadata['signerSha256']
    identity=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(file)],text=True)
    assert f"package: name='id.wynn.voclone' versionCode='{metadata['versionCode']}' versionName='{version}'" in identity
    # No Authorization header: this proves users can download both APK and AGPL source.
    verify_public(prefix+apk,root/'distribution'/apk)
    verify_public(prefix+source,root/'distribution'/source)
    endpoint=f'repos/{REPO}/contents/{PATH}'
    existing=subprocess.run(['gh','api',endpoint+'?ref=main'],capture_output=True,text=True)
    sha=None;current=None
    if existing.returncode==0:
        content=json.loads(existing.stdout);sha=content['sha']
        current=json.loads(base64.b64decode(content['content']))
    elif 'HTTP 404' not in existing.stderr:
        raise RuntimeError('Cannot read update channel; refusing blind overwrite')
    if not next_feed(current,metadata):
        print('Public update feed already matches verified release');return
    payload=dict(message=f'Publish VoClone {version} update feed [skip ci]',branch='main',
        content=base64.b64encode((json.dumps(metadata,indent=2)+'\n').encode()).decode())
    if sha:payload['sha']=sha
    subprocess.run(['gh','api','--method','PUT',endpoint,'--input','-'],input=json.dumps(payload),text=True,check=True,stdout=subprocess.DEVNULL)
    print('Published verified public VoClone feed; no Wynn-Store writes')

if __name__=='__main__':main()
