"""Offline employment tag selection with an auditable inventory of applicant roles."""
import argparse
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
NONCANONICAL = {78, 112, 117, 118, 122}
ROLE_IDS = {19, *range(46, 65)}
INTERN = r"(?:인턴(?:십|쉽|사원)?|intern(?:ship|s)?)"
REGULAR = r"(?:공채|정규직|정직원|일반직|계약직|신입(?:사원|직원|직)?|경력(?:사원|직원|직|자)?|풀타임|full[\s-]*time|permanent)"
SEP = r"(?:[/·,&+]|및|또는|와|과|and)"
JOINT = re.compile(
    rf"{REGULAR}\s*(?:채용|모집)?\s*{SEP}\s*(?:(?:채용연계형|체험형|청년|대학생|하계)\s*)?{INTERN}"
    rf"|{INTERN}\s*(?:채용|모집)?\s*{SEP}\s*(?:(?:\dQ|하계|상반기|하반기)\s*)?{REGULAR}", re.I
)
DIRECT = re.compile(
    rf"{REGULAR}\s*(?:(?:[가-힣A-Za-z]+)\s+){{0,2}}(?:공개\s*)?(?:채용|공채|모집|포지션|positions?|openings?|공석|직군|직무|트랙|[0-9○O]+\s*명)"
    rf"|(?:고용형태|모집규모|채용구분|모집인원)\s*[:：]\s*{REGULAR}", re.I
)
STAFF_LIST = re.compile(r"(?:모집\s*(?:부문|분야|규모|인원)|채용\s*(?:부문|분야|규모|인원))\s*[:：]?\s*(?P<list>[^.!?\n\r■▶●•]{4,150})")
STAFF = re.compile(r"변호사|연구원|기술자|교수|사무직|행정직|경력직|일반직|계약직|정규직|신입사원")
STAFF_TITLE = re.compile(r"(?:직원|사원|인재)\s*(?:공개\s*)?채용|직원\s*모집")
OPTION = re.compile(r"전환|전제|연계|채용형|연장|우대|우수|가능|검토|기회|혜택|수료\s*후|종료\s*후|가점|예정자\s*우대")
COMMIT = re.compile(
    r"(?:전원|모두|자동(?:으로)?|무조건)\s*(?:정규직|정직원)(?:으로)?\s*(?:전환|채용|임용)"
    r"|(?:정규직|정직원)\s*(?:전환|채용|입사)(?:을|를|이|가)?\s*(?:보장|확정)"
    r"|(?:guaranteed|guarantees?)\s+(?:(?:a|an)\s+)?(?:permanent|full[\s-]*time)\s+(?:employment|positions?|jobs?|offers?)", re.I
)
CAVEAT = re.compile(r"우수|일부|일정\s*기준|통과|합격|심사|검토|가능|평가|추천|에\s*한[하해]|보장(?:\s*되|\s*하)?지|아니|아님|없음|하지\s*않|미보장|subject to|not guaranteed", re.I)
PUBLIC_PREP = re.compile(r"(?:공기업|공공기관|공사[·/]공단).{0,30}(?:준비|대비|특강|모의|대비반|준비반|취업\s*(?:캠프|교육))|(?:준비|대비|특강|모의).{0,20}(?:공기업|공공기관)")
ACADEMIC = {
    15:"경영학",16:"경제학",17:"심리학",18:r"컴퓨터\s*(?:공학|과학)|computer science",20:"미디어학",21:r"정치외교|정치학|외교학",
    22:"법학",23:"교육학",24:"간호학",25:r"의학|의과학",26:"약학",27:r"생명\s*공학",28:r"화학\s*공학",29:r"기계(?:\s*공학|설계|직)?",
    30:r"건축(?:학|설계|공학)",31:"통계학",32:"사회학",33:"철학",34:r"국어국문|국문학",
}
ELIGIBILITY = re.compile(r"(?:전공(?:자)?\s*(?:우대|필수)|관련\s*(?:학과|전공)|지원\s*자격|우대\s*(?:요건|사항|전공)|자격\s*요건)")
ROLE_CUES = {
    47:r"(?:서비스|플랫폼|앱|웹)\s*기획|service\s*plan", 58:r"영상\s*(?:편집|제작)|영상편집|video\s*(?:edit|produc)",
    63:r"회계|재무|세무|재경|자금|결산|원가|account(?:ant|ing)|financial\s+(?:reporting|accounting|planning|control)|financ(?:e|ial)\s+(?:team|intern|department|analyst)",
}
CLAUSES = re.compile(r"[^.!?。;\n\r■▶●•]+")


def unique_keys(pairs):
    obj = {}
    for key, value in pairs:
        if key in obj:
            raise ValueError(f"Duplicate key {key!r}")
        obj[key] = value
    return obj


def decode(data):
    return json.loads(data, object_pairs_hook=unique_keys)


def quote_contexts(row, quote, radius=65):
    for field in ("title", "body"):
        for clause in CLAUSES.finditer(row[field]):
            value = clause.group()
            for hit in re.finditer(re.escape(quote), value):
                yield value[max(0, hit.start()-radius):hit.end()+radius]


