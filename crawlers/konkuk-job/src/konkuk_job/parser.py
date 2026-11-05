import hashlib
import json
import re
from datetime import datetime, timedelta, timezone
from urllib.parse import urljoin, urlsplit, urlunsplit

from bs4 import BeautifulSoup

BASE = 'https://job.konkuk.ac.kr'
LIST_URL = BASE + '/bbs/job/4084/artclList.do'
SOURCE = 'KONKUK_JOB_4084'
ARTICLE_PATH = re.compile(r'^/bbs/job/4084/(\d+)/artclView\.do$')
BLOCK_MARKERS = ('웹 공격 차단', '웹 방화벽에 의해 차단', '웹방화벽에 의해 차단', 'access denied')


class SourceError(RuntimeError):
    """A source response must not be treated as an empty/deleted board."""


class SourceBlocked(SourceError):
    """Stop the entire run rather than continuing requests to a blocking site."""


def compact(value):
    return re.sub(r'\s+', ' ', value).strip()


def public_url(value, base=BASE):
    parts = urlsplit(urljoin(base, value.strip()))
    if parts.scheme not in ('http', 'https') or not parts.hostname or parts.username or parts.password:
        return None
    # Only our source host is upgraded. External destinations are recorded, never fetched.
    scheme = 'https' if parts.hostname in ('job.konkuk.ac.kr', 'www.konkuk.ac.kr') else parts.scheme
    return urlunsplit((scheme, parts.netloc, parts.path, parts.query, ''))


def soup_for(html):
    if any(marker in html.lower() for marker in BLOCK_MARKERS):
        raise SourceBlocked('Source returned a block page')
    return BeautifulSoup(html, 'html.parser')


def parse_list(html):
    soup = soup_for(html)
    articles = {}
    for link in soup.select('a[href]'):
        url = public_url(link['href'])
        if not url or urlsplit(url).hostname != 'job.konkuk.ac.kr':
            continue
        match = ARTICLE_PATH.fullmatch(urlsplit(url).path)
        if match:
            articles[match[1]] = BASE + urlsplit(url).path
    if not articles:
        raise SourceError('No article links: empty board or changed layout needs inspection')
    pages = [int(n) for n in re.findall(r"page_link\(['\"](\d+)['\"]\)", html)]
    form = next((f for f in soup.select('form')
                 if urlsplit(urljoin(BASE, f.get('action', ''))).path == urlsplit(LIST_URL).path
                 and f.select_one('input[name=page]')), None)
    if form is None:
        raise SourceError('Pagination form missing')
    fields = {i['name']: i.get('value', '') for i in form.select('input[name]')}
    return {'articles': articles, 'pages': max(pages, default=1), 'fields': fields}


def body_text(node):
    # Preserve inline text (date numbers may span multiple <span>s), separate blocks.
    clone = BeautifulSoup(str(node), 'html.parser')
    for tag in clone.select('script,style,iframe,object,noscript'):
        tag.decompose()
    for tag in clone.select('br'):
        tag.replace_with('\n')
    for tag in clone.select('p,div,li,tr,h1,h2,h3,h4'):
        tag.insert_before('\n')
        tag.insert_after('\n')
    return '\n'.join(line for text in clone.get_text().splitlines() if (line := compact(text)))


DATE = re.compile(
    r'(?P<year>20\d{2})\s*[.년/-]\s*(?P<month>\d{1,2})\s*[.월/-]\s*'
    r'(?P<day>\d{1,2})(?:\s*[.일])?'
    r'(?:\s*\(\s*[월화수목금토일](?:요일)?\s*\))?'
    r'(?:\s*(?P<meridiem>오전|오후)?\s*(?P<hour>\d{1,2})\s*[:시]\s*(?P<minute>\d{2})?(?:분)?)?'
)
APPLY_LABEL = re.compile(r'(?:모집|접수|신청|지원|응모)\s*(?:기간|마감|시작|개시|종료|기한)')
KST = timezone(timedelta(hours=9))


