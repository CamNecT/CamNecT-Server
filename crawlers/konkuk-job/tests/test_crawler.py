import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
from urllib.error import HTTPError

from konkuk_job.http import HttpClient, PayloadTooLarge, SourceRedirect, source_url
from konkuk_job.parser import (BASE, LIST_URL, SourceBlocked, SourceError,
                               application_dates, content_hash, parse_detail, parse_list)
from konkuk_job.runner import collect_listing, collect_media, run
from konkuk_job.storage import AwsStore, LocalStore, encode


def listing(ids=('1', '1', '2'), pages=1):
    return ''.join(f'<a href="/bbs/job/4084/{i}/artclView.do">Title 새글</a>' for i in ids) + (
        '<form action="/bbs/job/4084/artclList.do" method="post">'
        '<input name="page" value="1"><input name="csrfToken" value="fixture"></form>'
        f'<a href="javascript:page_link(\'{pages}\')">끝</a>')


def detail(article_id='1', body='<p>접수기간</p><p>~ 2026. 10. 30. (금) 18:00 까지</p>', count=5):
    return f'''<dl class="view-num"><dd>{article_id}</dd></dl>
        <h2 class="view-title">테스트 공모전</h2><div class="view-util">
        <dl class="write"><dd>2026.10.28</dd></dl><dl class="writer"><dd>진로취업센터</dd></dl>
        <dl class="count"><dd>{count}</dd></dl></div><div class="view-con">{body}</div>
        <div class="view-file"></div>'''


class ParserTests(unittest.TestCase):
    def test_dedup_and_pagination_form(self):
        parsed = parse_list(listing(pages=4))
        self.assertEqual({'1', '2'}, set(parsed['articles']))
        self.assertEqual(4, parsed['pages'])
        self.assertEqual('fixture', parsed['fields']['csrfToken'])

    def test_block_and_unexpected_structure_are_not_empty_success(self):
        for html in ('<html>웹 방화벽에 의해 차단</html>', '<html>Login</html>'):
            with self.assertRaises(SourceError):
                parse_list(html)
        with self.assertRaises(SourceError):
            parse_detail(detail('2'), '1')
        with self.assertRaises(SourceError):
            parse_detail(detail(body=''), '1')

    def test_deadline_keeps_time_and_does_not_invent_start(self):
        value = parse_detail(detail(), '1')['applicationDates']['candidate']
        self.assertEqual('2026-10-30T18:00:00+09:00', value['endAt'])
        self.assertIsNone(value['startDate'])

    def test_training_dates_are_not_application_dates(self):
        text = '교육기간: 2026.12.14 ~ 2027.02.26\n모집기간: ~ 2026.11.22(일)까지'
        result, _ = application_dates(text)
        self.assertEqual('2026-11-22', result['candidate']['endDate'])
        self.assertIsNone(result['candidate']['endAt'])

    def test_abbreviated_or_conflicting_dates_need_review(self):
        for text in ('모집기간: 2026.10.01 ~ 10.30', '접수기간: ~2026.02.30',
                     '접수기간: 2026.10.01 ~ 2026.10.30\n모집마감: 2026.10.31'):
            value, reasons = application_dates(text)
            self.assertIsNone(value['candidate'])
            self.assertTrue(reasons)

    def test_inline_date_and_image_only_post(self):
        body = '<p>접수기간: ~ <span>2026.</span><span>10.</span><span>30.</span> 18:00</p>'
        self.assertEqual('2026-10-30', parse_detail(detail(body=body), '1')['applicationDates']['candidate']['endDate'])
        record = parse_detail(detail(body='<img src="http://job.konkuk.ac.kr/poster.jpg">'), '1')
        self.assertIn('IMAGE_DOMINANT', record['reviewReasons'])
        self.assertIsNone(record['applicationDates']['candidate'])
        self.assertEqual([BASE + '/poster.jpg'], record['images'])

    def test_no_active_content_and_counts_do_not_change_hash(self):
        first = parse_detail(detail(body='<p>Text</p><script>bad()</script><a href="javascript:bad()">link</a>'), '1')
        self.assertNotIn('bad', first['bodyText'])
        self.assertEqual([], first['externalLinks'])
        self.assertEqual(content_hash(parse_detail(detail(count=1), '1')),
                         content_hash(parse_detail(detail(count=99), '1')))


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.store = LocalStore(self.temp.name)
        self.record = parse_detail(detail(), '1')

    def tearDown(self):
        self.store.db.close()
        self.temp.cleanup()

    def test_idempotent_and_changed_revision_with_old_snapshot(self):
        self.assertEqual('created', self.store.save(self.record, b'first html'))
        old_key = self.store.get('1')['snapshotKey']
        self.assertEqual('unchanged', self.store.save(self.record, b'html with new views'))
        changed = copy.deepcopy(self.record)
        changed['bodyText'] += '\n마감 연장 안내'
        self.assertEqual('updated', self.store.save(changed, b'changed html'))
        self.assertEqual(2, self.store.get('1')['version'])
        self.assertEqual(2, self.store.db.execute('SELECT COUNT(*) FROM revisions').fetchone()[0])
        self.assertEqual(self.record, json.loads((Path(self.temp.name) / old_key).read_text(encoding='utf-8'))['record'])
        self.assertEqual(1, self.store.export())

    def test_same_url_media_change_creates_revision(self):
        client = Mock()
        client.fetch.side_effect = [(b'\xff\xd8\xffONE', 'application/x-msdownload'),
                                    (b'\xff\xd8\xffTWO', 'application/x-msdownload')]
        for expected in ('created', 'updated'):
            record = copy.deepcopy(self.record)
            record['attachments'] = [{'url': BASE + '/poster.jpg', 'name': 'poster.jpg'}]
            collect_media(record, client, self.store, {'bytes': 0})
            self.assertEqual('image/jpeg', record['media'][0]['mime'])
            self.assertEqual(expected, self.store.save(record, b'html'))

    def test_concurrent_state_update_is_rejected(self):
        self.store.save(self.record, b'html')
        old = self.store.get('1')
        self.store.save(self.record, b'html')
        with self.assertRaises(RuntimeError):
            self.store.put_meta(old, old, False)

    def test_failure_does_not_overwrite_previous_version(self):
        self.store.save(self.record, b'html')
        old = self.store.get('1')
        client = Mock()
        client.html.side_effect = [(listing(('1',)), b'list'), ('<html>Login</html>', b'bad')]
        with self.assertRaises(SourceError):
            run(client, self.store)
        self.assertEqual(old, self.store.get('1'))
        reports = list((Path(self.temp.name) / 'runs').glob('*.json'))
        self.assertEqual('FAILED', json.loads(reports[0].read_text())['status'])

    def test_capped_runs_rotate_and_missing_posts_are_preserved(self):
        class FakeClient:
            def load_robots(self):
                pass
            def html(self, url, fields=None):
                if url == LIST_URL:
                    return listing(('1', '2')), b'list'
                return detail(url.split('/')[-2]), b'raw'
        client = FakeClient()
        self.assertEqual(1, run(client, self.store, max_articles=1)['created'])
        self.assertEqual(1, run(client, self.store, max_articles=1)['created'])
        self.assertEqual(2, run(client, self.store)['unchanged'])
        client.html = lambda url, fields=None: (listing(('1',)), b'list') if url == LIST_URL else (detail(), b'raw')
        run(client, self.store)
        self.assertIsNotNone(self.store.get('2'))


