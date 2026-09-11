ALTER TABLE user_profile
    ADD COLUMN verification_complete_notified BIT NOT NULL DEFAULT 0;

-- Before V14, completed initial setup already led to HOME. Preserve that
-- behavior for existing accounts without guessing completion from optional
-- profile fields. Pending/new accounts still receive the approval notice.
UPDATE user_profile
SET verification_complete_notified = 1
WHERE initial_setup_completed = 1
  AND user_id IN (SELECT user_id FROM users WHERE status IN ('ACTIVE', 'SUSPENDED'));
