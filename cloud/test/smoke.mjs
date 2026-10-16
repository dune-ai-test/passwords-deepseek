#!/usr/bin/env node
/**
 * Aegis cloud smoke test — run against a live deployment (or `wrangler pages
 * dev` locally). Exercises the exact derivation and crypto the app uses:
 *
 *   node test/smoke.mjs https://aegis-cloud.pages.dev
 *   node test/smoke.mjs http://127.0.0.1:8788        (wrangler pages dev)
 *
 * It registers a throwaway account derived from a fixed test password,
 * checks vault put/get/conflict, runs a full relay handshake (ECDH + HKDF +
 * AES-GCM round trip), tests relink (password change) both ways, and finally
 * deletes the account so every run starts clean.
 */

const BASE = (process.argv[2] || "http://127.0.0.1:8788").replace(/\/+$/, "");
const PASSWORD = "aegis smoke test password one";
const PASSWORD2 = "aegis smoke test password two";

let failures = 0;

function check(label, condition, extra = "") {
  if (condition) {
    console.log(`  ✓ ${label}`);
  } else {
    failures += 1;
    console.log(`  ✗ ${label}${extra ? ` — ${extra}` : ""}`);
  }
}

/* ── crypto (mirror of Android ShareCrypto.kt) ────────────────────────────── */

const enc = (text) => new TextEncoder().encode(text);

function toB64url(bytes) {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function fromB64url(text) {
  const padded = text.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (text.length % 4)) % 4);
  const bin = atob(padded);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

async function hkdf(ikm, saltText, infoText, length) {
  const key = await crypto.subtle.importKey("raw", ikm, "HKDF", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "HKDF", hash: "SHA-256", salt: enc(saltText), info: enc(infoText) },
    key,
    length * 8,
  );
  return new Uint8Array(bits);
}

/** PBKDF2(password, "aegis-cloud-v1", 600k) → {accountId, authKey} (like the app). */
async function deriveCredentials(password) {
  const passwordKey = await crypto.subtle.importKey("raw", enc(password), "PBKDF2", false, ["deriveBits"]);
  const seed = new Uint8Array(
    await crypto.subtle.deriveBits(
      { name: "PBKDF2", hash: "SHA-256", salt: enc("aegis-cloud-v1"), iterations: 600000 },
      passwordKey,
      256,
    ),
  );
  const accountId = await hkdf(seed, "aegis-cloud-v1", "account", 16);
  const authKey = await hkdf(seed, "aegis-cloud-v1", "auth", 32);
  return { accountId: toB64url(accountId), authKey: toB64url(authKey) };
}

async function ecdhKeyPair() {
  return crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveBits"]);
}

async function sharedKey(privateKey, peerPublicRaw) {
  const peer = await crypto.subtle.importKey(
    "raw",
    peerPublicRaw,
    { name: "ECDH", namedCurve: "P-256" },
    false,
    [],
  );
  const shared = new Uint8Array(
    await crypto.subtle.deriveBits({ name: "ECDH", public: peer }, privateKey, 256),
  );
  const ikm = await crypto.subtle.importKey("raw", shared, "HKDF", false, ["deriveBits"]);
  return ikm;
}

async function aesKeyFrom(ikm, usages) {
  return crypto.subtle.deriveKey(
    { name: "HKDF", hash: "SHA-256", salt: enc("aegis-share-v1"), info: enc("payload") },
    ikm,
    { name: "AES-GCM", length: 256 },
    false,
    usages,
  );
}

/* ── http ─────────────────────────────────────────────────────────────────── */

async function call(method, path, body, token) {
  const headers = {};
  if (body !== undefined) headers["content-type"] = "application/json";
  if (token) headers["authorization"] = "Bearer " + token;
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let data = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  return { status: res.status, data };
}

/* ── the run ──────────────────────────────────────────────────────────────── */

