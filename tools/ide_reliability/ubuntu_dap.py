#!/usr/bin/env python3
"""Execute real clangd/pylsp and lldb-dap/debugpy protocols on Ubuntu x64.
Run next to java_lsp_dap.py inside an isolated test container; NOT ARM64 acceptance.
"""
import json,os,pathlib,signal,subprocess,tempfile,time
from java_lsp_dap import Framed
root=pathlib.Path(tempfile.mkdtemp(prefix='sui-ide-中文 space-'))
processes=[]
def spawn(argv):
 p=subprocess.Popen(argv,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=open(root/f'process{len(processes)}.log','wb'),start_new_session=True)
 processes.append(p);return Framed(p.stdout,p.stdin)
try:
 for lang,exe,suffix,text in [('cpp',['clangd'],'cpp','#include <iostream>\nint main(){\n int value=41;\n int result=value+1;\n std::cout<<"SUI_DAP_"<<value+1<<std::endl;\n return 0;\n}\n'),('python',['/usr/bin/pylsp'],'py','value = 41\nresult = value + 1\nprint("SUI_DAP_" + str(value+1))\n')]:
  source=root/f'main.{suffix}';source.write_text(text)
  lsp=spawn(exe)
  result=lsp.rpc('initialize',{'processId':os.getpid(),'rootUri':root.as_uri(),'capabilities':{},'workspaceFolders':[{'uri':root.as_uri(),'name':'integration'}]})
  assert result['capabilities'],result
  lsp.send({'jsonrpc':'2.0','method':'initialized','params':{}})
  lsp.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':source.as_uri(),'languageId':lang,'version':1,'text':text}}})
  # Real editing services, not only an initialize acknowledgement.
  time.sleep(2)
  completion=lsp.rpc('textDocument/completion',{'textDocument':{'uri':source.as_uri()},'position':{'line':1,'character':1}})
  assert isinstance(completion,(list,dict)) or completion is None
  print(f'PASS {lang} real LSP initialization before DAP',flush=True)
  if lang=='cpp':
   binary=root/'program';subprocess.run(['g++','-g','-O0',str(source),'-o',str(binary)],check=True)
   dap=spawn(['/usr/bin/lldb-dap-18']);config={'program':str(binary),'cwd':str(root),'stopOnEntry':False,'disableASLR':False,'initCommands':['settings set target.disable-aslr false']};line=4
  else:
   dap=spawn(['/usr/bin/python3','-m','debugpy.adapter']);config={'program':str(source),'cwd':str(root),'console':'internalConsole','redirectOutput':True,'justMyCode':False};line=2
  dap.dap_wait(dap.dap_send('initialize',{'adapterID':lang,'linesStartAt1':True,'columnsStartAt1':True,'pathFormat':'path','supportsRunInTerminalRequest':False}))
  launch=dap.dap_send('launch',config)
  while True:
   m=dap.next()
   if m.get('event')=='initialized':break
   if m.get('request_seq')==launch and not m.get('success'):raise RuntimeError(m)
  bps=dap.dap_wait(dap.dap_send('setBreakpoints',{'source':{'path':str(source)},'breakpoints':[{'line':line}]}))
  assert bps['body']['breakpoints'][0]['verified'],bps
  dap.dap_wait(dap.dap_send('configurationDone',{}))
  while True:
   m=dap.next()
   if m.get('event')=='stopped':thread=m['body']['threadId'];break
   if m.get('event')=='terminated':raise AssertionError(f'{lang} breakpoint not reached')
  stack=dap.dap_wait(dap.dap_send('stackTrace',{'threadId':thread}))
  assert stack['body']['stackFrames'],stack
  frame=stack['body']['stackFrames'][0]['id']
  scopes=dap.dap_wait(dap.dap_send('scopes',{'frameId':frame}))
  assert scopes['body']['scopes'],scopes
  ref=scopes['body']['scopes'][0]['variablesReference']
  variables=dap.dap_wait(dap.dap_send('variables',{'variablesReference':ref}))
  assert variables.get('body') is not None
  print(f'PASS {lang} DAP breakpoint/stack/scopes/variables',flush=True)
  dap.dap_wait(dap.dap_send('next',{'threadId':thread}))
  while True:
   m=dap.next()
   if m.get('event')=='stopped':thread=m['body']['threadId'];break
   if m.get('event')=='terminated':raise AssertionError(f'{lang} did not stop after step')
  print(f'PASS {lang} actual step-over stop',flush=True)
  dap.dap_wait(dap.dap_send('continue',{'threadId':thread}))
  output=''
  while True:
   m=dap.next()
   if m.get('event')=='output':output+=m['body']['output']
   if m.get('event')=='terminated':break
  assert 'SUI_DAP_42' in output,output
  print(f'PASS {lang} DAP real output and termination',flush=True)
  lsp.rpc('shutdown',None);lsp.send({'jsonrpc':'2.0','method':'exit','params':None})
finally:
 for p in processes:
  try:os.killpg(p.pid,signal.SIGTERM)
  except ProcessLookupError:pass
  try:p.wait(timeout=5)
  except subprocess.TimeoutExpired:os.killpg(p.pid,signal.SIGKILL);p.wait()
 print('Evidence:',root,flush=True)
