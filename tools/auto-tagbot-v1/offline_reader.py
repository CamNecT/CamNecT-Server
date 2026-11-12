"""Propose employment search tags from source text using a single offline rule set."""
import json
import re

from frozen.model2 import select_tags as selector


LABELS = {int(k): v for k, v in json.loads(
    (selector.HERE / "dictionary.json").read_text(encoding="utf-8-sig")
).items() if int(k) not in selector.NONCANONICAL}

HIRING = re.compile(r"채용|공채|신입사원|구인|직원\s*모집|사원\s*모집|recruit(?:ing|ment)?|hiring|job opening", re.I)
EVENT = re.compile(r"설명회|박람회|콘서트|세미나|특강|캠프|아카데미|교육생|상담회|멘토링|취업준비")
DIRECT_EVENT = re.compile(r"입사\s*지원서|채용\s*지원서|모집\s*인원\s*[:：]|(?:신입사원|경력사원|인턴십|공채).{0,12}모집\s*중")
INTERN = re.compile(r"(?:(?:체험형|채용연계형|채용형|전환형|청년)\s*)?인턴(?:사원|십|쉽)?|현장\s*실습(?:생)?|\bintern(?:ships?|s)?\b", re.I)
SECTION = re.compile(
    r"(?P<duties>모집\s*(?:부문|직무|분야|직종|인원)|채용\s*(?:부문|분야|직무)|담당\s*업무|주요\s*업무|수행\s*업무|직무\s*(?:내용|개요)|주요\s*역할|채용\s*직종|모집\s*포지션|지원\s*분야|근무\s*형태|responsibilities|open positions|what you.ll do|job description|hiring for)"
    r"|(?P<other>지원\s*(?:자격|방법|절차)|우대\s*(?:요건|사항|조건)|(?:응시|채용)\s*자격|자격\s*(?:요건|조건)|모집\s*기간|전형\s*절차|회사\s*소개|기업\s*소개|문의\s*처|접수\s*(?:기간|방법)|복리\s*후생|qualifications|requirements)", re.I)
ROLE = {
    47: selector.ROLE_CUES[47], 57: r"(?:UI\s*[/·&-]?\s*UX|UX\s*[/·&-]?\s*UI|UI|UX)\s*(?:디자인|디자이너|design(?:er)?)",
    58: selector.ROLE_CUES[58], 48: r"\bPM\b|프로젝트\s*매니저|project manager",
    49: r"\bPO\b|프로덕트\s*오너|product owner", 50: r"마케팅|마케터|marketing",
    51: r"데이터\s*(?:분석|애널리스트)|데이터\s*사이언(?:스|티스트)|data analy(?:st|sis|tics)|data scien(?:tist|ce)",
    52: r"프론트\s*엔드|front[ -]?end", 53: r"백\s*엔드|back[ -]?end",
    54: r"iOS\s*(?:개발|developer|engineer)", 55: r"(?:안드로이드|Android)\s*(?:개발|developer|engineer)",
    56: r"(?:AI|머신러닝|딥러닝)\s*(?:빅데이터|알고리즘)|(?:machine learning|deep learning|AI/ML)\s*(?:engineer|model|research)|(?:인공지능|딥러닝|머신러닝|\bAI)\s*(?:모델\s*)?(?:개발|엔지니어|연구|engineer|research)",
    59: r"브랜딩|브랜드\s*(?:전략|관리|매니저|마케팅)|branding",
    60: r"광고|홍보|public relations|\bPR\b", 61: r"영업(?!일|시간|점)|\bsales\b",
    62: r"인사(?!말|이트|용)|\bHR\b|human resources",
    63: selector.ROLE_CUES[63], 64: r"전략\s*컨설(?:팅|턴트)|strategy consult(?:ing|ant)",
    19: r"디자이너|디자인|\b(?:graphic|product|fashion|visual)\s+design(?:er)?\b|\bdesigner\b", 46: r"기획",
}
ROLE = {k: re.compile(v, re.I) for k, v in ROLE.items()}
TOPICS = {
    65: selector.PUBLIC_PREP, 109: r"모의\s*면접|면접\s*(?:특강|준비|대비|코칭)",
    110: r"포트폴리오\s*(?:특강|제작|클리닉|컨설팅)", 114: r"코딩\s*테스트\s*(?:대비|특강|준비)",
    90: r"멘토링|멘티\s*모집", 116: r"진로\s*(?:탐색|설계|상담|특강|교육)|커리어\s*멘토링|현직자.{0,10}직무\s*특강", 77: r"공모전|작품\s*공모|경진\s*대회",
    79: r"서포터즈", 83: r"봉사활동|봉사단", 89: r"해커톤", 87: r"학부\s*연구생",
    119: r"해외\s*(?:파견|근무)|국제\s*교류", 88: r"(?:예비)?창업(?:자|캠프|패키지)|창업\s*(?:캠프|경진대회|교육|프로그램|아이디어)|스타트업\s*멘토링",
    108: r"경력\s*(?:사원|직원|직|채용)|신입\s*[/·및]\s*경력|이직",
}
TOPICS = {k: re.compile(v) for k, v in TOPICS.items()}


