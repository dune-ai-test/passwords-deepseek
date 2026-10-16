/**
 * Aegis share page — receive exactly one item from the phone.
 *
 * Crypto (mirrors the Android side exactly):
 *   - ephemeral ECDH P-256 keypair generated here, public key sent to /create
 *   - the phone encrypts AES-256-GCM with a key derived via
 *     HKDF-SHA256(ECDH shared secret, salt "aegis-share-v1", info "payload")
 *   - fingerprint = SHA-256(pcPub)[0..4] as "XXXX-XXXX" — shown on both
 *     screens so a MITM relay cannot swap keys unnoticed
 *   - the ciphertext is fetched once (the relay deletes the row), decrypted
 *     in-memory, and wiped from the DOM after 60 seconds
 */

"use strict";

const $ = (id) => document.getElementById(id);

let keyPair = null;
let pcPub = null;
let code = null;
let pollTimer = null;
let wipeTimer = null;
let deadline = 0;

/* ── encodings ────────────────────────────────────────────────────────────── */

function b64urlEncode(bytes) {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function b64urlDecode(text) {
  const padded = text.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - (text.length % 4)) % 4);
  const bin = atob(padded);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function toHex(bytes) {
  return Array.from(bytes).map((b) => b.toString(16).padStart(2, "0")).join("");
}

/* ── network ──────────────────────────────────────────────────────────────── */

async function post(path, body) {
  const res = await fetch(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
  let data = null;
  try {
    data = await res.json();
  } catch {
    data = null;
  }
  return { status: res.status, data };
}

/* ── ui helpers ───────────────────────────────────────────────────────────── */

const STATES = ["state-connect", "state-item", "state-cleared", "state-error"];

function setState(visibleId) {
  for (const id of STATES) $(id).hidden = id !== visibleId;
}

function showError(message, title) {
  $("error-title").textContent = title || "Something went wrong";
  $("error-text").textContent = message;
  setState("state-error");
}

function stopTimers() {
  if (pollTimer) clearInterval(pollTimer);
  if (wipeTimer) clearInterval(wipeTimer);
  pollTimer = null;
  wipeTimer = null;
}

/* ── flow ─────────────────────────────────────────────────────────────────── */

async function fingerprintOf(rawPublicKey) {
  const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", rawPublicKey));
  const hex = toHex(digest.slice(0, 4)).toUpperCase();
  return hex.slice(0, 4) + "-" + hex.slice(4);
}

async function start() {
  stopTimers();
  setState("state-connect");
  $("waiting-text").textContent = "Waiting for your phone…";

  try {
    keyPair = await crypto.subtle.generateKey(
      { name: "ECDH", namedCurve: "P-256" },
      false,
      ["deriveBits"],
    );
    pcPub = new Uint8Array(await crypto.subtle.exportKey("raw", keyPair.publicKey));
  } catch {
    showError(
      "This browser doesn't support the required crypto. Try a current Chrome, Edge, Firefox or Safari.",
      "Browser not supported",
    );
    return;
  }

  $("fingerprint").textContent = await fingerprintOf(pcPub);

  let res;
  try {
    res = await post("/api/relay/create", { pcPub: b64urlEncode(pcPub) });
  } catch {
    showError("Couldn't reach the relay. Check your connection and start again.", "Network error");
    return;
  }
  if (res.status === 429) {
    showError("Too many codes were created from this network. Wait a few minutes, then start again.", "Slow down");
    return;
  }
  if (res.status !== 200 || !res.data || typeof res.data.code !== "string") {
    showError("The relay didn't accept the request. Start again in a moment.", "Server error");
    return;
  }

  code = res.data.code;
  $("code").textContent = code.slice(0, 3) + " " + code.slice(3);
  deadline = Date.now() + 3 * 60 * 1000;
  pollTimer = setInterval(poll, 2000);
}

async function poll() {
  if (Date.now() > deadline) {
    stopTimers();
    showError("This code expired after 3 minutes. Start again for a fresh one.", "Code expired");
    return;
  }
  let res;
  try {
    res = await post("/api/relay/fetch", { code });
  } catch {
    return; // transient — the next tick retries
  }
  if (!res) return;
  if (res.status === 404) {
    stopTimers();
    showError("This code expired or was already used. Start again for a fresh one.", "Code expired");
    return;
  }
  if (res.status !== 200 || !res.data) return;
  if (res.data.status === "waiting") return;
  if (res.data.status === "ready") {
    stopTimers();
    await reveal(res.data);
  }
}

async function reveal(data) {
  let payload;
  try {
    const senderPub = await crypto.subtle.importKey(
      "raw",
      b64urlDecode(data.senderPub),
      { name: "ECDH", namedCurve: "P-256" },
      false,
      [],
    );
    const shared = new Uint8Array(
      await crypto.subtle.deriveBits({ name: "ECDH", public: senderPub }, keyPair.privateKey, 256),
    );
    const ikm = await crypto.subtle.importKey("raw", shared, "HKDF", false, ["deriveKey"]);
    const aesKey = await crypto.subtle.deriveKey(
      {
        name: "HKDF",
        hash: "SHA-256",
        salt: new TextEncoder().encode("aegis-share-v1"),
        info: new TextEncoder().encode("payload"),
      },
      ikm,
      { name: "AES-GCM", length: 256 },
      false,
      ["decrypt"],
    );
    const plain = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: b64urlDecode(data.nonce) },
      aesKey,
      b64urlDecode(data.ciphertext),
    );
    payload = JSON.parse(new TextDecoder().decode(plain));
  } catch {
    showError("The item couldn't be decrypted — it may have been corrupted in transit.", "Decryption failed");
    return;
  }
  try {
    renderPayload(payload);
  } catch {
    showError("The item was received but couldn't be displayed.", "Unexpected payload");
    return;
  }
  setState("state-item");

  let remaining = 60;
  $("countdown").textContent = remaining + "s";
  wipeTimer = setInterval(() => {
    remaining -= 1;
    if (remaining <= 0) {
      wipe();
    } else {
      $("countdown").textContent = remaining + "s";
    }
  }, 1000);
}

