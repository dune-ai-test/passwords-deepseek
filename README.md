# Aegis — Android password manager

Local-only, iOS-styled password manager for Android.

> Codebase generated with **OpenCode CLI** using the **DeepSeek V4.1 Flash** model.

Aegis keeps your logins, cards, notes and identities encrypted on your device —
**no account, no cloud, no servers**. The app declares **zero Android permissions**
(not even `INTERNET`), so the vault physically cannot leave the phone except
through an encrypted backup file you export yourself.

## Features

- 🔐 **Master password vault** — AES-256-GCM encrypted file, PBKDF2-HMAC-SHA256 (600,000 iterations)
- 🫆 **Biometric unlock** — fingerprint / face via Android Keystore (key invalidates if biometrics change)
- 🗂️ **Logins, Cards, Notes, Identities** — tags, search, recently used, password history
- 🎲 **Password generator** — random (length, character classes, avoid look-alikes) and memorable (EFF wordlist passphrase)
- 🛡️ **Security audit** — weak, reused and old passwords scored locally
- ⏱️ **Auto-lock**, clipboard auto-clear (60 s), screenshot protection (`FLAG_SECURE`)
- 💾 **Encrypted export / restore** — `.aegis` backup files via the system file picker
- 🎨 **Fully custom UI** — Jetpack Compose with **no Material components**, matching the original iOS-style design 1:1

## Download & install

The debug APK is built automatically by GitHub Actions and published to the
[**Releases page as a pre-release**](../../releases):

1. Open the **Releases** page of this repository
2. Download `app-debug.apk` from the latest *debug* pre-release
3. Sideload it on your device (allow install from unknown sources for your browser/file manager)

The APK is refreshed on **every push to `main`** (rolling tag `debug-latest`),
and can also be built on demand via **Actions → Build debug APK → Run workflow**.

## Building

Everything is built in CI — no local toolchain is required:

| Tool | Version |
|---|---|
| JDK | 17 |
| Gradle | 8.10.2 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.0 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0+) |

```bash
# what CI runs:
gradle testDebugUnitTest
gradle assembleDebug
```

## Architecture

```
app/src/main/java/com/aegis/vault/
├── crypto/   → PBKDF2 KDF, AES-256-GCM, vault file format, Keystore wrapping
├── data/     → models, encrypted vault repository, audit, generator, search
├── ui/design → custom design system (palette, type, components — no Material)
├── ui/screens→ all app screens
└── MainActivity.kt
```

- The vault is a single JSON document encrypted with AES-256-GCM. A random
  256-bit vault key is generated at creation; it is wrapped (encrypted) once
  with a key derived from your master password, and optionally once more with
  a hardware-backed Android Keystore key that requires biometric auth.
- Changing the master password rotates the vault key and re-wraps everything.
- Writes are atomic (temp file + rename), so a crash can't corrupt the vault.

## Design

The UI recreates the original design mockups (14 screens) exactly: Inter
typeface, iOS palette, floating tab dock with a right-end **+** button, custom
switches, segmented controls, sliders and sheets — all drawn from scratch on
Compose foundation.

## Licenses

- App code: MIT (see `LICENSE`)
- Inter font: SIL Open Font License 1.1 (see `THIRD_PARTY_NOTICES.md`)
- Lucide icons: ISC License
- EFF wordlist: CC0
