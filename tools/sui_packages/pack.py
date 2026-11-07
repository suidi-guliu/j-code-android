#!/usr/bin/env python3
"""sui-code package authoring tool, format 1.0. Does not execute package code."""
import argparse
import base64
import hashlib
import io
import json
import pathlib
import struct
import subprocess
import tempfile
import zipfile

MAGIC = {"suic": b"SUICPKG\0", "suik": b"SUIKPKG\0"}
DOMAIN = b"SUI-PACK-SIGNATURE-V1\n"


def canonical(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False) + "\n").encode()


def build(source, destination, key=None):
    source = pathlib.Path(source)
    manifest = json.loads((source / "manifest.json").read_text())
    kind = manifest["kind"]
    if pathlib.Path(destination).suffix != "." + kind:
        raise ValueError("E_KIND: destination suffix differs from manifest")
    files = {}
    for p in sorted(source.rglob("*")):
        if p.is_symlink():
            raise ValueError("E_PATH: symlinks are forbidden")
        if p.is_file():
            name = p.relative_to(source).as_posix()
            if name in ("checksums.sha256", "signature.sig"):
                continue
            data = p.read_bytes()
            files[name] = canonical(json.loads(data)) if name.endswith(".json") else data
    checks = "".join(hashlib.sha256(data).hexdigest() + "  " + name + "\n" for name, data in sorted(files.items())).encode()
    files["checksums.sha256"] = checks
    if key:
        with tempfile.TemporaryDirectory() as tmp:
            data = pathlib.Path(tmp) / "signed"
            data.write_bytes(DOMAIN + MAGIC[kind] + checks)
            signature = subprocess.check_output(["openssl", "dgst", "-sha256", "-sign", str(key), str(data)])
            public = subprocess.check_output(["openssl", "pkey", "-in", str(key), "-pubout", "-outform", "DER"])
            files["signature.sig"] = canonical({"algorithm": "ECDSA-P256-SHA256", "publicKey": base64.b64encode(public).decode(), "signature": base64.b64encode(signature).decode()})
    payload = io.BytesIO()
    with zipfile.ZipFile(payload, "w", compression=zipfile.ZIP_DEFLATED) as z:
        for name, data in sorted(files.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, data)
    encoded = payload.getvalue()
    output = MAGIC[kind] + struct.pack(">HHQ", 1, 0, len(encoded)) + encoded
    pathlib.Path(destination).write_bytes(output)
    return {"file": str(destination), "bytes": len(output), "sha256": hashlib.sha256(output).hexdigest(), "signed": bool(key)}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source")
    parser.add_argument("destination")
    parser.add_argument("--key", help="Local ECDSA P-256 private key; never include it in a package")
    args = parser.parse_args()
    print(json.dumps(build(args.source, args.destination, args.key)))
