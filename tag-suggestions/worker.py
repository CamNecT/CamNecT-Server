"""Private, offline adapter for the frozen community draft reader."""
import hashlib
import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
import signal
from socketserver import ForkingMixIn
import sys
from tempfile import TemporaryDirectory
from zipfile import ZipFile


ARCHIVE = Path("/opt/reader/camnect-v1.3.1.zip")
RELEASE_SHA256 = "a988b4c1209582541db1df0bcc18c2c5fcf334440aa00dc21a9b83e02762f4b9"


def load_reader(archive, directory):
    if hashlib.sha256(archive.read_bytes()).hexdigest() != RELEASE_SHA256:
        raise ValueError("Reader archive does not match v1.3.1")
    with ZipFile(archive) as package:
        package.extractall(directory)
    sys.path.insert(0, directory)
    from v13.patch1 import classify
    return classify


def suggest(classify, draft):
    title, body = draft["title"], draft["content"]
    if not isinstance(title, str) or not isinstance(body, str) or len(title) > 200 or len(body) > 20_000:
        raise ValueError("Invalid draft")
    if draft["boardCode"] not in ("INFO", "QUESTION"):
        raise ValueError("Unknown board")
    if not title.strip() and not body.strip():
        return []
    result = classify({"id": "draft", "url": "", "title": title, "body": body})
    candidates = []
    for item in result["auto_candidates"]:
        evidence = item.get("evidence")
        if not isinstance(evidence, str) or not evidence or not (evidence in title or evidence in body):
            evidence = None
        candidates.append({"id": int(item["tag_id"]), "evidence": evidence[:160] if evidence else None})
    return candidates


class DraftHandler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Drafts and request paths do not enter access logs.

    def do_GET(self):
        self.send_response(204 if self.path == "/ready" else 404)
        self.end_headers()

    def do_POST(self):
        if self.path != "/suggest":
            self.send_error(404)
            return
        signal.alarm(6)
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 140_000:
                self.send_error(413)
                return
            draft = json.loads(self.rfile.read(length))
            if not isinstance(draft, dict):
                raise ValueError("Expected draft")
            rows = suggest(self.server.classify, draft)
        except (KeyError, ValueError, TypeError):
            self.send_error(400)
            return
        payload = json.dumps(rows, ensure_ascii=False).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


class DraftServer(ForkingMixIn, HTTPServer):
    # Each draft and the private reader's text caches expire with the child.
    max_children = 2
    request_queue_size = 4


if __name__ == "__main__":
    with TemporaryDirectory() as runtime:
        classify = load_reader(ARCHIVE, runtime)
        classify({"id": "warmup", "url": "", "title": "준비", "body": ""})
        with DraftServer(("0.0.0.0", 8081), DraftHandler) as server:
            server.classify = classify
            server.serve_forever()
