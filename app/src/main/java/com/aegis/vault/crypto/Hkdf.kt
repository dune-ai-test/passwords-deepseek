package com.aegis.vault.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal HKDF-SHA256 (RFC 5869) — used to split key material without ever
 * letting one key's bytes leak into another's use:
 *
 *  - cloud identity: PBKDF2 seed → HKDF → accountId / authKey
 *  - PC sharing:     ECDH secret  → HKDF → AES-256-GCM payload key
 *
 * Pure JVM, no dependencies. Verified against the RFC 5869 test vectors.
 */
object Hkdf {

    private const val HASH_LEN = 32
    private const val MAX_LENGTH = 255 * HASH_LEN

    /**
     * Extract-then-expand with HMAC-SHA256.
     *
     * @param ikm    input keying material
     * @param salt   optional salt; empty means HashLen zero bytes (RFC 5869)
     * @param info   context/application-specific info string
     * @param length desired output length in bytes (max 8160)
     */
    fun sha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..MAX_LENGTH) { "HKDF output length out of range" }

        // Extract: PRK = HMAC(salt, IKM)
        val effectiveSalt = if (salt.isEmpty()) ByteArray(HASH_LEN) else salt
        val extractMac = Mac.getInstance("HmacSHA256")
        extractMac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
        val prk = extractMac.doFinal(ikm)

        try {
            // Expand: T(n) = HMAC(PRK, T(n-1) || info || n)
            val expandMac = Mac.getInstance("HmacSHA256")
            expandMac.init(SecretKeySpec(prk, "HmacSHA256"))

            val okm = ByteArray(length)
            var previous = ByteArray(0)
            var position = 0
            var counter = 1
            while (position < length) {
                expandMac.update(previous)
                expandMac.update(info)
                expandMac.update(counter.toByte())
                previous = expandMac.doFinal()

                val take = minOf(previous.size, length - position)
                previous.copyInto(okm, position, 0, take)
                position += take
                counter++
            }
            return okm
        } finally {
            prk.fill(0)
        }
    }
}
