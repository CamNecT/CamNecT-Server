import hashlib
import time
import uuid
from urllib.parse import urlsplit

from .parser import BLOCK_MARKERS, LIST_URL, SourceBlocked, SourceError, parse_detail, parse_list
from .storage import now
from .http import PayloadTooLarge
from .publisher import PublishError


def collect_listing(client, max_pages=20):
    articles, seen_pages = {}, set()
    page, last_page, fields = 1, 1, None
    while page <= last_page:
        if page > max_pages:
            raise SourceError('Page limit reached; increase only after inspecting board size')
        html, _ = client.html(LIST_URL, fields)
        listing = parse_list(html)
        signature = tuple(sorted(listing['articles']))
        if signature in seen_pages:
            raise SourceError('Pagination repeated a page; refusing incomplete listing')
        seen_pages.add(signature)
        articles.update(listing['articles'])
        last_page = max(last_page, listing['pages'])
        fields = {**listing['fields'], 'page': str(page + 1)}
        page += 1
    return articles, len(seen_pages)


def collect_media(record, client, store, budget, max_media=8):
    urls = sorted(set(record['images'] + [a['url'] for a in record['attachments']]))
    if len(urls) > max_media:
        record['reviewReasons'].append('MEDIA_LIMIT')
    for url in urls[:max_media]:
        if urlsplit(url).hostname != 'job.konkuk.ac.kr':
            record['media'].append({'url': url, 'status': 'EXTERNAL_NOT_FETCHED'})
            continue
        if budget['bytes'] >= 64_000_000:
            raise SourceBlocked('Run media byte budget exceeded')
        remaining = 64_000_000 - budget['bytes']
        try:
            raw, mime = client.fetch(url, limit=min(8_000_000, remaining))
        except PayloadTooLarge as exc:
            budget['bytes'] += exc.bytes_read
            if remaining < 8_000_000:
                raise SourceBlocked('Run media byte budget exhausted; resume next run') from exc
            record['reviewReasons'].append('MEDIA_SIZE_LIMIT')
            record['media'].append({'url': url, 'status': 'TOO_LARGE'})
            continue
        # The board labels real JPG attachments application/x-msdownload. Use signatures.
        detected = media_type(raw)
        if any(marker in raw[:64_000].decode('utf-8', errors='replace').lower() for marker in BLOCK_MARKERS):
            raise SourceBlocked('Source returned a block page for media')
        if mime in ('text/html', 'application/xhtml+xml') or raw.lstrip().lower().startswith((b'<!doctype html', b'<html')):
            raise SourceError('Unexpected media response; original record retained')
        budget['bytes'] += len(raw)
        if detected is None:
            record['reviewReasons'].append('UNSUPPORTED_MEDIA')
            record['media'].append({'url': url, 'status': 'UNSUPPORTED',
                                    'sha256': hashlib.sha256(raw).hexdigest()})
            continue
        key = store.blob(raw)
        record['media'].append({'url': url, 'status': 'DOWNLOADED', 'mime': detected,
                                'sha256': hashlib.sha256(raw).hexdigest(),
                                'bytes': len(raw), 'blobKey': key})
    record['reviewReasons'] = sorted(set(record['reviewReasons']))


def media_type(raw):
    for signature, mime in ((b'\xff\xd8\xff', 'image/jpeg'), (b'\x89PNG\r\n\x1a\n', 'image/png'),
                            (b'GIF87a', 'image/gif'), (b'GIF89a', 'image/gif'),
                            (b'%PDF-', 'application/pdf'), (b'PK\x03\x04', 'application/zip'),
                            (b'\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1', 'application/x-cfb')):
        if raw.startswith(signature):
            return mime
    if raw.startswith(b'RIFF') and raw[8:12] == b'WEBP':
        return 'image/webp'
    return None


