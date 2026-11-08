#!/usr/bin/env python3
"""Real Java LSP + DAP smoke test on a host JVM, NOT Android/ARM64 acceptance.
Usage: script --jdk VERIFIED_JDK --server UNPACKED_JDTLS --debug-plugin JAR
"""
import argparse,json,os,pathlib,queue,shutil,signal,socket,subprocess,tempfile,threading,time

class Framed:
    def __init__(self, source, sink):
        self.source,self.sink=source,sink; self.messages=queue.Queue();self.counter=0
        threading.Thread(target=self.pump,daemon=True).start()
    def pump(self):
        try:
            while True:
                headers={}
                while True:
                    line=self.source.readline()
                    if not line: raise EOFError('protocol stream closed')
                    if line in (b'\r\n',b'\n'):break

                    if b':' not in line: raise ValueError('Unframed protocol line: '+repr(line[:300]))
                    k,v=line.decode().split(':',1);headers[k.lower()]=v.strip()
                size=int(headers['content-length'])
                self.messages.put(json.loads(self.source.read(size)))
        except Exception as e:self.messages.put(e)
    def send(self,value):
        data=json.dumps(value).encode();self.sink.write(f'Content-Length: {len(data)}\r\n\r\n'.encode()+data);self.sink.flush()
    def next(self,timeout=90):
        item=self.messages.get(timeout=timeout)
        if isinstance(item,Exception):raise item
        return item
    def rpc(self,method,params):
        self.counter+=1;ident=self.counter
        self.send({'jsonrpc':'2.0','id':ident,'method':method,'params':params})
        while True:
            item=self.next()
            if item.get('id')==ident and 'method' not in item:
                if 'error' in item:raise RuntimeError(item['error'])
                return item.get('result')
            if 'method' in item and 'id' in item:
                value=[{} for _ in item.get('params',{}).get('items',[])] if item['method']=='workspace/configuration' else None
                self.send({'jsonrpc':'2.0','id':item['id'],'result':value})
    def dap_send(self,command,args):
        self.counter+=1;ident=self.counter
        self.send({'seq':ident,'type':'request','command':command,'arguments':args});return ident
    def dap_wait(self,ident):
        while True:
            item=self.next()
            if item.get('request_seq')==ident:
                if not item.get('success'):raise RuntimeError(item)
                return item

