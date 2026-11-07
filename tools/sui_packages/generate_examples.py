import json,pathlib,sys,importlib.util
root=pathlib.Path(__file__).resolve().parents[2]
base=root/'docs/specifications/examples'
spec=importlib.util.spec_from_file_location('pack',root/'tools/sui_packages/pack.py');pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
def write(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(pack.canonical(data))
def template(name,kind,components,permissions,dependencies=[]):
 folder=base/name; prefix='plugin' if kind=='suic' else 'ide'
 m={'id':'dev.suicode.example.'+name.replace('_','-'),'name':name,'description':'Development example; not an official installed language package','author':'sui-code examples','version':'0.1.0','kind':kind,'api':{'min':1,'max':1},'permissions':permissions,'dependencies':dependencies,'components':[]}
 for cid,typ,data in components:
  entry=f'{prefix}/{cid}.json';write(folder/entry,data);m['components'].append({'id':cid,'type':typ,'entry':entry})
 write(folder/'manifest.json',m)
 pack.build(folder,base/'packages'/f'{name}.{kind}')
(base/'packages').mkdir(exist_ok=True)
for name,exts,env,tools,provider in [('java',['java'],'java',['java','javac','jshell'],'temurin'),('c-cpp',['c','cpp','cc','cxx','C'],'cpp',['gcc','g++'],'ubuntu-apt'),('python',['py'],'python',['python3'],'ubuntu-apt')]:
 language=name
 recipe={'languageId':language,'environmentId':env,'prepare':[],'run':['${tool}','${source}']}
 template(name,'suic',[
  ('language','language',{'languageId':language,'extensions':exts,'priority':50}),
  ('environment','environment',{'environmentId':env,'provider':provider,'configuration':f'/opt/sui-code/{env}.env','tools':tools}),
  ('run','simpleRun',recipe)],['file.read','process.run'])
 template(name+'-kit','suik', [('overview','panel',{'title':name+' tools','text':'Example panel; backend capability must be available before enabling commands.','status':'ready'}),('lint','lint',{'operation':'LINT','arguments':{'scope':'current-file'}})],['ui.panel','file.read'],[{'id':'dev.suicode.example.'+name,'min':'0.1.0','max':'1.0.0'}])
template('minimal','suic',[('language','language',{'languageId':'example','extensions':['example'],'priority':0})],[])
template('minimal-kit','suik',[('overview','panel',{'title':'Example','text':'Hello','status':'ready'})],['ui.panel'])
template('special-feature','suik',[('analysis','workflow',{'operation':'LINT','arguments':{'analysis':'project-statistics'}}),('statistics','panel',{'title':'Statistics','text':'No results yet','status':'empty'})],['ui.panel','file.read'])
template('illegal-plugin','suic',[('forbidden','panel',{'title':'Forbidden plugin panel','text':'Must be rejected','status':'ready'})],['ui.panel'])
