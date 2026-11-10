#!/usr/bin/env python3
"""Strict four-file CMake regression with real production LspSession and clangd in PRoot."""
import argparse,json,os,pathlib,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--classpath',required=True);p.add_argument('--library',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
assert os.environ.get('SUI_OWNED_INTEGRATION')=='1'
root=pathlib.Path(tempfile.mkdtemp(prefix='sui-v055-definition-'));a.output.mkdir(parents=True,exist_ok=True)
results=[]
for name in ['plain','中文 space']:
 project=root/name;(project/'src').mkdir(parents=True);(project/'include').mkdir()
 sources={
  'CMakeLists.txt':'cmake_minimum_required(VERSION 3.16)\nproject(sui_definition LANGUAGES C CXX)\nadd_executable(app src/main.cpp src/math.cpp src/c_math.c)\ntarget_include_directories(app PRIVATE include)\n',
  'include/math.hpp':'#pragma once\nint add(int a, int b);\nint missing(int a, int b);\n',
  'src/math.cpp':'#include "math.hpp"\nint add(int a, int b) { return a+b; }\n',
  'src/main.cpp':'#include "math.hpp"\nint main() { return add(1, 2) == 3 ? 0 : 1; }\n',
  'src/c_math.c':'#include <stdio.h>\nint c_add(int a,int b) { return a+b; }\n'}
 for relative,text in sources.items():(project/relative).write_text(text)
 prefix=['/home/ubuntu/bin/proot','--kill-on-exit','--link2symlink','-r','/','-w',str(project)]
 build=project/'.sui-code/build'
 def command(argv,label,expect=0):
  value=subprocess.run(prefix+argv,capture_output=True,text=True,timeout=120)
  (a.output/(name+'-'+label+'.log')).write_text(value.stdout+value.stderr)
  assert (value.returncode==0)==(expect==0),(label,value.returncode,value.stderr)
  return value.returncode
 command(['cmake','-S',str(project),'-B',str(build),'-G','Ninja','-DCMAKE_EXPORT_COMPILE_COMMANDS=ON','-DCMAKE_BUILD_TYPE=Debug'],'configure')
 command(['cmake','--build',str(build)],'build')
 command([str(build/'app')],'run')
 database=json.loads((build/'compile_commands.json').read_text());assert len(database)==3
 assert all(pathlib.Path(item['directory']).is_dir() and pathlib.Path(item['file']).is_file() for item in database)
 cfg={'project':str(project),'evidence':str(a.output/name),'descriptor':str(a.library/'C_CPP/suik/adapters/lsp.json'),'proot':prefix}
 config=project/'test-config.json';config.write_text(json.dumps(cfg))
 value=subprocess.run(['/jdk/bin/java','-cp',a.classpath,'CppDefinitionCli',str(config)],capture_output=True,text=True,timeout=180)
 (a.output/(name+'-client.log')).write_text(value.stdout+value.stderr);assert value.returncode==0,value.stdout+value.stderr
 # Missing definition is a genuine linker error, independent of a navigable declaration.
 source=project/'src/main.cpp';source.write_text(sources['src/main.cpp'].replace('return add(','return missing('))
 failure=command(['cmake','--build',str(build)],'missing-definition-link-failure',expect=1)
 source.write_text(sources['src/main.cpp']);command(['cmake','--build',str(build)],'restored-build')
 results.append({'project':str(project),'checks':json.loads((a.output/name/'results.json').read_text()),'compileDatabaseEntries':len(database),'missingDefinitionExit':failure,'result':'PASS','platform':'Ubuntu24.04 amd64 PRoot, production Kotlin LspSession; not Android ARM64'})
 print(value.stdout,flush=True)
(a.output/'cpp-definition-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
