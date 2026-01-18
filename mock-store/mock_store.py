#!/usr/bin/env python3
"""
Mini “Store” for local testing.
Run:  python mock_store.py
"""

from flask import Flask, jsonify, send_from_directory, request
import hashlib, os, pathlib
import re

_ver_re = re.compile(r'_(\d+\.\d+\.\d+)\.zip$')  # e.g. BackupAgent_1.2.3.zip
def _version(fname: str) -> str:
    m = _ver_re.search(fname)
    return m.group(1) if m else "0.0.0"

app = Flask(__name__)
DATA_DIR = pathlib.Path(__file__).parent / "data"

# ── helpers ──────────────────────────────────────────────────────────
def sha256sum(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

# ── routes ───────────────────────────────────────────────────────────
@app.route("/catalog")
def catalog():
    entries = [{"packageName": f.name, 
                "sha256": sha256sum(f),
                "version": _version(f.name)}
               for f in DATA_DIR.glob("*.zip")]
    return jsonify(entries)                       # ← StoreClient.fetchCatalog()

@app.route("/packages/<fname>")
def pkg(fname):
    return send_from_directory(DATA_DIR, fname, as_attachment=True)

# NEW: simple upload endpoint
@app.route("/upload", methods=["POST"])
def upload():
    # ── A. multipart/form-data (what request.files expects) ──────────
    f = request.files.get("file")
    if f:
        dest = DATA_DIR / f.filename
        f.save(dest)
        print(f"Received (multipart) ⇒ {dest}")
        return "ok"

    # ── B. raw application/zip body ──────────────────────────────────
    if request.content_type == "application/zip":
        fname = request.headers.get("X-Filename")
        if not fname:
            return "missing X-Filename header", 400
        dest = DATA_DIR / fname
        dest.write_bytes(request.get_data())
        print(f"Received (raw)      ⇒ {dest}")
        return "ok"

    return "unsupported content-type", 415


# ── main ─────────────────────────────────────────────────────────────
if __name__ == "__main__":
    port = 5001
    print(f"Mock store listening on http://localhost:{port}")
    app.run(port=port, debug=False)
