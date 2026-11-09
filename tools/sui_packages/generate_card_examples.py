from pathlib import Path
import json,sys,copy
r=Path(__file__).resolve().parents[2];sys.path.insert(0,str(r/'tools/sui_packages'));from pack import build,canonical
base=r/'docs/specifications/examples'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(canonical(d))
# Real third-party capability slots reference real production adapter/operation shapes.
src=base/'third-party-language-kit';src.mkdir(exist_ok=True)
manifest={'id':'org.example.python.ide','name':'Example Python IDE Kit','author':'Example third-party developer','description':'Declarative Python language card using the installed pylsp; no bundled runtime.','version':'1.0.0','kind':'suik','api':{'min':4,'max':4},'dependencies':[{'id':'dev.suicode.python.basic','min':'1.0.0','max':'2.0.0'}],'permissions':['file.read','file.write','process.run','language.lsp'],'components':[{'id':'server','type':'lsp','entry':'ide/server.json'},{'id':'requirements','type':'setup','entry':'ide/requirements.json'}],'card':{'version':1,'category':'language-ide-kit','basicPlugin':'dev.suicode.python.basic','languageIds':['python'],'slots':{'runtime':'runtime','lsp':'server'}}}
adapter=json.loads((r/'native-library/src/Python/suik/adapters/lsp.json').read_text());adapter['serverId']='example-pylsp';adapter['name']='Example Python LSP';write(src/'ide/server.json',{'operation':'LSP_START','arguments':adapter})
req=json.loads((r/'native-library/src/Python/suik/adapters/setup.json').read_text());req['requirements']=[x for x in req['requirements'] if x['id'] in ['runtime','pylsp']];write(src/'ide/requirements.json',req);write(src/'manifest.json',manifest)
# General feature executes only an existing permission-gated Core backend.
src=base/'third-party-utility-card';src.mkdir(exist_ok=True)
manifest={'id':'org.example.project.tools','name':'Project Tools','author':'Example third-party developer','description':'Project statistics through the host Project API.','version':'1.0.0','kind':'suik','api':{'min':4,'max':4},'dependencies':[],'permissions':['project.read'],'components':[{'id':'statistics','type':'command','entry':'ide/statistics.json'}],'card':{'version':1,'category':'utility','fields':[{'id':'scope','label':'Scope','type':'text','value':'Current project only'},{'id':'documentation','label':'Documentation','type':'link','value':'https://github.com/suidi-guliu/j-code-android'}],'actions':['statistics']}}
write(src/'manifest.json',manifest);write(src/'ide/statistics.json',{'operation':'PROJECT_READ','arguments':{'action':'statistics'}})
# Existing minimal resource example gains a genuine separately recognized localization card.
src=base/'localization-card';src.mkdir(exist_ok=True)
old=base/'zh-hans-kit';import shutil
shutil.copytree(old,src,dirs_exist_ok=True);manifest=json.loads((src/'manifest.json').read_text());manifest['id']='org.example.localization.zh-hans';manifest['card']={'version':1,'category':'localization','fields':[{'id':'locale','label':'Language','type':'text','value':'简体中文 · zh-Hans'}]};write(src/'manifest.json',manifest)
for name in ['third-party-language-kit','third-party-utility-card','localization-card']:print(build(base/name,base/'packages'/f'{name}.suik'))
