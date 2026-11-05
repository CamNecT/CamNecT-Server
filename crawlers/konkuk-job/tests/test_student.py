import json
import tempfile
import unittest
from datetime import datetime
from unittest.mock import Mock, patch

from konkuk_job.parser import application_dates, KST, SourceError
from konkuk_job.publisher import make_payload
from konkuk_job.runner import run
from konkuk_job.storage import LocalStore
from konkuk_job.student import student_listing
from test_crawler import detail


def feed(rows):
    items = ''.join(f'<item><title>{title}</title><link>{url}</link><pubDate>{date}</pubDate>'
                    '<topEndDt>2099-10-01</topEndDt></item>' for title, url, date in rows)
    return ('<rss><channel>' + items + '</channel></rss>').encode()


PATH = '/bbs/konkuk/238/1211392/artclView.do?layout=unknown'
TODAY = datetime(2026, 11, 5, tzinfo=KST)


class StudentTests(unittest.TestCase):
    def test_feed_window_and_revisions(self):
        with tempfile.TemporaryDirectory() as directory:
            store = LocalStore(directory)
            client = Mock()
            row = ('참가자 모집', PATH, '2026-11-04 11:11:00.0')
            admin = ('근로장학생 모집', PATH.replace('1211392', '1234'), row[2])
            client.fetch.return_value = (feed([row, row, admin]), None)
            body = detail('1211392', '<p>지원 마감: 2026. 11. 16.( 월 ) 09:00</p><img src="/poster.jpg">')
            client.html.return_value = (body, body.encode())
            try:
                with patch('konkuk_job.student.datetime', wraps=datetime) as dt:
                    dt.now.return_value = TODAY
                    first = run(client, store, student=True)
                    self.assertEqual((first['created'], first['discovered']), (1, 1))
                    client.fetch.return_value = (feed([admin]), None)
                    self.assertEqual(run(client, store, student=True)['unchanged'], 1)
                    changed = body.replace('16.( 월 )','17.( 화 )')
                    client.html.return_value = (changed, changed.encode())
                    self.assertEqual(run(client, store, student=True)['updated'], 1)
                meta = store.get('1211392')
                record = json.loads(store.read_bytes(meta['snapshotKey']))['record']
                payload = make_payload(record, 2, store)
                self.assertEqual(record['images'], ['https://www.konkuk.ac.kr/poster.jpg'])
                self.assertEqual(payload['applyEndAt'], '2026-11-17T09:00:00+09:00')
                self.assertNotIn('지원 마감:', payload['content'])
                self.assertIsNone(payload['thumbnail'])
                self.assertIn('/bbs/konkuk/238/1211392/artclView.do', payload['content'])
            finally:
                store.db.close()

    def test_untrusted_or_unsupported_feed_never_becomes_empty_success(self):
        client = Mock()
        for raw in (b'<rss/>', b'<html>denied</html>', b'<rss>', b'<!DOCTYPE rss><rss/>',
                    feed([('모집', 'https://other.invalid' + PATH, '2026-11-05')]),
                    feed([('모집', PATH, '')])):
            with self.subTest(raw=raw):
                client.fetch.return_value = (raw, None)
                with self.assertRaises(SourceError):
                    student_listing(client, TODAY)

    def test_dates_from_school_and_ambiguous_lookalikes(self):
        def parse(text):
            return application_dates(text, abbreviated=True)[0]['candidate']
        self.assertEqual(parse('접수 기간 및 제출 서류\n기간: 2026. 10. 20.(화) ~ 11. 10.(화) 17:00까지')['endAt'],
                         '2026-11-10T17:00:00+09:00')
        self.assertIsNone(parse('신청 기간: ~ 2026. 11. 8.(일)')['endAt'])
        for text in ('교육 일시: 2026.11.26 13:00 / 선착순 20명', '지원 마감: 추후 공지\n면접 일자: 2026.11.30',
                     '접수 마감: 미정 / 교육일: 2026.11.30', '접수 기한: 2026.10.20(화) ~ 11.10(수)',
                     '접수 기간: 2026.12.20 ~ 1.10(일)', '지원 마감: 2026.11.09\n신청 종료: 2026.11.16',
                     '공모전(~11/10 화)'):
            with self.subTest(text=text):
                self.assertIsNone(parse(text))
