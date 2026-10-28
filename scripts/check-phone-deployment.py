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
    provider = str(environment.get('PHONE_SMS_PROVIDER', 'solapi')).lower()
    if provider not in ('solapi', 'sens'):
        return ['PHONE_SMS_PROVIDER must be solapi or sens']
    mode = str(environment.get('PHONE_ROLLOUT_MODE', 'legacy')).lower()
    if mode not in ('legacy', 'verified-phone'):
        return ['PHONE_ROLLOUT_MODE must be legacy or verified-phone']
    enabled = str(environment.get('PHONE_VERIFICATION_ENABLED', 'false')).lower() == 'true'
    if mode == 'legacy' and not enabled:
        return errors
    if mode == 'verified-phone' and not enabled:
        errors.append('PHONE_VERIFICATION_ENABLED must be true: purchases require verification')
    if (mode == 'verified-phone'
            and str(environment.get('PHONE_FRONTEND_READY', '')).lower() != 'true'
            and str(environment.get('PHONE_ALLOW_PENDING_FRONTEND', '')).lower() != 'true'):
        errors.append('PHONE_FRONTEND_READY must be true, or an operator must explicitly approve PHONE_ALLOW_PENDING_FRONTEND')
    credentials = ('SOLAPI_API_KEY', 'SOLAPI_API_SECRET') if provider == 'solapi' else (
        'SENS_ACCESS_KEY', 'SENS_SECRET_KEY', 'SENS_SERVICE_ID')
    sender = str(environment.get('PHONE_SMS_SENDER_NUMBER', environment.get('SENS_SENDER_NUMBER', '')) or '')
    for key in (*credentials, 'PHONE_VERIFICATION_CODE_SECRET'):
        value = str(environment.get(key) or '')
        if not value.strip() or value.startswith('<') or value in ('CHANGE_ME', 'TODO'):
            errors.append(key + ' is missing or a placeholder')
    if not re.fullmatch(r'[0-9]{8,15}', sender):
        errors.append('PHONE_SMS_SENDER_NUMBER must contain 8-15 digits')
    if len(str(environment.get('PHONE_VERIFICATION_CODE_SECRET') or '')) < 32:
        errors.append('PHONE_VERIFICATION_CODE_SECRET must have at least 32 characters')
    for key, default in (('PHONE_SMS_DAILY_LIMIT', '20'), ('PHONE_SMS_MONTHLY_LIMIT', '50')):
        if not re.fullmatch(r'[1-9][0-9]*', str(environment.get(key, default))):
            errors.append(key + ' must be a positive integer')
        elif key == 'PHONE_SMS_MONTHLY_LIMIT' and int(environment.get(key, default)) > 50:
            errors.append('PHONE_SMS_MONTHLY_LIMIT must not exceed the approved 50 attempts')
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
        host = 'api.solapi.com' if str(environment.get('PHONE_SMS_PROVIDER', 'solapi')).lower() == 'solapi' else 'sens.apigw.ntruss.com'
        try:
            with socket.create_connection((host, 443), timeout=5) as raw:
                with ssl.create_default_context().wrap_socket(raw, server_hostname=host):
                    pass
        except OSError:
            print('ERROR: SMS provider DNS/TLS connectivity failed', file=sys.stderr)
            return 1
    if str(environment.get('PHONE_ROLLOUT_MODE', 'legacy')).lower() == 'legacy':
        print('Legacy rollout: email signup/purchases remain available; SMS enforcement is inactive.')
    else:
        print('Verified-phone configuration passed. Sender approval and live SMS delivery still require verification.')
        if str(environment.get('PHONE_FRONTEND_READY', '')).lower() != 'true':
            print('Operator-approved rollout before frontend deployment: older signup/purchase clients may receive validation errors.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
