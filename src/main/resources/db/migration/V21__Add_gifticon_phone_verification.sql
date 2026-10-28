-- Previous phone values were intentionally deleted by V13. Existing accounts start unverified.
ALTER TABLE users ADD COLUMN phone_num varchar(11) NULL;
ALTER TABLE users ADD COLUMN phone_verified_at datetime(6) NULL;
ALTER TABLE users ADD CONSTRAINT chk_user_verified_phone CHECK (phone_verified_at IS NULL OR phone_num IS NOT NULL);

CREATE TABLE phone_verification_challenges (
    user_id bigint NOT NULL PRIMARY KEY,
    challenge_id varchar(36) NOT NULL,
    phone_hash varchar(64) NOT NULL,
    code_hash varchar(64) NOT NULL,
    expires_at datetime(6) NOT NULL,
    attempts int NOT NULL DEFAULT 0,
    send_state varchar(16) NOT NULL,
    provider_request_id varchar(100) NULL,
    INDEX idx_phone_challenge_expiry (expires_at),
    CONSTRAINT fk_phone_challenge_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- A permanent row serializes budget reservations across replicas. Do not delete the lock row.
CREATE TABLE phone_sms_quotas (
    quota_key varchar(100) NOT NULL PRIMARY KEY,
    send_count int NOT NULL DEFAULT 0,
    last_sent_at datetime(6) NULL,
    expires_at datetime(6) NOT NULL,
    INDEX idx_phone_sms_quota_expiry (expires_at)
);
INSERT INTO phone_sms_quotas (quota_key, send_count, expires_at)
VALUES ('lock', 0, '9999-12-31 00:00:00');
