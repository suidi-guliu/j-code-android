"""Small lexical audit (not a Kotlin compiler). Never treats comments or source templates as UI keys.
Finds visible literals in display arguments, callbacks and explicitly labelled controls.
The checked-in audit lists unbound sites as gaps, never as translated coverage.
"""
import re, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]

def lex(s):
    mask=list(s);strings=[];i=0
    def quoted(i,quote):
        end=i+len(quote)
        while end<len(s):
            if s.startswith(quote,end):return end+len(quote)
            if len(quote)==1 and s[end]=='\\':end+=2;continue
            if quote=='"' and s.startswith('${',end):
                depth=1;end+=2
                while depth and end<len(s):
                    if s[end]=='"':end=quoted(end,'"');continue
                    if s[end]=='\'':end=quoted(end,'\'');continue
                    if s[end]=='{':depth+=1
                    if s[end]=='}':depth-=1
                    end+=1
                continue
            end+=1
        return end
    while i<len(s):
        start=i
        if s.startswith('//',i):
            i=s.find('\n',i)
            if i<0:i=len(s)
        elif s.startswith('/*',i):
            n=1;i+=2
            while n and i<len(s):
                if s.startswith('/*',i):n+=1;i+=2
                elif s.startswith('*/',i):n-=1;i+=2
                else:i+=1
        elif s[i] in ['"',"'"]:
            q='"""' if s.startswith('"""',i) else s[i];i=quoted(i,q)
            if q!='\'':strings.append((start,i,s[start:i]))
        else:i+=1;continue
        for n in range(start,i):
            if mask[n]!='\n':mask[n]=' '
    return ''.join(mask),strings

def end_arg(mask,start):
    level=0
    for i in range(start,len(mask)):
        c=mask[i]
        if c in '([{':level+=1
        elif c in ')]}':
            if level==0:return i
            level-=1
        elif c==',' and level==0:return i
    return len(mask)

def sites(path,include_resource_keys=False):
    path=path.resolve()
    s=path.read_text();mask,strings=lex(s);ranges=[]
    for m in re.finditer(r'\b(?:Text|JcTooltip|SectionLabel|showSnackbar|emitMessage|SettingsGroup)\s*\(|onSnackbar\?\.invoke\s*\(',mask):
        ranges.append((m.end(),end_arg(mask,m.end())))
    for m in re.finditer(r'\b(?:text|title|label|description|supporting|placeholder|contentDescription|thirdLabel|actionLabel|previewLabel)\s*=',mask):
        ranges.append((m.end(),end_arg(mask,m.end())))
    for m in re.finditer(r'\b(?:ContextAction|DebugAction)\s*\(',mask):
        n=end_arg(mask,m.end())
        if n<len(mask) and mask[n]==',':ranges.append((n+1,end_arg(mask,n+1)))
    out=[]
    merged=[]
    for start,end,raw in strings:
        if merged and '$' not in raw and '$' not in merged[-1][2] and re.fullmatch(r'\s*\+\s*',s[merged[-1][1]:start]):
            old=merged.pop();merged.append((old[0],end,old[2]+raw))
        else:merged.append((start,end,raw))
    for start,end,raw in merged:
        if any(a<=start<end<=b for a,b in ranges) and re.search(r'[A-Za-z]{2}',raw):
            if not include_resource_keys and re.search(r'(?:uiText|strings\.text)\s*\(\s*$',s[max(0,start-60):start]): continue
            value=None
            if '$' not in raw and not raw.startswith('"""'):
                try: value=''.join(json.loads(piece.replace("\\'","'")) for piece in re.findall(r'"(?:\\.|[^"\\])*"',s[start:end]))
                except ValueError: pass
            out.append({'value':value,'path':str(path.relative_to(ROOT)),'line':s.count('\n',0,start)+1,'start':start,'end':end,'literal':raw})
    return out

def files():
    for folder in ['app/src/main','core/design/src/main','feature']:
        yield from sorted((ROOT/folder).rglob('*.kt'))
if __name__=='__main__':
    import json
    rows=[r for p in files() for r in sites(p)]
    print(json.dumps(rows,ensure_ascii=False,indent=2))
