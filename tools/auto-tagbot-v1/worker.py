"""Konkuk job board 4084 candidate ledger. No application DB connection."""

import argparse
import concurrent.futures
import hashlib
import json
import logging
import os
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
from contextlib import contextmanager
from datetime import datetime, timezone

from votes import VIEWS, decide, parse_json, prompts_for, validate_ballot


ROOT = Path(__file__).resolve().parent
MODEL = "gpt-6-astra"
HIDDEN = getattr(subprocess, "CREATE_NO_WINDOW", 0)


def now():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def dump(obj):
    return json.dumps(obj, ensure_ascii=False, separators=(",", ":"))


def fetch(*args):
    proc = subprocess.run(
        [sys.executable, "-X", "utf8", str(ROOT / "source4084.py"), *args],
        capture_output=True, timeout=240, creationflags=HIDDEN,
    )
    if proc.returncode:
        raise RuntimeError("4084 fetch failed: " + proc.stderr.decode("utf-8", "replace")[-900:])
    return json.loads(proc.stdout)


def judge(row, view, prompt, executable, workspace):
    with tempfile.TemporaryDirectory(dir=workspace) as tmp:
        out = Path(tmp) / "ballot.json"
        args = [executable, "exec", "--ephemeral", "--ignore-user-config",
                "--skip-git-repo-check", "-s", "read-only", "--disable", "apps",
                "--disable", "plugins", "--disable", "multi_agent", "--disable", "shell_tool",
                "-c", 'web_search="disabled"', "-c", 'model_reasoning_effort="high"',
                "-m", MODEL, "--json", "-C", tmp, "-o", str(out), "-"]
        proc = subprocess.run(args, input=prompt.encode(), capture_output=True, timeout=360,
                              cwd=tmp, creationflags=HIDDEN)
        if proc.returncode or not out.is_file():
            raise RuntimeError(f"{view}: Codex failed ({proc.returncode}): " +
                               proc.stderr.decode("utf-8", "replace")[-500:])
        events = [json.loads(line) for line in proc.stdout.splitlines() if line.strip()]
        session = next((e["thread_id"] for e in events if e["type"] == "thread.started"), None)
        if not session or not any(e["type"] == "turn.completed" for e in events):
            raise ValueError(f"{view}: incomplete Astra turn")
        for event in events:
            item = event.get("item", {})
            if item and item.get("type") not in {"agent_message", "reasoning"}:
                raise ValueError(f"{view}: unexpected tool use ({item.get('type')})")
        ballot = parse_json(out.read_text(encoding="utf-8-sig"))
        validate_ballot(row, view, ballot)
        return {"model": MODEL, "session": session, "at": now(), "response": ballot}


def database(path):
    db = sqlite3.connect(path)
    db.row_factory = sqlite3.Row
    db.executescript("""
        CREATE TABLE IF NOT EXISTS notices (
            id TEXT PRIMARY KEY, state TEXT NOT NULL, discovered TEXT NOT NULL,
            source TEXT, result TEXT, error TEXT, attempted TEXT
        );
        CREATE TABLE IF NOT EXISTS ballots (
            id TEXT NOT NULL, view TEXT NOT NULL, record TEXT NOT NULL,
            PRIMARY KEY(id, view)
        );
        CREATE TABLE IF NOT EXISTS meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);
    """)
    return db


def discover(db, ids, baseline):
    if not isinstance(ids, list) or not ids or any(not isinstance(i, str) or not i.isdecimal() for i in ids):
        raise ValueError("4084 must return a nonempty list of numeric article IDs")
    initialized = db.execute("SELECT 1 FROM meta WHERE key='started'").fetchone()
    if not initialized and not baseline:
        raise RuntimeError("Missing deployment baseline. Run init explicitly.")
    stamp = now()
    with db:
        if baseline and initialized:
            raise ValueError("Baseline already established")
        if baseline:
            db.execute("INSERT INTO meta VALUES ('started',?)", (stamp,))
        db.executemany("INSERT OR IGNORE INTO notices(id,state,discovered) VALUES (?,?,?)",
                       ((i, "baseline" if baseline else "pending", stamp) for i in ids))
        db.execute("INSERT OR REPLACE INTO meta VALUES ('last_index',?)", (stamp,))


