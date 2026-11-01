import json
import tempfile
import unittest
from unittest.mock import Mock
from konkuk_job.parser import parse_detail
from konkuk_job.storage import LocalStore
from konkuk_job.repair import repair
from test_crawler import detail


class RepairTests(unittest.TestCase):
    def test_preview_writes_nothing_and_apply_reuses_signed_publication(self):
        with tempfile.TemporaryDirectory() as directory:
            store = LocalStore(directory)
            try:
                raw = detail('1210375', '<p>모집기간: 2026.10.19부터</p>').encode()
                record = parse_detail(raw.decode(), '1210375')
                record['applicationDates']['candidate'].update(startDate=None, endDate='2026-10-19')
                store.save(record, raw)
                publisher = Mock(url='https://api.camnect.site/api/internal/activity-imports/konkuk-job')
                publisher.publish.return_value = {'activityId': 38, 'status': 'UPDATED'}
                store.save_publication('1210375', {'activityId': 38, 'destination': publisher.url})
                before = store.get('1210375')
                preview = repair(store, None, '1210375')
                self.assertIsNone(preview['after']['candidate']['endDate'])
                self.assertEqual(before, store.get('1210375'))
                repair(store, publisher, '1210375', True)
                self.assertEqual(2, store.get('1210375')['version'])
                publisher.publish.assert_called_once()
                key = store.get('1210375')['snapshotKey']
                self.assertEqual('2026-10-19', json.loads(store.read_bytes(key))['record']['applicationDates']['candidate']['startDate'])
                store.save_publication('1210375', {'activityId': 999, 'destination': publisher.url})
                with self.assertRaisesRegex(ValueError, 'identity mismatch'):
                    repair(store, publisher, '1210375', True)
                with self.assertRaises(ValueError):
                    repair(store, publisher, '999', True)
            finally:
                store.db.close()
