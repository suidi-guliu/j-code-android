#!/usr/bin/env python3
"""Use immutable v0.5.3 API3 fixture bytes plus a real installed 1.0.0 Chinese Kit.
Store writes are produced by the actual compiled Core JVM API. Runtime sentinels are NOT runtimes.
"""
from pathlib import Path
import argparse,zipfile,tarfile,io,hashlib,json,subprocess,tempfile
r=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--classpath',required=True);p.add_argument('--java',required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(r/'.delivery/sui-code-v0.5.3-zh-hans/android-test-inputs.zip') as z:
 with tarfile.open(fileobj=io.BytesIO(z.read('payload.tar'))) as tar:files={m.name:tar.extractfile(m).read() for m in tar.getmembers() if m.isfile()}
(a.output/'legacy-state.json').write_bytes(files['files/sui-code/native-packages/state.json'])
with tempfile.TemporaryDirectory(prefix='sui-upgrade-fixture-') as temp:
 store=Path(temp)/'store';store.mkdir()
 prefix='files/sui-code/native-packages/'
 for name,data in files.items():
  if name.startswith(prefix):(store/name.removeprefix(prefix)).write_bytes(data)
 subprocess.run([a.java,'-cp',a.classpath,'FixtureStoreCli',str(store),str(r/'localization/releases/sui-code-zh-hans-1.0.0.suik')],check=True)
 files.update({prefix+p.name:p.read_bytes() for p in store.iterdir() if p.is_file()})
files['shared_prefs/sui-native-ui-language.xml']=b'<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="selection-v1">dev.suicode.localization.zh-hans/zh-Hans</string><string name="unrelated-user-setting">preserve</string></map>'
files['files/distros/fixture-v05/rootfs/opt/sui-code/cpp.env']=b'# retained C++ configuration sentinel; not an executable toolchain\n'
with tarfile.open(a.output/'payload.tar','w') as tar:
 for name,data in files.items():
  m=tarfile.TarInfo(name);m.size=len(data);m.mode=0o600;tar.addfile(m,io.BytesIO(data))
(a.output/'payload-sha256.json').write_text(json.dumps({name:hashlib.sha256(data).hexdigest() for name,data in files.items()},indent=2)+'\n')
print('Owned v0.5.3 upgrade fixture:',len(files),'real archive/config/file inputs; runtime sentinels only')
