import hashlib
import json
import sqlite3
import uuid
from datetime import datetime, timezone
from pathlib import Path

from .parser import SOURCE, content_hash


def now():
    return datetime.now(timezone.utc).isoformat()


def encode(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2).encode('utf-8')


class Store:
    def save(self, record, raw_html):
        old = self.get(record['articleId'])
        digest, timestamp = content_hash(record), now()
        changed = old is None or old['contentHash'] != digest
        version = int(old['version']) + int(changed) if old else 1
        if changed:
            raw_key = self.blob(raw_html, 'html')
            key = f"revisions/{record['articleId']}/{version:06d}-{uuid.uuid4().hex}.json"
            self.put_bytes(key, encode({'record': record, 'contentHash': digest,
                                       'version': version, 'observedAt': timestamp,
                                       'rawHtmlKey': raw_key}))
        else:
            key = old['snapshotKey']
        meta = {'articleId': record['articleId'], 'contentHash': digest, 'version': version,
                'writeToken': uuid.uuid4().hex,
                'firstSeenAt': old['firstSeenAt'] if old else timestamp,
                'lastSeenAt': timestamp, 'snapshotKey': key,
                'title': record['title'], 'reviewStatus': 'PENDING'}
        self.put_meta(meta, old, changed)
        return 'created' if old is None else 'updated' if changed else 'unchanged'

    def blob(self, data, extension='bin'):
        digest = hashlib.sha256(data).hexdigest()
        key = f'blobs/{digest}.{extension}'
        self.put_bytes(key, data)
        return key

    def save_run(self, summary):
        self.put_bytes(f"runs/{summary['runId']}.json", encode(summary))


class LocalStore(Store):
    def __init__(self, directory):
        self.root = Path(directory)
        self.root.mkdir(parents=True, exist_ok=True)
        self.db = sqlite3.connect(self.root / 'state.sqlite3', timeout=30)
        self.db.executescript('''
            CREATE TABLE IF NOT EXISTS articles (article_id TEXT PRIMARY KEY, metadata TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS revisions (
                article_id TEXT NOT NULL, version INTEGER NOT NULL, snapshot_key TEXT NOT NULL,
                PRIMARY KEY(article_id, version));
            CREATE TABLE IF NOT EXISTS publications (article_id TEXT PRIMARY KEY, receipt TEXT NOT NULL);
        ''')

    def get(self, article_id):
        row = self.db.execute('SELECT metadata FROM articles WHERE article_id=?', (article_id,)).fetchone()
        return json.loads(row[0]) if row else None

    def read_bytes(self, key):
        path = (self.root / key).resolve()
        if not path.is_relative_to(self.root.resolve()):
            raise ValueError('Archive path escaped data directory')
        return path.read_bytes()

    def get_publication(self, article_id):
        row = self.db.execute('SELECT receipt FROM publications WHERE article_id=?', (article_id,)).fetchone()
        return json.loads(row[0]) if row else None

    def save_publication(self, article_id, receipt):
        with self.db:
            self.db.execute('INSERT OR REPLACE INTO publications VALUES (?,?)', (article_id, encode(receipt).decode()))

    def all_metadata(self):
        return [json.loads(row[0]) for row in self.db.execute('SELECT metadata FROM articles ORDER BY article_id')]

    def put_bytes(self, key, value):
        path = self.root / key
        if key.startswith('blobs/') and path.exists():
            return
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(path.name + '.' + uuid.uuid4().hex + '.tmp')
        temporary.write_bytes(value)
        temporary.replace(path)

    def put_meta(self, meta, old, changed):
        with self.db:
            self.db.execute('BEGIN IMMEDIATE')
            if self.get(meta['articleId']) != old:
                raise RuntimeError('Concurrent update; retry on next run')
            self.db.execute('INSERT OR REPLACE INTO articles VALUES (?,?)',
                            (meta['articleId'], encode(meta).decode()))
            if changed:
                self.db.execute('INSERT INTO revisions VALUES (?,?,?)',
                                (meta['articleId'], meta['version'], meta['snapshotKey']))

    def export(self):
        records = []
        for (raw_meta,) in self.db.execute('SELECT metadata FROM articles ORDER BY article_id'):
            meta = json.loads(raw_meta)
            snapshot = json.loads((self.root / meta['snapshotKey']).read_text(encoding='utf-8'))
            records.append({**meta, 'record': snapshot['record'], 'publication': self.get_publication(meta['articleId'])})
        self.put_bytes('review-queue.json', encode(records))
        return len(records)


class AwsStore(Store):
    def __init__(self, table, bucket, s3):
        self.table, self.bucket, self.s3 = table, bucket, s3

    def get(self, article_id):
        result = self.table.get_item(Key={'pk': f'{SOURCE}#{article_id}'}, ConsistentRead=True)
        item = result.get('Item')
        return json.loads(item['metadata']) if item else None

    def read_bytes(self, key):
        if not key.startswith(('blobs/', 'revisions/')) or '..' in key:
            raise ValueError('Invalid archive key')
        response = self.s3.get_object(Bucket=self.bucket, Key=key)
        with response['Body'] as stream:
            data = stream.read(12_000_001)
        if len(data) > 12_000_000:
            raise ValueError('Archive object exceeds publication limit')
        return data

    def get_publication(self, article_id):
        item = self.table.get_item(Key={'pk': f'PUBLISH#{SOURCE}#{article_id}'}, ConsistentRead=True).get('Item')
        return json.loads(item['receipt']) if item else None

    def save_publication(self, article_id, receipt):
        self.table.put_item(Item={'pk': f'PUBLISH#{SOURCE}#{article_id}', 'receipt': encode(receipt).decode()})

    def put_bytes(self, key, value):
        from botocore.exceptions import ClientError
        try:
            self.s3.put_object(Bucket=self.bucket, Key=key, Body=value, IfNoneMatch='*',
                               ContentType='application/json' if key.endswith('.json') else 'application/octet-stream',
                               ServerSideEncryption='AES256')
        except ClientError as exc:
            if exc.response['Error']['Code'] != 'PreconditionFailed':
                raise

    def put_meta(self, meta, old, changed):
        kwargs = {'ConditionExpression': 'attribute_not_exists(pk)'}
        if old:
            kwargs = {'ConditionExpression': '#metadata = :previous',
                      'ExpressionAttributeNames': {'#metadata': 'metadata'},
                      'ExpressionAttributeValues': {':previous': encode(old).decode()}}
        self.table.put_item(Item={'pk': f"{SOURCE}#{meta['articleId']}",
                                  'lastSeenAt': meta['lastSeenAt'],
                                  'metadata': encode(meta).decode()}, **kwargs)