def hiring_evidence(row, kind):
    """Return only positive, source-exact proof of the declared route."""
    for field in ("title", "body"):
        for clause in CLAUSES.finditer(row[field]):
            value = clause.group()
            if kind == "conversion":
                for match in COMMIT.finditer(value):
                    context = value[max(0, match.start()-45):match.end()+32]
                    if not CAVEAT.search(context):
                        return match.group()
                continue
            for match in JOINT.finditer(value):
                tail = value[match.end():match.end()+24]
                if not re.match(r"\s*(?:전환|채용\s*(?:가능|기회)|우대|지원\s*가능)", tail):
                    return match.group()
            if STAFF_TITLE.search(row["title"]):
                for heading in STAFF_LIST.finditer(value):
                    listing = re.split(r"근무지|지원자격|우대사항|접수기간|기업홈페이지", heading["list"])[0]
                    entries = re.split(r"[/·,|]", listing)
                    if (any(re.fullmatch(r"\s*(?:청년|체험형|채용형)?\s*인턴(?:십)?\s*", e) for e in entries)
                            and any(STAFF.fullmatch(e.strip()) for e in entries)):
                        return value[heading.start():heading.start("list")] + listing
            for match in DIRECT.finditer(value):
                local = value[max(0, match.start()-22):match.end()+15]
                if not OPTION.search(local) and not re.search(INTERN, match.group(), re.I):
                    if re.search(INTERN, local, re.I) and not re.search(r"별도|각각|별개|추가|일반직|정규직\s*\d+\s*명", local):
                        continue
                    return match.group()
    return None


def eligible(tag, quote, row):
    if tag == 77 and re.fullmatch(r"[\[\]()\s]*공모[\[\]()\s]*", quote):
        return False
    if tag == 119 and re.search(r"파트너|고객|바이어", quote) and not re.search(r"해외\s*(?:근무|파견)|국제교류", quote):
        return False
    if tag == 63 and re.fullmatch(r"finance", quote, re.I):
        return any(re.search(r"recruit|position|department|team|직무|모집|부문", s, re.I) for s in quote_contexts(row, quote, 48))
    if tag == 65:
        return any(PUBLIC_PREP.search(s) for s in quote_contexts(row, quote, 32))
    if tag in ACADEMIC:
        return bool(re.search(ACADEMIC[tag], quote, re.I)) and any(
            not ELIGIBILITY.search(s) for s in quote_contexts(row, quote, 20))
    if tag in ROLE_CUES:
        return bool(re.search(ROLE_CUES[tag], quote, re.I))
    if tag == 108:
        return bool(re.search(r"경력\s*(?:사원|직원|직|채용)|신입\s*[/·및]\s*경력|이직", quote))
    return True


def parse_pairs(items, row, allowed, limit):
    if not isinstance(items, list) or len(items) > limit:
        raise ValueError(f"Expected up to {limit} pairs")
    result = {}
    seen = set()
    for item in items:
        if not isinstance(item, list) or len(item) != 2:
            raise ValueError(f"Invalid pair {item!r}")
        tag, quote = item
        if type(tag) is not int or tag not in allowed or tag in seen:
            raise ValueError(f"Invalid or repeated tag {tag!r}")
        seen.add(tag)
        if not isinstance(quote, str) or len(quote.strip()) < 2 or not any(quote in row[k] for k in ("title", "body")):
            raise ValueError(f"Invalid quote for {tag}: {quote!r}")
        if eligible(tag, quote, row):
            result[tag] = quote
    return result


def choose(row, raw, allowed):
    route = raw["route"]
    if route not in {"ordinary", "intern", "conversion", "parallel"}:
        raise ValueError(f"Invalid route {route!r}")
    items = parse_pairs(raw["items"], row, allowed, 8)
    roles = parse_pairs(raw["roles"], row, ROLE_IDS, len(ROLE_IDS))
    if (84 in items) != (route != "ordinary"):
        raise ValueError("Internship evidence and route disagree")
    if re.search(r"채용\s*설명회|기업\s*설명회", row["title"]):
        direct_applications = re.search(
            r"(?:신입사원|경력사원|인턴십|공채).{0,12}모집\s*중|입사\s*지원서|채용\s*지원서|모집\s*인원\s*[:：]",
            row["body"]
        )
        if not direct_applications:
            roles.clear()
            items = {tag: q for tag, q in items.items() if tag not in ROLE_IDS or q in row["title"]}
    if 47 in items and 46 in items and items[46] in items[47]:
        items.pop(46)
    if 47 in roles and 46 in roles and roles[46] in roles[47]:
        roles.pop(46)
    if route == "ordinary":
        order = list(items)
    else:
        order = [84]
        if route in {"conversion", "parallel"}:
            proof = hiring_evidence(row, route)
            if proof:
                items[107] = proof
                order.append(107)
        order.extend(tag for tag in items if tag not in {84, 107})
    order.extend(tag for tag in roles if tag not in order)
    return [[t, items[t] if t in items else roles[t]] for t in order[:4]], list(map(list, roles.items()))


def select(source, raw, allowed):
    return choose(source, raw, allowed)[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("source", "raw", "output"):
        parser.add_argument(name, type=Path)
    parser.add_argument("--roles-out", type=Path)
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
    output, roles = {}, {}
    for key, row in rows.items():
        try:
            output[key], roles[key] = choose(row, raw[key], allowed)
        except (ValueError, TypeError, KeyError) as error:
            raise ValueError(f"{key}: {error}") from error
    for path, data in [(args.output, output), (args.roles_out, roles)]:
        if path:
            with path.open("x", encoding="utf-8") as out:
                json.dump(data, out, ensure_ascii=False, indent=2)
                out.write("\n")


if __name__ == "__main__":
    main()

