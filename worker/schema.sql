CREATE TABLE IF NOT EXISTS purchase_bindings (
 token_hash TEXT PRIMARY KEY,
 account_hash TEXT NOT NULL,
 product TEXT NOT NULL,
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS approved_migrations (
 token_hash TEXT PRIMARY KEY,
 account_hash TEXT NOT NULL,
 product TEXT NOT NULL,
 approved_at INTEGER NOT NULL,
 approval_note TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS review_attempts (
 ip_hash TEXT PRIMARY KEY,
 window INTEGER NOT NULL,
 attempts INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS review_attempts_window ON review_attempts(window);
