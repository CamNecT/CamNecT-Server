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
    base_url = os.environ['CAMNECT_API_BASE_URL']
    secret = None
    job_mode = os.environ.get('JOB_TAG_PUBLICATION_MODE', 'shadow')
    table = boto3.resource('dynamodb').Table(os.environ['STATE_TABLE'])
    bucket, s3 = os.environ['ARCHIVE_BUCKET'], boto3.client('s3')
    mode = event.get('board', 'both')
    if mode not in ('student', 'job', 'both'):
        raise ValueError('Unknown board')
    reports, failures = {}, []
    for board in (('student', 'job') if mode == 'both' else (mode,)):
        student = board == 'student'
        try:
            if (student or job_mode == 'tagged') and secret is None:
                secret = json.loads(boto3.client('secretsmanager').get_secret_value(
                    SecretId=os.environ['CAMNECT_IMPORT_SECRET_ARN'])['SecretString'])['sharedSecret']
            store = AwsStore(table, bucket, s3, 'KONKUK_STUDENT_238' if student else 'KONKUK_JOB_4084')
            budget = min(120 if student else 480, context.get_remaining_time_in_millis() / 1000 - 45)
            reports[board] = run(HttpClient(host='www.konkuk.ac.kr' if student else 'job.konkuk.ac.kr'),
                                 store, max_articles=8 if student else int(os.environ.get('MAX_ARTICLES', '20')),
                                 seconds=budget, publisher=Publisher(base_url, secret, student=student, job_mode=job_mode), student=student)
            print(json.dumps({'board': board, **reports[board]}))
        except Exception as exc:
            failures.append(f'{board}: {type(exc).__name__}: {str(exc)[:180]}')
    if failures:
        raise RuntimeError('; '.join(failures))
    return {**reports.get('job', reports.get('student', {})), 'boards': reports}
