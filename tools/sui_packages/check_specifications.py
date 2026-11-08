#!/usr/bin/env python3
"""Fail on stale normative schema/manifest appendices and invalid example descriptors.
Requires jsonschema (development dependency only, never used by Android).
"""
import json
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
for p in sorted((base / "examples").glob("*/manifest.json")):
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
assert "0.2.0" in doc and "0.2.0" in ui
print(f"PASS: schemas, embedded schema/manifest snapshots, {count} valid descriptors, illegal example rejected")
