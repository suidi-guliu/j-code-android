#!/usr/bin/env python3
"""Fail on stale normative schema/manifest appendices and invalid example descriptors.
Requires jsonschema (development dependency only, never used by Android).
"""
import json
import re
import pathlib
import sys
import jsonschema

root = pathlib.Path(__file__).resolve().parents[2]
base = root / "docs/specifications"
doc = (base / "SUIC-SUIK-Development-Specification.md").read_text()
ui = (base / "SuiCode-UI-Design-Specification.md").read_text()
count = 0
for kind, example in [("suic", "minimal"), ("suik", "minimal-kit")]:
    for p in [base / "schemas" / f"{kind}-manifest.schema.json", base / "examples" / example / "manifest.json"]:
        if "```json\n" + p.read_text() + "```" not in doc:
            raise AssertionError(f"E_SPEC_STALE: {p.name}")
for p in sorted((base / "examples").glob("*/manifest.json")) + sorted((root / "native-library/src").glob("*/*/manifest.json")) + sorted((root/"localization").glob("*/manifest.json")):
    m = json.loads(p.read_text())
    schema = json.loads((base / "schemas" / (m["kind"] + "-manifest.schema.json")).read_text())
    validator = jsonschema.Draft202012Validator(schema)
    errors = list(validator.iter_errors(m))
    if p.parent.name == "illegal-plugin":
        assert errors, "E_SPEC_TEST: invalid plugin must violate schema"
        continue
    if errors:
        raise AssertionError(f"E_SPEC_SCHEMA: {p}: {errors[0].message}")
    for c in m["components"]:
        descriptor = json.loads((p.parent / c["entry"]).read_text())
        ds = json.loads((base / "schemas" / (c["type"] + ".schema.json")).read_text())
        jsonschema.Draft202012Validator(ds).validate(descriptor)
        count += 1
keys=json.loads((root/'localization/en.json').read_text())
import sys
sys.path.insert(0,str(root/'tools/localization'))
from catalog import main as check_catalog
check_catalog()
resources=json.loads((base/'schemas/localization-resources.schema.json').read_text())
assert set(resources['properties'])==set(keys), 'E_SPEC_STALE: localization keys'
for descriptor in list((base/'examples').glob('*/ide/*.json'))+list((root/'localization').glob('*/ide/*.json')):
 d=json.loads(descriptor.read_text())
 if 'locale' in d and 'resource' in d:
  strings=json.loads((descriptor.parent.parent/d['resource']).read_text())
  jsonschema.Draft202012Validator(resources).validate(strings)
  for key,value in strings.items():assert set(re.findall(r'\{([a-z][a-zA-Z0-9]*)\}',value))==set(re.findall(r'\{([a-z][a-zA-Z0-9]*)\}',keys[key]))
assert "规范版本 **0.4.3**" in doc and "规范版本 **0.4.3**" in ui
print(f"PASS: schemas, embedded schema/manifest snapshots, {count} valid descriptors, illegal example rejected")
