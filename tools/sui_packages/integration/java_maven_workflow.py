#!/usr/bin/env python3
"""Actual Maven project import + source definition + Java DAP, Ubuntu x64 only."""
import json,os,pathlib,shutil,signal,socket,subprocess,tempfile,time,urllib.parse
from java_lsp_dap import Framed
base=pathlib.Path(tempfile.mkdtemp(prefix='sui-maven-中文 + #-',dir='/evidence'))
project=base/'project';shutil.copytree('/repo/native-library/src/Java/suik/resources/templates',project)
source=project/'src/main/java/app/Main.java';helper=source.with_name('Calc.java')
source.write_text('package app;\npublic final class Main {\n public static int add(int a,int b) { return Calc.add(a,b); }\n public static void main(String[] args) {\n  int value=41;\n  System.out.println("SUI_MAVEN_DAP_"+(value+1));\n }\n}\n')
helper.write_text('package app;\npublic final class Calc { public static int add(int a,int b) { return a+b; } }\n')
state=pathlib.Path(tempfile.mkdtemp(prefix='sui-maven-jdt-state-'))
config=state/'configuration';shutil.copytree('/opt/sui-code/ide/java/current/config_linux',config)
launcher=next(pathlib.Path('/opt/sui-code/ide/java/current/plugins').glob('org.eclipse.equinox.launcher_*.jar'))
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
env={**os.environ,'JAVA_HOME':'/jdk','JAVA_TOOL_OPTIONS':f'-Dhttp.proxyHost={proxy.hostname} -Dhttp.proxyPort={proxy.port} -Dhttps.proxyHost={proxy.hostname} -Dhttps.proxyPort={proxy.port} -Djavax.net.ssl.trustStore=/run/java-cacerts'}
command=['/jdk/bin/java','-Xmx768m','-Declipse.application=org.eclipse.jdt.ls.core.id1','-Declipse.product=org.eclipse.jdt.ls.core.product','-Dosgi.bundles.defaultStartLevel=4','--add-modules=ALL-SYSTEM','--add-opens','java.base/java.util=ALL-UNNAMED','--add-opens','java.base/java.lang=ALL-UNNAMED','-jar',str(launcher),'-configuration',config.as_uri(),'-data',(state/'workspace').as_uri()]
p=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=open(base/'jdtls.log','wb'),env=env,start_new_session=True)
sock=None
try:
 lsp=Framed(p.stdout,p.stdin)
 lsp.rpc('initialize',{'processId':os.getpid(),'rootUri':project.as_uri(),'workspaceFolders':[{'uri':project.as_uri(),'name':'maven'}],'capabilities':{'workspace':{'workspaceFolders':True}},'initializationOptions':{'bundles':['/opt/sui-code/ide/java/current/java-debug.jar'],'extendedClientCapabilities':{'classFileContentsSupport':True}}})
 lsp.send({'jsonrpc':'2.0','method':'initialized','params':{}})
 for f in [source,helper]:lsp.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':f.as_uri(),'languageId':'java','version':1,'text':f.read_text()}}})
 def rpc(cmd,args):return lsp.rpc('workspace/executeCommand',{'command':cmd,'arguments':args})
 mains=[]
 for _ in range(30):
  mains=rpc('vscode.java.resolveMainClass',[project.as_uri()])
  if mains:break
  time.sleep(1)
 assert mains,mains
 # Active editor is helper Calc.java; one project entry point must remain selectable.
 assert len(mains)==1 and mains[0]['mainClass']=='app.Main',mains
 selected=mains[0];print('PASS Maven main target selected from helper context:',selected,flush=True)
 definition=lsp.rpc('textDocument/definition',{'textDocument':{'uri':source.as_uri()},'position':{'line':2,'character':source.read_text().splitlines()[2].index('Calc')+1}})
 assert definition and 'Calc.java' in str(definition),definition
 print('PASS Maven Go to Definition into real project source, encoded Unicode/+/hash path',flush=True)
 build=rpc('vscode.java.buildWorkspace',[json.dumps({'projectName':selected['projectName'],'mainClass':selected['mainClass'],'isFullBuild':False})]);assert build==1,build
 paths=rpc('vscode.java.resolveClasspath',[selected['mainClass'],selected['projectName']])
 port=rpc('vscode.java.startDebugSession',[])
 sock=socket.create_connection(('127.0.0.1',int(port)),timeout=15);dap=Framed(sock.makefile('rb'),sock.makefile('wb'))
 dap.dap_wait(dap.dap_send('initialize',{'adapterID':'java','linesStartAt1':True,'columnsStartAt1':True,'pathFormat':'path','supportsRunInTerminalRequest':False}))
 dap.dap_send('launch',dict(type='java',request='launch',mainClass=selected['mainClass'],projectName=selected['projectName'],classPaths=paths[1],modulePaths=paths[0],cwd=str(project),console='internalConsole',stopOnEntry=False,javaExec='/jdk/bin/java'))
 while dap.next().get('event')!='initialized':pass
 bps=dap.dap_wait(dap.dap_send('setBreakpoints',{'source':{'path':str(source)},'breakpoints':[{'line':5}]}));assert bps['body']['breakpoints'][0]['verified'],bps
 dap.dap_wait(dap.dap_send('configurationDone',{}))
 def stopped():
  while True:
   m=dap.next()
   if m.get('event')=='stopped':return m['body']['threadId']
   assert m.get('event')!='terminated','Maven debuggee missed breakpoint'
 thread=stopped();dap.dap_wait(dap.dap_send('next',{'threadId':thread}));thread=stopped()
 stack=dap.dap_wait(dap.dap_send('stackTrace',{'threadId':thread}))['body']['stackFrames'];assert stack and stack[0]['line']==6,stack
 value=dap.dap_wait(dap.dap_send('evaluate',{'expression':'value','frameId':stack[0]['id'],'context':'watch'}));assert value['body']['result']=='41',value
 dap.dap_send('continue',{'threadId':thread});output=''
 while True:
  m=dap.next()
  if m.get('event')=='output':output+=m['body']['output']
  if m.get('event')=='terminated':break
 assert 'SUI_MAVEN_DAP_42' in output,output
 print('PASS Maven Java real breakpoint line5 → step line6 → evaluate41 → continue → output42 → termination',flush=True)
 # A retained language server must terminate a debuggee when its DAP client disconnects.
 dap.dap_wait(dap.dap_send('disconnect',{'terminateDebuggee':True}));sock.close()
 port=rpc('vscode.java.startDebugSession',[])
 sock=socket.create_connection(('127.0.0.1',int(port)),timeout=15);dap=Framed(sock.makefile('rb'),sock.makefile('wb'))
 dap.dap_wait(dap.dap_send('initialize',{'adapterID':'java','linesStartAt1':True,'columnsStartAt1':True,'pathFormat':'path','supportsRunInTerminalRequest':False}))
 dap.dap_send('launch',dict(type='java',request='launch',mainClass=selected['mainClass'],projectName=selected['projectName'],classPaths=paths[1],modulePaths=paths[0],cwd=str(project),console='internalConsole',stopOnEntry=False,javaExec='/jdk/bin/java'))
 while dap.next().get('event')!='initialized':pass
 dap.dap_wait(dap.dap_send('setBreakpoints',{'source':{'path':str(source)},'breakpoints':[{'line':5}]}));dap.dap_wait(dap.dap_send('configurationDone',{}))
 thread=stopped();dap.dap_wait(dap.dap_send('next',{'threadId':thread}));thread=stopped()
 frame=dap.dap_wait(dap.dap_send('stackTrace',{'threadId':thread}))['body']['stackFrames'][0]['id']
 pid=dap.dap_wait(dap.dap_send('evaluate',{'expression':'java.lang.ProcessHandle.current().pid()','frameId':frame,'context':'watch'}))['body']['result']
 pid=int(pid.rstrip('L'));assert pathlib.Path(f'/proc/{pid}').exists()
 dap.dap_wait(dap.dap_send('disconnect',{'terminateDebuggee':True}))
 deadline=time.monotonic()+5
 while pathlib.Path(f'/proc/{pid}').exists() and time.monotonic()<deadline:time.sleep(.05)
 assert not pathlib.Path(f'/proc/{pid}').exists(),'Java debuggee survived DAP disconnect'
 print('PASS Java DAP terminateDebuggee leaves no child process; language server stays alive',flush=True)

finally:
 if sock:sock.close()
 try:os.killpg(p.pid,signal.SIGTERM)
 except ProcessLookupError:pass
 try:p.wait(timeout=5)
 except subprocess.TimeoutExpired:os.killpg(p.pid,signal.SIGKILL)
 print('Evidence:',base,flush=True)
