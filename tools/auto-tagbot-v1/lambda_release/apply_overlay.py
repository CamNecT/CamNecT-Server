"""Patch only the known live job-only crawler, preserving its deployed parser and dependencies."""
import hashlib
import re
import sys
from zipfile import ZipFile, ZIP_DEFLATED

EXPECTED = {"app.py": "d0622c27340a9b42", "konkuk_job/runner.py": "33a7350cd0e8a59b",
            "konkuk_job/publisher.py": "0eb9f49e3de4e00d", "konkuk_job/storage.py": "29a4cf3e3361655d"}


def once(src, old, new):
    if src.count(old) != 1:
        raise ValueError("Unexpected live crawler layout")
    return src.replace(old, new)


def assemble(original, patch, destination):
    with ZipFile(original) as base, ZipFile(patch) as overlay:
        for file, digest in EXPECTED.items():
            if not hashlib.sha256(base.read(file)).hexdigest().startswith(digest):
                raise ValueError(f"Unexpected original Lambda code in {file}")
        app = base.read("app.py").decode()
        start = app.index("    base_url = os.environ['CAMNECT_API_BASE_URL']")
        end = app.index("    try:\n", start)
        app = app[:start] + "    publisher = Publisher('', '', job_mode='shadow')\n" + app[end:]
        runner = base.read("konkuk_job/runner.py").decode()
        start = runner.index("            receipt = store.get_publication(article_id)")
        end = runner.index("            return ", start)
        runner = runner[:start] + "            pending = not meta\n" + runner[end:]
        runner = once(runner, "                summary['published'] += 1\n", "")
        runner = once(runner, "                status = receipt['status']\n",
                      "                status = receipt['status']\n                if status not in ('SHADOW_TAGGED', 'AWAITING_TAGS'):\n                    summary['published'] += 1\n")
        additions = {key: overlay.read(key) for key in overlay.namelist() if key != "apply_overlay.py"}
        additions.update({"app.py": app.encode(), "konkuk_job/runner.py": runner.encode()})
        for name, data in additions.items():
            if name.endswith(".py"):
                compile(data, name, "exec")
        with ZipFile(destination, "x", ZIP_DEFLATED) as result:
            for name in base.namelist():
                if name not in additions and "__pycache__/" not in name:
                    result.writestr(base.getinfo(name), base.read(name))
            for name, data in additions.items():
                result.writestr(name, data)


if __name__ == "__main__":
    assemble(*sys.argv[1:])