class TransportAndPaginationTests(unittest.TestCase):
    def test_exact_host_https_and_redirect_guard(self):
        for url in ('http://job.konkuk.ac.kr/x', 'https://job.konkuk.ac.kr.evil/x',
                    'https://127.0.0.1/x', 'https://user@job.konkuk.ac.kr/x'):
            with self.assertRaises(SourceError):
                source_url(url)
        with self.assertRaises(SourceError):
            SourceRedirect().redirect_request(None, None, 302, '', {}, 'https://example.com/')

    def test_429_stops_without_retries(self):
        opener = Mock()
        opener.open.side_effect = HTTPError(LIST_URL, 429, 'Rate limited', {}, None)
        with patch('konkuk_job.http.time.sleep'):
            with self.assertRaises(SourceBlocked):
                HttpClient(opener=opener).fetch(LIST_URL)
        self.assertEqual(1, opener.open.call_count)

    def test_retry_transient_503_is_bounded(self):
        opener = Mock()
        opener.open.side_effect = HTTPError(LIST_URL, 503, 'Unavailable', {}, None)
        with patch('konkuk_job.http.time.sleep'):
            with self.assertRaises(SourceError):
                HttpClient(opener=opener).fetch(LIST_URL)
        self.assertEqual(3, opener.open.call_count)

    def test_pagination_carries_form_and_detects_repeat(self):
        client = Mock()
        client.html.side_effect = [(listing(('1',), 2), b''), (listing(('2',), 2), b'')]
        articles, pages = collect_listing(client)
        self.assertEqual({'1', '2'}, set(articles))
        self.assertEqual(2, pages)
        self.assertEqual('2', client.html.call_args.args[1]['page'])
        self.assertEqual('fixture', client.html.call_args.args[1]['csrfToken'])
        client.html.side_effect = [(listing(('1',), 2), b'')] * 2
        with self.assertRaises(SourceError):
            collect_listing(client)

    def test_external_media_never_fetched(self):
        record = parse_detail(detail(body='<img src="https://example.com/image.png">'), '1')
        client = Mock()
        collect_media(record, client, Mock(), {'bytes': 0})
        client.fetch.assert_not_called()
        self.assertEqual('EXTERNAL_NOT_FETCHED', record['media'][0]['status'])

    def test_large_media_is_reviewable_but_run_budget_is_not_mislabelled(self):
        record = parse_detail(detail(body='<img src="/poster.jpg">'), '1')
        client = Mock()
        client.fetch.side_effect = PayloadTooLarge()
        collect_media(record, client, Mock(), {'bytes': 0})
        self.assertEqual('TOO_LARGE', record['media'][0]['status'])
        self.assertIn('MEDIA_SIZE_LIMIT', record['reviewReasons'])
        with self.assertRaises(SourceBlocked):
            collect_media(record, client, Mock(), {'bytes': 60_000_000})

    def test_media_block_page_stops_entire_run(self):
        record = parse_detail(detail(body='<img src="/poster.jpg">'), '1')
        client = Mock()
        client.fetch.return_value = ('<html>웹 방화벽에 의해 차단</html>'.encode(), 'text/html')
        with self.assertRaises(SourceBlocked):
            collect_media(record, client, Mock(), {'bytes': 0})


