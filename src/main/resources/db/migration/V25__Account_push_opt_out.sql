CREATE TABLE push_opt_outs (
    user_id BIGINT PRIMARY KEY,
    CONSTRAINT fk_push_opt_out_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);
