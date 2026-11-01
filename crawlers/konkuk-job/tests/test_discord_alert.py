import json
import os
import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch
from urllib.error import HTTPError, URLError

from konkuk_job.discord_alert import NoRedirect, NotificationError, handler, make_payload, send


ACCOUNT = '117088935250'
TOPIC = f'arn:aws:sns:ap-northeast-2:{ACCOUNT}:crawler-alerts'
ALARM = f'arn:aws:cloudwatch:ap-northeast-2:{ACCOUNT}:alarm:crawler-errors'
CHANNEL = '1426565454343831572'
URL = 'https://discord.com/api/webhooks/123456789012345678/' + 'x' * 60


def event(state='ALARM', previous='OK', **extra):
    body = {'AlarmArn': ALARM, 'AWSAccountId': ACCOUNT, 'AlarmName': 'crawler-errors',
            'NewStateValue': state, 'OldStateValue': previous,
            'StateChangeTime': '2026-11-01T04:00:00.000+0000',
            'NewStateReason': 'DO_NOT_FORWARD secret @everyone', **extra}
    return {'Records': [{'EventSource': 'aws:sns', 'Sns': {
        'TopicArn': TOPIC, 'Message': json.dumps(body)}}]}


class DiscordAlertTests(unittest.TestCase):
    def payload(self, value):
        return make_payload(value, topic_arn=TOPIC, alarm_arn=ALARM)

    def response(self, body=None, status=200):
        response = Mock(status=status)
        response.read.return_value = (json.dumps(body if body is not None else {
            'id': '123456789012345678', 'channel_id': CHANNEL}).encode())
        response.__enter__ = Mock(return_value=response)
        response.__exit__ = Mock(return_value=False)
        opener = Mock()
        opener.open.return_value = response
        return opener

    def test_alarm_omits_raw_reason_and_disables_mentions(self):
        result = self.payload(event())
        self.assertEqual({'parse': []}, result['allowed_mentions'])
        self.assertNotIn('DO_NOT_FORWARD', json.dumps(result))
        self.assertIn('장애 감지', result['embeds'][0]['title'])

    def test_only_alarm_to_ok_is_reported_as_cleared_not_confirmed_success(self):
        result = self.payload(event('OK', 'ALARM'))
        self.assertIn('다음 수집 성공 여부', result['embeds'][0]['description'])
        self.assertIsNone(self.payload(event('OK', 'INSUFFICIENT_DATA')))
        self.assertIsNone(self.payload(event('OK', 'OK')))

    def test_connection_test_is_obvious(self):
        result = self.payload(event(ConnectionTest=True))
        self.assertIn('실제 장애 아님', result['embeds'][0]['title'])

    def test_rejects_other_topic_source_or_batched_records(self):
        variants = []
        for field, value in [('EventSource', 'other'), ('TopicArn', TOPIC + '-other')]:
            data = event()
            target = data['Records'][0] if field == 'EventSource' else data['Records'][0]['Sns']
            target[field] = value
            variants.append(data)
        data = event()
        data['Records'] *= 2
        variants.extend([data, {}, None, {'Records': [None]}])
        for data in variants:
            with self.subTest(data=data), self.assertRaises(NotificationError):
                self.payload(data)

    def test_rejects_foreign_alarm_and_malformed_alarm(self):
        variants = [event(AlarmArn=ALARM + '-other'), event(AWSAccountId='123'),
                    event(AlarmName='other'), event(StateChangeTime='2026-11-01'),
                    event('OTHER'), event(previous='@everyone')]
        for message in ('invalid', '[]', 'null', '{}'):
            data = event()
            data['Records'][0]['Sns']['Message'] = message
            variants.append(data)
        for data in variants:
            with self.subTest(data=data), self.assertRaises(NotificationError):
                self.payload(data)

    def test_valid_delivery_waits_for_receipt(self):
        opener = self.response()
        result = send(URL, self.payload(event()), channel_id=CHANNEL, opener=opener)
        self.assertEqual('123456789012345678', result)
        request = opener.open.call_args.args[0]
        self.assertEqual(URL + '?wait=true', request.full_url)
        self.assertEqual(10, opener.open.call_args.kwargs['timeout'])

    def test_rejects_unsafe_urls_before_network(self):
        for url in (URL.replace('discord.com', 'discord.com.evil.test'),
                    URL.replace('https:', 'http:'), URL + '?wait=false',
                    URL.replace('discord.com', 'user@discord.com'), URL + '/github', None):
            opener = Mock()
            with self.subTest(url=url), self.assertRaises(NotificationError):
                send(url, {}, channel_id=CHANNEL, opener=opener)
            opener.open.assert_not_called()

    def test_rejects_redirects(self):
        with self.assertRaises(NotificationError):
            NoRedirect().redirect_request(None, None, 302, '', {}, 'https://evil.test')

    def test_bad_receipts_never_mark_delivery_successful(self):
        for body in ({'id': '1', 'channel_id': 'other'}, {'id': True, 'channel_id': CHANNEL},
                     [], {}, {'id': 'not-a-snowflake', 'channel_id': CHANNEL}):
            with self.subTest(body=body), self.assertRaises(NotificationError):
                send(URL, {}, channel_id=CHANNEL, opener=self.response(body))
        for raw in (b'<html>error</html>', b'x' * 32769):
            opener = self.response()
            opener.open.return_value.read.return_value = raw
            with self.assertRaises(NotificationError):
                send(URL, {}, channel_id=CHANNEL, opener=opener)

    def test_http_errors_and_timeouts_do_not_expose_secret_url(self):
        for cause in (HTTPError(URL, 429, 'rate limited ' + URL, {}, None),
                      HTTPError(URL, 500, 'error', {}, None), URLError(URL), TimeoutError(URL)):
            opener = Mock()
            opener.open.side_effect = cause
            with self.assertRaises(NotificationError) as caught:
                send(URL, {}, channel_id=CHANNEL, opener=opener)
            self.assertNotIn(URL, str(caught.exception))
            self.assertTrue(caught.exception.__suppress_context__)

    def test_handler_checks_account_and_skips_unneeded_secret_read(self):
        env = {'EXPECTED_ACCOUNT_ID': ACCOUNT, 'ALARM_TOPIC_ARN': TOPIC, 'CRAWLER_ALARM_ARN': ALARM}
        with patch.dict(os.environ, env), patch('konkuk_job.discord_alert.boto3.client') as client:
            with self.assertRaises(NotificationError):
                handler(event(), SimpleNamespace(invoked_function_arn='arn:aws:lambda:r:wrong:f:x'))
            context = SimpleNamespace(invoked_function_arn=f'arn:aws:lambda:r:{ACCOUNT}:f:x')
            self.assertEqual({'status': 'IGNORED'}, handler(event('OK', 'OK'), context))
            client.assert_not_called()

    def test_handler_fetches_secret_and_returns_delivery_receipt(self):
        env = {'EXPECTED_ACCOUNT_ID': ACCOUNT, 'ALARM_TOPIC_ARN': TOPIC, 'CRAWLER_ALARM_ARN': ALARM,
               'DISCORD_WEBHOOK_SECRET_ARN': 'secret-arn', 'DISCORD_CHANNEL_ID': CHANNEL}
        context = SimpleNamespace(invoked_function_arn=f'arn:aws:lambda:r:{ACCOUNT}:f:x')
        with patch.dict(os.environ, env), patch('konkuk_job.discord_alert.boto3.client') as client, \
                patch('konkuk_job.discord_alert.send', return_value='123') as sender:
            client.return_value.get_secret_value.return_value = {'SecretString': json.dumps({'webhookUrl': URL})}
            self.assertEqual('DELIVERED', handler(event(), context)['status'])
            client.return_value.get_secret_value.assert_called_once_with(SecretId='secret-arn')
            self.assertEqual(URL, sender.call_args.args[0])
            self.assertEqual(CHANNEL, sender.call_args.kwargs['channel_id'])


if __name__ == '__main__':
    unittest.main()
