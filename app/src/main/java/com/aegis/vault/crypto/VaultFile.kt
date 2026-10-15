package com.aegis.vault.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The on-disk vault format:
 *
 * ```json
 * {
 *   "format": "aegis-vault",
 *   "kdf": "PBKDF2-HMAC-SHA256", "iterations": 600000, "salt": "base64",
 *   "wrappedKeyNonce": "base64", "wrappedKey": "base64",   ← vault key encrypted with the password-derived key
 *   "payloadNonce": "base64",    "payload": "base64",      ← VaultData JSON encrypted with the vault key
 *   ...
 * }
 * ```
 *
 * Everything sensitive lives inside `payload`; only the format metadata is cleartext.
 */
@Serializable
data class VaultEnvelope(
    val format: String = "aegis-vault",
    val version: Int = 1,
    val kdf: String = "PBKDF2-HMAC-SHA256",
    val iterations: Int = Crypto.KDF_ITERATIONS,
    val salt: String = "",
    val wrappedKeyNonce: String = "",
    val wrappedKey: String = "",
    val payloadNonce: String = "",
    val payload: String = "",
    val createdAt: Long = 0L,
)

/** Plaintext sidecar with just enough info for the unlock screen. */
@Serializable
data class VaultMeta(
    val itemCount: Int = 0,
    val updatedAt: Long = 0L,
)

object VaultFileCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(envelope: VaultEnvelope): String =
        json.encodeToString(VaultEnvelope.serializer(), envelope)

    fun parse(text: String): VaultEnvelope? = try {
        json.decodeFromString(VaultEnvelope.serializer(), text).takeIf { it.format == "aegis-vault" }
    } catch (e: Exception) {
        null
    }

    fun encodeMeta(meta: VaultMeta): String = json.encodeToString(VaultMeta.serializer(), meta)

    fun parseMeta(text: String): VaultMeta? = try {
        json.decodeFromString(VaultMeta.serializer(), text)
    } catch (e: Exception) {
        null
    }
}
