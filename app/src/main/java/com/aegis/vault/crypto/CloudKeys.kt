package com.aegis.vault.crypto

/** PII-free cloud identity derived from the master password. */
data class CloudCredentials(val accountId: String, val authKey: String)

/**
 * Derives the cloud account identifier and auth key from the master password.
 *
 * This is the second, independent use of the master password (the first being
 * the vault KEK): a separate fixed public salt feeds PBKDF2, and the resulting
 * seed is split with HKDF so the account id and auth key never share bytes:
 *
 *   seed      = PBKDF2-HMAC-SHA256(password, "aegis-cloud-v1", 600k)
 *   accountId = base64url(HKDF(seed, "aegis-cloud-v1", "account", 16))  ← 128-bit, PII-free
 *   authKey   = base64url(HKDF(seed, "aegis-cloud-v1", "auth",    32))
 *
 * The server never sees the password — only `authKey`, and even that only as
 * HMAC(SERVER_SECRET, authKey). Two devices with the same master password
 * derive the same identity, which is what makes restore-on-a-new-phone work.
 *
 * Wipes the `password` array it is given.
 */
object CloudKeys {

    const val CLOUD_SALT = "aegis-cloud-v1"

    fun derive(password: CharArray, iterations: Int = Crypto.KDF_ITERATIONS): CloudCredentials {
        val salt = CLOUD_SALT.toByteArray(Charsets.UTF_8)
        val seed = try {
            Crypto.deriveKey(password, salt, iterations)
        } finally {
            password.fill('\u0000')
        }
        try {
            val accountId = Hkdf.sha256(seed, salt, "account".toByteArray(Charsets.UTF_8), 16)
            val authKey = Hkdf.sha256(seed, salt, "auth".toByteArray(Charsets.UTF_8), 32)
            return CloudCredentials(Crypto.b64Url(accountId), Crypto.b64Url(authKey))
        } finally {
            Crypto.wipe(seed)
        }
    }
}
