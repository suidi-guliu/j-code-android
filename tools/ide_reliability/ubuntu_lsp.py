#!/usr/bin/env python3
"""Real JDT LS/clangd/pylsp requests under modern Termux PRoot using APK-generated paths.
No canned completion responses. This is Ubuntu x64 evidence, never ARM64 tablet acceptance.
"""
import argparse,hashlib,json,os,pathlib,re,shlex,signal,subprocess,sys,time,traceback
p=argparse.ArgumentParser();p.add_argument('--library',type=pathlib.Path,default=pathlib.Path('/library'));p.add_argument('--evidence',type=pathlib.Path,default=pathlib.Path('/evidence'));p.add_argument('--only-language',choices=['Java','C_CPP','Python']);p.add_argument('--protocol',type=pathlib.Path,default=pathlib.Path('/opt/v054-tests/java_lsp_dap.py'));a=p.parse_args()
import importlib.util
spec=importlib.util.spec_from_file_location('protocol',a.protocol);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);Framed=module.Framed
plans=json.loads((a.evidence/'task-path-plans.json').read_text());mounts={b['guest']:b['host'] for v in plans.values() for b in v['binds']};results=[]
env=dict(os.environ,HOME='/home/ubuntu',LANG='C.UTF-8',LC_ALL='C.UTF-8',PATH='/home/ubuntu/bin:'+os.environ['PATH'],JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/run/java-cacerts')
def case(name,fn):
 start=time.monotonic()
 try:detail=fn();row={'name':name,'result':'PASS','details':detail}
 except Exception:row={'name':name,'result':'FAIL','error':traceback.format_exc()}
 row['seconds']=round(time.monotonic()-start,3);results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True)
 (a.evidence/('lsp-'+a.only_language+'-results.json' if a.only_language else 'lsp-results.json')).write_text(json.dumps({'platform':'Ubuntu24.04 amd64, actual Termux PRoot and real language servers; not ARM64','cases':results},ensure_ascii=False,indent=2)+'\n')

