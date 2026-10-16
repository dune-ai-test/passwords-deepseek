# Aegis cloud (Cloudflare Pages + D1)

One Cloudflare Pages project hosts everything Aegis needs online:

| Path           | What it is                                                        |
| -------------- | ----------------------------------------------------------------- |
| `/`            | Static **share page** — receives one item from the phone (WebCrypto) |
| `/api/relay/*` | One-time **share relay** (Pages Function, D1-backed)              |
| `/v1/*`        | **Sync API** — encrypted vault backup/restore (Pages Function, D1) |

**Zero-knowledge:** the app encrypts everything before upload. The server only
ever stores ciphertext, public keys, and HMAC digests. It cannot read a vault,
a password, or a shared item — by construction, not by policy.

---

## 1. What you need

- A [Cloudflare account](https://dash.cloudflare.com/sign-up) (free plan is plenty).
- **Node.js 18+** on your PC → https://nodejs.org (LTS installer, defaults are fine).
  Verify with `node --version` in a terminal (PowerShell works fine).

The Pages *project name* determines the default URL. This guide uses:

- project: `aegis-cloud` → URL `https://aegis-cloud.pages.dev`
- D1 database: `aegis-db`

If `aegis-cloud` is already taken by someone else, pick any other name
(e.g. `aegis-cloud-yourname`) — everything below still applies, and the app
has a **Server** field where you type your actual URL, so nothing else changes.

## 1b. Alternative — set up entirely from the dashboards (no command line)

Everything can be done in a browser: the Cloudflare dashboard plus one small
edit on github.com. No Node.js, no wrangler. (The smoke test in step 3 is the
only CLI extra; the app itself is the real end-to-end test.)

### A. Create the database

1. Cloudflare dashboard → **Workers & Pages → D1** (newer dashboards:
   **Storage & Databases → D1 SQL database**) → **Create database** → name it
   `aegis-db` → Create.
2. Open `aegis-db` and copy its **Database ID** (a UUID).
3. Open the **Console** tab, paste the contents of `cloud/schema.sql` from the
   repo, and **Execute**. If the console refuses multiple statements, run the
   nine `CREATE TABLE` / `CREATE INDEX` statements from that file one at a
   time.
4. Check the **Tables** tab — you should see `accounts`, `sessions`, `vaults`,
   `vault_history`, `relay` and `rate_limits`.

### B. Point the project config at the database (github.com)

Open `cloud/wrangler.toml` on github.com → pencil icon → replace
`PASTE_YOUR_DATABASE_ID_HERE` with the Database ID → **Commit changes** to
`main`. (If the repo lives in an organization you can't authorize, fork it to
your own account first — then use the fork below.)

### C. Create the Pages project

Workers & Pages → **Create** → **Pages** → **Connect to Git** → authorize the
Cloudflare GitHub app → pick the repo and the `main` branch → in the setup
form:

| Field | Value |
| --- | --- |
| Project name | `aegis-cloud` — or any name if it's taken (e.g. `aegis-cloud-yourname`); the app's **Server** field takes whatever URL you get |
| Production branch | `main` |
| Framework preset | None |
| Build command | *(leave empty)* |
| Build output directory | `public` |
| Root directory (advanced) | `cloud` |

**Save and Deploy** — the first build takes a minute or two.

### D. Add the server secret

1. Project → **Settings → Variables and secrets → Add** → type **Secret**,
   name `SERVER_SECRET`, value: a long random string (40+ characters — you can
   generate one in the Aegis app itself). Save it in your password manager:
   it peppers the account verifier, and losing it means every account must
   re-enable cloud backup.
2. The change applies to *new* deployments: go to **Deployments** → latest →
   **⋯ → Retry deployment**.

### E. Verify without a command line

- `https://<project>.pages.dev` shows the dark share page **with a 6-character
  code** (proves Functions + D1 are wired — the code comes from the relay).
- `https://<project>.pages.dev/v1/vault` shows `{"error":"auth"}` (sync API
  is up).
- The real test: in the app, Settings → **Cloud backup** → master password +
  your URL → turn it on → **Back up now** → "Last backup just now". Then try
  **Send to PC**.

## 2. Set up (one time, ~5 minutes)

Open a terminal in this `cloud/` folder and run these in order.

```powershell
# 1. Log in to Cloudflare (opens a browser window).
npx wrangler@latest login

# 2. Create the D1 database. Copy the "database_id" from the output.
npx wrangler@latest d1 create aegis-db

# 3. Open wrangler.toml and replace PASTE_YOUR_DATABASE_ID_HERE with that id.

# 4. Create the tables (schema.sql) in the remote database.
npx wrangler@latest d1 execute aegis-db --remote --file=./schema.sql

# 5. Create the Pages project.
npx wrangler@latest pages project create aegis-cloud --production-branch main

# 6. Generate a server pepper secret (prints a random string — copy it).
node -e "console.log(require('crypto').randomBytes(32).toString('base64url'))"

# 7. Store it as a Pages secret (paste the string when prompted).
npx wrangler@latest pages secret put SERVER_SECRET --project-name aegis-cloud

# 8. Deploy. The output prints your URL, e.g. https://aegis-cloud.pages.dev
npx wrangler@latest pages deploy
```

> **Why the secret?** It peppers the account verifier
> (`HMAC(SERVER_SECRET, authKey)`), so a stolen D1 dump alone can't be used to
> test guessed passwords. Keep it; if you ever lose it, delete `accounts`
> rows and re-enable cloud backup in the app (the vault blob itself is not
> affected).

## 3. Verify the deployment

Run the built-in end-to-end smoke test — it performs the same cryptography
the app does (PBKDF2 + HKDF credentials, ECDH + HKDF + AES-GCM share round
trip, vault put/get/conflict, relink, delete) against your live URL:

```powershell
node test/smoke.mjs https://aegis-cloud.pages.dev
```

You should see `All smoke checks passed ✔`. Open the URL in a browser too —
you should see the dark **“Aegis — Receive from phone”** share page with a
6-character code.

> Prefer to test locally first? `npx wrangler@latest d1 execute aegis-db --local --file=./schema.sql`
> then `npx wrangler@latest pages dev` and point the smoke test at `http://127.0.0.1:8788`.

## 4. Connect the app

1. Settings → **Cloud backup** → type your master password (and your
   `https://….pages.dev` URL if it isn't the default) → *Turn on cloud backup*.
2. The first upload can take a minute on older phones — your password is
   being stretched into keys (PBKDF2, 600 000 iterations) on the device.
3. To receive something on the PC: open the share page in a browser, then on
   the phone pick any item → **Send to PC** → type the 6-character code →
   **check the fingerprint matches on both screens** → send. The phone shows
   it once, encrypted; the page wipes itself after 60 seconds.

## 5. Day-2 operations

- **Redeploy after changes:** `npx wrangler@latest pages deploy` (run from this folder).
- **Update `schema.sql` later?** Re-run step 4 — all statements are idempotent (`IF NOT EXISTS`).
- **See D1 data:** `npx wrangler@latest d1 execute aegis-db --remote --command "SELECT COUNT(*) FROM accounts"`.
- **Rename to a different domain later:** create a new project, deploy the
  same folder there, and change the Server field in the app's cloud settings.
- **Backups of D1 itself:** Cloudflare keeps 30 days of Time Travel
  (`wrangler d1 time-travel info aegis-db`) — everything stored is ciphertext
  anyway.
- **Rotating `SERVER_SECRET`:** must be done by deleting `accounts` rows and
  re-enabling cloud backup in the app (the verifier is derived from it).

## 6. What lives in D1 (data model)

| Table            | Contents                                                             |
| ---------------- | -------------------------------------------------------------------- |
| `accounts`       | 128-bit PII-free account id, HMAC verifier, timestamps, hashed IP     |
| `sessions`       | SHA-256 hashes of 256-bit bearer tokens (30-day sliding expiry)       |
| `vaults`         | latest **encrypted** vault envelope + revision counter                |
| `vault_history`  | previous 5 revisions (accident rollback)                              |
| `relay`          | one-time share rows (code, public keys, ciphertext; 3-minute TTL)     |
| `rate_limits`    | fixed-window counters keyed by hashed IP                              |

No emails, no phone numbers, no device ids, no plaintext metadata, no raw IPs.
Lazy cleanup keeps sessions/relay/rate-limit tables small — no cron needed.

## 7. Files

```
cloud/
├─ wrangler.toml            Pages + D1 bindings
├─ schema.sql               D1 tables (run once per environment)
├─ public/                  static share page  (index.html, app.js, style.css, _headers)
├─ functions/v1/[[path]].js       sync API:     register, login, vault, relink, delete
├─ functions/api/relay/[[path]].js share relay: create, lookup, upload, fetch
├─ functions/_lib/core.js   shared helpers (b64url, HMAC, rate limits, auth)
└─ test/
   ├─ core.test.js          unit tests (vitest — run in CI)
   └─ smoke.mjs             end-to-end test against a live URL
```

## 8. Troubleshooting

| Symptom | Fix |
| ------- | --- |
| `wrangler` asks to log in repeatedly | run `npx wrangler@latest login` again — the session expired |
| Dashboard path: register still 500 `not-configured` after adding `SERVER_SECRET` | retry the latest deployment (Settings apply to new deployments only) |
| Deploy succeeds but API returns 500 `not-configured` | the `SERVER_SECRET` is missing — re-run step 7 and redeploy |
| App says “Couldn't reach the server” | check the Server URL in the app (https, no trailing slash is fine either way) |
| `d1 execute` says “no migrations” | you ran it against the wrong database name; use `aegis-db` |
| Smoke test 429 | you ran it many times quickly; wait an hour or bump the limits in `functions/_lib/core.js` |
