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
    scheme = 'https' if parts.hostname == 'job.konkuk.ac.kr' else parts.scheme
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
    r'(?:\s*\([월화수목금토일](?:요일)?\))?'
    r'(?:\s*(?P<hour>\d{1,2})\s*[:시]\s*(?P<minute>\d{2})?(?:분)?)?'
)
APPLY_LABEL = re.compile(r'(?:모집|접수|신청)\s*(?:기간|마감)')
KST = timezone(timedelta(hours=9))


def application_dates(text):
    """Conservative candidates with evidence; no invented years or midnight deadlines."""
    lines = text.splitlines()
    candidates, issues = [], []
    for index, line in enumerate(lines):
        if not APPLY_LABEL.search(line):
            continue
        evidence = line
        if not DATE.search(line) and index + 1 < len(lines):
            evidence += ' ' + lines[index + 1]
        dates = []
        for match in DATE.finditer(evidence):
            try:
                dt = datetime(int(match['year']), int(match['month']), int(match['day']),
                              int(match['hour'] or 0), int(match['minute'] or 0), tzinfo=KST)
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
        # If a range has an abbreviated second date, do not mistake the first for its end.
        if len(dates) == 1 and re.search(r'[~～∼–]', evidence[DATE.search(evidence).end():]):
            issues.append('AMBIGUOUS_DATE_RANGE')
            continue
        candidates.append({'startDate': dates[0]['date'] if len(dates) == 2 else None,
                           'endDate': dates[-1]['date'], 'endAt': dates[-1]['at'],
                           'evidence': evidence})
    unique = {(c['startDate'], c['endDate'], c['endAt']) for c in candidates}
    selected = candidates[0] if len(unique) == 1 and not issues else None
    if len(unique) > 1:
        issues.append('CONFLICTING_APPLICATION_DATES')
    if selected is None:
        issues.append('APPLICATION_DATE_REVIEW')
    return {'candidate': selected, 'evidence': candidates}, issues


def parse_detail(html, article_id):
    soup = soup_for(html)
    title, body = soup.select_one('.view-title'), soup.select_one('.view-con')
    identity = soup.select_one('.view-num dd')
    if not title or body is None or not identity or compact(identity.get_text()) != article_id:
        raise SourceError(f'Unexpected detail layout or identity for {article_id}')
    text = body_text(body)
    images = sorted({url for i in body.select('img[src]') if (url := public_url(i['src']))})
    attachments = sorted({(url, compact(a.get_text())) for a in soup.select('.view-file a[href]')
                          if (url := public_url(a['href']))})
    links = sorted({url for a in body.select('a[href]') if (url := public_url(a['href']))})
    if not compact(title.get_text()) or not (text or images or attachments):
        raise SourceError('Empty detail content needs inspection')
    dates, reasons = application_dates(text)
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
    return {'schemaVersion': 1, 'source': SOURCE, 'boardId': '4084', 'articleId': article_id,
            'sourceUrl': f'{BASE}/bbs/job/4084/{article_id}/artclView.do',
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
