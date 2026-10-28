import importlib.util
import pathlib
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('check_phone', pathlib.Path(__file__).with_name('check-phone-deployment.py'))
check_phone = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check_phone)


class DeploymentCheckTest(unittest.TestCase):
    def environment(self):
        return dict(PHONE_VERIFICATION_ENABLED='true', PHONE_FRONTEND_READY='true',
                    SENS_ACCESS_KEY='test-access', SENS_SECRET_KEY='sensitive-do-not-print',
                    SENS_SERVICE_ID='ncp:sms:kr:test:camnect', SENS_SENDER_NUMBER='0212345678',
                    PHONE_VERIFICATION_CODE_SECRET='x' * 32)

    def test_complete_configuration(self):
        self.assertEqual([], check_phone.validate(self.environment()))

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
