#!/usr/bin/env python3
"""Owned v0.5.4 upgrade data, original signed 1.2.0 Kits and Chinese1.0.1.
Uses the production store; environment file sentinels prove byte retention, not executable Ubuntu.
"""
import argparse,hashlib,io,json,pathlib,subprocess,tarfile,tempfile,zipfile
p=argparse.ArgumentParser();p.add_argument('output',type=pathlib.Path);p.add_argument('--classpath',required=True);p.add_argument('--java',required=True);a=p.parse_args()
r=pathlib.Path(__file__).resolve().parents[2];old=r/'.delivery/sui-code-v0.5.4';a.output.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(old/'android-test-inputs.zip') as z:
 with tarfile.open(fileobj=io.BytesIO(z.read('payload.tar'))) as tar:files={m.name:tar.extractfile(m).read() for m in tar.getmembers() if m.isfile()}
prefix='files/sui-code/native-packages/'
with tempfile.TemporaryDirectory(prefix='sui-v055-upgrade-') as temporary:
 root=pathlib.Path(temporary);store=root/'store';store.mkdir()
 for name,data in files.items():
  if name.startswith(prefix):(store/name.removeprefix(prefix)).write_bytes(data)
 with zipfile.ZipFile(old/'official-packages.zip') as packages:
  for suffix in ['suic','suik']:
   for language in ['Java','C_CPP','Python']:
    file=root/(language+'.'+suffix);file.write_bytes(packages.read(file.name))
    subprocess.run([a.java,'-cp',a.classpath,'FixtureStoreCli',str(store),str(file)],check=True)
 subprocess.run([a.java,'-cp',a.classpath,'FixtureStoreCli',str(store),str(old/'sui-code-zh-hans-1.0.1.suik')],check=True)
 state=json.loads((store/'state.json').read_text())
 for language in ['java','ccpp','python']:assert state['dev.suicode.'+language+'.kit']['version']=='1.2.0',state
 assert state['dev.suicode.localization.zh-hans']['version']=='1.0.1'
 legacy={k:v for k,v in state.items() if k!='dev.suicode.localization.zh-hans'}
 (a.output/'legacy-state.json').write_text(json.dumps(legacy,indent=2)+'\n')
 files.update({prefix+p.name:p.read_bytes() for p in store.iterdir() if p.is_file()})
with tarfile.open(a.output/'payload.tar','w') as tar:
 for name,data in files.items():
  member=tarfile.TarInfo(name);member.size=len(data);member.mode=0o600;tar.addfile(member,io.BytesIO(data))
(a.output/'payload-sha256.json').write_text(json.dumps({name:hashlib.sha256(data).hexdigest() for name,data in files.items()},indent=2)+'\n')
print('Owned v0.5.4 fixture:',len(files),'retained files; real three IDE Kit1.2.0 packages and Chinese1.0.1; environment sentinels only')