# Activities often describe a curriculum or a submission rather than a job vacancy.
# Public search topics may use those phrases, but the job role index must not.
LEARNING = re.compile(r"교육|양성|특강|아카데미|부트\s*캠프|공모|경진|워크숍|과정|세미나|해커톤|멘토링|연구생|프로젝트|대회|캠프|창업|training|bootcamp|contest", re.I)
THEMES = {
    56: r"인공\s*지능|딥\s*러닝|머신\s*러닝|생성형\s*AI|(?<![A-Za-z])AI(?![A-Za-z])|machine learning|deep learning",
    51: r"데이터\s*(?:분석|사이언스)|빅\s*데이터|data\s*(?:analysis|science|analytics)",
    19: r"디자인|디자이너|graphic design|fashion design",
    46: r"콘텐츠\s*기획|상품\s*기획|사업\s*기획",
    47: selector.ROLE_CUES[47], 50: r"마케팅|마케터|marketing", 53: ROLE[53], 52: ROLE[52],
    58: selector.ROLE_CUES[58], 59: ROLE[59],
    106: r"파이썬|\bPython\b", 120: r"팀\s*(?:별\s*)?프로젝트|팀별\s*사업계획서|협업\s*프로젝트",
    121: r"연구실.{0,18}(?:연구생|연구원|인턴)\s*(?:모집|채용)",
    91: r"교육\s*봉사|학습\s*봉사", 83: r"봉사단|봉사\s*활동", 90: TOPICS[90],
    77: TOPICS[77], 88: TOPICS[88], 109: TOPICS[109], 116: TOPICS[116],
}
THEMES = {k: re.compile(v, re.I) if isinstance(v, str) else v for k, v in THEMES.items()}
SUBJECT_SECTIONS = re.compile(r"(?:교육\s*(?:내용|과정|분야)|커리큘럼|실습\s*(?:주제|내용)|공모\s*(?:주제|분야)|활동\s*(?:내용|분야)|프로그램\s*(?:내용|구성)|지원\s*분야|주요\s*내용)\s*[:：]?\s*([^.!?\n]{2,230})")
EMPLOYMENT_ACTIVITY = re.compile(r"취업(?:준비)?|채용\s*(?:설명회|박람회)|(?:일자리|공직)\s*(?:박람회|콘서트|아카데미)|커리어\s*멘토링|job\s*fair", re.I)


def subject_topics(subject, body):
    fragments = [subject]
    if LEARNING.search(subject):
        fragments.extend(m[1] for m in SUBJECT_SECTIONS.finditer(body))
        # Clear body phrases identify the actual programme even when the title is abbreviated.
        fragments.extend(m.group() for m in re.finditer(
            r"(?:생성형\s*AI|\bAI|인공지능|데이터\s*분석|디자인)\s*(?:실무|교육|부트캠프|프로덕션|경진대회|개발자\s*양성)"
            r"|팀\s*프로젝트|소그룹\s*멘토링|선배사원\s*멘토링|모의\s*면접", body, re.I))
    for fragment in fragments:
        for tag, pattern in THEMES.items():
            match = pattern.search(fragment)
            if match:
                yield tag, match.group()

def duty_sections(body):
    matches = list(SECTION.finditer(body))
    for i, heading in enumerate(matches):
        if heading.lastgroup == "duties":
            end = matches[i + 1].start() if i + 1 < len(matches) else len(body)
            yield re.split(r"https?://|기업홈페이지|첨부파일 다운로드", body[heading.end():end], maxsplit=1)[0]


def role_pairs(fragments):
    roles = {}
    for fragment in fragments:
        hits = sorted((m.start(), -len(m.group()), tag, m.group())
                      for tag, pattern in ROLE.items() for m in pattern.finditer(fragment))
        end = 0
        for start, length, tag, quote in hits:
            if start >= end:
                roles.setdefault(tag, quote)
                end = start - length
    return list(map(list, roles.items()))


