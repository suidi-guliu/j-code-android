#!/usr/bin/env python3
"""Execute the shipped three-language project resources in Ubuntu; no Android acceptance claims."""
import os,pathlib,shutil,subprocess,urllib.parse
root=pathlib.Path('/repo/native-library/src');evidence=pathlib.Path('/evidence/v05-projects');evidence.mkdir(exist_ok=True)
proxy=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
os.environ['JAVA_TOOL_OPTIONS']=f'-Dhttp.proxyHost={proxy.hostname} -Dhttp.proxyPort={proxy.port} -Dhttps.proxyHost={proxy.hostname} -Dhttps.proxyPort={proxy.port} -Djavax.net.ssl.trustStore=/run/java-cacerts'
pathlib.Path('/root/.m2').mkdir(exist_ok=True)
pathlib.Path('/root/.m2/settings.xml').write_text(f'<settings><proxies><proxy><id>cloud-test</id><active>true</active><protocol>http</protocol><host>{proxy.hostname}</host><port>{proxy.port}</port></proxy></proxies></settings>')
original=pathlib.Path('/opt/sui-code/java.env').read_bytes()
for language in ['Java','C_CPP','Python']:
    project=evidence/f'中文 + {language} Project'
    shutil.copytree(root/language/'suik/resources/templates',project)
    script=root/language/'suik/resources/project-task.sh'
    for action in ['build','test','run']:
        print('RUN',language,action,flush=True)
        p=subprocess.run(['bash',str(script),action,str(project)],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=300)
        print(p.stdout,flush=True);assert p.returncode==0,(language,action,p.returncode)
        if action=='run':assert 'Hello from sui-code! 42' in p.stdout,p.stdout
        print('PASS',language,action,flush=True)
assert pathlib.Path('/opt/sui-code/java.env').read_bytes()==original
print('PASS all nine real project workflows; installed Java environment unchanged byte-for-byte',flush=True)
