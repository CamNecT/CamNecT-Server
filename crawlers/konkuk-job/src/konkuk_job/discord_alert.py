"""Forward the configured CloudWatch alarm from SNS to one Discord channel.

No source article, raw alarm reason, credentials, or user data is forwarded.
AWS retries failures; the notification stack retains exhausted deliveries in SQS.
"""
import json
import os
import re
from datetime import datetime
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import HTTPRedirectHandler, Request, build_opener

import boto3


class NotificationError(RuntimeError):
    pass


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise NotificationError('Discord redirect rejected')


def make_payload(event, *, topic_arn, alarm_arn):
    records = event.get('Records') if isinstance(event, dict) else None
    if not isinstance(records, list) or len(records) != 1:
        raise NotificationError('Expected one SNS notification')
    record = records[0]
    if not isinstance(record, dict) or record.get('EventSource') != 'aws:sns':
        raise NotificationError('Unexpected event source')
    sns = record.get('Sns')
    if not isinstance(sns, dict) or sns.get('TopicArn') != topic_arn:
        raise NotificationError('Unexpected SNS topic')
    try:
        alarm = json.loads(sns['Message'])
        account = alarm_arn.split(':')[4]
        region = alarm_arn.split(':')[3]
        name = alarm_arn.split(':alarm:', 1)[1]
        if (alarm['AlarmArn'] != alarm_arn or alarm['AWSAccountId'] != account
                or alarm['AlarmName'] != name):
            raise ValueError()
        state = alarm['NewStateValue']
        previous = alarm['OldStateValue']
        if previous not in ('OK', 'ALARM', 'INSUFFICIENT_DATA'):
            raise ValueError()
        changed = datetime.fromisoformat(alarm['StateChangeTime'].replace('Z', '+00:00'))
        if changed.tzinfo is None:
            raise ValueError()
        test = alarm.get('ConnectionTest') is True
    except (KeyError, TypeError, ValueError, AttributeError, IndexError):
        raise NotificationError('Invalid alarm notification') from None
    if state == 'OK' and previous != 'ALARM':
        return None  # Ignore initial setup and ordinary healthy periods.
    if state not in ('ALARM', 'OK'):
        raise NotificationError('Unsupported alarm state')
    title = '크롤러 장애 감지' if state == 'ALARM' else '크롤러 오류 경보 해제'
    detail = ('수집·자동 게시 실행에 오류가 발생했습니다. 실행 기록을 확인해 주세요.'
              if state == 'ALARM' else
              'CloudWatch 오류 지표가 정상 범위로 돌아왔습니다. 다음 수집 성공 여부는 실행 기록에서 확인해 주세요.')
    if test:
        title = '연결 테스트 / 실제 장애 아님'
        detail = '크롤러 장애 알림 경로(SNS → Lambda → Discord)를 확인했습니다.'
    url = f'https://{region}.console.aws.amazon.com/cloudwatch/home?region={region}#alarmsV2:alarm/{quote(name, safe="")}'
    return {
        'username': 'Camnect 크롤러 알림',
        'allowed_mentions': {'parse': []},
        'embeds': [{
            'title': title, 'description': detail,
            'color': 0x5865F2 if test else (0xED4245 if state == 'ALARM' else 0x57F287),
            'fields': [{'name': '대상', 'value': '건국대학교 대외활동 크롤러'},
                       {'name': '상태', 'value': f'{previous} → {state}'}],
            'timestamp': changed.isoformat(), 'url': url,
        }],
    }


def send(webhook_url, payload, *, channel_id, opener=None):
    # An exact host/path allowlist also prevents userinfo, ports, query injection,
    # redirection of the token, and accidentally using GitHub-compatible hooks.
    if not isinstance(webhook_url, str) or not re.fullmatch(
            r'https://discord\.com/api/webhooks/[0-9]{17,22}/[A-Za-z0-9_-]{40,200}', webhook_url):
        raise NotificationError('Invalid Discord webhook URL')
    opener = opener or build_opener(NoRedirect())
    request = Request(webhook_url + '?wait=true',
                      data=json.dumps(payload, ensure_ascii=False).encode('utf-8'),
                      headers={'Content-Type': 'application/json',
                               'User-Agent': 'Camnect-Crawler-Alerts/1.0'}, method='POST')
    try:
        with opener.open(request, timeout=10) as response:
            if response.status != 200:
                raise NotificationError('Unexpected Discord status')
            raw = response.read(32769)
            if len(raw) > 32768:
                raise NotificationError('Discord receipt too large')
            receipt = json.loads(raw)
            if (not isinstance(receipt, dict) or receipt.get('channel_id') != channel_id
                    or not isinstance(receipt.get('id'), str) or not receipt['id'].isdigit()):
                raise NotificationError('Discord receipt mismatch')
            return receipt['id']
    except HTTPError as exc:
        # urllib's default exception text contains the secret webhook URL.
        raise NotificationError(f'Discord HTTP {exc.code}') from None
    except NotificationError:
        raise
    except Exception:
        raise NotificationError('Discord delivery failed') from None


def handler(event, context):
    if context.invoked_function_arn.split(':')[4] != os.environ['EXPECTED_ACCOUNT_ID']:
        raise NotificationError('Unexpected AWS account')
    payload = make_payload(event, topic_arn=os.environ['ALARM_TOPIC_ARN'],
                           alarm_arn=os.environ['CRAWLER_ALARM_ARN'])
    if payload is None:
        return {'status': 'IGNORED'}
    try:
        secret = json.loads(boto3.client('secretsmanager').get_secret_value(
            SecretId=os.environ['DISCORD_WEBHOOK_SECRET_ARN'])['SecretString'])
        url = secret['webhookUrl']
    except Exception:
        raise NotificationError('Webhook secret unavailable') from None
    message_id = send(url, payload, channel_id=os.environ['DISCORD_CHANNEL_ID'])
    print(json.dumps({'status': 'DELIVERED', 'messageId': message_id}))
    return {'status': 'DELIVERED', 'messageId': message_id}
