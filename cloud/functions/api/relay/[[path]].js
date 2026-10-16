/**
 * /api/relay/* — one-time PC share relay.
 *
 * The PC (browser) generates an ephemeral P-256 keypair and asks for a code;
 * the phone looks the code up, encrypts one item to the PC's public key and
 * uploads the ciphertext; the PC fetches it exactly once. The relay only ever
 * sees public keys and ciphertext, and rows self-destruct on fetch or after
 * 3 minutes.
 *
 * Routes (all JSON, base64url values):
 *   POST create  {pcPub}                                → {code}
 *   POST lookup  {code}                                 → {pcPub}
 *   POST upload  {code, senderPub, nonce, ciphertext}   → {ok}
 *   POST fetch   {code}            → {status:"waiting"} | {status:"ready", …}
 */

import {
  RELAY_TTL_MS,
  allowRate,
  clientIpHash,
  fail,
  fromB64url,
  json,
  normalizeCode,
  randomCode,
  readJson,
  validNonce,
  validPublicKey,
} from "../../_lib/core.js";

const MAX_CIPHERTEXT_CHARS = 6000; // ≈ 4.4 KB of ciphertext

export async function onRequest(context) {
  const { request, env } = context;
  if (request.method !== "POST") return fail(405, "method");
  const segments = Array.isArray(context.params.path) ? context.params.path : [];
  const route = segments.join("/");
  try {
    switch (route) {
      case "create":
        return await handleCreate(request, env);
      case "lookup":
        return await handleLookup(request, env);
      case "upload":
        return await handleUpload(request, env);
      case "fetch":
        return await handleFetch(request, env);
      default:
        return fail(404, "not-found");
    }
  } catch (error) {
    return fail(500, "server-error");
  }
}

/* ── create (PC) ──────────────────────────────────────────────────────────── */

async function handleCreate(request, env) {
  const body = await readJson(request, 2048);
  if (!body) return fail(400, "bad-request");
  if (!validPublicKey(body.pcPub)) return fail(400, "bad-request");

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:relay-create:" + ipHash, 40, 60 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const nowMs = Date.now();
  // Lazy sweep of everything that has expired.
  await env.DB.prepare("DELETE FROM relay WHERE expires_at < ?").bind(nowMs).run();

  const expiresAt = nowMs + RELAY_TTL_MS;
  let code = null;
  for (let attempt = 0; attempt < 5 && code === null; attempt++) {
    const candidate = randomCode();
    const result = await env.DB
      .prepare("INSERT OR IGNORE INTO relay (code, pc_pub, created_at, expires_at) VALUES (?, ?, ?, ?)")
      .bind(candidate, body.pcPub, nowMs, expiresAt)
      .run();
    if (result.meta && result.meta.changes > 0) code = candidate;
  }
  if (code === null) return fail(500, "server-error");
  return json({ code, expiresAt });
}

/* ── lookup (phone) ───────────────────────────────────────────────────────── */

async function handleLookup(request, env) {
  const body = await readJson(request, 512);
  if (!body) return fail(400, "bad-request");
  const code = normalizeCode(body.code);
  if (!code) return fail(400, "bad-request");

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:relay-lookup:" + ipHash, 200, 10 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const row = await env.DB
    .prepare("SELECT pc_pub, expires_at FROM relay WHERE code = ?")
    .bind(code)
    .first();
  if (!row || row.expires_at <= Date.now()) return fail(404, "not-found");
  return json({ pcPub: row.pc_pub });
}

/* ── upload (phone) ───────────────────────────────────────────────────────── */

async function handleUpload(request, env) {
  const body = await readJson(request, MAX_CIPHERTEXT_CHARS + 2048);
  if (!body) return fail(400, "bad-request");
  const code = normalizeCode(body.code);
  if (!code) return fail(400, "bad-request");
  if (!validPublicKey(body.senderPub) || !validNonce(body.nonce)) return fail(400, "bad-request");

  const ciphertextBytes = fromB64url(body.ciphertext);
  if (
    !ciphertextBytes ||
    typeof body.ciphertext !== "string" ||
    body.ciphertext.length > MAX_CIPHERTEXT_CHARS ||
    ciphertextBytes.length > 4608
  ) {
    return fail(400, "bad-request");
  }

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:relay-upload:" + ipHash, 120, 60 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const nowMs = Date.now();
  const row = await env.DB
    .prepare("SELECT expires_at, ciphertext FROM relay WHERE code = ?")
    .bind(code)
    .first();
  if (!row || row.expires_at <= nowMs) return fail(404, "not-found");
  if (row.ciphertext) return fail(409, "already-sent");

  const result = await env.DB
    .prepare(
      "UPDATE relay SET sender_pub = ?, nonce = ?, ciphertext = ? " +
        "WHERE code = ? AND ciphertext IS NULL AND expires_at > ?",
    )
    .bind(body.senderPub, body.nonce, body.ciphertext, code, nowMs)
    .run();
  if (!result.meta || result.meta.changes === 0) return fail(409, "already-sent");
  return json({ ok: true });
}

/* ── fetch (PC, single use) ───────────────────────────────────────────────── */

async function handleFetch(request, env) {
  const body = await readJson(request, 512);
  if (!body) return fail(400, "bad-request");
  const code = normalizeCode(body.code);
  if (!code) return fail(400, "bad-request");

  const ipHash = await clientIpHash(request);
  if (!(await allowRate(env, "r:relay-fetch:" + ipHash, 3000, 10 * 60 * 1000))) {
    return fail(429, "rate-limited");
  }

  const nowMs = Date.now();
  const row = await env.DB
    .prepare("SELECT sender_pub, nonce, ciphertext, expires_at FROM relay WHERE code = ?")
    .bind(code)
    .first();
  if (!row) return fail(404, "not-found");
  if (row.expires_at <= nowMs) {
    await env.DB.prepare("DELETE FROM relay WHERE code = ?").bind(code).run();
    return fail(404, "not-found");
  }
  if (!row.ciphertext) return json({ status: "waiting" });

  await env.DB.prepare("DELETE FROM relay WHERE code = ?").bind(code).run();
  return json({
    status: "ready",
    senderPub: row.sender_pub,
    nonce: row.nonce,
    ciphertext: row.ciphertext,
  });
}
