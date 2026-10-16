/**
 * /v1/* — zero-knowledge sync API.
 *
 * Routes (all JSON):
 *   POST /v1/register {accountId, authKey}         → {ok}
 *   POST /v1/login    {accountId, authKey}         → {token, expiresAt}
 *   GET  /v1/vault    (Bearer)                     → {revision, blob, updatedAt}
 *   PUT  /v1/vault    (Bearer) {blob, baseRevision}→ {revision, updatedAt} | 409
 *   POST /v1/relink   {old…, new…}                 → {ok}   (master-password change)
 *   POST /v1/delete   (Bearer)                     → {ok}
 *
 * accountId/authKey are derived on the device from the master password; the
 * server only stores HMAC digests and ciphertext. Blobs are the untouched
 * VaultEnvelope JSON — the server can never decrypt them.
 */

import {
  MAX_VAULT_BLOB,
  allowRate,
  authAccount,
  checkLoginThrottle,
  clientIpHash,
  fail,
  json,
  noteLoginFailure,
  randomToken,
  readJson,
  serverConfigured,
  sha256Hex,
  timingSafeEqual,
  validAccountId,
  validAuthKey,
  verifierFor,
} from "../_lib/core.js";

export async function onRequest(context) {
  const { request, env } = context;
  if (!serverConfigured(env)) return fail(500, "not-configured");
  const segments = Array.isArray(context.params.path) ? context.params.path : [];
  const route = segments.join("/");
  try {
    switch (route) {
      case "register":
        return request.method === "POST" ? await handleRegister(request, env) : fail(405, "method");
      case "login":
        return request.method === "POST" ? await handleLogin(request, env) : fail(405, "method");
      case "vault":
        return await handleVault(request, env);
      case "relink":
        return request.method === "POST" ? await handleRelink(request, env) : fail(405, "method");
      case "delete":
        return request.method === "POST" ? await handleDelete(request, env) : fail(405, "method");
      default:
        return fail(404, "not-found");
    }
  } catch (error) {
    return fail(500, "server-error");
  }
}

/* ── register ─────────────────────────────────────────────────────────────── */

async function handleRegister(request, env) {
  const body = await readJson(request, 4096);
  if (!body) return fail(400, "bad-request");
  const { accountId, authKey } = body;
  if (!validAccountId(accountId) || !validAuthKey(authKey)) return fail(400, "bad-request");

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:register:" + ipHash, 20, 60 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const existing = await env.DB
    .prepare("SELECT account_id FROM accounts WHERE account_id = ?")
    .bind(accountId)
    .first();
  if (existing) return fail(409, "exists");

  const verifier = await verifierFor(env, authKey);
  await env.DB
    .prepare(
      "INSERT INTO accounts (account_id, verifier, created_at, created_ip, fail_count, fail_at) " +
        "VALUES (?, ?, ?, ?, 0, 0)",
    )
    .bind(accountId, verifier, Date.now(), ipHash)
    .run();
  return json({ ok: true });
}

/* ── login ────────────────────────────────────────────────────────────────── */

async function handleLogin(request, env) {
  const body = await readJson(request, 4096);
  if (!body) return fail(400, "bad-request");
  const { accountId, authKey } = body;
  if (!validAccountId(accountId) || !validAuthKey(authKey)) return fail(400, "bad-request");

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:login:" + ipHash, 120, 60 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const nowMs = Date.now();
  const row = await env.DB
    .prepare("SELECT verifier, fail_count, fail_at FROM accounts WHERE account_id = ?")
    .bind(accountId)
    .first();
  if (!row) return fail(401, "auth");
  const throttled = await checkLoginThrottle(env, accountId, row);
  if (throttled) return throttled;

  const verifier = await verifierFor(env, authKey);
  if (!timingSafeEqual(verifier, row.verifier)) {
    await noteLoginFailure(env, accountId, row);
    return fail(401, "auth");
  }

  const token = randomToken();
  const tokenHash = await sha256Hex(token);
  const expiresAt = nowMs + 30 * 24 * 60 * 60 * 1000;
  await env.DB.batch([
    env.DB
      .prepare("INSERT INTO sessions (token_hash, account_id, expires_at, created_at) VALUES (?, ?, ?, ?)")
      .bind(tokenHash, accountId, expiresAt, nowMs),
    env.DB
      .prepare("UPDATE accounts SET fail_count = 0, fail_at = 0 WHERE account_id = ?")
      .bind(accountId),
    // Lazy pruning: expired sessions and stale rate-limit buckets.
    env.DB.prepare("DELETE FROM sessions WHERE expires_at < ?").bind(nowMs),
    env.DB.prepare("DELETE FROM rate_limits WHERE window_start < ?").bind(nowMs - 24 * 60 * 60 * 1000),
  ]);
  return json({ token, expiresAt });
}

/* ── vault ────────────────────────────────────────────────────────────────── */

