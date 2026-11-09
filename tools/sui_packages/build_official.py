#!/usr/bin/env python3
"""Build first-party packages from reviewed resources. Requires an external P-256 signing key."""
import argparse, pathlib, json, hashlib, shutil, zipfile, io, subprocess, tempfile
from pack import build, canonical
root=pathlib.Path(__file__).resolve().parents[2]
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(canonical(data))
def main(key):
 catalog=[]
 def package(name,kind,language_components,permissions,deps=[]):
  src=root/'native-library/src'/name/kind
  prefix='plugin' if kind=='suic' else 'ide'
  manifest={'id':'dev.suicode.'+name.lower().replace('_','')+('.basic' if kind=='suic' else '.kit'),'name':name+(' Basic Plugin' if kind=='suic' else ' IDE Kit'),'author':'sui-code','description':'Native language execution' if kind=='suic' else 'Language services, project tools and development workflows','version':'1.1.0' if kind=='suic' else '1.2.0','kind':kind,'api':{'min':3,'max':3} if kind=='suic' else {'min':4,'max':4},'dependencies':deps,'permissions':permissions,'components':[]}
  for cid,typ,data in language_components:
   path=f'{prefix}/{cid}.json';write(src/path,data);manifest['components'].append({'id':cid,'type':typ,'entry':path})
  if kind=='suik': manifest['card']={'version':1,'category':'language-ide-kit','basicPlugin':deps[0]['id'],'languageIds':{'Java':['java'],'C_CPP':['c','cpp'],'Python':['python']}[name],'slots':{'runtime':'runtime','lsp':'language-server','dap':'debugger','build':'build','test':'test','run':'run-project','format':'format','lint':'diagnostics'},'actions':['overview']}
  referenced={c['entry'] for c in manifest['components']}
  for old in (src/prefix).glob('*.json'):
   if old.relative_to(src).as_posix() not in referenced:old.unlink()
  write(src/'manifest.json',manifest)
  out=root/'native-library/v1'/f'{name}.{kind}'
  if kind=='suic' and out.exists():
   with tempfile.TemporaryDirectory() as td:
    candidate=pathlib.Path(td)/out.name;build(src,candidate)
    with zipfile.ZipFile(io.BytesIO(candidate.read_bytes()[20:])) as a, zipfile.ZipFile(io.BytesIO(out.read_bytes()[20:])) as b:
     unchanged=a.read('checksums.sha256')==b.read('checksums.sha256')
   if not unchanged: raise RuntimeError('Basic payload changed: explicitly version and review it before replacing an installed package')
   result={'bytes':out.stat().st_size,'sha256':hashlib.sha256(out.read_bytes()).hexdigest()}
  else: result=build(src,out,key)
  with zipfile.ZipFile(io.BytesIO(out.read_bytes()[20:])) as z:
   signature=json.loads(z.read('signature.sig'))
  public=(root/'native-library/official-p256.der').read_bytes()
  catalog.append({'id':manifest['id'],'name':manifest['name'],'version':manifest['version'],'kind':kind,'url':f'https://raw.githubusercontent.com/suidi-guliu/j-code-android/sui-code/v0.5.4-ide-reliability-management/native-library/v1/{out.name}','bytes':result['bytes'],'sha256':result['sha256'],'author':manifest['author'],'description':manifest['description'],'dependencies':deps,'changeLog':'v0.5.4: shared verified mounts, optional IDE components, runtime ownership and declarative management cards.','signer':hashlib.sha256(public).hexdigest(),'signature':signature['signature'],'api':manifest['api'],'cardCategory':manifest.get('card',{}).get('category','general')})
  return manifest['id']
 for name,languages,env,tools,provider,server in [
  ('Java',[('java',['java'])],'java',['java','javac','jshell'],'temurin','sui-jdtls'),
  ('C_CPP',[('c',['c']),('cpp',['cpp','cc','cxx','C'])],'cpp',['gcc','g++'],'ubuntu-apt','sui-clangd'),
  ('Python',[('python',['py'])],'python',['python3'],'ubuntu-apt','sui-pylsp')]:
  components=[('environment','environment',{'environmentId':env,'provider':provider,'configuration':f'/opt/sui-code/{env}.env','tools':tools})]
  for language,exts in languages:
   components.extend([(language,'language',{'languageId':language,'extensions':exts,'priority':50}),('run-'+language,'simpleRun',{'languageId':language,'environmentId':env,'prepare':[],'run':['/bin/bash','${resource:resources/run-'+language+'.sh}','${source}']})])
  basic=package(name,'suic',components,['file.read','process.run'])
  package(name,'suik',[
   ('prerequisites','setup',json.loads((root/'native-library/src'/name/'suik/adapters/setup.json').read_text())),
   ('language-server','lsp',{'operation':'LSP_START','arguments':json.loads((root/'native-library/src'/name/'suik/adapters/lsp.json').read_text())}),
   ('debugger','dap',{'operation':'DAP_START','arguments':json.loads((root/'native-library/src'/name/'suik/adapters/dap.json').read_text())}),
   ('diagnostics','lint',{'operation':'LINT','arguments':{'serverId':server}}),
   ('format','format',{'operation':'FORMAT','arguments':{'serverId':server}}),
   ('build','build',{'operation':'BUILD','arguments':{'argv':json.dumps(['/bin/bash','${resource:resources/project-task.sh}','build','${project}'])}}),
   ('test','test',{'operation':'TEST','arguments':{'argv':json.dumps(['/bin/bash','${resource:resources/project-task.sh}','test','${project}'])}}),
   ('run-project','command',{'operation':'SIMPLE_RUN','arguments':{'argv':json.dumps(['/bin/bash','${resource:resources/project-task.sh}','run','${project}'])}}),
   ('statistics','command',{'operation':'PROJECT_READ','arguments':{'action':'statistics'}}),
   *[('debug-'+action.lower(),'command',{'operation':'DAP_START','arguments':{'action':action}}) for action in ['state','continue','pause','next','stepIn','stepOut','stop']],
   ('project','project',{'operation':'PROJECT_READ','arguments':{'role':'template','markers':json.dumps({'Java':['pom.xml'],'C_CPP':['CMakeLists.txt'],'Python':['main.py']}[name]),'title':name+' Project','description':'A multi-file project with a real automated test','files':json.dumps({p.relative_to(root/'native-library/src'/name/'suik/resources/templates').as_posix():'resources/templates/'+p.relative_to(root/'native-library/src'/name/'suik/resources/templates').as_posix() for p in (root/'native-library/src'/name/'suik/resources/templates').rglob('*') if p.is_file()})}}),
   ('overview','panel',{'title':name+' IDE','text':'Language services use the selected Ubuntu environment. Runtime and tool availability are checked before operations.','status':'ready','nodes':[{'id':'workspace-tools','type':'row','children':[{'id':'refresh-statistics','type':'button','label':'Read project statistics','command':'statistics'},{'id':'build-action','type':'button','label':'Build','command':'build'},{'id':'test-action','type':'button','label':'Test','command':'test'},{'id':'run-action','type':'button','label':'Run Project','command':'run-project'}]},{'id':'debug-controls','type':'row','children':[{'id':'debug-'+action.lower(),'type':'button','label':label,'command':'debug-'+action.lower()} for action,label in [('state','Refresh debug state'),('continue','Continue'),('pause','Pause'),('next','Step over'),('stepIn','Step into'),('stepOut','Step out'),('stop','Stop debug')]]},{'id':'debug-state-value','type':'text','text':'Debug: ${result:debug-state.state}'},{'id':'statistics-table','type':'table','columns':['Project','Files','Bytes'],'rows':[['${result:statistics.name}','${result:statistics.files}','${result:statistics.bytes}']]}]})
  ],['language.lsp','file.read','file.write','process.run','debug.dap','project.read','ui.panel'],[{'id':basic,'min':'1.0.0','max':'2.0.0'}])
 # Directory approval authenticates this download hash, not the unsigned author's package signature.
 zh=root/'localization/zh-Hans';manifest=json.loads((zh/'manifest.json').read_text())
 out=root/'localization/releases/sui-code-zh-hans-1.0.1.suik'
 result=build(zh,out)
 catalog.append({'id':manifest['id'],'name':manifest['name'],'version':manifest['version'],'kind':'suik','url':'https://raw.githubusercontent.com/suidi-guliu/j-code-android/sui-code/v0.5.4-ide-reliability-management/localization/releases/'+out.name,'bytes':result['bytes'],'sha256':result['sha256'],'author':manifest['author'],'description':manifest['description'],'dependencies':[],'changeLog':'v1.0.1: management translations and independent localization card; original translations preserved.','signer':'','signature':'','api':manifest['api'],'cardCategory':'localization'})
 write(root/'native-library/v1/index.json',{'format':1,'packages':catalog})
 with tempfile.TemporaryDirectory() as tmp:
  message=pathlib.Path(tmp)/'catalog'
  message.write_bytes(b'SUI-LIBRARY-V1\n'+(root/'native-library/v1/index.json').read_bytes())
  signature=subprocess.check_output(['openssl','dgst','-sha256','-sign',str(key),str(message)])
  (root/'native-library/v1/index.sig').write_bytes(signature)
 assets=root/'app/src/main/assets/sui-native';assets.mkdir(parents=True,exist_ok=True)
 shutil.copyfile(root/'native-library/official-p256.der',assets/'official-p256.der')
 shutil.copyfile(root/'native-library/v1/index.json',assets/'index.json')
 shutil.copyfile(root/'native-library/v1/index.sig',assets/'index.sig')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--key',required=True);args=p.parse_args();main(args.key)
