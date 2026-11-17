import io
import json
import tempfile
import time
import unittest
from email.message import Message
from unittest.mock import MagicMock, Mock, patch
from urllib.error import HTTPError

from konkuk_job.http import HttpClient, IncompleteMedia, PayloadTooLarge, RunBudgetReached
from konkuk_job.parser import LIST_URL, SourceBlocked, SourceError
from konkuk_job.publisher import PublishError, Publisher
from konkuk_job.runner import run
from konkuk_job.storage import AwsStore, LocalStore
from test_crawler import detail, listing


class FixtureClient:
    def __init__(self):
        self.ids = ['1', '2', '3']
        self.visits = []
        self.files = {}
        self.error = {}

    def load_robots(self):
        pass

    def html(self, url, fields=None):
        if url == LIST_URL:
            return listing(self.ids), b'list'
        article = url.split('/')[-2]
        self.visits.append(article)
        if article in self.error:
            raise self.error[article]
        return detail(article, '<img src="/' + article + '.jpg">'), b'html'

    def fetch(self, url, *, limit, media):
        data = self.files.get(url.rsplit('/', 1)[-1], b'GIF89a')
        if len(data) > limit:
            raise PayloadTooLarge()
        return data, 'image/gif'


class ResumeTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.store = LocalStore(tmp.name)
        self.addCleanup(self.store.db.close)
        self.client = FixtureClient()
        self.enterContext(patch('konkuk_job.runner.RUN_MEDIA_BYTES', 10))
        self.enterContext(patch('konkuk_job.runner.FILE_MEDIA_BYTES', 8))

    def test_resume_survives_restart_and_new_notices(self):
        first = run(self.client, self.store)
        self.assertEqual(('SUCCESS', 'MEDIA_BUDGET', 1, 2, 6, '2'),
                         tuple(first[k] for k in ('status', 'stopReason', 'processed', 'remaining', 'mediaBytes', 'nextArticleId')))
        self.assertEqual(['2', '1'], self.store.get_pending())
        self.assertIsNone(self.store.get('2'))
        self.assertIsNone(self.store.get_failure('2'))
        reports = list((self.store.root / 'runs').glob('*.json'))
        self.assertEqual(first, json.loads(reports[0].read_bytes()))
        second_store = LocalStore(self.store.root)
        self.addCleanup(second_store.db.close)
        self.client.ids.append('4')
        self.client.visits.clear()
        run(self.client, second_store, max_articles=1)
        self.assertEqual(['2'], self.client.visits)
        self.assertEqual(['1'], second_store.get_pending())
        run(self.client, second_store, max_articles=1)
        self.assertEqual([], second_store.get_pending())
        self.assertEqual(1, second_store.get('2')['version'])

    def test_withdrawn_and_backed_off_pending_do_not_block_new_work(self):
        self.store.save_pending([], ['9', '2'])
        self.store.record_failure('2', 'source', ValueError('old'))
        run(self.client, self.store, max_articles=1)
        self.assertEqual(['3'], self.client.visits)
        self.assertEqual([], self.store.get_pending())

    def test_prior_article_error_still_fails_when_budget_runs_out(self):
        self.client.error['3'] = SourceError('broken detail')
        with self.assertRaisesRegex(SourceError, 'Some articles failed'):
            run(self.client, self.store)
        self.assertEqual(['1'], self.store.get_pending())
        self.assertTrue(self.store.get_failure('3'))
        self.assertEqual(1, self.store.get('2')['version'])

    def test_source_block_after_resume_still_alarms(self):
        run(self.client, self.store)
        self.client.error['2'] = SourceBlocked('403')
        with self.assertRaisesRegex(SourceBlocked, '403'):
            run(self.client, self.store)
        self.assertEqual(['2', '1'], self.store.get_pending())

    def test_publication_timeout_keeps_saved_revision_pending(self):
        publisher = Mock(url='https://api.example')
        publisher.publish.side_effect = RunBudgetReached('TIME_BUDGET')
        first = run(self.client, self.store, publisher=publisher)
        self.assertEqual((1, 0, '3'), (first['processed'], first['published'], first['nextArticleId']))
        self.assertIsNone(self.store.get_publication('3'))
        publisher.publish.side_effect = None
        publisher.publish.return_value = {'status': 'UNCHANGED'}
        self.client.ids.append('4')
        second = run(self.client, self.store, publisher=publisher, max_articles=1)
        self.assertEqual((1, 1), (second['unchanged'], second['published']))
        self.assertEqual('3', self.client.visits[-1])

    def test_repeated_zero_progress_is_visible(self):
        self.client.error['3'] = RunBudgetReached('TIME_BUDGET')
        first = run(self.client, self.store)
        self.assertEqual('TIME_BUDGET', first['stopReason'])
        with self.assertRaisesRegex(SourceError, 'fresh run budget'):
            run(self.client, self.store)
        self.assertEqual(['3', '2', '1'], self.store.get_pending())

    def test_exact_exhaustion_stops_before_next_download(self):
        self.client.files['3.jpg'] = b'GIF89abc'
        self.client.files['2.jpg'] = b'xx'
        first = run(self.client, self.store)
        self.assertEqual((10, 2, '1'), (first['mediaBytes'], first['processed'], first['nextArticleId']))

    def test_stale_cursor_cannot_overwrite_progress(self):
        self.store.save_pending([], ['3'])
        with self.assertRaisesRegex(RuntimeError, 'Concurrent'):
            self.store.save_pending([], ['1'])


