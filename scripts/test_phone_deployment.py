import importlib.util
import pathlib
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('check_phone', pathlib.Path(__file__).with_name('check-phone-deployment.py'))
check_phone = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check_phone)


class DeploymentCheckTest(unittest.TestCase):
    def environment(self):
        return dict(PHONE_SMS_PROVIDER='sens', PHONE_ROLLOUT_MODE='verified-phone', PHONE_VERIFICATION_ENABLED='true', PHONE_FRONTEND_READY='true',
                    SENS_ACCESS_KEY='test-access', SENS_SECRET_KEY='sensitive-do-not-print',
                    SENS_SERVICE_ID='ncp:sms:kr:test:camnect', SENS_SENDER_NUMBER='0212345678',
                    PHONE_VERIFICATION_CODE_SECRET='x' * 32)

    def test_complete_configuration(self):
        self.assertEqual([], check_phone.validate(self.environment()))

    def test_solapi_does_not_require_sens_credentials(self):
        env = {key: value for key, value in self.environment().items() if not key.startswith('SENS_')}
        env.update(PHONE_SMS_PROVIDER='solapi', SOLAPI_API_KEY='test-key', SOLAPI_API_SECRET='test-secret',
                   PHONE_SMS_SENDER_NUMBER='01000000000', PHONE_SMS_MONTHLY_LIMIT='50')
        self.assertEqual([], check_phone.validate(env))
        env['PHONE_SMS_MONTHLY_LIMIT'] = '51'
        self.assertIn('50 attempts', str(check_phone.validate(env)))
        env['PHONE_SMS_PROVIDER'] = 'unknown'
        self.assertEqual(['PHONE_SMS_PROVIDER must be solapi or sens'], check_phone.validate(env))

    def test_default_provider_is_solapi_and_sens_keys_cannot_enable_it(self):
        env = self.environment()
        del env['PHONE_SMS_PROVIDER']
        self.assertEqual(['SOLAPI_API_KEY is missing or a placeholder',
                          'SOLAPI_API_SECRET is missing or a placeholder'], check_phone.validate(env))

    def test_legacy_rollout_requires_no_sms_credentials(self):
        self.assertEqual([], check_phone.validate({}))
        self.assertEqual([], check_phone.validate({'PHONE_ROLLOUT_MODE': 'legacy'}))
        self.assertTrue(check_phone.validate({'PHONE_ROLLOUT_MODE': 'typo'}))
        self.assertTrue(check_phone.validate({'PHONE_ROLLOUT_MODE': 'legacy', 'PHONE_VERIFICATION_ENABLED': 'true'}))

    def test_missing_and_placeholder_configuration_never_echoes_secrets(self):
        environment = self.environment()
        environment.update(PHONE_VERIFICATION_ENABLED='false', PHONE_FRONTEND_READY='false',
                           SENS_ACCESS_KEY='<access-key>', SENS_SENDER_NUMBER='02123',
                           PHONE_VERIFICATION_CODE_SECRET='short-secret', PHONE_SMS_MONTHLY_LIMIT='0')
        errors = check_phone.validate(environment)
        self.assertEqual(6, len(errors))
        self.assertNotIn('sensitive-do-not-print', str(errors))
        self.assertNotIn('short-secret', str(errors))

    def test_compose_error_does_not_leak_output(self):
        with patch('sys.argv', ['check', '--compose', 'test.yml']), \
                patch.object(check_phone.subprocess, 'run') as run, patch('sys.stderr') as stderr:
            run.return_value.returncode = 1
            run.return_value.stderr = 'secret-value'
            self.assertEqual(1, check_phone.main())
            self.assertNotIn('secret-value', str(stderr.write.call_args_list))


if __name__ == '__main__':
    unittest.main()
