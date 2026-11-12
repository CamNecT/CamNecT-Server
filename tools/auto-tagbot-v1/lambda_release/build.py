"""Prepare a bot-only overlay for the existing job-only AWS Lambda ZIP."""
import argparse
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

HERE = Path(__file__).resolve().parent
BOT = HERE.parent
PUBLISHER = BOT.parents[1] / "crawlers/konkuk-job/src/konkuk_job/publisher.py"


def build(target, version):
    files = {"konkuk_job/publisher.py": PUBLISHER, "apply_overlay.py": HERE / "apply_overlay.py"}
    for name in ("offline_reader.py", "frozen/model2/select_tags.py", "frozen/model2/dictionary.json"):
        files[name] = BOT / name
    if version == "1.0":
        for name in ("server_worker.py", "server/policy.json", "server/policy-review.json"):
            files[name] = BOT / name
        adapter = "from server_worker import classify_import, load_policy\nPOLICY = load_policy()\ndef classify(row):\n    return classify_import(row, POLICY)\n"
    else:
        files["v11/reader.py"] = BOT / "v11/reader.py"
        adapter = "from v11.reader import classify\n"
    with ZipFile(target, "x", ZIP_DEFLATED) as archive:
        for name, path in files.items():
            archive.write(path, name)
        archive.writestr("camnect_tagger.py", f"READER_VERSION = {version!r}\n" + adapter)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version", choices=["1.0", "1.1"])
    parser.add_argument("target", type=Path)
    args = parser.parse_args()
    build(args.target, args.version)