class AwsTests(unittest.TestCase):
    def test_lambda_rejects_wrong_account_before_aws_calls(self):
        import app
        context = Mock(invoked_function_arn='arn:aws:lambda:ap-northeast-2:000000000000:function:wrong')
        with patch.dict('os.environ', {'EXPECTED_ACCOUNT_ID': '117088935250'}):
            with patch.object(app.boto3, 'resource') as resource:
                with self.assertRaisesRegex(RuntimeError, 'Unexpected AWS account'):
                    app.handler({}, context)
                resource.assert_not_called()

    def test_real_sdk_validates_conditional_s3_and_dynamodb_requests(self):
        import boto3
        from botocore.stub import Stubber
        session = boto3.Session(aws_access_key_id='testing', aws_secret_access_key='testing', region_name='ap-northeast-2')
        s3 = session.client('s3')
        table = session.resource('dynamodb').Table('test-table')
        store = AwsStore(table, 'test-bucket', s3)
        with Stubber(s3) as stub:
            stub.add_response('put_object', {}, {'Bucket': 'test-bucket', 'Key': 'test.json', 'Body': b'{}',
                'IfNoneMatch': '*', 'ContentType': 'application/json', 'ServerSideEncryption': 'AES256'})
            store.put_bytes('test.json', b'{}')
            stub.assert_no_pending_responses()
        with Stubber(table.meta.client) as stub:
            stub.add_response('get_item', {})
            self.assertIsNone(store.get('1'))
            stub.add_response('put_item', {})
            store.put_meta({'articleId': '1', 'lastSeenAt': 'now'}, None, True)
            stub.assert_no_pending_responses()

    def test_s3_duplicate_blob_is_success_and_other_errors_propagate(self):
        from botocore.exceptions import ClientError
        s3 = Mock()
        store = AwsStore(Mock(), 'bucket', s3)
        s3.put_object.side_effect = ClientError({'Error': {'Code': 'PreconditionFailed'}}, 'PutObject')
        store.blob(b'content')
        self.assertEqual('*', s3.put_object.call_args.kwargs['IfNoneMatch'])
        s3.put_object.side_effect = ClientError({'Error': {'Code': 'AccessDenied'}}, 'PutObject')
        with self.assertRaises(ClientError):
            store.blob(b'content')

    def test_dynamodb_uses_conditional_write(self):
        table = Mock()
        store = AwsStore(table, 'bucket', Mock())
        meta = {'articleId': '1', 'lastSeenAt': 'new'}
        store.put_meta(meta, None, True)
        self.assertEqual('attribute_not_exists(pk)', table.put_item.call_args.kwargs['ConditionExpression'])
        store.put_meta(meta, {'lastSeenAt': 'old'}, False)
        self.assertEqual({':previous': encode({'lastSeenAt': 'old'}).decode()},
                         table.put_item.call_args.kwargs['ExpressionAttributeValues'])


if __name__ == '__main__':
    unittest.main()