async function handleVault(request, env) {
  const accountId = await authAccount(env, request);
  if (!accountId) return fail(401, "auth");

  if (request.method === "GET") {
    const row = await env.DB
      .prepare("SELECT revision, blob, updated_at FROM vaults WHERE account_id = ?")
      .bind(accountId)
      .first();
    if (!row) return fail(404, "no-vault");
    return json({ revision: row.revision, blob: row.blob, updatedAt: row.updated_at });
  }

  if (request.method === "PUT") {
    const body = await readJson(request, MAX_VAULT_BLOB + 4096);
    if (!body) return fail(400, "bad-request");
    const { blob, baseRevision } = body;
    if (typeof blob !== "string" || blob.length === 0 || blob.length > MAX_VAULT_BLOB) {
      return fail(400, "bad-request");
    }
    if (!Number.isInteger(baseRevision) || baseRevision < 0) return fail(400, "bad-request");

    const nowMs = Date.now();
    const row = await env.DB
      .prepare("SELECT revision, blob, updated_at FROM vaults WHERE account_id = ?")
      .bind(accountId)
      .first();

    if (!row) {
      if (baseRevision !== 0) return json({ error: "conflict", revision: 0 }, 409);
      await env.DB
        .prepare("INSERT INTO vaults (account_id, revision, blob, updated_at) VALUES (?, 1, ?, ?)")
        .bind(accountId, blob, nowMs)
        .run();
      return json({ revision: 1, updatedAt: nowMs });
    }

    if (row.revision !== baseRevision) {
      return json({ error: "conflict", revision: row.revision }, 409);
    }

    const results = await env.DB.batch([
      env.DB
        .prepare("INSERT INTO vault_history (account_id, revision, blob, updated_at) VALUES (?, ?, ?, ?)")
        .bind(accountId, row.revision, row.blob, row.updated_at),
      env.DB
        .prepare(
          "UPDATE vaults SET revision = ?, blob = ?, updated_at = ? WHERE account_id = ? AND revision = ?",
        )
        .bind(row.revision + 1, blob, nowMs, accountId, row.revision),
      env.DB
        .prepare(
          "DELETE FROM vault_history WHERE account_id = ? AND id NOT IN " +
            "(SELECT id FROM vault_history WHERE account_id = ? ORDER BY revision DESC, id DESC LIMIT 5)",
        )
        .bind(accountId, accountId),
    ]);
    if (!results[1] || !results[1].meta || results[1].meta.changes === 0) {
      return json({ error: "conflict", revision: row.revision }, 409);
    }
    return json({ revision: row.revision + 1, updatedAt: nowMs });
  }

  return fail(405, "method");
}

/* ── relink (master password change) ──────────────────────────────────────── */

async function handleRelink(request, env) {
  const body = await readJson(request, 8192);
  if (!body) return fail(400, "bad-request");
  const { oldAccountId, oldAuthKey, newAccountId, newAuthKey } = body;
  if (
    !validAccountId(oldAccountId) ||
    !validAuthKey(oldAuthKey) ||
    !validAccountId(newAccountId) ||
    !validAuthKey(newAuthKey)
  ) {
    return fail(400, "bad-request");
  }

  const row = await env.DB
    .prepare("SELECT verifier, fail_count, fail_at FROM accounts WHERE account_id = ?")
    .bind(oldAccountId)
    .first();
  if (!row) return fail(401, "auth");
  const throttled = await checkLoginThrottle(env, oldAccountId, row);
  if (throttled) return throttled;

  const verifier = await verifierFor(env, oldAuthKey);
  if (!timingSafeEqual(verifier, row.verifier)) {
    await noteLoginFailure(env, oldAccountId, row);
    return fail(401, "auth");
  }

  // Same credentials again — nothing to move.
  if (newAccountId === oldAccountId) return json({ ok: true });

  const target = await env.DB
    .prepare("SELECT account_id FROM accounts WHERE account_id = ?")
    .bind(newAccountId)
    .first();
  if (target) return fail(409, "target-exists");

  const ipHash = await clientIpHash(request);
  const nowMs = Date.now();
  const newVerifier = await verifierFor(env, newAuthKey);
  await env.DB.batch([
    env.DB
      .prepare(
        "INSERT INTO accounts (account_id, verifier, created_at, created_ip, fail_count, fail_at) " +
          "VALUES (?, ?, ?, ?, 0, 0)",
      )
      .bind(newAccountId, newVerifier, nowMs, ipHash),
    env.DB
      .prepare("UPDATE vaults SET account_id = ? WHERE account_id = ?")
      .bind(newAccountId, oldAccountId),
    env.DB
      .prepare("UPDATE vault_history SET account_id = ? WHERE account_id = ?")
      .bind(newAccountId, oldAccountId),
    env.DB.prepare("DELETE FROM sessions WHERE account_id = ?").bind(oldAccountId),
    env.DB.prepare("DELETE FROM accounts WHERE account_id = ?").bind(oldAccountId),
  ]);
  return json({ ok: true });
}

/* ── delete ───────────────────────────────────────────────────────────────── */

async function handleDelete(request, env) {
  const accountId = await authAccount(env, request);
  if (!accountId) return fail(401, "auth");
  await env.DB.batch([
    env.DB.prepare("DELETE FROM sessions WHERE account_id = ?").bind(accountId),
    env.DB.prepare("DELETE FROM vault_history WHERE account_id = ?").bind(accountId),
    env.DB.prepare("DELETE FROM vaults WHERE account_id = ?").bind(accountId),
    env.DB.prepare("DELETE FROM accounts WHERE account_id = ?").bind(accountId),
  ]);
  return json({ ok: true });
}
