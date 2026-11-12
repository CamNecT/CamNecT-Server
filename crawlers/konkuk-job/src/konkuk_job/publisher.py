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
from .parser import content_hash

PATH = '/api/internal/activity-imports/konkuk-job/tagged'
MAPPING_VERSION = 1


class PublishError(RuntimeError):
    def __init__(self, message, fatal=False):
        super().__init__(message)
        self.fatal = fatal


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise PublishError('Publication redirects are forbidden')


def make_payload(record, revision, store, *, tag_ids=None, mapping_version=MAPPING_VERSION):
    candidate = record['applicationDates']['candidate'] or {}
    links = list(dict.fromkeys([record['sourceUrl']] + record['externalLinks'] + record['images'] +
                               [a['url'] for a in record['attachments']]))
    student = record.get('source') == 'KONKUK_STUDENT_238'
    if student:
        links = [record['sourceUrl']]
    suffix = ('출처: 건국대학교 학생공지\n' if student else '\n\n원문 및 관련 자료\n') + '\n'.join(links)
    notice = '' if candidate.get('endDate') else '신청기간은 원문에서 확인해 주세요.\n\n'
    # MySQL TEXT is a byte limit; preserve links and disclose any truncated body.
    text = notice + ('' if student else record['bodyText'])
    marker = '\n(전체 내용은 원문 참고)'
    if len(suffix.encode('utf-16-le')) > 38000 or len(suffix.encode()) > 57000:
        raise PublishError('Source links exceed activity content capacity')
    if len((text + suffix).encode()) > 58000 or len((text + suffix).encode('utf-16-le')) > 39000:
        text = text.encode('utf-16-le')[:39000 - len((suffix + marker).encode('utf-16-le'))].decode('utf-16-le', errors='ignore')
        text = text.encode()[:58000 - len((suffix + marker).encode())].decode('utf-8', errors='ignore') + marker
    organizer = re.match(r'^\[([^\]]+)\]', record['title'])
    payload = {'articleId': record['articleId'], 'revision': revision * 1000 + mapping_version,
               'title': record['title'].encode('utf-16-le')[:400].decode('utf-16-le', errors='ignore'), 'content': text + suffix,
               'organizer': organizer[1].encode('utf-16-le')[:200].decode('utf-16-le', errors='ignore') if organizer else None,
               'applyStartDate': candidate.get('startDate'), 'applyEndDate': candidate.get('endDate'),
               'applyEndAt': candidate.get('endAt'), 'thumbnail': None}
    if tag_ids is not None:
        payload['tagIds'] = sorted(tag_ids)
    for media in ([] if student else record['media']):
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
    def __init__(self, base_url, secret, allow_local=False, opener=None, student=False,
                 job_mode='shadow', reader=None, reader_version=None):
        if not student and job_mode not in ('shadow', 'tagged'):
            raise ValueError('Unknown 4084 publication mode')
        self.student = student
        self.shadow = not student and job_mode == 'shadow'
        self.reader, self.reader_version = reader, reader_version
        self.deadline = None
        if self.shadow:
            self.url = 'shadow://KONKUK_JOB_4084'
            return
        p = urlsplit(base_url)
        local = allow_local and p.scheme == 'http' and p.hostname in ('127.0.0.1', 'localhost')
        if not (p.scheme == 'https' or local) or not p.hostname or p.username or p.password or p.query or p.fragment or p.path not in ('', '/'):
            raise ValueError('Publication base URL must be an HTTPS origin (or explicit local test origin)')
        if len(secret) < 32:
            raise ValueError('Publication signing secret must be at least 32 characters')
        self.path = '/api/internal/activity-imports/konkuk-student' if student else PATH
        self.url, self.secret = base_url.rstrip('/') + self.path, secret
        self.opener = opener or build_opener(NoRedirect())

    def job_decision(self, record, meta, store):
        if record['articleId'] != meta['articleId'] or content_hash(record) != meta['contentHash']:
            raise PublishError('Archived source changed before tagging')
        if self.reader is None:
            # A release supplies the frozen standalone reader as a module or Lambda layer.
            from camnect_tagger import READER_VERSION, classify
            self.reader, self.reader_version = classify, READER_VERSION
        if not isinstance(self.reader_version, str) or not re.fullmatch(r'[0-9A-Za-z][0-9A-Za-z._-]{0,63}', self.reader_version):
            raise PublishError('Invalid reader version', fatal=True)
        row = {'id': record['articleId'], 'title': record['title'], 'body': record['bodyText'], 'url': record['sourceUrl']}
        try:
            result = self.reader(row)
        except Exception as exc:
            raise PublishError('Reader failed before publication') from exc
        ids = result['assigned_tag_ids']
        if not isinstance(ids, list) or any(type(i) is not int or i <= 0 for i in ids) or len(set(ids)) != len(ids):
            raise PublishError('Invalid tag IDs from the reader')
        evidence = {entry['tag_id'] for entry in result['auto_candidates']
                    if isinstance(entry.get('evidence'), str) and entry['evidence'].strip()
                    and (entry['evidence'] in row['title'] or entry['evidence'] in row['body'])}
        if not set(ids) <= evidence:
            raise PublishError('Assigned tag lacks source evidence')
        key = f"tag-decisions/4084/{self.reader_version}/{row['id']}/{meta['version']}-{meta['contentHash']}.json"
        decision = {'articleId': row['id'], 'sourceVersion': meta['version'], 'sourceHash': meta['contentHash'],
                    'snapshotKey': meta['snapshotKey'], 'readerVersion': self.reader_version, 'result': result}
        store.put_bytes(key, json.dumps(decision, ensure_ascii=False, sort_keys=True).encode())
        return ids, key

    def publish(self, record, meta, store):
        if record.get('source') != ('KONKUK_STUDENT_238' if self.student else 'KONKUK_JOB_4084'):
            raise PublishError('Board does not match publication destination', fatal=True)
        tag_ids = None
        mapping_version = MAPPING_VERSION
        if not self.student:
            tag_ids, key = self.job_decision(record, meta, store)
            if self.shadow or not tag_ids:
                return {'status': 'SHADOW_TAGGED' if tag_ids else 'AWAITING_TAGS', 'decisionKey': key}
            release = re.fullmatch(r'([1-9])\.([0-9]{1,2})', self.reader_version)
            if not release:
                raise PublishError('Tagged publication requires a numbered major.minor reader release', fatal=True)
            mapping_version = 2 + (int(release[1]) - 1) * 100 + int(release[2])
        payload = make_payload(record, int(meta['version']), store, tag_ids=tag_ids, mapping_version=mapping_version)
        body = json.dumps(payload, ensure_ascii=False, separators=(',', ':')).encode()
        for attempt in range(3):
            if self.deadline is not None and time.monotonic() + 30 >= self.deadline:
                raise PublishError('Publication time budget exhausted', fatal=True)
            timestamp = str(int(time.time()))
            signature = hmac.new(self.secret.encode(),
                f'{timestamp}\nPOST\n{self.path}\n{hashlib.sha256(body).hexdigest()}'.encode(), hashlib.sha256).hexdigest()
            request = Request(self.url, data=body, headers={'Content-Type': 'application/json',
                'X-Crawler-Timestamp': timestamp, 'X-Crawler-Signature': signature})
            try:
                with self.opener.open(request, timeout=30) as response:
                    try:
                        result = json.loads(response.read(64000))
                    except (ValueError, UnicodeError) as exc:
                        raise PublishError('Invalid publication JSON', fatal=True) from exc
                if not isinstance(result, dict) or not isinstance(result.get('data'), dict):
                    raise PublishError('Invalid publication response shape', fatal=True)
                data = result.get('data', {})
                if result.get('status') != 200 or data.get('status') not in (
                        'CREATED', 'UPDATED', 'UNCHANGED', 'MANUAL_OVERRIDE', 'DELETED', 'STALE') or (
                        type(data.get('activityId')) is not int or data['activityId'] <= 0):
                    raise PublishError('Unexpected publication response', fatal=True)
                if data['status'] == 'STALE':
                    raise PublishError('Server revision is ahead; restore source state before retrying')
                if data['status'] in ('CREATED', 'UPDATED', 'UNCHANGED') and (
                        type(data.get('revision')) is not int or data['revision'] != payload['revision']):
                    raise PublishError('Publication revision mismatch', fatal=True)
                receipt = {'sourceVersion': meta['version'], 'payloadHash': payload['contentHash'],
                           'destination': self.url,
                           'activityId': data['activityId'], 'status': data['status'], 'publishedAt': timestamp}
                store.save_publication(record['articleId'], receipt)
                return receipt
            except HTTPError as exc:
                if exc.code not in (409, 429, 500, 502, 503, 504) or attempt == 2:
                    raise PublishError(f'Publication HTTP {exc.code}',
                                       fatal=exc.code in (401, 403, 429) or exc.code >= 500) from exc
            except (URLError, TimeoutError) as exc:
                if attempt == 2:
                    raise PublishError('Publication connection failed', fatal=True) from exc
            time.sleep(2 ** attempt)
        raise PublishError('No publication result')


def from_environment(allow_local=False, *, student=False):
    base = os.environ.get('CAMNECT_API_BASE_URL', '')
    secret = os.environ.get('CAMNECT_IMPORT_SECRET', '')
    if not base and not secret and (student or not os.environ.get('JOB_TAG_PUBLICATION_MODE')):
        return None
    return Publisher(base, secret, allow_local, student=student, job_mode=os.environ.get('JOB_TAG_PUBLICATION_MODE', 'shadow'))
