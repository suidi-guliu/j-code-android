#!/usr/bin/env python3
"""Reproducible source audit. Lexical visible-literal inventory, not a claim of full semantic UI coverage.
Protected consent text and verbatim technical/user/extension data are accounted separately.
Any newly introduced visible literal must be explicitly reviewed in ui-literal-baseline.json.
"""
from pathlib import Path
from collections import Counter
import json,re,hashlib,sys
from ui_scan import ROOT,files,sites,lex
EN=ROOT/'localization/en.json'
ZH=ROOT/'localization/zh-Hans/resources/i18n/zh-Hans.json'
BASE=ROOT/'localization/ui-literal-baseline.json'
def visible(raw):
    # Suppress interpolation expressions, including nested quoted strings, for technical-value classification.
    out=[];i=1
    while i<len(raw)-1:
        if raw.startswith('${',i):
            i+=2;depth=1
            while i<len(raw) and depth:
                if raw[i] in ['"',"'"]:
                    q=raw[i];i+=1
                    while i<len(raw) and raw[i]!=q:
                        if raw[i]=='\\':i+=1
                        i+=1
                elif raw[i]=='{':depth+=1
                elif raw[i]=='}':depth-=1
                i+=1
        elif raw[i]=='$':
            i+=1
            while i<len(raw) and (raw[i].isalnum() or raw[i]=='_'):i+=1
        else:out.append(raw[i]);i+=1
    return ''.join(out).replace('\\n',' ').replace('\\t',' ').strip()
def classify(row,english):
    path=row['path'];v=row['value'] if row['value'] is not None else visible(row['literal'])
    source=(ROOT/path).read_text()
    if source[row['end']:].lstrip().startswith('->') or re.search(r'(?:==|!=|removeSuffix\(|removePrefix\()\s*$',source[max(0,row['start']-40):row['start']]):return 'verbatim-technical-or-resource-id'
    if path.endswith('NativePackageReview.kt') or any(t in v for t in ['Unverified publisher','Verified sui-code signature','Permissions:','Neither is verified by the marketplace','signed privately by the JCode maintainers']):return 'protected-host-English'
    if v in english or not re.search('[A-Za-z]{2}',v) or re.fullmatch('[a-z][a-z0-9_.-]*',v) or v in ['JCode','Java','Python','C/C++','Ubuntu','Android','Core API','IDE Kit','LSP','DAP','SDK','Aa','ADB bridge','Chromium','MiB','MB','GB','sp','SHA-256:','pid',' · .',' · Core API','%.1f GB free · %.1f GB'] or v.startswith(('SHA-256:','Chromium ')) or ('.' in v and ' ' not in v):return 'verbatim-technical-or-resource-id'
    return 'unbound-gap'
def collect():
    english=json.loads(EN.read_text());chinese=json.loads(ZH.read_text());bindings=[];literals=[];display_bindings=[]
    for p in files():
        source=p.read_text();_,tokens=lex(source)
        for start,end,raw in tokens:
            if raw.startswith('"""') or '$' in raw:continue
            try:key=json.loads(raw)
            except ValueError:continue
            if key in english:bindings.append({'path':str(p.relative_to(ROOT)),'line':source.count('\n',0,start)+1,'key':key})
        for r in sites(p,include_resource_keys=True):
            if r['value'] in english:
                display_bindings.append({'path':r['path'],'line':r['line'],'key':r['value']});continue
            r['category']=classify(r,english);r.pop('start');r.pop('end');literals.append(r)
    return english,chinese,bindings,literals,display_bindings
