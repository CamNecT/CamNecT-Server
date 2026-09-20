-- 이미 삭제된 전화번호는 복구할 수 없다. 기존 회원은 NULL을 유지한다.
-- 신규 가입에는 애플리케이션에서 전화번호를 필수로 검증한다.
ALTER TABLE users ADD COLUMN phone_num VARCHAR(20) NULL;
CREATE UNIQUE INDEX uk_users_phone_num ON users (phone_num);
