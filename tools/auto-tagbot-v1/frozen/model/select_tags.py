"""Validate offline employment reader output and select up to four evidenced tags."""
import argparse
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
NONCANONICAL = {78, 112, 117, 118, 122}
# A positive, named internship track or a committed appointment must overlap evidence.
# Plain "전환" is common in a discretionary benefit and never suffices by itself.
INTERN_WORD = r"(?:인턴(?:십|사원)?|intern(?:ship|s)?)"
FORMAL_TRACK = (
    r"(?:(?:채용|입사|정규직|정직원|정사원)\s*(?:연계|전환|전제|조건)\s*형"
    r"|(?:전환|전제|채용)\s*형|정규직\s*(?:채용|입사)(?:을|를)?\s*전제)"
)
OFFICIAL = re.compile(
    rf"{FORMAL_TRACK}[\s()·:/-]{{0,6}}(?:[가-힣A-Za-z]+\s+){{0,2}}{INTERN_WORD}"
    rf"|{INTERN_WORD}[\s(·:/-]{{0,6}}{FORMAL_TRACK}"
    r"|(?:전원|모두|자동(?:으로)?|무조건)\s*(?:정규직|정직원)(?:으로)?\s*(?:전환|채용|임용)"
    r"|(?:정규직|정직원)\s*(?:전환|채용)(?:을|이|이\s*)?\s*(?:보장|확정)"
    r"|guaranteed\s+(?:full[- ]time\s+)?employment", re.I
)
NON_INTERN = r"(?:신입(?:사원|직원|직)?|경력(?:사원|직원|직|자)?|정규직|정직원|계약직|full[- ]time)"
JOINT = re.compile(
    rf"(?:{NON_INTERN}\s*(?:채용|공채|모집)?\s*(?:/|및|과|와|또는|,|·|&|\+|and)\s*(?:[가-힣A-Za-z0-9]+\s+){{0,3}}{INTERN_WORD}"
    rf"|{INTERN_WORD}\s*(?:채용|공채|모집)?\s*(?:/|및|과|와|또는|,|·|&|\+|and)\s*(?:[가-힣A-Za-z0-9]+\s+){{0,3}}{NON_INTERN})", re.I
)
DIRECT = re.compile(
    rf"{NON_INTERN}\s*(?:공개\s*)?(?:채용|공채|모집|공석|포지션|트랙|부문|직군|직무|전형|인원)"
    rf"|(?:고용\s*형태|모집\s*(?:직군|부문|유형)|채용\s*구분)\s*[:：]\s*{NON_INTERN}", re.I
)
CONTINGENT = re.compile(
    r"전환|전제|연계|우수자|우수\s*인턴|우대|가능|검토|기회|혜택|수료\s*후|종료\s*후|가점|채용\s*형|"
    r"(?:[가-힣]+\s*)?에\s*한해|경력\s*(?:우대|필수)|경험자", re.I
)
CLAUSE = re.compile(r"[^.!?。;\n\r■▶●•]+")


