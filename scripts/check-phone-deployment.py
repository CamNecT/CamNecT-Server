#!/usr/bin/env python3
"""Validate resolved Compose settings without printing secrets or sending SMS."""
import argparse
import json
import re
import shutil
import socket
import ssl
import subprocess
import sys


def validate(environment):
    errors = []
    mode = str(environment.get('PHONE_ROLLOUT_MODE', 'legacy')).lower()
    if mode not in ('legacy', 'verified-phone'):
        return ['PHONE_ROLLOUT_MODE must be legacy or verified-phone']
    enabled = str(environment.get('PHONE_VERIFICATION_ENABLED', 'false')).lower() == 'true'
    if mode == 'legacy' and not enabled:
        return errors
    if mode == 'verified-phone' and not enabled:
        errors.append('PHONE_VERIFICATION_ENABLED must be true: purchases require verification')
    if mode == 'verified-phone' and str(environment.get('PHONE_FRONTEND_READY', '')).lower() != 'true':
        errors.append('PHONE_FRONTEND_READY must be true after signup/settings/shop integration is ready')
    for key in ('SENS_ACCESS_KEY', 'SENS_SECRET_KEY', 'SENS_SERVICE_ID', 'SENS_SENDER_NUMBER',
                'PHONE_VERIFICATION_CODE_SECRET'):
        value = str(environment.get(key) or '')
        if not value.strip() or value.startswith('<') or value in ('CHANGE_ME', 'TODO'):
            errors.append(key + ' is missing or a placeholder')
    if not re.fullmatch(r'[0-9]{8,15}', str(environment.get('SENS_SENDER_NUMBER') or '')):
        errors.append('SENS_SENDER_NUMBER must contain 8-15 digits')
    if len(str(environment.get('PHONE_VERIFICATION_CODE_SECRET') or '')) < 32:
        errors.append('PHONE_VERIFICATION_CODE_SECRET must have at least 32 characters')
    for key, default in (('PHONE_SMS_DAILY_LIMIT', '20'), ('PHONE_SMS_MONTHLY_LIMIT', '50')):
        if not re.fullmatch(r'[1-9][0-9]*', str(environment.get(key, default))):
            errors.append(key + ' must be a positive integer')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--compose', required=True)
    parser.add_argument('--check-network', action='store_true')
    args = parser.parse_args()
    # Resolve quoting/interpolation with Compose itself; never source an environment file.
    docker_compose = ['docker-compose'] if shutil.which('docker-compose') else ['docker', 'compose']
    try:
        result = subprocess.run(docker_compose + ['-f', args.compose, 'config', '--format', 'json'],
                                capture_output=True, text=True, timeout=30)
        if result.returncode:
            print('ERROR: Compose config failed; output suppressed to protect secrets', file=sys.stderr)
            return 1
        environment = json.loads(result.stdout)['services']['app'].get('environment', {})
        errors = validate(environment)
    except (OSError, subprocess.TimeoutExpired, ValueError, KeyError, TypeError):
        print('ERROR: Cannot resolve app environment with Docker Compose', file=sys.stderr)
        return 1
    for error in errors:
        print('ERROR: ' + error, file=sys.stderr)
    if errors:
        return 1
    if args.check_network and str(environment.get('PHONE_VERIFICATION_ENABLED', 'false')).lower() == 'true':
        try:
            with socket.create_connection(('sens.apigw.ntruss.com', 443), timeout=5) as raw:
                with ssl.create_default_context().wrap_socket(raw, server_hostname='sens.apigw.ntruss.com'):
                    pass
        except OSError:
            print('ERROR: SENS DNS/TLS connectivity failed', file=sys.stderr)
            return 1
    if str(environment.get('PHONE_ROLLOUT_MODE', 'legacy')).lower() == 'legacy':
        print('Legacy rollout: email signup/purchases remain available; SMS enforcement is inactive.')
    else:
        print('Verified-phone configuration passed. Sender approval and live SMS delivery still require verification.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