def process(db, notice_id, executable, workspace):
    saved = db.execute("SELECT source FROM notices WHERE id=?", (notice_id,)).fetchone()[0]
    if saved is None:
        batch = fetch("--ids", notice_id)
        if not isinstance(batch, list) or len(batch) != 1 or batch[0]["id"] != notice_id:
            raise ValueError("Unexpected article in 4084 detail response")
        row = batch[0]
        prompts_for(row)
        with db:
            db.execute("UPDATE notices SET source=? WHERE id=?", (dump(row), notice_id))
    else:
        row = json.loads(saved)
    records = {r["view"]: json.loads(r["record"])
               for r in db.execute("SELECT view,record FROM ballots WHERE id=?", (notice_id,))}
    failures = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        jobs = {pool.submit(judge, row, view, prompt, executable, workspace): view
                for view, prompt in prompts_for(row).items() if view not in records}
        for job in concurrent.futures.as_completed(jobs):
            view = jobs[job]
            try:
                records[view] = job.result()
                with db:
                    db.execute("INSERT INTO ballots VALUES (?,?,?)", (notice_id, view, dump(records[view])))
            except Exception as exc:
                failures.append(str(exc))
    if failures:
        raise RuntimeError(" | ".join(failures))
    if set(records) != set(VIEWS) or len({r["session"] for r in records.values()}) != 3:
        raise ValueError("Expected three independent Astra sessions")
    result = decide(row, {view: record["response"] for view, record in records.items()})
    result.update(decided=now(), source_sha256=hashlib.sha256(dump(row).encode()).hexdigest())
    with db:
        db.execute("UPDATE notices SET state='done',result=?,error=NULL WHERE id=?", (dump(result), notice_id))


def publish(db, directory):
    counts = dict(db.execute("SELECT state,COUNT(*) FROM notices GROUP BY state"))
    records = []
    for row in db.execute("SELECT * FROM notices WHERE state!='baseline' ORDER BY CAST(id AS INTEGER) DESC"):
        item = dict(row)
        item["source"] = json.loads(item["source"]) if item["source"] else None
        item["result"] = json.loads(item["result"]) if item["result"] else None
        records.append(item)
    report = {"version": "1.0", "board": 4084, "updated": now(),
              "meta": dict(db.execute("SELECT key,value FROM meta")), "counts": counts, "notices": records}
    dest = directory / "candidates.json"
    tmp = dest.with_suffix(".tmp")
    tmp.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    os.replace(tmp, dest)
    return counts


@contextmanager
def exclusive(path):
    import msvcrt
    with path.open("a+b") as lock:
        if lock.tell() == 0:
            lock.write(b"0")
            lock.flush()
        lock.seek(0)
        msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        try:
            yield
        finally:
            lock.seek(0)
            msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


def run(args):
    directory = args.state.resolve()
    directory.mkdir(parents=True, exist_ok=True)
    with exclusive(directory / "worker.lock"), database(directory / "candidates.sqlite3") as db:
        if args.command == "smoke":
            # Smoke has its own ledger and cannot enqueue a deployment baseline article.
            if db.execute("SELECT 1 FROM meta WHERE key='started'").fetchone():
                raise ValueError("Smoke requires its own state directory")
            if args.id not in fetch("--index"):
                raise ValueError("Smoke article missing from current 4084 index")
            with db:
                db.execute("INSERT OR IGNORE INTO notices(id,state,discovered) VALUES (?,'pending',?)",
                           (args.id, now()))
                db.execute("INSERT OR REPLACE INTO meta VALUES ('smoke','true')")
        else:
            if db.execute("SELECT 1 FROM meta WHERE key='smoke'").fetchone():
                raise ValueError("Production polling requires a separate directory from smoke")
            discover(db, fetch("--index"), args.command == "init")
        if args.command != "init":
            items = db.execute("SELECT id FROM notices WHERE state='pending' "
                               "ORDER BY attempted IS NOT NULL, attempted, CAST(id AS INTEGER) LIMIT ?",
                               (args.limit,)).fetchall()
            for (notice_id,) in items:
                with db:
                    db.execute("UPDATE notices SET attempted=? WHERE id=?", (now(), notice_id))
                try:
                    process(db, notice_id, args.codex, directory)
                except Exception as exc:
                    with db:
                        db.execute("UPDATE notices SET error=? WHERE id=?", (str(exc), notice_id))
                    logging.exception("4084 article %s pending retry", notice_id)
        counts = publish(db, directory)
        logging.info("%s complete %s", args.command, counts)
        if sys.stdout is not None:
            print(dump(counts))
        return int(db.execute("SELECT COUNT(*) FROM notices WHERE state='pending' AND error IS NOT NULL").fetchone()[0] > 0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["init", "poll", "smoke"])
    parser.add_argument("--state", required=True, type=Path)
    parser.add_argument("--codex", default="codex.exe")
    parser.add_argument("--id", help="Existing 4084 ID to verify in the separate smoke ledger")
    parser.add_argument("--limit", type=int, default=8)
    arguments = parser.parse_args()
    arguments.state.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(filename=arguments.state / "worker.log", level=logging.INFO,
                        format="%(asctime)s %(levelname)s %(message)s", encoding="utf-8")
    try:
        sys.exit(run(arguments))
    except Exception:
        logging.exception("Worker failed")
        raise
