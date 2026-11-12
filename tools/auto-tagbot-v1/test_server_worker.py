import copy
import json
from pathlib import Path
import unittest
from tempfile import TemporaryDirectory

from server_worker import classify_import, load_policy, run


class ServerWorkerTest(unittest.TestCase):
    def test_rule_scope_and_two_approval_routing(self):
        row = {"id": "120", "title": "취업준비콘서트", "body": ""}
        policy = load_policy()
        self.assertEqual(classify_import(row, policy)["assigned_tag_ids"], [107])
        other = copy.deepcopy(policy)
        next(rule for rule in other["rules"] if rule["id"] == "job-preparation-event")["support"] = 2
        result = classify_import(row, other)
        self.assertEqual(result["assigned_tag_ids"], [])
        self.assertEqual([x["tag_id"] for x in result["review_candidates"]], [107])

    def test_imports_revisions_restart_and_live_cohort(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            src, state = root / "imports", root / "ledger"
            src.mkdir()
            def stage(records):
                files = []
                for id_, rev in records:
                    digest = str(rev) * 64
                    name = f"{id_}-{rev}-{digest}.json"
                    row = {"id": id_, "revision": rev, "hash": digest, "activity_id": int(id_),
                           "title": "취업준비콘서트", "body": "", "url": ""}
                    (src / name).write_text(json.dumps(row), encoding="utf-8")
                    files.append(name)
                (src / "index.json").write_text(json.dumps({"board": 4084, "files": files, "exported": "start"}))
            stage([("120", 1)])
            self.assertEqual(run(src, state), 0)
            stage([("120", 2), ("121", 1)])
            self.assertEqual(run(src, state), 0)
            self.assertEqual(run(src, state), 0)
            report = json.loads((state / "candidates.json").read_bytes())
            self.assertEqual(report["counts"], {"bootstrap": 1, "live": 1})
            self.assertEqual([row["revision"] for row in report["notices"]], [2, 1])
            self.assertEqual(report["notices"][1]["result"]["assigned_tag_ids"], [107])
