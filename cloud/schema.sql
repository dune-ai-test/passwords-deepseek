-- Aegis cloud schema (Cloudflare D1 / SQLite).
--
-- Apply once per environment:
--   npx wrangler d1 execute aegis-db --remote --file=./schema.sql
-- (and --local for `wrangler pages dev`.)
--
-- Zero-knowledge: every value below is either a public identifier, a random
-- token hash, or opaque ciphertext. No plaintext vault data ever reaches D1.

-- Accounts. account_id is a 128-bit PII-free identifier derived from the
-- master password (never an email / phone / device id). verifier is
-- HMAC(SERVER_SECRET, authKey): a database dump alone cannot be used to
-- verify guessed credentials.
CREATE TABLE IF NOT EXISTS accounts (
  account_id TEXT PRIMARY KEY,
  verifier   TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  created_ip TEXT NOT NULL DEFAULT '',
  fail_count INTEGER NOT NULL DEFAULT 0,
  fail_at    INTEGER NOT NULL DEFAULT 0
);

-- Bearer sessions (tokens are stored as SHA-256 hashes only).
CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,
  account_id TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_account ON sessions (account_id);

-- The latest encrypted vault blob per account. `blob` is the full
-- VaultEnvelope JSON — ciphertext and KDF metadata only.
CREATE TABLE IF NOT EXISTS vaults (
  account_id TEXT PRIMARY KEY,
  revision   INTEGER NOT NULL DEFAULT 0,
  blob       TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);

-- Previous revisions, kept so a bad upload can be walked back (last 5 kept).
CREATE TABLE IF NOT EXISTS vault_history (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  account_id TEXT NOT NULL,
  revision   INTEGER NOT NULL,
  blob       TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_vault_history_account ON vault_history (account_id, revision);

-- One-time share relay rows (PC ⇄ phone handshake). Short-lived, deleted on
-- fetch. pc_pub / sender_pub / nonce / ciphertext are all public-key or
-- encrypted material; the relay can never read the payload.
CREATE TABLE IF NOT EXISTS relay (
  code       TEXT PRIMARY KEY,
  pc_pub     TEXT NOT NULL,
  sender_pub TEXT,
  nonce      TEXT,
  ciphertext TEXT,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_relay_expires ON relay (expires_at);

-- Simple rate limiting buckets (keyed by hashed IP; no raw IPs stored).
CREATE TABLE IF NOT EXISTS rate_limits (
  key          TEXT PRIMARY KEY,
  window_start INTEGER NOT NULL,
  count        INTEGER NOT NULL
);
