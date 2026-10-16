package com.aegis.vault.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Pure-JVM crypto primitives used by the vault.
 *
 * - Key derivation: PBKDF2-HMAC-SHA256, 600,000 iterations (OWASP 2023+ guidance)
 * - Encryption: AES-256-GCM with a fresh 96-bit nonce per operation
 * - The GCM auth tag doubles as the master-password verifier: a wrong password
 *   or a tampered file fails to decrypt.
 */
object Crypto {
    const val KDF_ITERATIONS = 600_000
    const val SALT_BYTES = 16
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 12
    const val GCM_TAG_BITS = 128

    private val random = SecureRandom()

    fun randomBytes(count: Int): ByteArray = ByteArray(count).also { random.nextBytes(it) }

    /** Derive a 256-bit key from a password and salt. Caller should wipe the password. */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = KDF_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Encrypt with a fresh random nonce. Returns (nonce, ciphertext). */
    fun encrypt(key: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = randomBytes(NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return nonce to cipher.doFinal(plaintext)
    }

    /** Decrypt; returns null when the key is wrong or the data was tampered with. */
    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray? = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.doFinal(ciphertext)
    } catch (e: Exception) {
        null
    }

    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun unb64(text: String): ByteArray? = try {
        Base64.getDecoder().decode(text)
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Unpadded base64url — the wire format of the cloud API (URL/JSON safe). */
    fun b64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun unb64Url(text: String): ByteArray? = try {
        Base64.getUrlDecoder().decode(text)
    } catch (e: IllegalArgumentException) {
        null
    }

    fun wipe(bytes: ByteArray?) {
        bytes?.fill(0)
    }
}
