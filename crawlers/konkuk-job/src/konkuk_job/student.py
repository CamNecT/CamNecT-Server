"""Discovery from the public Konkuk student notice feed, without fetching external media."""
import re
from datetime import datetime, timedelta
from xml.etree import ElementTree

from .parser import KST, SourceError

ORIGIN = 'https://www.konkuk.ac.kr'
FEED = ORIGIN + '/bbs/konkuk/238/rssList.do?row=50'
PATH = re.compile(r'/bbs/konkuk/238/([0-9]{1,20})/artclView\.do(?:\?layout=unknown)?')
ACTIVITY = re.compile(r'모집|참가|공모전|특강|워크숍|서포터즈|교육생|경진대회|봉사활동|강연|챌린지|문화상')
ADMIN = re.compile(r'직원\s*채용|근로장학|입찰|학사일정|등록금\s*납부')

def student_listing(client, now=None, store=None):
    raw, _ = client.fetch(FEED, limit=1_000_000)
    xml = raw.decode('utf-8-sig')
    if re.search(r'<!DOCTYPE|<!ENTITY', xml, re.I):
        raise SourceError('RSS declarations are forbidden')
    try:
        root = ElementTree.fromstring(xml)
    except ElementTree.ParseError as exc:
        raise SourceError('Malformed student RSS') from exc
    items = root.findall('./channel/item')
    if root.tag != 'rss' or not items or len(items) > 100:
        raise SourceError('Unexpected student RSS response')
    found = {}
    watched = store.get_watch() if store else {}
    today = (now or datetime.now(KST)).date()
    keep = {key: until for key, until in watched.items() if until >= today.isoformat()}
    for item in items:
        match = PATH.fullmatch((item.findtext('link') or '').strip())
        if not match:
            raise SourceError('Unexpected student notice URL')
        article_id = match[1]
        title = item.findtext('title') or ''
        try:
            published = datetime.fromisoformat(item.findtext('pubDate') or '').date()
        except ValueError as exc:
            raise SourceError('Invalid student RSS publication date') from exc
        if (ACTIVITY.search(title) and not ADMIN.search(title)
                and today - timedelta(days=30) <= published <= today):
            keep[article_id] = (published + timedelta(days=90)).isoformat()
    # Revisit previously discovered content beyond the RSS window. Losing a link
    # does not close/delete an activity. Use one board-local, conditional write.
    if store and keep != watched:
        store.save_watch(watched, keep)
    for article_id in keep:
        found[article_id] = f'{ORIGIN}/bbs/konkuk/238/{article_id}/artclView.do'
    return found
