import copy
import hashlib
import hmac
import json
import tempfile
import unittest
from unittest.mock import Mock, patch
from urllib.error import HTTPError, URLError

from konkuk_job.parser import LIST_URL, parse_detail
from konkuk_job.publisher import PATH, NoRedirect, PublishError, Publisher, make_payload
from konkuk_job.runner import run
from konkuk_job.storage import LocalStore
from test_crawler import detail, listing


class PublisherTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.store = LocalStore(self.tmp.name)
        self.record = parse_detail(detail(body='<p>기간 미정</p><img src="/poster.png">'), '1')
        self.store.save(self.record, b'html')
        self.meta = self.store.get('1')

    def tearDown(self):
        self.store.db.close()
        self.tmp.cleanup()

    def response(self, status='CREATED'):
        response = Mock()
        response.read.return_value = json.dumps({'status': 200, 'data': {'status': status, 'activityId': 42, 'revision': 1001}}).encode()
        response.__enter__ = Mock(return_value=response)
        response.__exit__ = Mock(return_value=False)
        return response

    def test_unknown_dates_publish_with_source_and_no_invented_deadline(self):
        payload = make_payload(self.record, 1, self.store)
        self.assertEqual(1001, payload['revision'])
        self.assertIn('신청기간은 원문에서 확인', payload['content'])
        self.assertIn(self.record['sourceUrl'], payload['content'])
        self.assertIsNone(payload['applyEndDate'])
        self.assertIsNone(payload['thumbnail'])

    def test_signed_delivery_records_acknowledgement_only_after_success(self):
        opener = Mock()
        opener.open.return_value = self.response()
        publisher = Publisher('https://api.camnect.site', 'x' * 32, opener=opener)
        receipt = publisher.publish(self.record, self.meta, self.store)
        request = opener.open.call_args.args[0]
        timestamp = request.get_header('X-crawler-timestamp')
        expected = hmac.new(b'x' * 32, f'{timestamp}\nPOST\n{PATH}\n{hashlib.sha256(request.data).hexdigest()}'.encode(), hashlib.sha256).hexdigest()
        self.assertEqual(expected, request.get_header('X-crawler-signature'))
        self.assertEqual(receipt, self.store.get_publication('1'))
        self.assertEqual(publisher.url, receipt['destination'])

    def test_response_loss_retries_same_payload_without_duplicate_semantics(self):
        opener = Mock()
        opener.open.side_effect = [URLError('response lost'), self.response('UNCHANGED')]
        publisher = Publisher('https://api.camnect.site', 'x' * 32, opener=opener)
        with patch('konkuk_job.publisher.time.sleep'):
            receipt = publisher.publish(self.record, self.meta, self.store)
        self.assertEqual('UNCHANGED', receipt['status'])
        self.assertEqual(opener.open.call_args_list[0].args[0].data, opener.open.call_args_list[1].args[0].data)

    def test_bad_auth_does_not_acknowledge_or_retry(self):
        opener = Mock()
        opener.open.side_effect = HTTPError('https://api.camnect.site', 401, 'no', {}, None)
        with self.assertRaises(PublishError):
            Publisher('https://api.camnect.site', 'x' * 32, opener=opener).publish(self.record, self.meta, self.store)
        self.assertIsNone(self.store.get_publication('1'))
        self.assertEqual(1, opener.open.call_count)

    def test_url_validation_and_redirects_prevent_secret_forwarding(self):
        for url in ('http://api.camnect.site', 'http://127.0.0.1:8080', 'https://user:password@api.camnect.site', 'https://api.camnect.site/other'):
            with self.assertRaises(ValueError):
                Publisher(url, 'x' * 32)
        Publisher('http://127.0.0.1:8080', 'x' * 32, allow_local=True)
        with self.assertRaises(PublishError):
            NoRedirect().redirect_request(None, None, 302, '', {}, 'https://other.example')

    def test_archive_integrity_before_thumbnail_publication(self):
        self.record['media'] = [{'status': 'DOWNLOADED', 'mime': 'image/png',
                                 'blobKey': self.store.blob(b'corrupt'), 'sha256': '0' * 64}]
        with self.assertRaises(PublishError):
            make_payload(self.record, 1, self.store)

    def test_long_emoji_content_fits_java_utf16_and_mysql_text_limits(self):
        self.record['title'] = '[' + '😀' * 100 + '] ' + '😀' * 200
        self.record['bodyText'] = '😀' * 20000
        payload = make_payload(self.record, 1, self.store)
        self.assertLessEqual(len(payload['content'].encode('utf-16-le')), 40000)
        self.assertLessEqual(len(payload['content'].encode()), 60000)
        self.assertIn(self.record['sourceUrl'], payload['content'])
        self.assertLessEqual(len(payload['title'].encode('utf-16-le')), 400)
        self.assertLessEqual(len(payload['organizer'].encode('utf-16-le')), 200)

    def test_publication_failure_remains_retryable_after_archive_commit(self):
        class FakeClient:
            def load_robots(self): pass
            def html(self, url, fields=None):
                return (listing(('2',)), b'list') if url == LIST_URL else (detail('2'), b'html')
        opener = Mock()
        opener.open.side_effect = HTTPError('https://api.camnect.site', 401, 'no', {}, None)
        publisher = Publisher('https://api.camnect.site', 'x' * 32, opener=opener)
        with self.assertRaises(PublishError):
            run(FakeClient(), self.store, publisher=publisher)
        self.assertIsNotNone(self.store.get('2'))
        self.assertIsNone(self.store.get_publication('2'))
        opener.open.side_effect = None
        opener.open.return_value = self.response()
        result = run(FakeClient(), self.store, publisher=publisher)
        self.assertEqual(1, result['unchanged'])
        self.assertEqual(1, result['published'])


if __name__ == '__main__':
    unittest.main()
