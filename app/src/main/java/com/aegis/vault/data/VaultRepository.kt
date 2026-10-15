package com.aegis.vault.data

import android.content.Context
import com.aegis.vault.crypto.Crypto
import com.aegis.vault.crypto.VaultEnvelope
import com.aegis.vault.crypto.VaultFileCodec
import com.aegis.vault.crypto.VaultMeta
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Owns the encrypted vault file and the in-memory session.
 *
 * File layout (all inside app-private storage):
 *  - vault.aegis   → VaultEnvelope JSON (encrypted payload + wrapped vault key)
 *  - vault.meta    → plaintext item count / updatedAt (for the unlock screen)
 *  - biometric.wrap→ [1-byte nonce length][nonce][AES-GCM(vault key)]
 */
class VaultRepository(private val context: Context) {

    private companion object {
        const val VAULT_FILE = "vault.aegis"
        const val META_FILE = "vault.meta"
        const val BIO_FILE = "biometric.wrap"
    }

    private val dataJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "t"
    }

    private val vaultFile = File(context.filesDir, VAULT_FILE)
    private val metaFile = File(context.filesDir, META_FILE)
    private val bioFile = File(context.filesDir, BIO_FILE)

    @Volatile private var vaultKey: ByteArray? = null
    @Volatile private var kek: ByteArray? = null

    /* ── Queries ──────────────────────────────────────────────────────────── */

    fun vaultExists(): Boolean = vaultFile.exists()

    fun biometricWrapExists(): Boolean = bioFile.exists()

    fun sessionKey(): ByteArray? = vaultKey

    fun isUnlocked(): Boolean = vaultKey != null

    fun vaultSizeBytes(): Long = vaultFile.length() + metaFile.length()

    fun itemCountHint(): Int = readMeta()?.itemCount ?: 0

    fun lastSavedHint(): Long? = readMeta()?.updatedAt

    fun exportBytes(): ByteArray? = try {
        if (vaultFile.exists()) vaultFile.readBytes() else null
    } catch (e: Exception) {
        null
    }

    fun inspectBackup(backup: ByteArray): VaultEnvelope? =
        VaultFileCodec.parse(String(backup, Charsets.UTF_8))

    /* ── Lifecycle ────────────────────────────────────────────────────────── */

    suspend fun create(password: CharArray): VaultData = withContext(Dispatchers.Default) {
        val newSalt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val newKek = Crypto.deriveKey(password, newSalt)
        password.fill('\u0000')
        val newVaultKey = Crypto.randomBytes(Crypto.KEY_BYTES)
        val (wrappedNonce, wrapped) = Crypto.encrypt(newKek, newVaultKey)
        val data = VaultData()
        val (payloadNonce, payload) = Crypto.encrypt(newVaultKey, encodeData(data))
        val envelope = VaultEnvelope(
            iterations = Crypto.KDF_ITERATIONS,
            salt = Crypto.b64(newSalt),
            wrappedKeyNonce = Crypto.b64(wrappedNonce),
            wrappedKey = Crypto.b64(wrapped),
            payloadNonce = Crypto.b64(payloadNonce),
            payload = Crypto.b64(payload),
            createdAt = System.currentTimeMillis(),
        )
        writeAtomic(vaultFile, VaultFileCodec.encode(envelope).toByteArray(Charsets.UTF_8))
        writeMeta(data)
        vaultKey = newVaultKey
        kek = newKek
        data
    }

    /** Returns null when the password is wrong or the file is damaged. */
    suspend fun unlock(password: CharArray): VaultData? = withContext(Dispatchers.Default) {
        val envelope = loadEnvelope() ?: return@withContext null
        val salt = Crypto.unb64(envelope.salt) ?: return@withContext null
        val derived = Crypto.deriveKey(password, salt, envelope.iterations)
        password.fill('\u0000')
        val wrappedNonce = Crypto.unb64(envelope.wrappedKeyNonce)
        val wrapped = Crypto.unb64(envelope.wrappedKey)
        if (wrappedNonce == null || wrapped == null) {
            Crypto.wipe(derived)
            return@withContext null
        }
        val key = Crypto.decrypt(derived, wrappedNonce, wrapped)
        if (key == null) {
            Crypto.wipe(derived)
            return@withContext null
        }
        val data = decryptPayload(envelope, key)
        if (data == null) {
            Crypto.wipe(derived)
            Crypto.wipe(key)
            return@withContext null
        }
        vaultKey = key
        kek = derived
        data
    }

    /** Unlock with a vault key unwrapped through the biometric Keystore key. */
    suspend fun unlockWithKey(key: ByteArray): VaultData? = withContext(Dispatchers.Default) {
        val envelope = loadEnvelope() ?: return@withContext null
        val data = decryptPayload(envelope, key) ?: return@withContext null
        vaultKey = key
        kek = null
        data
    }

    suspend fun persist(data: VaultData) = withContext(Dispatchers.Default) {
        val key = vaultKey ?: return@withContext
        val envelope = loadEnvelope() ?: return@withContext
        val (nonce, payload) = Crypto.encrypt(key, encodeData(data))
        val updated = envelope.copy(
            payloadNonce = Crypto.b64(nonce),
            payload = Crypto.b64(payload),
        )
        writeAtomic(vaultFile, VaultFileCodec.encode(updated).toByteArray(Charsets.UTF_8))
        writeMeta(data)
    }

    fun lock() {
        Crypto.wipe(vaultKey)
        Crypto.wipe(kek)
        vaultKey = null
        kek = null
    }

    enum class PasswordChangeResult { SUCCESS, WRONG_PASSWORD, ERROR }

    /**
     * Re-wraps the (unchanged) vault key with a key derived from the new
     * master password. The payload does not need re-encrypting.
     */
    suspend fun changeMasterPassword(
        current: CharArray,
        next: CharArray,
    ): PasswordChangeResult = withContext(Dispatchers.Default) {
        val key = vaultKey ?: return@withContext PasswordChangeResult.ERROR
        val envelope = loadEnvelope() ?: return@withContext PasswordChangeResult.ERROR
        val currentSalt = Crypto.unb64(envelope.salt) ?: return@withContext PasswordChangeResult.ERROR

        val checkKek = Crypto.deriveKey(current, currentSalt, envelope.iterations)
        current.fill('\u0000')
        val wrappedNonce = Crypto.unb64(envelope.wrappedKeyNonce)
        val wrapped = Crypto.unb64(envelope.wrappedKey)
        if (wrappedNonce == null || wrapped == null) {
            Crypto.wipe(checkKek)
            return@withContext PasswordChangeResult.ERROR
        }
        val check = Crypto.decrypt(checkKek, wrappedNonce, wrapped)
        Crypto.wipe(checkKek)
        val matches = check != null && MessageDigest.isEqual(check, key)
        Crypto.wipe(check)
        if (!matches) return@withContext PasswordChangeResult.WRONG_PASSWORD

        val newSalt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val newKek = Crypto.deriveKey(next, newSalt)
        next.fill('\u0000')
        val (newNonce, newWrapped) = Crypto.encrypt(newKek, key)
        val updated = envelope.copy(
            salt = Crypto.b64(newSalt),
            wrappedKeyNonce = Crypto.b64(newNonce),
            wrappedKey = Crypto.b64(newWrapped),
        )
        writeAtomic(vaultFile, VaultFileCodec.encode(updated).toByteArray(Charsets.UTF_8))
        Crypto.wipe(this@VaultRepository.kek)
        this@VaultRepository.kek = newKek
        PasswordChangeResult.SUCCESS
    }

    /** Replace the current vault with an exported backup. Returns null on bad password/file. */
    suspend fun importFrom(backup: ByteArray, password: CharArray): VaultData? = withContext(Dispatchers.Default) {
        val envelope = VaultFileCodec.parse(String(backup, Charsets.UTF_8)) ?: return@withContext null
        val salt = Crypto.unb64(envelope.salt) ?: return@withContext null
        val derived = Crypto.deriveKey(password, salt, envelope.iterations)
        password.fill('\u0000')
        val wrappedNonce = Crypto.unb64(envelope.wrappedKeyNonce)
        val wrapped = Crypto.unb64(envelope.wrappedKey)
        if (wrappedNonce == null || wrapped == null) {
            Crypto.wipe(derived)
            return@withContext null
        }
        val key = Crypto.decrypt(derived, wrappedNonce, wrapped)
        if (key == null) {
            Crypto.wipe(derived)
            return@withContext null
        }
        val data = decryptPayload(envelope, key)
        if (data == null) {
            Crypto.wipe(derived)
            Crypto.wipe(key)
            return@withContext null
        }
        val clean = data.copy(settings = data.settings.copy(biometricEnabled = false))
        writeAtomic(vaultFile, backup)
        writeMeta(clean)
        deleteBiometricWrap()
        vaultKey = key
        kek = derived
        clean
    }

    suspend fun wipeEverything() = withContext(Dispatchers.Default) {
        lock()
        vaultFile.delete()
        metaFile.delete()
        deleteBiometricWrap()
    }

    /* ── Biometric wrap ───────────────────────────────────────────────────── */

    fun storeBiometricWrap(nonce: ByteArray, ciphertext: ByteArray) {
        val buffer = ByteArray(1 + nonce.size + ciphertext.size)
        buffer[0] = nonce.size.toByte()
        nonce.copyInto(buffer, destinationOffset = 1)
        ciphertext.copyInto(buffer, destinationOffset = 1 + nonce.size)
        bioFile.writeBytes(buffer)
    }

    fun readBiometricWrap(): Pair<ByteArray, ByteArray>? = try {
        val bytes = bioFile.readBytes()
        if (bytes.isEmpty()) {
            null
        } else {
            val nonceLength = bytes[0].toInt()
            if (nonceLength <= 0 || bytes.size <= 1 + nonceLength) null
            else bytes.copyOfRange(1, 1 + nonceLength) to bytes.copyOfRange(1 + nonceLength, bytes.size)
        }
    } catch (e: Exception) {
        null
    }

    fun deleteBiometricWrap() {
        bioFile.delete()
    }

    /* ── Internals ────────────────────────────────────────────────────────── */

    fun encodeData(data: VaultData): ByteArray =
        dataJson.encodeToString(VaultData.serializer(), data).toByteArray(Charsets.UTF_8)

    private fun decryptPayload(envelope: VaultEnvelope, key: ByteArray): VaultData? {
        val nonce = Crypto.unb64(envelope.payloadNonce) ?: return null
        val payload = Crypto.unb64(envelope.payload) ?: return null
        val plain = Crypto.decrypt(key, nonce, payload) ?: return null
        return try {
            dataJson.decodeFromString(VaultData.serializer(), String(plain, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    private fun loadEnvelope(): VaultEnvelope? = try {
        if (vaultFile.exists()) VaultFileCodec.parse(vaultFile.readText(Charsets.UTF_8)) else null
    } catch (e: Exception) {
        null
    }

    private fun readMeta(): VaultMeta? = try {
        if (metaFile.exists()) VaultFileCodec.parseMeta(metaFile.readText(Charsets.UTF_8)) else null
    } catch (e: Exception) {
        null
    }

    private fun writeMeta(data: VaultData) {
        try {
            val meta = VaultMeta(
                itemCount = data.items.count { !it.isTrashed },
                updatedAt = System.currentTimeMillis(),
            )
            metaFile.writeBytes(VaultFileCodec.encodeMeta(meta).toByteArray(Charsets.UTF_8))
        } catch (_: Exception) {
        }
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.flush()
            try {
                out.fd.sync()
            } catch (_: Exception) {
            }
        }
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) {
                throw java.io.IOException("Could not replace ${target.name}")
            }
        }
    }
}
