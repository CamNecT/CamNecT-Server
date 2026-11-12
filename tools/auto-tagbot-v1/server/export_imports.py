"""Export completed 4084 crawler imports into a local, read-only handoff for the bot."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
from urllib.parse import urlparse

import pymysql


def container_setting(name):
    return subprocess.check_output(
        ["/usr/bin/docker", "exec", "camnect-server", "printenv", name],
        text=True, timeout=10, stderr=subprocess.DEVNULL).strip()


def atomic_json(path, obj):
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as f:
        try:
            json.dump(obj, f, ensure_ascii=False)
            f.flush()
            os.fsync(f.fileno())
            os.fchmod(f.fileno(), 0o640)
        except BaseException:
            os.unlink(f.name)
            raise
    os.replace(f.name, path)


def export(directory):
    addr = urlparse(container_setting("SPRING_DATASOURCE_URL").removeprefix("jdbc:"))
    if addr.scheme != "mysql" or addr.path != "/camnect":
        raise RuntimeError("Unexpected source database")
    db = pymysql.connect(host=addr.hostname, port=addr.port or 3306, database="camnect",
                         user=container_setting("SPRING_DATASOURCE_USERNAME"),
                         password=container_setting("SPRING_DATASOURCE_PASSWORD"),
                         charset="utf8mb4", connect_timeout=8, read_timeout=30, autocommit=False)
    paths = []
    try:
        with db.cursor() as cur:
            cur.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ")
            cur.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY")
            cur.execute("SELECT i.source_id,i.activity_id,i.source_revision,i.content_hash "
                        "FROM activity_imports i JOIN external_activities a USING(activity_id) "
                        "WHERE i.source_id LIKE %s ORDER BY i.activity_id", (r"KONKUK\_JOB\_4084#%",))
            for source_id, activity, revision, digest in cur.fetchall():
                found = re.fullmatch(r"KONKUK_JOB_4084#([0-9]+)", source_id)
                if not found or int(revision) < 1 or not re.fullmatch("[0-9a-f]{64}", digest):
                    raise ValueError("Invalid crawler receipt")
                article = found[1]
                filename = f"{article}-{revision}-{digest}.json"
                path = directory / filename
                if not path.exists():
                    cur.execute("SELECT title, context, official_url FROM external_activities WHERE activity_id=%s",
                                (activity,))
                    title, body, url = cur.fetchone()
                    # Remove only the crawler's display suffix and deadline placeholder.
                    text = (body or "").partition("\n\n원문 및 관련 자료\n")[0]
                    text = text.removeprefix("신청기간은 원문에서 확인해 주세요.\n\n")
                    atomic_json(path, {"id": article, "activity_id": activity, "title": title,
                                       "body": text, "url": url, "revision": revision, "hash": digest})
                paths.append(filename)
    finally:
        db.rollback()
        db.close()
    atomic_json(directory / "index.json",
                {"board": 4084, "exported": datetime.now(timezone.utc).isoformat(), "files": paths})
    print(f"Exported {len(paths)} crawler import receipts")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    export(parser.parse_args().directory)
