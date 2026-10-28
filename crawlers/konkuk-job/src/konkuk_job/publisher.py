"""Signed, idempotent delivery into CamNecT's actual external activity domain."""
import base64
import hashlib
import hmac
import json
import os
import re
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

PATH = '/api/internal/activity-imports/konkuk-job'
MAPPING_VERSION = 1


class PublishError(RuntimeError):
    pass


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise PublishError('Publication redirects are forbidden')


def make_payload(record, revision, store):
    candidate = record['applicationDates']['candidate'] or {}
    links = list(dict.fromkeys([record['sourceUrl']] + record['externalLinks'] + record['images'] +
                               [a['url'] for a in record['attachments']]))
    suffix = '\n\n원문 및 관련 자료\n' + '\n'.join(links)
    notice = '' if candidate.get('endDate') else '신청기간은 원문에서 확인해 주세요.\n\n'
    # MySQL TEXT is a byte limit; preserve links and disclose any truncated body.
    text = notice + record['bodyText']
    marker = '\n(전체 내용은 원문 참고)'
    if len(suffix.encode('utf-16-le')) > 38000 or len(suffix.encode()) > 57000:
        raise PublishError('Source links exceed activity content capacity')
    if len((text + suffix).encode()) > 58000 or len((text + suffix).encode('utf-16-le')) > 39000:
        text = text.encode('utf-16-le')[:39000 - len((suffix + marker).encode('utf-16-le'))].decode('utf-16-le', errors='ignore')
        text = text.encode()[:58000 - len((suffix + marker).encode())].decode('utf-8', errors='ignore') + marker
    organizer = re.match(r'^\[([^\]]+)\]', record['title'])
    payload = {'articleId': record['articleId'], 'revision': revision * 1000 + MAPPING_VERSION,
               'title': record['title'].encode('utf-16-le')[:400].decode('utf-16-le', errors='ignore'), 'content': text + suffix,
               'organizer': organizer[1].encode('utf-16-le')[:200].decode('utf-16-le', errors='ignore') if organizer else None,
               'applyStartDate': candidate.get('startDate'), 'applyEndDate': candidate.get('endDate'),
               'applyEndAt': candidate.get('endAt'), 'thumbnail': None}
    for media in record['media']:
        if media['status'] == 'DOWNLOADED' and media.get('mime') in ('image/jpeg', 'image/png', 'image/webp'):
            raw = store.read_bytes(media['blobKey'])
            if hashlib.sha256(raw).hexdigest() != media['sha256']:
                raise PublishError('Archived thumbnail hash mismatch')
            payload['thumbnail'] = {'contentType': media['mime'], 'sha256': media['sha256'],
                                    'base64': base64.b64encode(raw).decode()}
            break
    # Includes mapping version and canonical data, so changes to the publication mapper are detectable.
    payload['contentHash'] = hashlib.sha256(json.dumps(
        {k: v for k, v in payload.items() if k != 'revision'}, sort_keys=True, ensure_ascii=False,
        separators=(',', ':')).encode()).hexdigest()
    return payload


class Publisher:
    def __init__(self, base_url, secret, allow_local=False, opener=None):
        p = urlsplit(base_url)
        local = allow_local and p.scheme == 'http' and p.hostname in ('127.0.0.1', 'localhost')
        if not (p.scheme == 'https' or local) or not p.hostname or p.username or p.password or p.query or p.fragment or p.path not in ('', '/'):
            raise ValueError('Publication base URL must be an HTTPS origin (or explicit local test origin)')
        if len(secret) < 32:
            raise ValueError('Publication signing secret must be at least 32 characters')
        self.url, self.secret = base_url.rstrip('/') + PATH, secret
        self.opener = opener or build_opener(NoRedirect())

    def publish(self, record, meta, store):
        payload = make_payload(record, int(meta['version']), store)
        body = json.dumps(payload, ensure_ascii=False, separators=(',', ':')).encode()
        for attempt in range(3):
            timestamp = str(int(time.time()))
            signature = hmac.new(self.secret.encode(),
                f'{timestamp}\nPOST\n{PATH}\n{hashlib.sha256(body).hexdigest()}'.encode(), hashlib.sha256).hexdigest()
            request = Request(self.url, data=body, headers={'Content-Type': 'application/json',
                'X-Crawler-Timestamp': timestamp, 'X-Crawler-Signature': signature})
            try:
                with self.opener.open(request, timeout=30) as response:
                    result = json.loads(response.read(64000))
                data = result.get('data', {})
                if result.get('status') != 200 or data.get('status') not in (
                        'CREATED', 'UPDATED', 'UNCHANGED', 'MANUAL_OVERRIDE', 'DELETED', 'STALE') or not data.get('activityId'):
                    raise PublishError('Unexpected publication response')
                receipt = {'sourceVersion': meta['version'], 'payloadHash': payload['contentHash'],
                           'destination': self.url,
                           'activityId': data['activityId'], 'status': data['status'], 'publishedAt': timestamp}
                store.save_publication(record['articleId'], receipt)
                return receipt
            except HTTPError as exc:
                if exc.code not in (409, 429, 500, 502, 503, 504) or attempt == 2:
                    raise PublishError(f'Publication HTTP {exc.code}') from exc
            except (URLError, TimeoutError) as exc:
                if attempt == 2:
                    raise PublishError('Publication connection failed') from exc
            time.sleep(2 ** attempt)
        raise PublishError('No publication result')


def from_environment(allow_local=False):
    base = os.environ.get('CAMNECT_API_BASE_URL', '')
    secret = os.environ.get('CAMNECT_IMPORT_SECRET', '')
    if not base and not secret:
        return None
    return Publisher(base, secret, allow_local)
