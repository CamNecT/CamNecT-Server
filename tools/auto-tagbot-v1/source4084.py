"""Read public Konkuk employment notices. Only print a result after a complete read."""

import argparse
import json
from pathlib import Path
import re
import sys


LIBS = Path(__file__).resolve().with_name("source-libs.zip")


def article_id(value):
    if not re.fullmatch(r"[1-9][0-9]{0,19}", value):
        raise argparse.ArgumentTypeError("expected a numeric 4084 article ID")
    return value


def read_notices(index_only=False, ids=None):
    if not LIBS.is_file():
        raise RuntimeError("Missing source-libs.zip from the bot release")
    sys.path.insert(0, str(LIBS))
    from konkuk_job.http import HttpClient
    from konkuk_job.parser import BASE, parse_detail
    from konkuk_job.listing import collect_listing

    client = HttpClient()
    client.load_robots()
    if ids is None:
        listing, _ = collect_listing(client, max_pages=100)
        ids = sorted(listing, key=int)
    if index_only:
        return ids
    notices = []
    for key in dict.fromkeys(ids):
        url = f"{BASE}/bbs/job/4084/{key}/artclView.do"
        html, _ = client.html(url)
        item = parse_detail(html, key)
        notices.append({"id": key, "title": item["title"], "body": item["bodyText"], "url": url})
    return notices


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--index", action="store_true", help="read all listing pages, emit article IDs only")
    mode.add_argument("--ids", nargs="+", type=article_id, help="read the detail pages for the supplied IDs")
    args = parser.parse_args()
    rows = read_notices(args.index, args.ids)
    # ASCII escapes are valid UTF-8, including when a Windows terminal uses a legacy code page.
    print(json.dumps(rows, ensure_ascii=True, allow_nan=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
