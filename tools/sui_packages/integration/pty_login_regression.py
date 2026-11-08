#!/usr/bin/env python3
"""Linux PTY/su regression; Android PRoot still requires device acceptance."""
import subprocess
subprocess.run(['useradd','-m','-s','/bin/bash','sui-test'],check=True)
commands=b"python3 -c 'import os; print(\"SUI_FOREGROUND\",os.getpgrp()==os.tcgetpgrp(0))'\nprintf 'SUI_INPUT_OK\\n'\nexit\n"
for option in ['-c','--session-command']:
    p=subprocess.run(['script','-qefc',f"su - sui-test {option} '/bin/bash --login'",'/dev/null'],input=commands,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30)
    output=p.stdout.decode(errors='replace')
    print('PTY option',option,'exit',p.returncode,flush=True);print(output,flush=True)
    if option=='--session-command':
        assert p.returncode==0 and 'SUI_FOREGROUND True' in output and 'SUI_INPUT_OK' in output,output
        assert 'no job control' not in output and 'cannot set terminal process group' not in output,output
        print('PASS controlling PTY foreground group and interactive input retained',flush=True)
