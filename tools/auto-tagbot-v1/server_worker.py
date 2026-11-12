"""Assign tags to crawler imports offline. The only writable DB is the bot's own ledger."""
import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3

from offline_reader import classify

ROOT = Path(__file__).resolve().parent
READER_VERSION = "1.0"
SNAPSHOT = re.compile(r"([0-9]+)-([1-9][0-9]*)-([a-f0-9]{64})\.json")


def load_policy():
    policy_bytes = (ROOT / "server/policy.json").read_bytes()
    policy = json.loads(policy_bytes)
    manifest = json.loads((ROOT / "server/policy-review.json").read_bytes())
    reviews = manifest["reviews"]
    ids = {r["id"] for r in policy["rules"]}
    if (manifest["sha256"] != hashlib.sha256(policy_bytes).hexdigest()
            or manifest["scope"] != "development_rule_review" or len(reviews) != 3
            or len({v["session"] for v in reviews}) != 3
            or any(v["model"] != "gpt-6-astra" or set(v["rules"]) != ids for v in reviews)):
        raise ValueError("Rule review does not match this release")
    for rule in policy["rules"]:
        votes = [v["rules"][rule["id"]]["approve"] for v in reviews]
        if any(type(x) != bool for x in votes):
            raise ValueError("Invalid development ballot")
        rule["support"] = sum(votes)
    return policy


def classify_import(row, policy):
    decision = classify(row)
    auto, review, remaining = [], [], []
    title = row["title"]
    excluded = re.search(policy["exclude_title"], title, re.I)
    for tag in decision["suggestions"]:
        evidence = tag["evidence"]
        if not evidence or not any(evidence in row[key] for key in ("title", "body")):
            raise ValueError("Classifier returned evidence outside the source")
        applicable = [r for r in policy["rules"] if not excluded and tag["tag_id"] in r["tags"]
                      and re.search(r["title"], title, re.I)
                      and not (r.get("exclude") and re.search(r["exclude"], title, re.I))
                      and (not r.get("quote_in_title") or evidence in title)]
        best = max(applicable, key=lambda r: r["support"], default=None)
        item = dict(tag, rule=best["id"] if best else None, rule_approvals=best["support"] if best else 0)
        (auto if item["rule_approvals"] == 3 else review if item["rule_approvals"] == 2 else remaining).append(item)
    return {"assigned_tag_ids": [t["tag_id"] for t in auto], "auto_candidates": auto,
            "review_candidates": review, "other_suggestions": remaining, "role_index": decision["role_index"]}


def run(source, state):
    index = json.loads((source / "index.json").read_bytes())
    if index["board"] != 4084 or not isinstance(index["files"], list):
        raise ValueError("Invalid 4084 handoff")
    files = {}
    for filename in index["files"]:
        match = SNAPSHOT.fullmatch(filename)
        if not match or match[1] in files:
            raise ValueError("Invalid or duplicate import")
        files[match[1]] = (filename, int(match[2]), match[3])
    policy = load_policy()
    state.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(state / "candidates.sqlite3")
    try:
        db.executescript("""
            CREATE TABLE IF NOT EXISTS articles (id TEXT PRIMARY KEY, cohort TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS outputs (
                id TEXT, revision INTEGER, digest TEXT, version TEXT, data TEXT NOT NULL,
                PRIMARY KEY (id,revision,digest,version)
            );
            CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL);
        """)
        first = not db.execute("SELECT 1 FROM metadata WHERE key='baseline'").fetchone()
        with db:
            db.executemany("INSERT OR IGNORE INTO articles VALUES (?,?)",
                           ((k, "bootstrap" if first else "live") for k in files))
            db.execute("INSERT OR IGNORE INTO metadata VALUES ('baseline',?)", (index["exported"],))
        report, errors = [], []
        for article, (filename, rev, digest) in files.items():
            key = (article, rev, digest, READER_VERSION)
            cached = db.execute("SELECT data FROM outputs WHERE id=? AND revision=? AND digest=? AND version=?",
                                key).fetchone()
            if cached:
                report.append(json.loads(cached[0]))
                continue
            try:
                row = json.loads((source / filename).read_bytes())
                if (row["id"], row["revision"], row["hash"]) != key[:3]:
                    raise ValueError("Receipt mismatch")
                result = classify_import(row, policy)
                cohort = db.execute("SELECT cohort FROM articles WHERE id=?", (article,)).fetchone()[0]
                item = {"id": article, "activity_id": row["activity_id"], "title": row["title"], "url": row["url"],
                        "revision": rev, "hash": digest, "cohort": cohort, "state": "done",
                        "body_available": bool(row["body"].strip()), "result": result}
                with db:
                    db.execute("INSERT INTO outputs VALUES (?,?,?,?,?)", (*key, json.dumps(item, ensure_ascii=False)))
                report.append(item)
            except Exception as exc:
                errors.append({"id": article, "error": str(exc)})
        counts = dict(Counter(item["cohort"] for item in report))
        out = {"board": 4084, "version": READER_VERSION, "input": "activity_imports",
               "rule_review_scope": "local_development_only", "counts": counts, "errors": errors,
               "meta": {"baseline": db.execute("SELECT value FROM metadata WHERE key='baseline'").fetchone()[0]},
               "updated": datetime.now(timezone.utc).isoformat(), "notices": report}
        tmp = state / "candidates.tmp"
        tmp.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
        os.replace(tmp, state / "candidates.json")
        print(f"Processed {len(report)} crawler imports, {len(errors)} errors, cohorts {counts}", flush=True)
        return int(bool(errors))
    finally:
        db.close()


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--input", required=True, type=Path)
    p.add_argument("--state", required=True, type=Path)
    args = p.parse_args()
    raise SystemExit(run(args.input, args.state))
