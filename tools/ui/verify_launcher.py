#!/usr/bin/env python3
"""Check all launcher source resources against the delivered v0.5 bytes, without regenerating icons."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[2]
snapshot=json.loads((root/'tools/ui/launcher-v05-sha256.json').read_text())
actual={str(p.relative_to(root)) for p in (root/'app/src/main/res').rglob('*') if p.is_file() and 'ic_launcher' in p.name}
assert actual==set(snapshot['files']), 'Launcher resource inventory differs from v0.5'
for path,digest in snapshot['files'].items():
 assert hashlib.sha256((root/path).read_bytes()).hexdigest()==digest, f'Launcher modified: {path}'
print(f"PASS: {len(actual)} launcher resources byte-identical to v0.5 {snapshot['baseline']}")