def run(client, store, max_articles=20, max_pages=20, seconds=480, publisher=None, student=False):
    if not 1 <= max_articles <= 100 or not 1 <= max_pages <= 100 or seconds <= 0:
        raise ValueError('Invalid crawl limits')
    started = time.monotonic()
    client.deadline = started + seconds
    if publisher:
        publisher.deadline = client.deadline
    summary = {'runId': uuid.uuid4().hex, 'startedAt': now(), 'status': 'RUNNING',
               'board': 'student' if student else 'job', 'discovered': 0, 'processed': 0, 'created': 0, 'updated': 0, 'unchanged': 0,
               'articleErrors': [], 'publicationErrors': [], 'deferred': 0,
               'published': 0, 'publicationResults': {}}
    try:
        client.load_robots()
        if student:
            from .student import student_listing
            articles, pages = student_listing(client, store=store), 1
        else:
            articles, pages = collect_listing(client, max_pages)
        summary.update(discovered=len(articles), pages=pages)
        # Oldest or unseen first, so a capped recurring run eventually revisits every post.
        failures = {article_id: store.get_failure(article_id) for article_id in articles}
        def priority(article_id):
            meta = store.get(article_id)
            receipt = store.get_publication(article_id) if publisher and student else None
            # Held jobs deliberately have no publication receipt. Rotate archived jobs
            # by lastSeenAt so these cannot starve new postings or older revisions.
            pending = not meta or (publisher is not None and student and
                                   (not receipt or receipt.get('destination') != publisher.url or
                                    receipt['sourceVersion'] != meta['version']))
            # A poison record cannot consume every slot ahead of healthy/new records.
            return (1 if failures[article_id] else 0, 0 if pending else 1,
                    meta['lastSeenAt'] if meta else '', -int(article_id))
        eligible = [i for i in articles if not failures[i] or failures[i]['nextRetryAt'] <= now()]
        summary['deferred'] = len(articles) - len(eligible)
        ids = sorted(eligible, key=priority)
        # Reserve one slot for an eligible retry so a busy board cannot starve repairs.
        retries = [i for i in ids if failures[i]]
        if retries and len(ids) > max_articles:
            chosen = ids[:max_articles]
            if not any(failures[i] for i in chosen):
                chosen[-1] = retries[0]
            ids = chosen
        budget = {'bytes': 0}
        for article_id in ids[:max_articles]:
            if time.monotonic() - started > seconds:
                summary['stopReason'] = 'TIME_BUDGET'
                break
            try:
                html, raw = client.html(articles[article_id])
                record = parse_detail(html, article_id, student=student)
                if not student:
                    collect_media(record, client, store, budget)
            except SourceBlocked:
                raise
            except SourceError as exc:
                summary['articleErrors'].append({'articleId': article_id, 'error': str(exc)})
                store.record_failure(article_id, 'source', exc)
                continue
            result = store.save(record, raw)
            summary[result] += 1
            summary['processed'] += 1
            if publisher:
                try:
                    receipt = publisher.publish(record, store.get(article_id), store)
                except PublishError as exc:
                    if exc.fatal:
                        raise
                    store.record_failure(article_id, 'publication', exc)
                    summary['publicationErrors'].append({'articleId': article_id, 'error': str(exc)})
                    continue
                status = receipt['status']
                if status not in ('SHADOW_TAGGED', 'AWAITING_TAGS'):
                    summary['published'] += 1
                summary['publicationResults'][status] = summary['publicationResults'].get(status, 0) + 1
            store.clear_failure(article_id)
        summary.update(status='SUCCESS', mediaBytes=budget['bytes'],
                       remaining=len(articles) - summary['processed'])
        if summary['articleErrors'] or summary['publicationErrors']:
            raise SourceError('Some articles failed; healthy articles completed; see run report')
    except Exception as exc:
        summary.update(status='FAILED', errorType=type(exc).__name__, error=str(exc))
        raise
    finally:
        summary['finishedAt'] = now()
        store.save_run(summary)
    return summary