def main(args):
    root=pathlib.Path(tempfile.mkdtemp(prefix='sui-real-java-'))
    process=None; sock=None
    try:
        source=root/'中文 + space'/'Hello.java';source.parent.mkdir()
        source.write_text('public class Hello {\n public static void main(String[] args) {\n  int value=41;\n  System.out.println("SUI_DAP_"+(value+1));\n }\n}\n')
        config=root/'configuration';shutil.copytree(pathlib.Path(args.server)/'config_linux',config)
        launcher=next((pathlib.Path(args.server)/'plugins').glob('org.eclipse.equinox.launcher_*.jar'))
        stderr=open(root/'stderr.log','wb')
        command=[str(pathlib.Path(args.jdk)/'bin/java'),'-Xmx768m','-Declipse.application=org.eclipse.jdt.ls.core.id1','-Declipse.product=org.eclipse.jdt.ls.core.product','-Dosgi.bundles.defaultStartLevel=4','--add-modules=ALL-SYSTEM','--add-opens','java.base/java.util=ALL-UNNAMED','--add-opens','java.base/java.lang=ALL-UNNAMED','-jar',str(launcher),'-configuration',str(config),'-data',str(root/'workspace')]
        process=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=stderr,start_new_session=True,env={**os.environ,'JAVA_HOME':args.jdk})
        lsp=Framed(process.stdout,process.stdin)
        initialized=lsp.rpc('initialize',{'processId':os.getpid(),'rootUri':source.parent.as_uri(),'workspaceFolders':[{'uri':source.parent.as_uri(),'name':'smoke'}],'capabilities':{'workspace':{'workspaceFolders':True}},'initializationOptions':{'bundles':[str(pathlib.Path(args.debug_plugin).resolve())],'extendedClientCapabilities':{'classFileContentsSupport':True}}})
        assert initialized.get('capabilities'),initialized
        print('PASS Java LSP initialize',flush=True)
        lsp.send({'jsonrpc':'2.0','method':'initialized','params':{}})
        lsp.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':source.as_uri(),'languageId':'java','version':1,'text':source.read_text()}}})
        mains=[]
        for _ in range(30):
            mains=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.resolveMainClass','arguments':[source.parent.as_uri()]})
            if mains:break
            time.sleep(1)
        assert mains,'No main class was resolved'
        selected=next(x for x in mains if x['mainClass']=='Hello')
        print('PASS Java main-class resolution',selected,flush=True)
        build=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.buildWorkspace','arguments':[json.dumps({'projectName':selected['projectName'],'mainClass':selected['mainClass'],'isFullBuild':False})]})
        print('Java build response',build,flush=True)
        paths=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.resolveClasspath','arguments':['Hello',selected['projectName']]})
        assert len(paths)==2,paths
        port=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.startDebugSession','arguments':[]})
        sock=socket.create_connection(('127.0.0.1',int(port)),timeout=15)
        dap=Framed(sock.makefile('rb'),sock.makefile('wb'))
        dap.dap_wait(dap.dap_send('initialize',{'adapterID':'java','clientID':'sui-code-integration','linesStartAt1':True,'columnsStartAt1':True,'pathFormat':'path','supportsRunInTerminalRequest':False}))
        print('PASS Java DAP initialize',flush=True)
        launch=dap.dap_send('launch',{'type':'java','request':'launch','mainClass':'Hello','projectName':selected['projectName'],'modulePaths':paths[0],'classPaths':paths[1],'cwd':str(source.parent),'console':'internalConsole','stopOnEntry':False,'javaExec':str(pathlib.Path(args.jdk)/'bin/java')})
        while True:
            item=dap.next()
            if item.get('event')=='initialized':break
            if item.get('request_seq')==launch and not item.get('success'):raise RuntimeError(item)
        bps=dap.dap_wait(dap.dap_send('setBreakpoints',{'source':{'path':str(source)},'breakpoints':[{'line':3}]}))
        assert bps['body']['breakpoints'][0]['verified'],bps
        dap.dap_wait(dap.dap_send('configurationDone',{}))
        while True:
            item=dap.next()
            if item.get('event')=='stopped':
                thread=item['body']['threadId'];break
            if item.get('event')=='terminated':raise AssertionError('Program exited without a breakpoint')
        stack=dap.dap_wait(dap.dap_send('stackTrace',{'threadId':thread}))
        assert stack['body']['stackFrames'],stack
        print('PASS Java breakpoint and stack trace',flush=True)
        dap.dap_wait(dap.dap_send('next',{'threadId':thread}))
        while True:
            item=dap.next()
            if item.get('event')=='stopped':thread=item['body']['threadId'];break
            if item.get('event')=='terminated':raise AssertionError('Java step ended before the next statement')
        print('PASS Java DAP step-over',flush=True)
        dap.dap_wait(dap.dap_send('continue',{'threadId':thread}))
        output=''
        while True:
            item=dap.next()
            if item.get('event')=='output':output+=item['body']['output']
            if item.get('event')=='terminated':break
        assert 'SUI_DAP_42' in output,output
        print('PASS Java debuggee output and termination',flush=True)
        original=source.read_text()
        partial=original.replace('System.out.println("SUI_DAP_"+(value+1));','System.out.pr')
        lsp.send({'jsonrpc':'2.0','method':'textDocument/didChange','params':{'textDocument':{'uri':source.as_uri(),'version':2},'contentChanges':[{'text':partial}]}})
        items=lsp.rpc('textDocument/completion',{'textDocument':{'uri':source.as_uri()},'position':{'line':3,'character':len('  System.out.pr')},'context':{'triggerKind':1}})
        items=items if isinstance(items,list) else items['items']
        assert any('println' in x.get('filterText',x['label']) for x in items),items
        print('PASS Java System.out.pr real member completion',flush=True)
        lsp.send({'jsonrpc':'2.0','method':'textDocument/didChange','params':{'textDocument':{'uri':source.as_uri(),'version':3},'contentChanges':[{'text':original}]}})
        definitions=lsp.rpc('textDocument/definition',{'textDocument':{'uri':source.as_uri()},'position':{'line':3,'character':5}})
        assert definitions,definitions
        location=definitions[0] if isinstance(definitions,list) else definitions
        uri=location.get('uri',location.get('targetUri'))
        assert uri.startswith('jdt:'),definitions
        text=lsp.rpc('java/classFileContents',{'uri':uri})
        assert isinstance(text,str) and 'class System' in text,str(text)[:300]
        print('PASS Java Go to Definition virtual JDK source and classFileContents',flush=True)
        lsp.rpc('shutdown',None);lsp.send({'jsonrpc':'2.0','method':'exit','params':None})
    finally:
        if sock: sock.close()
        if process:
            try:os.killpg(process.pid,signal.SIGTERM)
            except ProcessLookupError:pass
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid,signal.SIGKILL);process.wait()
        print('Evidence directory:',root,flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--jdk',required=True);p.add_argument('--server',required=True);p.add_argument('--debug-plugin',required=True);main(p.parse_args())