# Allow a shortened range end only when it carries a weekday that corroborates
# the explicit start year. Never borrow a year from today's date or the RSS item.
SHORT_END = re.compile(r'(?P<sep>[~～∼–—]\s*)(?P<m>\d{1,2})\s*[.월/]\s*(?P<d>\d{1,2})[.일]?\s*\(\s*(?P<w>[월화수목금토일])\s*\)')
NEXT_DATE = re.compile(r'^\s*(?:기간\s*[:：]\s*)?[~～∼\s:：-]*20\d{2}')
OTHER_FIELD = re.compile(r'(?:교육|행사|발표|면접|시험|운영|파견|활동)\s*(?:일시|일정|기간|일자|일|날짜)\s*[:：]?')

def application_dates(text, *, abbreviated=False):
    """Conservative candidates with evidence; no invented years or midnight deadlines."""
    lines = text.splitlines()
    candidates, issues = [], []
    for index, line in enumerate(lines):
        label = APPLY_LABEL.search(line)
        if not label:
            continue
        evidence = line[label.start():]
        if (not DATE.search(line) and index + 1 < len(lines)
                and not re.search(r"미정|추후|선착순|상시|예산 소진", evidence)):
            if NEXT_DATE.match(lines[index + 1]):
                evidence += ' ' + lines[index + 1]
        original = evidence
        evidence = OTHER_FIELD.split(evidence, maxsplit=1)[0]
        if abbreviated and (short := SHORT_END.search(evidence)):
            start_token = DATE.search(evidence[:short.start()])
            if start_token and not evidence[start_token.end():short.start()].strip():
                try:
                    end = datetime(int(start_token['year']), int(short['m']), int(short['d']))
                except ValueError:
                    end = None
                if end and '월화수목금토일'[end.weekday()] == short['w']:
                    evidence = evidence[:short.start('m')] + start_token['year'] + '.' + evidence[short.start('m'):]
        dates = []
        matches = list(DATE.finditer(evidence))
        for match in matches:
            try:
                hour = int(match['hour'] or 0)
                if match['meridiem']:
                    if not 1 <= hour <= 12:
                        raise ValueError('Invalid 12-hour clock')
                    hour = hour % 12 + (12 if match['meridiem'] == '오후' else 0)
                dt = datetime(int(match['year']), int(match['month']), int(match['day']),
                              hour, int(match['minute'] or 0), tzinfo=KST)
                weekday = re.search(r'\(\s*([월화수목금토일])(?:요일)?\s*\)', match[0])
                if abbreviated and weekday and weekday[1] != '월화수목금토일'[dt.weekday()]:
                    raise ValueError('Calendar weekday disagrees with the source')
            except ValueError:
                issues.append('INVALID_DATE')
                continue
            dates.append({'date': dt.date().isoformat(),
                          'at': dt.isoformat() if match['hour'] is not None else None})
        if not dates:
            continue
        if len(dates) > 2:
            issues.append('AMBIGUOUS_DATE_RANGE')
            continue
        # A partial date/time outside the recognized tokens must never silently turn
        # a range into a deadline, or 18:3 into 18:00. Keep the original as evidence.
        rest = re.sub(r'^\s*\d+[.)]\s*', '', DATE.sub('', evidence))
        if re.search(r'\d\s*(?:[.월/:시]|일)|오전|오후|정오|자정|AM|PM', rest, re.I):
            issues.append('AMBIGUOUS_DATE_RANGE')
            continue
        if any(re.match(r'\d', evidence[m.end():]) for m in matches):
            issues.append('INVALID_DATE')
            continue
        start, end = None, None
        if len(dates) == 2:
            between = evidence[matches[0].end():matches[1].start()]
            if not re.search(r'[~～∼–—-]|부터', between):
                issues.append('AMBIGUOUS_DATE_RANGE')
                continue
            start, end = dates
            if start['date'] > end['date'] or (start['at'] and end['at'] and start['at'] > end['at']):
                issues.append('REVERSED_DATE_RANGE')
                continue
        else:
            before, after = evidence[:matches[0].start()], evidence[matches[0].end():]
            starts = bool(re.search(r'부터|시작|개시', after) or re.search(r'시작|개시', before))
            ends = bool(re.search(r'까지|마감|종료', after) or re.search(r'마감|종료|기한|[~～∼]', before))
            if starts and not ends:
                start = dates[0]
            elif ends and not starts and not re.search(r'[~～∼–—-]', after):
                end = dates[0]
            else:
                issues.append('AMBIGUOUS_APPLICATION_DATE')
                continue
        candidates.append({'startDate': start['date'] if start else None,
                           'endDate': end['date'] if end else None, 'endAt': end['at'] if end else None,
                           'evidence': original})
    unique = {(c['startDate'], c['endDate'], c['endAt']) for c in candidates}
    selected = candidates[0] if len(unique) == 1 and not issues else None
    if len(unique) > 1:
        issues.append('CONFLICTING_APPLICATION_DATES')
    if selected is None:
        issues.append('APPLICATION_DATE_REVIEW')
    elif selected['endDate'] is None:
        issues.append('APPLICATION_END_DATE_UNKNOWN')
    return {'candidate': selected, 'evidence': candidates}, issues


