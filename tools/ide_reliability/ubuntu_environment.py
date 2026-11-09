#!/usr/bin/env python3
"""Actual production NativeInstallPlan under --link2symlink with isolated /opt/sui-code.
No package transaction may run when GCC/G++ already compile successfully.
"""
import json,os,pathlib,subprocess,shutil
root=pathlib.Path('/evidence/environment-tests');root.mkdir(exist_ok=True)
os.environ['PATH']='/home/ubuntu/bin:'+os.environ['PATH']
cp='/opt/v054-tests:/opt/v054-tests/distro-classes.jar:/opt/v054-tests/kotlin-stdlib.jar'
script=subprocess.check_output(['/jdk/bin/java','-cp',cp,'EnvironmentRepairCli'],text=True)
# Prevent any accidental package installer; existing actual system compilers must be reused.
guard=root/'guard';guard.mkdir(exist_ok=True);(guard/'sudo').write_text('#!/bin/sh\necho UNEXPECTED_PACKAGE_INSTALL >&2;exit 99\n');(guard/'sudo').chmod(0o755)
results=[]
for name in ['normal','missing','broken-link','unreadable','directory','rollback']:
 target=root/name;target.mkdir(exist_ok=True)
 c=target/'c.env';cpp=target/'cpp.env'
 for file in [c,cpp]:
  if file.is_dir():file.rmdir()
  elif file.exists() or file.is_symlink():file.unlink()
 cpp.write_text("export SUI_CPP='/usr/bin/g++'\n# user custom selection\n")
 if name=='normal':c.write_text("export SUI_C='/usr/bin/gcc'\n# preserve custom\n")
 if name in ['broken-link','rollback']:c.symlink_to('/tmp/deleted-install/env-c')
 if name=='unreadable':c.write_text('preserve unreadable data\n');c.chmod(0)
 if name=='directory':c.mkdir()
 before=cpp.read_bytes();content=script
 if name=='rollback':
  cpp.unlink()
  content='mv() { case "${!#}" in /opt/sui-code/cpp.env) return 91;; *) /bin/mv "$@";; esac; }\n'+content
 command=['proot','--kill-on-exit','--link2symlink','-r','/','-b',str(target)+':/opt/sui-code','-w','/tmp','/bin/bash','-c','export PATH='+str(guard)+':"$PATH"\n'+content]
 p=subprocess.run(command,capture_output=True,text=True,timeout=60)
 (root/(name+'.log')).write_text(p.stdout+p.stderr)
 assert 'UNEXPECTED_PACKAGE_INSTALL' not in p.stdout+p.stderr,name
 if name in ['normal','missing','broken-link']:
  assert p.returncode==0,(name,p.returncode,p.stderr)
  assert not c.is_symlink() if name!='normal' else True
  assert c.is_file() and cpp.read_bytes()==before,(name,'custom cpp environment was overwritten')
  if name=='normal':assert b'# preserve custom' in c.read_bytes()
 elif name=='unreadable':
  assert p.returncode==77,(name,p.returncode);c.chmod(0o600);assert c.read_text()=='preserve unreadable data\n'
 elif name=='directory':assert p.returncode==73 and c.is_dir(),(name,p.returncode)
 elif name=='rollback':assert p.returncode!=0 and c.is_symlink() and os.readlink(c)=='/tmp/deleted-install/env-c' and not cpp.exists(),(name,p.returncode)
 results.append({'case':name,'result':'PASS','exit':p.returncode,'no_apt':True});print(json.dumps(results[-1]),flush=True)
pathlib.Path('/evidence/environment-results.json').write_text(json.dumps(results,indent=2)+'\n')
