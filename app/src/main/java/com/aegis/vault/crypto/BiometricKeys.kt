package com.aegis.vault.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed key used for biometric unlock.
 *
 * The Keystore key never leaves the TEE / StrongBox: it requires a strong
 * biometric authentication for every use and is invalidated when biometrics
 * are re-enrolled. The vault key is wrapped (AES-GCM) with this key and stored
 * on disk; unwrapping always triggers a BiometricPrompt via CryptoObject.
 */
object BiometricKeys {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "aegis.biometric.key"

    fun exists(): Boolean = try {
        val ks = KeyStore.getInstance(STORE)
        ks.load(null)
        ks.containsAlias(ALIAS)
    } catch (e: Exception) {
        false
    }

    fun delete() {
        try {
            val ks = KeyStore.getInstance(STORE)
            ks.load(null)
            ks.deleteEntry(ALIAS)
        } catch (_: Exception) {
        }
    }

    fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun loadKey(): SecretKey {
        val ks = KeyStore.getInstance(STORE)
        ks.load(null)
        val entry = ks.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /** Prepared encrypt cipher — pass to BiometricPrompt via CryptoObject. */
    fun cipherForEncrypt(): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, loadKey()) }

    /** Prepared decrypt cipher — pass to BiometricPrompt via CryptoObject. */
    fun cipherForDecrypt(nonce: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, loadKey(), GCMParameterSpec(128, nonce))
        }
}
