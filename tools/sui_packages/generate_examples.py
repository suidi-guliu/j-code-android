"""Build runnable development templates using the same protocol/resources as the Android host."""
import json,pathlib,importlib.util,shutil
root=pathlib.Path(__file__).resolve().parents[2]
base=root/'docs/specifications/examples'
spec=importlib.util.spec_from_file_location('pack',root/'tools/sui_packages/pack.py');pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
def write(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(pack.canonical(data))
def template(name,kind,components,permissions,dependencies=None):
 folder=base/name;prefix='plugin' if kind=='suic' else 'ide'
 m={'id':'dev.suicode.example.'+name,'name':name,'description':'Local development example; not an official publisher','author':'sui-code examples','version':'0.1.0','kind':kind,'api':{'min':2,'max':2},'permissions':permissions,'dependencies':dependencies or [],'components':[]}
 for cid,typ,data in components:
  entry=f'{prefix}/{cid}.json';write(folder/entry,data);m['components'].append({'id':cid,'type':typ,'entry':entry})
 write(folder/'manifest.json',m);pack.build(folder,base/'packages'/f'{name}.{kind}')
(base/'packages').mkdir(exist_ok=True)
for name,language in [('java','Java'),('c-cpp','C_CPP'),('python','Python')]:
 source=root/'native-library/src'/language/'suic';folder=base/name
 m=json.loads((source/'manifest.json').read_text());m.update(id='dev.suicode.example.'+name,name=name,version='0.1.0',api={'min':2,'max':2},author='sui-code examples',description='Runnable basic language development template; review process.run before enabling')
 for child in source.iterdir():
  if child.is_dir():shutil.copytree(child,folder/child.name,dirs_exist_ok=True)
 write(folder/'manifest.json',m);pack.build(folder,base/'packages'/f'{name}.suic')
 template(name+'-kit','suik',[('overview','panel',{'title':name+' tools','text':'Uses the matching enabled native IDE Kit language services.','status':'ready'}),('lint','lint',{'operation':'LINT','arguments':{'scope':'current-file'}})],['ui.panel','file.read'],[{'id':'dev.suicode.example.'+name,'min':'0.1.0','max':'1.0.0'}])
template('minimal','suic',[('language','language',{'languageId':'example','extensions':['example'],'priority':0})],[])
template('minimal-kit','suik',[('overview','panel',{'title':'Example','text':'Hello from an independent native Kit','status':'ready'})],['ui.panel'])
template('special-feature','suik',[
 ('analysis','command',{'operation':'PROJECT_READ','arguments':{'action':'statistics'}}),
 ('statistics','panel',{'title':'Project statistics','text':'Read the selected project or persistent Internal Files.','status':'ready','nodes':[
  {'id':'refresh','type':'button','label':'Refresh statistics','command':'analysis'},
  {'id':'counts','type':'table','columns':['Name','Files','Bytes'],'rows':[['${result:analysis.name}','${result:analysis.files}','${result:analysis.bytes}']]}
 ]})],['ui.panel','project.read'])
template('illegal-plugin','suic',[('forbidden','panel',{'title':'Forbidden plugin panel','text':'Must be rejected','status':'ready'})],['ui.panel'])
