/**
 * Aegis cloud — shared helpers for Pages Functions.
 *
 * Security notes:
 *  - The server is zero-knowledge: it only ever stores ciphertext, public
 *    identifiers and HMAC/SHA-256 digests. It can never decrypt a vault.
 *  - `verifier` = HMAC(SERVER_SECRET, "aegis-verifier:" + authKey) — a D1 dump
 *    alone cannot be used as an oracle for guessing credentials.
 *  - Sessions store SHA-256(token) only; the raw token never touches the DB.
 *  - IPs are never stored in raw form — only sha256("aegis-ip:" + ip) for
 *    rate limiting.
 */

export const SESSION_MS = 30 * 24 * 60 * 60 * 1000; // 30 days
export const RELAY_TTL_MS = 3 * 60 * 1000; // 3 minutes
export const MAX_VAULT_BLOB = 2 * 1024 * 1024; // 2 MB of envelope JSON

const B64URL_RE = /^[A-Za-z0-9_-]+$/;
const CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"; // no 0/O/1/I/L

/* ── Responses ────────────────────────────────────────────────────────────── */

export function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
    },
  });
}

export function fail(status, error) {
  return json({ error }, status);
}

/* ── Encodings ────────────────────────────────────────────────────────────── */

export function toB64url(bytes) {
  let bin = "";
  for (const byte of bytes) bin += String.fromCharCode(byte);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Strict base64url decode. Returns Uint8Array or null. */
export function fromB64url(text) {
  if (typeof text !== "string" || text.length === 0 || text.length > 8192) return null;
  if (!B64URL_RE.test(text) || text.length % 4 === 1) return null;
  const padded = text.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (text.length % 4)) % 4);
  try {
    const bin = atob(padded);
    const out = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
    return out;
  } catch {
    return null;
  }
}

export function toHex(bytes) {
  let out = "";
  for (const byte of bytes) out += byte.toString(16).padStart(2, "0");
  return out;
}

/* ── Hashing / HMAC ───────────────────────────────────────────────────────── */

export async function sha256Hex(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return toHex(new Uint8Array(digest));
}

export async function hmacHex(secret, message) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(message));
  return toHex(new Uint8Array(signature));
}

/** Length-agnostic constant-time string comparison. */
export function timingSafeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

/* ── Validation ───────────────────────────────────────────────────────────── */

/** 16-byte (128-bit) account id. */
export function validAccountId(text) {
  const bytes = fromB64url(text);
  return bytes !== null && bytes.length === 16;
}

/** 32-byte auth key. */
export function validAuthKey(text) {
  const bytes = fromB64url(text);
  return bytes !== null && bytes.length === 32;
}

/** 65-byte uncompressed P-256 point (0x04 || X || Y). */
export function validPublicKey(text) {
  const bytes = fromB64url(text);
  return bytes !== null && bytes.length === 65 && bytes[0] === 0x04;
}

/** 12-byte AES-GCM nonce. */
export function validNonce(text) {
  const bytes = fromB64url(text);
  return bytes !== null && bytes.length === 12;
}

/** Normalize a share code ("km4-z7q" → "KM4Z7Q"). Returns null when invalid. */
export function normalizeCode(raw) {
  if (typeof raw !== "string" || raw.length > 32) return null;
  const cleaned = raw.toUpperCase().replace(/[^0-9A-Z]/g, "");
  if (cleaned.length !== 6) return null;
  for (const ch of cleaned) {
    if (!CODE_ALPHABET.includes(ch)) return null;
  }
  return cleaned;
}

export function randomCode() {
  const bytes = new Uint8Array(6);
  crypto.getRandomValues(bytes);
  let code = "";
  for (const byte of bytes) code += CODE_ALPHABET[byte % CODE_ALPHABET.length];
  return code;
}

export function randomToken() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return toB64url(bytes);
}

/* ── Requests ─────────────────────────────────────────────────────────────── */

/** Read a JSON object body with a hard size cap. Returns null when invalid. */
export async function readJson(request, maxChars = 8192) {
  let text;
  try {
    text = await request.text();
  } catch {
    return null;
  }
  if (text.length === 0 || text.length > maxChars) return null;
  try {
    const body = JSON.parse(text);
    if (body === null || typeof body !== "object" || Array.isArray(body)) return null;
    return body;
  } catch {
    return null;
  }
}

export async function clientIpHash(request) {
  const ip = request.headers.get("CF-Connecting-IP") || request.headers.get("x-forwarded-for") || "local";
  return sha256Hex("aegis-ip:" + ip);
}

/* ── Rate limiting ────────────────────────────────────────────────────────── */

/** Fixed-window limiter. Returns true when the request is allowed. */
export async function allowRate(env, key, limit, windowMs) {
  const nowMs = Date.now();
  const row = await env.DB
    .prepare("SELECT window_start, count FROM rate_limits WHERE key = ?")
    .bind(key)
    .first();
  if (!row || nowMs - row.window_start >= windowMs) {
    await env.DB
      .prepare(
        "INSERT INTO rate_limits (key, window_start, count) VALUES (?, ?, 1) " +
          "ON CONFLICT(key) DO UPDATE SET window_start = excluded.window_start, count = 1",
      )
      .bind(key, nowMs)
      .run();
    return true;
  }
  if (row.count >= limit) return false;
  await env.DB.prepare("UPDATE rate_limits SET count = count + 1 WHERE key = ?").bind(key).run();
  return true;
}

/* ── Auth ─────────────────────────────────────────────────────────────────── */

export function serverConfigured(env) {
  return typeof env.SERVER_SECRET === "string" && env.SERVER_SECRET.length >= 16;
}

export async function verifierFor(env, authKey) {
  return hmacHex(env.SERVER_SECRET, "aegis-verifier:" + authKey);
}

/**
 * Resolve the bearer session to an account id. Returns the account id string,
 * or null when the token is missing / expired / unknown. Sliding renewal: a
 * session that is used gets its expiry pushed back (at most once per day).
 */
export async function authAccount(env, request) {
  const header = request.headers.get("authorization") || "";
  const match = /^Bearer ([A-Za-z0-9_-]{20,120})$/.exec(header);
  if (!match) return null;
  const tokenHash = await sha256Hex(match[1]);
  const row = await env.DB
    .prepare("SELECT account_id, expires_at FROM sessions WHERE token_hash = ?")
    .bind(tokenHash)
    .first();
  const nowMs = Date.now();
  if (!row || row.expires_at <= nowMs) return null;
  if (row.expires_at < nowMs + SESSION_MS - 24 * 60 * 60 * 1000) {
    await env.DB
      .prepare("UPDATE sessions SET expires_at = ? WHERE token_hash = ?")
      .bind(nowMs + SESSION_MS, tokenHash)
      .run();
  }
  return row.account_id;
}

/** Failed-login bookkeeping shared by login and relink. Returns a Response on rejection. */
export async function checkLoginThrottle(env, accountId, row) {
  const nowMs = Date.now();
  if (row.fail_count >= 20 && nowMs - row.fail_at < 15 * 60 * 1000) {
    return fail(429, "rate-limited");
  }
  return null;
}

export async function noteLoginFailure(env, accountId, row) {
  const nowMs = Date.now();
  const count = nowMs - row.fail_at >= 15 * 60 * 1000 ? 1 : row.fail_count + 1;
  await env.DB
    .prepare("UPDATE accounts SET fail_count = ?, fail_at = ? WHERE account_id = ?")
    .bind(count, nowMs, accountId)
    .run();
}