def supports(source, quote, kind):
    evidence_words = (
        r"연계|전제|조건|전환|채용|보장|확정|전원|guaranteed|employment"
        if kind == "conversion" else NON_INTERN
    )
    if not re.search(evidence_words, quote, re.I):
        return False
    for field in ("title", "body"):
        original = source[field]
        for hit in re.finditer(re.escape(quote), original):
            for clause in CLAUSE.finditer(original):
                if clause.start() >= hit.end():
                    break
                if clause.end() <= hit.start():
                    continue
                value = clause.group()
                patterns = [OFFICIAL] if kind == "conversion" else [JOINT, DIRECT]
                for pattern in patterns:
                    for cue in pattern.finditer(value):
                        start, end = clause.start() + cue.start(), clause.start() + cue.end()
                        if end <= hit.start() or hit.end() <= start:
                            continue
                        if pattern is JOINT and re.search(
                            r"^\s*(?:전환|채용\s*(?:기회|가능)|우대|지원\s*가능)",
                            value[cue.end():cue.end() + 18]
                        ):
                            continue
                        if pattern is DIRECT:
                            nearby = value[max(0, cue.start() - 38):cue.end() + 30]
                            if CONTINGENT.search(nearby) or re.search(INTERN_WORD, nearby, re.I):
                                continue
                        if pattern is OFFICIAL and not re.search(INTERN_WORD, cue.group(), re.I):
                            local = value[max(0, cue.start() - 35):cue.end() + 18]
                            if re.search(r"우수\s*(?:자|인턴)|검토|가능|한[하해]|일부|선발된|평가에\s*따라", local):
                                continue
                        if pattern is OFFICIAL and re.search(
                            r"아니|아님|없음|하지\s*않|보장하지|보장되지|미보장",
                            value[cue.end():cue.end() + 16]
                        ):
                            continue
                        return True
    return False
def unique_keys(pairs):
    obj = {}
    for key, value in pairs:
        if key in obj:
            raise ValueError(f"Duplicate key {key!r}")
        obj[key] = value
    return obj


def decode(data):
    return json.loads(data, object_pairs_hook=unique_keys)


def select(source, raw, allowed):
    route = raw["route"]
    items = raw["items"]
    if route not in {"ordinary", "intern", "conversion", "parallel"}:
        raise ValueError(f"Invalid route {route!r}")
    if not isinstance(items, list) or len(items) > 8:
        raise ValueError("Expected up to eight items")
    quotes = {}
    for item in items:
        if not isinstance(item, list) or len(item) != 2:
            raise ValueError(f"Invalid pair {item!r}")
        tag, quote = item
        if type(tag) is not int or tag not in allowed or tag in quotes:
            raise ValueError(f"Invalid or repeated tag {tag!r}")
        if not isinstance(quote, str) or len(quote.strip()) < 2 or not any(
            quote in source[field] for field in ("title", "body")
        ):
            raise ValueError(f"Invalid quote for tag {tag}: {quote!r}")
        quotes[tag] = quote
    if (84 in quotes) != (route != "ordinary"):
        raise ValueError("Internship evidence and recruitment route disagree")
    if route in {"parallel", "conversion"} and 107 not in quotes:
        raise ValueError("Missing hiring evidence for declared recruitment route")
    if route == "ordinary":
        order = list(quotes)
    else:
        roles = [tag for tag in quotes if tag not in {84, 107}]
        order = [84]
        if route == "parallel" and supports(source, quotes[107], "parallel"):
            order.append(107)
        order.extend(roles)
        if route == "conversion" and supports(source, quotes[107], "conversion"):
            order.append(107)
    return [[tag, quotes[tag]] for tag in order[:4]]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ("source", "raw", "output"):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    rows = {}
    for line in args.source.read_text(encoding="utf-8-sig").splitlines():
        if line.strip():
            row = decode(line)
            key = str(row["id"])
            if key in rows or not all(isinstance(row.get(k), str) for k in ("title", "body")):
                raise ValueError(f"Invalid source record {key!r}")
            rows[key] = row
    raw = decode(args.raw.read_text(encoding="utf-8-sig"))
    if not rows or not isinstance(raw, dict) or rows.keys() != raw.keys():
        raise ValueError("Expected exactly the source notice IDs")
    dictionary = decode((HERE / "dictionary.json").read_text(encoding="utf-8-sig"))
    allowed = set(map(int, dictionary)) - NONCANONICAL
    output = {}
    for key, row in rows.items():
        try:
            output[key] = select(row, raw[key], allowed)
        except (ValueError, TypeError, KeyError) as error:
            raise ValueError(f"{key}: {error}") from error
    with args.output.open("x", encoding="utf-8") as out:
        json.dump(output, out, ensure_ascii=False, indent=2)
        out.write("\n")


if __name__ == "__main__":
    main()



