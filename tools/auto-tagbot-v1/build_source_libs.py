"""Build source-libs.zip with the crawler requirements installed in this interpreter."""

import argparse
import ast
from importlib import metadata
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo


def entries(crawler):
    for package, version in (("beautifulsoup4", "4.14.3"), ("soupsieve", "3.0.4"), ("typing_extensions", "4.16.0")):
        dist = metadata.distribution(package)
        if dist.version != version:
            raise ValueError(f"{package} must be {version}")
        for name in dist.files:
            if name.suffix == ".py" or "/licenses/" in str(name).replace("\\", "/"):
                yield name.as_posix(), dist.locate_file(name).read_bytes()

    for filename in ("__init__.py", "parser.py", "http.py"):
        yield f"konkuk_job/{filename}", (crawler / filename).read_bytes()
    # Use the repository's exact pagination logic without importing any publication or storage code.
    runner = (crawler / "runner.py").read_text(encoding="utf-8")
    function = next(node for node in ast.parse(runner).body if getattr(node, "name", "") == "collect_listing")
    body = ast.get_source_segment(runner, function)
    yield "konkuk_job/listing.py", ("from .parser import LIST_URL, SourceError, parse_list\n\n" + body + "\n").encode()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("crawler", type=Path, help="repository crawlers/konkuk-job/src/konkuk_job directory")
    args = parser.parse_args()
    with ZipFile(Path(__file__).with_name("source-libs.zip"), "w") as archive:
        for path, content in sorted(entries(args.crawler)):
            archive.writestr(ZipInfo(path), content, compress_type=ZIP_DEFLATED)
