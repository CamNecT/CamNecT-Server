"""Measure prospective tag performance on independently annotated 4084 IDs."""

import argparse
import json
from pathlib import Path


def score(report, gold):
    rows = {row["id"]: row for row in report["notices"]}
    if not isinstance(gold, dict) or not gold:
        raise ValueError("Expected a nonempty JSON object mapping article ID to tag IDs")
    if report.get("meta", {}).get("smoke"):
        raise ValueError("Use the prospective deployment ledger")
    scores = {}
    for label, fields in (("unanimous", ("auto_candidates",)),
                          ("including_review", ("auto_candidates", "review_candidates"))):
        tp = fp = fn = 0
        errors = []
        for key, tags in gold.items():
            if key not in rows or rows[key]["state"] != "done":
                raise ValueError(f"{key} has no completed prospective prediction")
            if not isinstance(tags, list) or any(type(tag) is not int for tag in tags) or len(set(tags)) != len(tags):
                raise ValueError(f"{key} needs unique integer reference tags")
            actual = set(tags)
            pred = {t["tag_id"] for f in fields for t in rows[key]["result"][f]}
            tp += len(actual & pred)
            fp += len(pred - actual)
            fn += len(actual - pred)
            if pred != actual:
                errors.append({"id": key, "extra": sorted(pred - actual), "missed": sorted(actual - pred)})
        p = tp / (tp + fp) if tp + fp else None
        r = tp / (tp + fn) if tp + fn else None
        scores[label] = {"articles": len(gold), "tp": tp, "fp": fp, "fn": fn,
                         "precision": p, "recall": r, "errors": errors,
                         "exceeds_targets": p is not None and r is not None and p > .95 and r > .85}
    return scores


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidates", type=Path)
    parser.add_argument("gold", type=Path, help='UTF-8 JSON, e.g. {"1234567": [84, 54], "1234568": []}')
    args = parser.parse_args()
    report = json.loads(args.candidates.read_text(encoding="utf-8-sig"))
    reference = json.loads(args.gold.read_text(encoding="utf-8-sig"))
    print(json.dumps(score(report, reference), ensure_ascii=False, indent=2))