def internship(title, body, sections):
    for fragment in [title, *sections, *re.findall(
            r"[^.!?\n]{0,45}(?:인턴(?:십|사원)?|현장실습생|\bintern(?:ship)?\b)\s*(?:공개\s*)?(?:채용|모집)[^.!?\n]{0,10}",
            body, re.I)]:
        for m in INTERN.finditer(fragment):
            if (re.match(r"\s*(?:경험|경력|수료|우대|유경험|출신|프로그램\s*설명회)", fragment[m.end():])
                    or re.search(r"(?:과거|지난해|지난|전직)\s*$", fragment[:m.start()])):
                continue
            return m.group()
    return None


def classify(row):
    """Return source-exact proposals and a separate complete index of matched roles."""
    source = {k: row.get(k) or "" for k in ("title", "body")}
    if not all(isinstance(s, str) for s in source.values()):
        raise ValueError("title and body must be strings")
    title, body = source["title"], source["body"]
    # Bracketed publisher prefixes and URLs often contain industry/department names.
    subject = re.sub(r"^\s*(?:\[[^\]]+\]\s*)+", "", title) or title
    sections = list(duty_sections(body))
    event = EVENT.search(subject)
    direct = not event or DIRECT_EVENT.search(body)
    intern = internship(subject, body, sections) if direct else None
    if direct and not intern:
        bracket = re.search(r"\[\s*(인턴(?:십|쉽)?)\s*\]", title)
        intern = bracket[1] if bracket else None
    hiring = HIRING.search(subject) or re.search(r"\[채용\]", title)
    if not hiring and not event:
        hiring = re.search(r"(?:디자이너|개발자|엔지니어|연구원|사원|직원|매니저|경력직|신입|교사|강사|designer|developer|engineer)\s*(?:을|를|분을)?\s*(?:모집|구함|구인|구합니다|채용)|(?:모집|구인)\s*[:：]\s*(?:디자이너|개발자|엔지니어)|정규직|계약직", subject, re.I)
    if not hiring:
        hiring = re.search(r"(?:신입사원|정규직|경력사원|직원)\s*(?:공개\s*)?(?:채용|모집)", body)
    role_sources = [subject, *sections]
    if direct and (intern or hiring):
        role_sources.extend(m.group() for m in re.finditer(
            r"(?:[가-힣A-Za-z/&·]{1,16}[ \t]+){0,2}(?:디자이너|개발자|엔지니어|기획자|마케터|분석가)\s*(?:모집|채용|인턴|담당)"
            r"|(?:담당|수행)\s*[:：]\s*[^.!?\n]{2,80}", body, re.I))
    roles = role_pairs(role_sources) if direct and (intern or hiring or sections) else []
    # Brand marketing explicitly names both the branding and the marketing role.
    if any(tag == 59 and "마케팅" in q for tag, q in roles) and not any(tag == 50 for tag, q in roles):
        roles.append([50, "마케팅"])
    items = []
    route = "ordinary"
    if intern:
        items.append([84, intern])
        route = next((kind for kind in ("conversion", "parallel")
                      if selector.hiring_evidence(source, kind)), "intern")
    else:
        employment = EMPLOYMENT_ACTIVITY.search(subject) or hiring
        if not employment and (LEARNING.search(subject) or event):
            employment = re.search(r"취업\s*(?:준비|역량\s*강화|교육)|채용\s*설명회", body)
        if employment:
            items.append([107, employment.group()])
    items.extend(roles)
    for tag, pattern in TOPICS.items():
        m = pattern.search(subject)
        if m:
            items.append([tag, m.group()])
    if LEARNING.search(subject) and not intern and not hiring:
        items.extend(subject_topics(subject, body))
    if event and not intern:
        career = re.search(r"진로\s*(?:탐색|설계|상담)|직무\s*탐색|커리어\s*(?:탐색|설계|상담)", subject + "\n" + body)
        if career and "\n" not in career.group():
            items.append([116, career.group()])
    for tag, pattern in selector.ACADEMIC.items():
        m = re.search(pattern, subject, re.I)
        if m and len(m.group()) > 1:
            items.append([tag, m.group()])
    # Keep the highest-ranked exact phrase for each distinct proposal.
    distinct = {}
    for tag, quote in items:
        distinct.setdefault(tag, quote)
    items = list(map(list, distinct.items()))
    selected, index = selector.choose(source, {"route": route, "items": items[:8], "roles": roles}, LABELS)

    def records(pairs):
        return [{"tag_id": tag, "label": LABELS[tag], "evidence": quote} for tag, quote in pairs]

    return {"suggestions": records(selected), "role_index": records(index)}
