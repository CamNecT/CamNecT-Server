"""Evaluate three separate Astra responses to the frozen employment reader."""

import hashlib
import json
import types
from collections import Counter
from pathlib import Path


FROZEN = Path(__file__).resolve().parent / "frozen"
LOCK_SHA256 = "2294adc802e762855c3e2619fe523ec204dd657250dd079a197a755016ac6d87"
VIEWS = {
    "spec": "model2/prompt.txt",
    "neutral": "prompt-neutral.txt",
    "search": "prompt-search.txt",
}


def _read_resources():
    lock = (FROZEN / "final-reader-lock.json").read_bytes()
    if hashlib.sha256(lock).hexdigest() != LOCK_SHA256:
        raise ValueError("Frozen employment reader manifest differs from release")
    files = {}
    for name, digest in json.loads(lock)["model"].items():
        data = (FROZEN / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != digest:
            raise ValueError(f"Frozen employment reader differs from release: {name}")
        files[name] = data
    return files


_files = _read_resources()
_source = str(FROZEN / "model2" / "select_tags.py")
_rules = types.ModuleType("employment_rules_v2")
_rules.__file__ = _source
# Execute exactly the bytes checked above. Ignore any Python bytecode caches.
exec(compile(_files["model2/select_tags.py"], _source, "exec"), _rules.__dict__)
parse_json = _rules.decode
_labels = {int(k): v for k, v in parse_json(_files["model2/dictionary.json"]).items()}
_allowed = set(_labels) - _rules.NONCANONICAL
_prompts = {view: _files[name].decode("utf-8") for view, name in VIEWS.items()}


def _source_row(row):
    notice_id = row.get("id")
    if type(notice_id) not in (str, int) or not str(notice_id).strip():
        raise ValueError("Missing notice ID")
    if not isinstance(row.get("title"), str) or not row["title"].strip():
        raise ValueError("Missing notice title")
    if not isinstance(row.get("body"), str):
        raise ValueError("Missing notice body")
    return {key: row[key] for key in ("id", "title", "body")}


def prompts_for(row):
    """Return independent, full prompts. Each response must have the exact notice ID as its only key."""
    data = json.dumps(_source_row(row), ensure_ascii=False)
    return {view: prompt + "\n\n입력 공고 JSON\n" + data for view, prompt in _prompts.items()}


def validate_ballot(row, view, response):
    """Validate one parsed, ID-wrapped JSON ballot. Return the frozen tag and role selections."""
    source = _source_row(row)
    notice_id = str(source["id"])
    if view not in VIEWS:
        raise ValueError(f"Unknown Astra view: {view!r}")
    if not isinstance(response, dict) or set(response) != {notice_id}:
        raise ValueError(f"{view}: expected a ballot for {notice_id} only")
    ballot = response[notice_id]
    if not isinstance(ballot, dict) or set(ballot) != {"route", "items", "roles"}:
        raise ValueError(f"{view}: expected route, items and roles")
    if not isinstance(ballot["route"], str):
        raise ValueError(f"{view}: route must be a string")
    return _rules.choose(source, ballot, _allowed)


def _rank(selections, roles=False):
    evidence = {}
    points = Counter()
    for view, pairs in selections.items():
        for rank, (tag, quote) in enumerate(pairs):
            evidence.setdefault(tag, {})[view] = quote
            points[tag] += (len(pairs) if roles else 4) - rank
    ranked = sorted(evidence, key=lambda tag: (-len(evidence[tag]), -points[tag], tag))
    return [
        {"tag_id": tag, "label": _labels[tag], "votes": len(evidence[tag]), "evidence": evidence[tag]}
        for tag in ranked if len(evidence[tag]) >= 2
    ]


def decide(row, ballots):
    """Route public tag candidates by per-tag agreement and preserve the frozen role index."""
    if not isinstance(ballots, dict) or set(ballots) != set(VIEWS):
        raise ValueError("Expected exactly one complete ballot from each of spec, neutral and search")
    tags_by_view, roles_by_view = {}, {}
    for view in VIEWS:
        tags_by_view[view], roles_by_view[view] = validate_ballot(row, view, ballots[view])
    ranked = _rank(tags_by_view)
    return {
        "auto_candidates": [entry for entry in ranked if entry["votes"] == 3],
        "review_candidates": [entry for entry in ranked if entry["votes"] == 2],
        # Retain all 2/3 candidates for review, including rare ties outside the frozen public four.
        "public_tag_ids": [entry["tag_id"] for entry in ranked[:4]],
        "role_index": _rank(roles_by_view, roles=True),
    }
