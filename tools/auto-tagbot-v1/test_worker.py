import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

import worker
from score import score


class ProspectiveWorkerTest(unittest.TestCase):
    def test_only_new_ids_and_reuse_completed_independent_ballots(self):
        with TemporaryDirectory() as tmp, worker.database(":memory:") as db:
            folder = Path(tmp)
            worker.discover(db, ["10", "11"], True)
            worker.discover(db, ["11", "10", "12"], False)
            worker.discover(db, ["12", "11"], False)
            self.assertEqual(dict(db.execute("SELECT state,count(*) FROM notices GROUP BY state")),
                             {"baseline": 2, "pending": 1})
            row = {"id": "12", "title": "백엔드 인턴십 모집", "body": "", "url": "4084/12"}
            calls = []

            def judge(row, view, prompt, exe, cwd):
                calls.append(view)
                if view == "neutral" and calls.count(view) == 1:
                    raise RuntimeError("interrupted")
                items = [[84, "인턴십"]]
                if view != "search":
                    items.append([53, "백엔드"])
                return {"session": view, "model": worker.MODEL, "response": {
                    "12": {"route": "intern", "items": items, "roles": []}}}

            with patch.object(worker, "fetch", return_value=[row]) as fetch, patch.object(worker, "judge", judge):
                with self.assertRaisesRegex(RuntimeError, "interrupted"):
                    worker.process(db, "12", "unused", folder)
                self.assertIsNone(db.execute("SELECT result FROM notices WHERE id='12'").fetchone()[0])
                worker.process(db, "12", "unused", folder)
                fetch.assert_called_once_with("--ids", "12")
            self.assertCountEqual(calls, ["spec", "neutral", "search", "neutral"])
            self.assertEqual(worker.publish(db, folder), {"baseline": 2, "done": 1})
            report = json.loads((folder / "candidates.json").read_text(encoding="utf-8"))
            metrics = score(report, {"12": [84, 53]})
            self.assertEqual(metrics["unanimous"]["recall"], .5)
            self.assertEqual(metrics["including_review"]["precision"], 1)
            self.assertEqual(metrics["including_review"]["recall"], 1)
            with self.assertRaises(ValueError):
                score(report, {"11": []})

    def test_no_silent_rebaseline_or_empty_snapshot(self):
        with worker.database(":memory:") as db:
            with self.assertRaises(RuntimeError):
                worker.discover(db, ["10"], False)
            with self.assertRaises(ValueError):
                worker.discover(db, [], True)
            worker.discover(db, ["10"], True)
            with self.assertRaises(ValueError):
                worker.discover(db, ["12"], True)


if __name__ == "__main__":
    unittest.main()