def fingerprint(r):return hashlib.sha256((r['path']+'\n'+r['literal']).encode()).hexdigest()
def main(write=False):
    english,chinese,bindings,literals,display_bindings=collect()
    assert english.keys()==chinese.keys(),'Every published key needs an authored Chinese value'
    baseline=sorted({fingerprint(r) for r in literals})
    if write:BASE.write_text(json.dumps(baseline,indent=2)+'\n')
    else:
        allowed=set(json.loads(BASE.read_text()))
        added=[r for r in literals if fingerprint(r) not in allowed]
        assert not added,'E_UI_LITERAL: new unreviewed hardcoded display text: '+json.dumps(added[:8],ensure_ascii=False)
    gaps=sum(r['category']=='unbound-gap' for r in literals)
    stats={'display_bound_key_sites':len(display_bindings),'unbound_display_literal_sites':gaps,'scanned_display_literal_coverage_percent':round(len(display_bindings)*100/(len(display_bindings)+gaps),2),'catalog_keys':len(english),'translated_keys':len(chinese),'catalog_translation_percent':round(len(chinese)*100/len(english),2),'key_reference_sites':len(bindings),'referenced_keys':len({r['key'] for r in bindings}),'remaining_literal_sites':len(literals),'literal_categories':dict(Counter(r['category'] for r in literals)),'physical_arm64_tested':False,'method':'Lexical display-argument scan; dynamic backend/extension prose is not fully measurable by this scan. Catalog completeness does not equal whole-application translation.'}
    common={'JCodeShell.kt','ExplorerView.kt','SettingsFeature.kt','NativeLanguageCenter.kt','NativePackageCenter.kt','WorkbenchPanels.kt','WorkbenchDialogs.kt','SearchToolPanel.kt','DebugSessionPanel.kt','EditorPane.kt'}
    bound=sum(Path(r['path']).name in common for r in display_bindings)
    unbound=sum(Path(r['path']).name in common and r['category']=='unbound-gap' for r in literals)
    stats['common_scope']={'files':sorted(common),'bound_display_key_sites':bound,'unbound_display_literal_sites':unbound,'scanned_display_literal_coverage_percent':round(bound*100/(bound+unbound),2)}
    out=ROOT/'docs/localization';out.mkdir(parents=True,exist_ok=True)
    (out/'coverage.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2)+'\n')
    (out/'locale-key-audit.json').write_text(json.dumps({'bindings':bindings,'remaining_literals':literals,'display_bindings':display_bindings},ensure_ascii=False,indent=2)+'\n')
    def cell(s):return s.replace('|','\\|').replace('\n','<br>')
    lines=['# UI 本地化资源接入清单','', '由 `python3 tools/localization/audit.py` 从生产源码及独立语言包资源生成。清单为词法扫描，动态后端日志和扩展作者文案不据此声称已翻译。**资源键翻译率不是全应用汉化率。**','',f"公开键：{len(english)}；中文值：{len(chinese)}；键引用位置：{len(bindings)}；仍有硬编码位置：{len(literals)}（分别列出受保护宿主英文、技术原文及缺口）。",'', '## 页面/源码汇总','','|源码|全部键引用|显示参数内键引用|未接入缺口|受保护英文|技术原文|','|---|---:|---:|---:|---:|---:|']
    counts=Counter(r['path'] for r in bindings)
    for path in sorted(set(counts)|{r['path'] for r in literals}):
        c=Counter(r['category'] for r in literals if r['path']==path)
        lines.append(f"|{path}|{counts[path]}|{sum(b['path']==path for b in display_bindings)}|{c['unbound-gap']}|{c['protected-host-English']}|{c['verbatim-technical-or-resource-id']}|")
    lines+=['','## 实际资源键绑定','','|源码位置|Core 资源键|简体中文|状态|','|---|---|---|---|']
    for b in bindings:lines.append(f"|{b['path']}:{b['line']}|{b['key']}|{cell(chinese[b['key']])}|已绑定|")
    lines+=['','## 保留原文和未完成项','','|源码位置|源码文字|分类|','|---|---|---|']
    for r in literals:lines.append(f"|{r['path']}:{r['line']}|{cell(r['literal'])}|{r['category']}|")
    (out/'locale-key-audit.md').write_text('\n'.join(lines)+'\n')
    print(json.dumps(stats,ensure_ascii=False))
if __name__=='__main__':main('--update-baseline' in sys.argv)