class LimitTransportTests(unittest.TestCase):
    def media(self, content, length=None):
        response = MagicMock()
        response.__enter__.return_value = response
        response.url = LIST_URL
        response.headers = Message()
        if length is not None:
            response.headers['Content-Length'] = str(length)
        response.read.side_effect = io.BytesIO(content).read
        opener = Mock()
        opener.open.return_value = response
        return HttpClient(opener=opener), response

    def test_media_never_probes_past_budget_and_accepts_verified_exact_fit(self):
        client, response = self.media(b'123456')
        with self.assertRaises(PayloadTooLarge) as caught:
            client.fetch(LIST_URL, limit=4, media=True)
        response.read.assert_called_once_with(4)
        self.assertTrue(caught.exception.size_unverified)
        self.assertEqual(4, caught.exception.bytes_read)
        client, _ = self.media(b'1234', 4)
        self.assertEqual(b'1234', client.fetch(LIST_URL, limit=4, media=True)[0])
        client, response = self.media(b'123456', 6)
        with self.assertRaises(PayloadTooLarge):
            client.fetch(LIST_URL, limit=4, media=True)
        response.read.assert_not_called()

    def test_truncated_declared_body_is_not_archived(self):
        client, _ = self.media(b'12', 4)
        with self.assertRaises(IncompleteMedia) as caught:
            client.fetch(LIST_URL, limit=4, media=True)
        self.assertEqual(2, caught.exception.bytes_read)

    def test_request_deadline_does_not_hide_http_failure(self):
        client = HttpClient(opener=Mock())
        client.deadline = time.monotonic() - 1
        with self.assertRaisesRegex(RunBudgetReached, 'TIME_BUDGET'):
            client.fetch(LIST_URL)
        client.opener.open.assert_not_called()
        client.deadline = float('inf')
        def fail(*args, **kwargs):
            client.deadline = time.monotonic()
            raise HTTPError(LIST_URL, 503, 'down', {}, None)
        client.opener.open.side_effect = fail
        with patch('konkuk_job.http.time.sleep'), self.assertRaisesRegex(SourceError, 'no time to retry'):
            client.fetch(LIST_URL)

    def test_publisher_retry_deadline_preserves_real_failure(self):
        opener = Mock()
        pub = Publisher('https://api.example', 'x' * 32, opener=opener)
        def fail(*args, **kwargs):
            pub.deadline = 0
            raise HTTPError(pub.url, 503, 'down', {}, None)
        opener.open.side_effect = fail
        with patch('konkuk_job.publisher.make_payload', return_value={}), patch('konkuk_job.publisher.time.sleep'):
            with self.assertRaises(PublishError) as caught:
                pub.publish({}, {'version': 1}, Mock())
        self.assertTrue(caught.exception.fatal)
        self.assertEqual(503, caught.exception.__cause__.code)

    def test_aws_cursor_is_board_scoped_and_conditional(self):
        table = Mock()
        store = AwsStore(table, 'bucket', Mock())
        table.get_item.return_value = {}
        self.assertEqual([], store.get_pending())
        key = table.get_item.call_args.kwargs['Key']
        store.save_pending([], ['2'])
        args = table.put_item.call_args.kwargs
        self.assertEqual(key['pk'], args['Item']['pk'])
        self.assertEqual(['2'], args['Item']['pending'])
        self.assertIn(':previous', args['ConditionExpression'])
        self.assertEqual({':previous': []}, args['ExpressionAttributeValues'])