async function main() {
  console.log(`Aegis cloud smoke test against ${BASE}\n`);

  console.log("1. credential derivation (PBKDF2 600k + HKDF)");
  const creds = await deriveCredentials(PASSWORD);
  const creds2 = await deriveCredentials(PASSWORD2);
  check("accountId is 16 bytes b64url", fromB64url(creds.accountId).length === 16);
  check("authKey is 32 bytes b64url", fromB64url(creds.authKey).length === 32);
  check("different passwords → different accountIds", creds.accountId !== creds2.accountId);

  console.log("2. register + login");
  const register = await call("POST", "/v1/register", creds);
  check("register accepted (200 or already exists 409)", register.status === 200 || register.status === 409,
    `status ${register.status}`);
  const badLogin = await call("POST", "/v1/login", { accountId: creds.accountId, authKey: creds2.authKey });
  check("wrong authKey is rejected (401)", badLogin.status === 401, `status ${badLogin.status}`);
  const login = await call("POST", "/v1/login", creds);
  check("login returns a token", login.status === 200 && typeof login.data?.token === "string",
    `status ${login.status}`);
  const token = login.data?.token;

  console.log("3. vault put / get / conflict");
  const current = await call("GET", "/v1/vault", undefined, token);
  const baseRevision = current.status === 200 ? current.data.revision : 0;
  const blob = JSON.stringify({ format: "aegis-vault", version: 1, smoke: "run-" + Date.now() });
  const put = await call("PUT", "/v1/vault", { blob, baseRevision }, token);
  check("put returns the next revision", put.status === 200 && put.data?.revision === baseRevision + 1,
    `status ${put.status} revision ${put.data?.revision}`);
  const stale = await call("PUT", "/v1/vault", { blob: blob + "x", baseRevision }, token);
  check("stale baseRevision is a 409 conflict", stale.status === 409, `status ${stale.status}`);
  const fetched = await call("GET", "/v1/vault", undefined, token);
  check("stored blob round-trips", fetched.status === 200 && fetched.data?.blob === blob);

  console.log("4. relay handshake (PC ⇄ phone crypto round trip)");
  const pc = await ecdhKeyPair();
  const pcPub = toB64url(new Uint8Array(await crypto.subtle.exportKey("raw", pc.publicKey)));
  const create = await call("POST", "/api/relay/create", { pcPub });
  check("relay create returns a code", create.status === 200 && /^[2-9A-HJKMNP-Z]{6}$/.test(create.data?.code ?? ""),
    `status ${create.status}`);
  const code = create.data?.code;
  const lookup = await call("POST", "/api/relay/lookup", { code });
  check("lookup returns the PC public key", lookup.status === 200 && lookup.data?.pcPub === pcPub);
  const phone = await ecdhKeyPair();
  const phonePub = new Uint8Array(await crypto.subtle.exportKey("raw", phone.publicKey));
  const payload = JSON.stringify({
    type: "Login",
    title: "Smoke test",
    fields: [{ label: "Password", value: "s3cret-" + Date.now(), mono: true }],
    notes: "",
  });
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const phoneIkm = await sharedKey(phone.privateKey, fromB64url(pcPub));
  const phoneAes = await aesKeyFrom(phoneIkm, ["encrypt"]);
  const ciphertext = new Uint8Array(
    await crypto.subtle.encrypt({ name: "AES-GCM", iv }, phoneAes, enc(payload)),
  );
  const upload = await call("POST", "/api/relay/upload", {
    code,
    senderPub: toB64url(phonePub),
    nonce: toB64url(iv),
    ciphertext: toB64url(ciphertext),
  });
  check("upload accepted", upload.status === 200, `status ${upload.status}`);
  const fetch1 = await call("POST", "/api/relay/fetch", { code });
  check("fetch returns the payload once", fetch1.status === 200 && fetch1.data?.status === "ready");
  const pcIkm = await sharedKey(pc.privateKey, fromB64url(fetch1.data.senderPub));
  const pcAes = await aesKeyFrom(pcIkm, ["decrypt"]);
  const plain = new TextDecoder().decode(
    await crypto.subtle.decrypt({ name: "AES-GCM", iv: fromB64url(fetch1.data.nonce) }, pcAes, fromB64url(fetch1.data.ciphertext)),
  );
  check("decrypted payload matches", plain === payload);
  const fetch2 = await call("POST", "/api/relay/fetch", { code });
  check("second fetch is refused (single use)", fetch2.status === 404, `status ${fetch2.status}`);

  console.log("5. relink (master password change)");
  const relink1 = await call("POST", "/v1/relink", {
    oldAccountId: creds.accountId,
    oldAuthKey: creds.authKey,
    newAccountId: creds2.accountId,
    newAuthKey: creds2.authKey,
  });
  check("relink to the new password", relink1.status === 200, `status ${relink1.status}`);
  const login2 = await call("POST", "/v1/login", creds2);
  check("new credentials can log in", login2.status === 200);
  const vault2 = await call("GET", "/v1/vault", undefined, login2.data?.token);
  check("vault moved with the account", vault2.status === 200 && vault2.data?.blob === blob);
  const relink2 = await call("POST", "/v1/relink", {
    oldAccountId: creds2.accountId,
    oldAuthKey: creds2.authKey,
    newAccountId: creds.accountId,
    newAuthKey: creds.authKey,
  });
  check("relink back (keeps the smoke test rerunnable)", relink2.status === 200);

  console.log("6. delete account");
  const loginAgain = await call("POST", "/v1/login", creds);
  const del = await call("POST", "/v1/delete", {}, loginAgain.data?.token);
  check("account deleted", del.status === 200, `status ${del.status}`);
  const ghost = await call("POST", "/v1/login", creds);
  check("deleted account can no longer log in", ghost.status === 401);

  console.log("");
  if (failures === 0) {
    console.log("All smoke checks passed ✔");
    process.exit(0);
  } else {
    console.log(`${failures} check(s) failed ✘`);
    process.exit(1);
  }
}

main().catch((error) => {
  console.error("Smoke test crashed:", error);
  process.exit(1);
});