def parse_detail(html, article_id, *, student=False):
    soup = soup_for(html)
    title, body = soup.select_one('.view-title'), soup.select_one('.view-con')
    identity = soup.select_one('.view-num dd')
    if not title or body is None or not identity or compact(identity.get_text()) != article_id:
        raise SourceError(f'Unexpected detail layout or identity for {article_id}')
    base = 'https://www.konkuk.ac.kr' if student else BASE
    text = body_text(body)
    images = sorted({url for i in body.select('img[src]') if (url := public_url(i['src'], base))})
    attachments = sorted({(url, compact(a.get_text())) for a in soup.select('.view-file a[href]')
                          if (url := public_url(a['href'], base))})
    links = sorted({url for a in body.select('a[href]') if (url := public_url(a['href'], base))})
    if not compact(title.get_text()) or not (text or images or attachments):
        raise SourceError('Empty detail content needs inspection')
    dates, reasons = application_dates(text, abbreviated=student)
    board = "238" if student else "4084"
    source = "KONKUK_STUDENT_238" if student else SOURCE
    section = "konkuk" if student else "job"
    if images or attachments:
        reasons.append('MEDIA_REVIEW')
    if len(text) < 200 and images:
        reasons.append('IMAGE_DOMINANT')
    def metadata(selector):
        node = soup.select_one(selector)
        return compact(node.get_text()) if node else None
    published = metadata('.view-util .write dd')
    if not published:
        raise SourceError('Publication date missing')
    return {'schemaVersion': 1, 'source': source, 'boardId': board, 'articleId': article_id,
            'sourceUrl': f'{base}/bbs/{section}/{board}/{article_id}/artclView.do',
            'title': compact(title.get_text()), 'publisher': metadata('.view-util .writer dd'),
            'publishedAtRaw': published, 'modifiedAtRaw': metadata('.view-util .modify dd'),
            'bodyText': text, 'images': images,
            'attachments': [{'url': u, 'name': n} for u, n in attachments],
            'externalLinks': links, 'applicationDates': dates,
            'reviewStatus': 'PENDING', 'reviewReasons': sorted(set(reasons)),
            'media': []}


def content_hash(record):
    # Counts, last-seen timestamps and source HTML presentation do not cause new revisions.
    return hashlib.sha256(json.dumps(record, ensure_ascii=False, sort_keys=True,
                                     separators=(',', ':')).encode()).hexdigest()
