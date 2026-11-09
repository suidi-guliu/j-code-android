#!/usr/bin/env python3
"""Mechanical SVG→adaptive foreground conversion, without changing the supplied artwork.
Requires Inkscape. The exact source asset remains unchanged and hash-pinned. No AI image generation.
"""
from pathlib import Path
import hashlib,os,subprocess,tempfile,xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parents[2]
source=ROOT/'design/user-assets/sui-code-app-icon.svg'
assert hashlib.sha256(source.read_bytes()).hexdigest()=='39003c5686cdeaf2e09d018c17787f883667815d43227a8dd38b9b54634a60c3'
ns='http://www.w3.org/2000/svg';E.register_namespace('',ns)
svg=E.fromstring(source.read_bytes());rect=svg.find('{'+ns+'}rect');assert rect is not None and rect.attrib['fill']=='#151922'
svg.remove(rect) # Background belongs only to Android's adaptive background layer.
# Uniform optical inset places the unchanged geometry within Android's safe circle.
group=E.Element('{'+ns+'}g',{'transform':'translate(600 600) scale(0.8)'})
for element in list(svg):
 if element.tag!='{'+ns+'}defs':svg.remove(element);group.append(element)
svg.append(group)
out=ROOT/'app/src/main/res/drawable-nodpi/sui_launcher_foreground.png';out.parent.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='sui-launcher-') as td:
 path=Path(td)/'foreground.svg';E.ElementTree(svg).write(path,encoding='utf-8',xml_declaration=True)
 subprocess.run(['inkscape',str(path),'--export-type=png','--export-filename='+str(out),'--export-width=864','--export-height=864'],check=True,env={**os.environ,'INKSCAPE_PROFILE_DIR':str(Path(td)/'profile')})
for suffix in ['', '_debug','_beta']:
 (ROOT/f'app/src/main/res/drawable/ic_launcher_foreground{suffix}.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/sui_launcher_foreground" android:inset="0dp" />
''')
print('User SVG preserved; transparent adaptive foreground:',out.stat().st_size,'bytes',hashlib.sha256(out.read_bytes()).hexdigest())
