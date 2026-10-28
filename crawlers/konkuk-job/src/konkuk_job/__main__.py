import argparse
import json
import sys

from .http import HttpClient
from .runner import run
from .storage import LocalStore
from .publisher import from_environment


def main():
    parser = argparse.ArgumentParser(description='Konkuk job board review queue; no service DB writes')
    parser.add_argument('--data-dir', default='data/local')
    parser.add_argument('--max-articles', type=int, default=20)
    parser.add_argument('--max-pages', type=int, default=20)
    parser.add_argument('--seconds', type=int, default=480)
    parser.add_argument('--export-only', action='store_true')
    parser.add_argument('--publish-only', action='store_true', help='Deliver saved articles without crawling again')
    parser.add_argument('--allow-local-api', action='store_true', help='Allow HTTP only for loopback integration tests')
    args = parser.parse_args()
    store = LocalStore(args.data_dir)
    try:
        publisher = from_environment(args.allow_local_api)
        if args.publish_only:
            if not publisher:
                raise ValueError('CAMNECT_API_BASE_URL and CAMNECT_IMPORT_SECRET are required')
            results = {}
            for meta in store.all_metadata()[:args.max_articles]:
                snapshot = json.loads(store.read_bytes(meta['snapshotKey']))
                receipt = publisher.publish(snapshot['record'], meta, store)
                results[receipt['status']] = results.get(receipt['status'], 0) + 1
            print(json.dumps({'publicationResults': results}))
        elif not args.export_only:
            print(json.dumps(run(HttpClient(), store, args.max_articles, args.max_pages, args.seconds, publisher),
                             ensure_ascii=False, indent=2))
        print(json.dumps({'exported': store.export(), 'file': str(store.root / 'review-queue.json')}))
    except Exception as exc:
        print(f'{type(exc).__name__}: {exc}', file=sys.stderr)
        return 1
    finally:
        store.db.close()
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
