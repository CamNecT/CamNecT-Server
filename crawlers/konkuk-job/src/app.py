import json
import os

import boto3

from konkuk_job.http import HttpClient
from konkuk_job.runner import run
from konkuk_job.storage import AwsStore
from konkuk_job.publisher import Publisher


def handler(event, context):
    # Guard manual invocations and deployments into the wrong AWS account as well.
    account = context.invoked_function_arn.split(':')[4]
    if account != os.environ['EXPECTED_ACCOUNT_ID']:
        raise RuntimeError('Unexpected AWS account')
    store = AwsStore(boto3.resource('dynamodb').Table(os.environ['STATE_TABLE']),
                     os.environ['ARCHIVE_BUCKET'], boto3.client('s3'))
    base_url = os.environ['CAMNECT_API_BASE_URL']
    secret = json.loads(boto3.client('secretsmanager').get_secret_value(
        SecretId=os.environ['CAMNECT_IMPORT_SECRET_ARN'])['SecretString'])['sharedSecret']
    publisher = Publisher(base_url, secret)
    try:
        summary = run(HttpClient(), store, max_articles=int(os.environ.get('MAX_ARTICLES', '20')),
                      seconds=min(480, context.get_remaining_time_in_millis() / 1000 - 150), publisher=publisher)
        print(json.dumps(summary))
        return summary
    except Exception as exc:
        print(json.dumps({'status': 'FAILED', 'errorType': type(exc).__name__}))
        raise
