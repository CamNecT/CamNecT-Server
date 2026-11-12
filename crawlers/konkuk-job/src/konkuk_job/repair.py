"""Guarded, replayable repair of the two verified 2026-11-01 date incidents."""
import argparse
import copy
import json

from .parser import parse_detail, content_hash
from .publisher import Publisher
from .storage import AwsStore

TARGETS = {'1210375': 38, '1210911': 23}


def repair(store, publisher, article_id, apply=False):
    if article_id not in TARGETS:
        raise ValueError('Not an approved incident target')
    meta, receipt = store.get(article_id), store.get_publication(article_id)
    if not meta or not receipt or receipt['activityId'] != TARGETS[article_id]:
        raise ValueError('Source/activity identity mismatch')
    snapshot = json.loads(store.read_bytes(meta['snapshotKey']))
    original = snapshot['record']
    if content_hash(original) != meta['contentHash']:
        raise ValueError('Archive content hash mismatch')
    raw = store.read_bytes(snapshot['rawHtmlKey'])
    parsed = parse_detail(raw.decode('utf-8-sig'), article_id)
    record = copy.deepcopy(original)
    record['applicationDates'] = parsed['applicationDates']
    # Retain media inspection evidence but replace the old date-parser decisions.
    record['reviewReasons'] = sorted(set(parsed['reviewReasons'] + [
        reason for reason in original['reviewReasons'] if reason.startswith(('MEDIA_', 'UNSUPPORTED_', 'IMAGE_'))]))
    dates = record['applicationDates']['candidate'] or {}
    if article_id == '1210375' and (dates.get('startDate') != '2026-10-19' or dates.get('endDate') is not None):
        raise ValueError('Unexpected seminar dates; inspect the source again')
    if article_id == '1210911' and dates.get('endAt') != '2026-10-30T18:00:00+09:00':
        raise ValueError('Unexpected competition deadline; inspect the source again')
    result = {'articleId': article_id, 'activityId': receipt['activityId'],
              'before': original['applicationDates'], 'after': record['applicationDates'], 'applied': apply}
    if apply:
        if not publisher or publisher.shadow is True or receipt.get('destination') not in (
                publisher.url, publisher.url.removesuffix('/tagged')):
            raise ValueError('Publication destination mismatch')
        if store.get(article_id) != meta:
            raise RuntimeError('Source changed during repair; retry after inspection')
        # Existing versioned archive + signed API preserve manual edits and deletion tombstones.
        # If delivery fails, the new source version remains pending for retry.
        store.save(record, raw, expected_meta=meta)
        acknowledgement = publisher.publish(record, store.get(article_id), store)
        result['status'] = acknowledgement['status']
        if acknowledgement['activityId'] != TARGETS[article_id] or acknowledgement['status'] not in ('UPDATED', 'UNCHANGED'):
            raise RuntimeError('Repair was not applied to the expected managed activity')
    return result


def main():
    import boto3
    args = argparse.ArgumentParser(description=__doc__)
    args.add_argument('--function-name', required=True)
    args.add_argument('--expected-code-sha256', required=True)
    args.add_argument('--apply', action='store_true', help='Default is read-only preview')
    options = args.parse_args()
    session = boto3.Session(region_name='ap-northeast-2')
    if session.client('sts').get_caller_identity()['Account'] != '117088935250':
        raise RuntimeError('Unexpected AWS account')
    config = session.client('lambda').get_function_configuration(FunctionName=options.function_name)
    if config['CodeSha256'] != options.expected_code_sha256 or config['LastUpdateStatus'] != 'Successful':
        raise RuntimeError('Deploy and verify the fixed crawler before repairing records')
    env = config['Environment']['Variables']
    if env['CAMNECT_API_BASE_URL'].rstrip('/') != 'https://api.camnect.site':
        raise RuntimeError('Unexpected publication destination')
    store = AwsStore(session.resource('dynamodb').Table(env['STATE_TABLE']), env['ARCHIVE_BUCKET'], session.client('s3'))
    publisher = None
    if options.apply:
        secret = json.loads(session.client('secretsmanager').get_secret_value(
            SecretId=env['CAMNECT_IMPORT_SECRET_ARN'])['SecretString'])['sharedSecret']
        if env.get('JOB_TAG_PUBLICATION_MODE', 'shadow') != 'tagged':
            raise ValueError('Repair publication requires tagged publication mode')
        publisher = Publisher(env['CAMNECT_API_BASE_URL'], secret, job_mode='tagged')
    for article_id in TARGETS:
        print(json.dumps(repair(store, publisher, article_id, options.apply), ensure_ascii=False))


if __name__ == '__main__':
    main()