function renderPayload(payload) {
  $("item-type").textContent = typeof payload.type === "string" && payload.type ? payload.type : "Item";
  $("item-title").textContent = typeof payload.title === "string" && payload.title ? payload.title : "Untitled";

  const fields = $("fields");
  fields.textContent = "";
  const list = Array.isArray(payload.fields) ? payload.fields : [];
  for (const field of list) {
    if (!field || typeof field.label !== "string" || typeof field.value !== "string") continue;

    const row = document.createElement("div");
    row.className = "field";

    const col = document.createElement("div");
    col.className = "col";

    const label = document.createElement("div");
    label.className = "label";
    label.textContent = field.label;

    const value = document.createElement("div");
    value.className = field.mono ? "value mono" : "value";
    value.textContent = field.value;

    col.append(label, value);

    const button = document.createElement("button");
    button.className = "copy";
    button.textContent = "Copy";
    button.addEventListener("click", () => copyText(field.value, button));

    row.append(col, button);
    fields.append(row);
  }

  const notesWrap = $("notes-wrap");
  if (typeof payload.notes === "string" && payload.notes.trim().length > 0) {
    $("notes").textContent = payload.notes;
    notesWrap.hidden = false;
  } else {
    $("notes").textContent = "";
    notesWrap.hidden = true;
  }
}

async function copyText(text, button) {
  try {
    await navigator.clipboard.writeText(text);
    button.textContent = "Copied";
    setTimeout(() => {
      button.textContent = "Copy";
    }, 1200);
  } catch {
    button.textContent = "Copy failed";
    setTimeout(() => {
      button.textContent = "Copy";
    }, 1200);
  }
}

/** Remove every trace of the item from the page. */
function wipe() {
  stopTimers();
  $("item-title").textContent = "";
  $("item-type").textContent = "";
  $("fields").textContent = "";
  $("notes").textContent = "";
  $("notes-wrap").hidden = true;
  $("countdown").textContent = "60s";
  setState("state-cleared");
}

/* ── wiring ───────────────────────────────────────────────────────────────── */

$("clear-now").addEventListener("click", wipe);
$("restart-cleared").addEventListener("click", () => location.reload());
$("restart-error").addEventListener("click", () => location.reload());

start();