def run_language(language,variant):
 host=a.evidence/'task-projects'/(language+' '+variant);guest=plans[str(host)]['guest'];descriptor=json.loads((a.library/language/'suik/adapters/lsp.json').read_text())
 cmd=descriptor['runCommand'].replace('${workspaceId}',hashlib.sha256(str(host).encode()).hexdigest()).replace('${workspaceRoot}',shlex.quote(guest))
 args=['proot','--kill-on-exit','--link2symlink','-r','/']
 for target,source in sorted(mounts.items(),key=lambda item:item[0].count('/')):args+=['-b',source+':'+target]
 args+=['-w',guest,'/bin/bash','--noprofile','--norc','-c',cmd]
 log=a.evidence/('lsp-'+language+'-'+('plain' if variant=='plain' else 'unicode')+'.log')
 proc=subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=log.open('wb'),env=env,start_new_session=True)
 wire=Framed(proc.stdout,proc.stdin);details=[];version=1
 def uri(relative):return pathlib.Path(guest,relative).as_uri()
 def opened(relative,text,language_id):wire.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':uri(relative),'languageId':language_id,'version':1,'text':text}}})
 def change(relative,text):
  nonlocal version
  version+=1;wire.send({'jsonrpc':'2.0','method':'textDocument/didChange','params':{'textDocument':{'uri':uri(relative),'version':version},'contentChanges':[{'text':text}]}})
 def point(text,marker):
  pos=text.index(marker)+len(marker);before=text[:pos];return {'line':before.count('\n'),'character':len(before.rsplit('\n',1)[-1].encode('utf-16-le'))//2}
 def completion(relative,text,marker,expected):
  change(relative,text);position=point(text,marker);deadline=time.monotonic()+60;attempts=0;begin=time.monotonic()
  while True:
   attempts+=1;value=wire.rpc('textDocument/completion',{'textDocument':{'uri':uri(relative)},'position':position,'context':{'triggerKind':1}})
   items=value.get('items',[]) if isinstance(value,dict) else value or []
   labels=[str(i.get('filterText',i.get('label',''))) for i in items]
   matched=[label for label in labels if (re.match(r'\s*([A-Za-z_]\w*)',label).group(1) if re.match(r'\s*([A-Za-z_]\w*)',label) else '')==expected]
   if matched:break
   assert time.monotonic()<deadline,(language,expected,labels[:30],value)
   time.sleep(.4)
  details.append({'feature':'completion','expected':expected,'attempts':attempts,'ms':round((time.monotonic()-begin)*1000),'sample':labels[:8],'matched':matched[:3]})
 def definition(relative,text,marker,expected):
  change(relative,text);position=point(text,marker);position['character']-=1
  deadline=time.monotonic()+30
  while True:
   value=wire.rpc('textDocument/definition',{'textDocument':{'uri':uri(relative)},'position':position});locations=value if isinstance(value,list) else [value] if value else []
   targets=[v.get('uri',v.get('targetUri','')) for v in locations]
   if any(expected in target for target in targets):break
   assert time.monotonic()<deadline,(language,expected,targets);time.sleep(.4)
  details.append({'feature':'definition','expected':expected,'targets':targets})
 try:
  options=json.loads(descriptor.get('initializationOptions','{}'))
  def expand(v):
   if isinstance(v,str):return v.replace('${workspaceRoot}',guest)
   if isinstance(v,list):return [expand(x) for x in v]
   if isinstance(v,dict):return {k:expand(x) for k,x in v.items()}
   return v
  init_start=time.monotonic()
  value=wire.rpc('initialize',{'processId':os.getpid(),'rootUri':pathlib.Path(guest).as_uri(),'workspaceFolders':[{'uri':pathlib.Path(guest).as_uri(),'name':language}],'capabilities':{'textDocument':{'completion':{'completionItem':{'snippetSupport':True}},'definition':{'linkSupport':False}},'workspace':{'configuration':True,'workspaceFolders':True}},'initializationOptions':expand(options)})
  assert value.get('capabilities',{}).get('completionProvider') is not None,value
  details.append({'feature':'initialize','ms':round((time.monotonic()-init_start)*1000),'root':guest})
  wire.send({'jsonrpc':'2.0','method':'initialized','params':{}})
  if language=='Java':
   relative='src/main/java/app/Main.java';original=(host/relative).read_text();helper='package app; public class Helper { public static String greeting() { return "hello"; } }\n'
   (host/'src/main/java/app/Helper.java').write_text(helper);opened('src/main/java/app/Helper.java',helper,'java');opened(relative,original,'java')
   prefix='package app;\nimport java.util.*;\npublic class Main { public static void main(String[] args) {\n'
   completion(relative,prefix+' System.\n}}\n','System.','out')
   completion(relative,prefix+' System.out.pr\n}}\n','System.out.pr','println')
   completion(relative,prefix+' ArrayL\n}}\n','ArrayL','ArrayList')
   completion(relative,prefix+' Helper.gr\n}}\n','Helper.gr','greeting')
   definition(relative,prefix+' Helper.greeting();\n}}\n','Helper.greeting','Helper.java')
   change(relative,original);wire.send({'jsonrpc':'2.0','method':'textDocument/didSave','params':{'textDocument':{'uri':uri(relative)},'text':original}})
   wire.send({'jsonrpc':'2.0','method':'textDocument/didClose','params':{'textDocument':{'uri':uri(relative)}}});opened(relative,original,'java')
   completion(relative,prefix+' System.out.pr\n}}\n','System.out.pr','println')
  elif language=='C_CPP':
   relative='src/main.cpp';original=(host/relative).read_text();opened(relative,original,'cpp')
   completion(relative,'#include <iostream>\n#include "math.hpp"\nint main(){ std::co\n}\n','std::co','cout')
   completion(relative,'#include <iostream>\n#include "math.hpp"\nint main(){ ad\n}\n','ad','add')
   definition(relative,original,'add','math')
   c='src/c_math.c';c_original=(host/c).read_text();opened(c,c_original,'c')
   completion(c,'#include <stdio.h>\nint main(){ prin\n}\n','prin','printf')
   change(relative,original);wire.send({'jsonrpc':'2.0','method':'textDocument/didClose','params':{'textDocument':{'uri':uri(relative)}}});opened(relative,original,'cpp')
   completion(relative,'#include "math.hpp"\nint main(){ ad\n}\n','ad','add')
  else:
   relative='main.py';original=(host/relative).read_text();opened(relative,original,'python')
   completion(relative,'import pathlib\npathlib.Pa\n','pathlib.Pa','Path')
   definition(relative,original+'\nadd(1, 2)\n','\nadd','main.py')
   completion(relative,'import pathlib\npathlib.Pa\n','pathlib.Pa','Path')
   change(relative,original);wire.send({'jsonrpc':'2.0','method':'textDocument/didClose','params':{'textDocument':{'uri':uri(relative)}}});opened(relative,original,'python')
   completion(relative,'import pathlib\npathlib.Pa\n','pathlib.Pa','Path')
  wire.rpc('shutdown',None);wire.send({'jsonrpc':'2.0','method':'exit','params':None})
  return details
 finally:
  try:os.killpg(proc.pid,signal.SIGTERM)
  except ProcessLookupError:pass
  try:proc.wait(timeout=5)
  except subprocess.TimeoutExpired:os.killpg(proc.pid,signal.SIGKILL);proc.wait()

for language in ([a.only_language] if a.only_language else ['Java','C_CPP','Python']):
 for variant in ['plain',"中文 空格 + dollar$ quote' semi; colon:"]:
  case(language+' '+variant+' actual LSP',lambda language=language,variant=variant:run_language(language,variant))
assert all(r['result']=='PASS' for r in results),str(sum(r['result']=='FAIL' for r in results))+' real LSP scenarios failed'
print('PASS',len(results),'real LSP project scenarios',flush=True)
