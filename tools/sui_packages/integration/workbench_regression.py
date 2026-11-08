#!/usr/bin/env python3
"""Ubuntu x64 protocol/regression evidence. Never substitutes for Android/ARM64 acceptance."""
import json,os,pathlib,shutil,signal,subprocess,tempfile,time
from java_lsp_dap import Framed
ROOT=pathlib.Path('/repo')
processes=[]
def spawn(cmd,cwd):
    p=subprocess.Popen(cmd,cwd=cwd,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=open(str(cwd/'server-stderr.log'),'ab'),start_new_session=True)
    processes.append(p);return Framed(p.stdout,p.stdin)
def dap_stop_step(dap,config,source,line,label):
    dap.dap_wait(dap.dap_send('initialize',dict(adapterID=label,linesStartAt1=True,columnsStartAt1=True,pathFormat='path',supportsRunInTerminalRequest=False)))
    launch=dap.dap_send('launch',config)
    while True:
        m=dap.next()
        if m.get('event')=='initialized':break
        if m.get('request_seq')==launch and not m.get('success'):raise AssertionError(m)
    bps=dap.dap_wait(dap.dap_send('setBreakpoints',{'source':{'path':str(source)},'breakpoints':[{'line':line}]}))['body']['breakpoints']
    assert bps and bps[0]['verified'],bps
    dap.dap_wait(dap.dap_send('configurationDone',{}))
    def stopped():
        while True:
            m=dap.next()
            if m.get('event')=='stopped':return m['body']['threadId']
            assert m.get('event')!='terminated','Program exited without hitting breakpoint'
    thread=stopped()
    stack=dap.dap_wait(dap.dap_send('stackTrace',{'threadId':thread}))['body']['stackFrames']
    assert pathlib.Path(stack[0]['source']['path']).resolve()==source.resolve(),stack
    assert stack[0]['line']>=line,stack
    dap.dap_wait(dap.dap_send('next',{'threadId':thread}));thread=stopped()
    dap.dap_wait(dap.dap_send('continue',{'threadId':thread}))
    while dap.next().get('event')!='terminated':pass
    print('PASS',label,'real breakpoint, matching source path, step, continue and terminate',flush=True)
try:
    base=pathlib.Path(tempfile.mkdtemp(prefix='sui-v05-中文 + space-',dir='/evidence'))
    cpp=base/'cpp';shutil.copytree(ROOT/'native-library/src/C_CPP/suik/resources/templates',cpp)
    # Reproduce precisely v0.4's ephemeral alias persisted in CMake caches.
    alias=pathlib.Path('/sui-quickrun');alias.symlink_to(cpp)
    subprocess.run(['cmake','-S',str(alias),'-B',str(alias/'.sui-code/build'),'-G','Ninja','-DCMAKE_EXPORT_COMPILE_COMMANDS=ON','-DCMAKE_BUILD_TYPE=Debug'],check=True,stdout=subprocess.DEVNULL)
    alias.unlink()
    subprocess.run(['bash',str(ROOT/'native-library/src/C_CPP/suik/resources/project-task.sh'),'test',str(cpp)],check=True)
    db=json.loads((cpp/'.sui-code/build/compile_commands.json').read_text())
    assert db and all(pathlib.Path(e['directory']).is_dir() and '/sui-quickrun' not in json.dumps(e) for e in db),db
    assert list((cpp/'.sui-code').glob('build.pre-v05-*')),'Old cache was not preserved'
    print('PASS CMake alias migration, retained backup, stable compilation database and real CTest',flush=True)
    source=cpp/'src/main.cpp'
    clang=spawn(['clangd',f'--compile-commands-dir={cpp}/.sui-code/build','--background-index'],cpp)
    t=time.monotonic();clang.rpc('initialize',{'processId':os.getpid(),'rootUri':cpp.as_uri(),'capabilities':{'textDocument':{'completion':{'contextSupport':True,'completionItem':{'snippetSupport':True}}}}});clang.send({'jsonrpc':'2.0','method':'initialized','params':{}})
    clang.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':source.as_uri(),'languageId':'cpp','version':1,'text':source.read_text()}}})
    # Resolve add from the real header, not an empty/non-null completion response.
    text=source.read_text();lines=text.splitlines();ln=next(i for i,x in enumerate(lines) if 'add(' in x);col=lines[ln].index('add(')+1
    definitions=clang.rpc('textDocument/definition',{'textDocument':{'uri':source.as_uri()},'position':{'line':ln,'character':col}})
    assert definitions and any('math' in str(x) for x in definitions),definitions
    print('PASS clangd project header definition; cold seconds',round(time.monotonic()-t,3),flush=True)
    changed=text.replace('add(20, 22)','ad')
    clang.send({'jsonrpc':'2.0','method':'textDocument/didChange','params':{'textDocument':{'uri':source.as_uri(),'version':2},'contentChanges':[{'text':changed}]}})
    items=[]
    for attempt in range(4):
        completion=clang.rpc('textDocument/completion',{'textDocument':{'uri':source.as_uri()},'position':{'line':ln,'character':col+1},'context':{'triggerKind':1}})
        items=completion if isinstance(completion,list) else completion['items']
        if any(x.get('filterText',x['label']).strip()=='add' for x in items):break
        if attempt<3:time.sleep(.2)
    assert any(x.get('filterText',x['label']).strip()=='add' for x in items),items
    print('PASS clangd actual add completion from project math.hpp',flush=True)

    prep=subprocess.check_output(['bash',str(ROOT/'native-library/src/C_CPP/suik/resources/prepare-debug.sh'),str(source),str(cpp)],text=True)
    program=json.loads(prep)['program']
    dap_stop_step(spawn(['/usr/bin/lldb-dap-18'],cpp),dict(program=program,cwd=str(cpp),stopOnEntry=False,disableASLR=False,initCommands=['settings set target.disable-aslr false']),source,5,'C++ CMake DAP')
    py=base/'python';shutil.copytree(ROOT/'native-library/src/Python/suik/resources/templates',py)
    source=py/'main.py'
    pylsp=spawn(['/usr/bin/pylsp'],py)
    t=time.monotonic();pylsp.rpc('initialize',{'processId':os.getpid(),'rootUri':py.as_uri(),'capabilities':{}});pylsp.send({'jsonrpc':'2.0','method':'initialized','params':{}})
    pylsp.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':source.as_uri(),'languageId':'python','version':1,'text':'import os\nos.pa\n'}}})
    cold=pylsp.rpc('textDocument/completion',{'textDocument':{'uri':source.as_uri()},'position':{'line':1,'character':5}})
    cold_ms=(time.monotonic()-t)*1000
    t=time.monotonic();warm=pylsp.rpc('textDocument/completion',{'textDocument':{'uri':source.as_uri()},'position':{'line':1,'character':5}});warm_ms=(time.monotonic()-t)*1000
    items=warm if isinstance(warm,list) else warm['items'];assert any(x['label'].startswith('path') for x in items),items
    print(f'PASS Python actual os.path completion cold={cold_ms:.1f}ms warm={warm_ms:.1f}ms',flush=True)
    dap_stop_step(spawn(['/usr/bin/python3','-m','debugpy.adapter'],py),dict(program=str(source),cwd=str(py),console='internalConsole',redirectOutput=True,justMyCode=False),source,5,'Python Project DAP')
finally:
    for p in processes:
        try:os.killpg(p.pid,signal.SIGTERM)
        except ProcessLookupError:pass
    for p in processes:
        try:p.wait(timeout=5)
        except subprocess.TimeoutExpired:os.killpg(p.pid,signal.SIGKILL)
