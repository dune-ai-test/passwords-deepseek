package com.aegis.vault.crypto

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.KeyAgreement

/**
 * End-to-end encryption for the "Send to PC" handoff.
 *
 * The PC (browser, WebCrypto) generates an ephemeral P-256 keypair and shows a
 * one-time code. The phone fetches the PC's public key, derives a shared ECDH
 * secret, extracts an AES-256-GCM key with HKDF and uploads only ciphertext.
 * A short fingerprint of the PC's public key is shown on both screens so a
 * swapped key (MITM relay) is caught by the human eye.
 *
 * Mirrored exactly by cloud/public/app.js — same curve, same HKDF
 * salt/info (`"aegis-share-v1"` / `"payload"`), same fingerprint format.
 */
object ShareCrypto {

    private const val CURVE = "secp256r1"
    private const val SHARE_SALT = "aegis-share-v1"
    private const val SHARE_INFO = "payload"

    /** Share-code alphabet — no 0/O, 1/I/L, so codes survive being read aloud. */
    const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    const val CODE_LENGTH = 6

    private val p256: ECParameterSpec by lazy {
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec(CURVE))
        params.getParameterSpec(ECParameterSpec::class.java)
    }

    fun generateEphemeralKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(CURVE)) }.generateKeyPair()

    /** 65-byte uncompressed point: 0x04 || X(32) || Y(32). */
    fun rawPublicKey(key: ECPublicKey): ByteArray {
        val out = ByteArray(65)
        out[0] = 0x04
        writeFixed(key.w.affineX, out, 1)
        writeFixed(key.w.affineY, out, 33)
        return out
    }

    fun rawPublicKey(pair: KeyPair): ByteArray = rawPublicKey(pair.public as ECPublicKey)

    private fun writeFixed(value: BigInteger, target: ByteArray, offset: Int) {
        val bytes = value.toByteArray() // may carry a leading 0x00 sign byte
        val src = if (bytes.size > 32) bytes.copyOfRange(bytes.size - 32, bytes.size) else bytes
        java.util.Arrays.fill(target, offset, offset + 32, 0.toByte())
        src.copyInto(target, offset + 32 - src.size)
    }

    /** ECDH shared secret (the 32-byte X coordinate). Returns null on a bad peer point. */
    fun sharedSecret(privateKey: PrivateKey, peerRaw: ByteArray): ByteArray? {
        if (peerRaw.size != 65 || peerRaw[0] != 0x04.toByte()) return null
        return try {
            val x = BigInteger(1, peerRaw.copyOfRange(1, 33))
            val y = BigInteger(1, peerRaw.copyOfRange(33, 65))
            val peer = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), p256))
            val agreement = KeyAgreement.getInstance("ECDH")
            agreement.init(privateKey)
            agreement.doPhase(peer, true)
            agreement.generateSecret()
        } catch (e: Exception) {
            null
        }
    }

    /** AES-256-GCM key for the share payload (HKDF over the ECDH secret). */
    fun payloadKey(sharedSecret: ByteArray): ByteArray = Hkdf.sha256(
        ikm = sharedSecret,
        salt = SHARE_SALT.toByteArray(Charsets.UTF_8),
        info = SHARE_INFO.toByteArray(Charsets.UTF_8),
        length = 32,
    )

    data class SealedPayload(
        val senderPublicKey: ByteArray,
        val nonce: ByteArray,
        val ciphertext: ByteArray,
    )

    /** Encrypt `payload` to the PC's public key. Returns null when the key is malformed. */
    fun sealToPc(pcRawPublicKey: ByteArray, payload: ByteArray): SealedPayload? {
        val pair = generateEphemeralKeyPair()
        val shared = sharedSecret(pair.private, pcRawPublicKey) ?: return null
        return try {
            val key = payloadKey(shared)
            val (nonce, ciphertext) = Crypto.encrypt(key, payload)
            Crypto.wipe(key)
            SealedPayload(rawPublicKey(pair), nonce, ciphertext)
        } finally {
            Crypto.wipe(shared)
        }
    }

    /**
     * Short human-comparable fingerprint: "XXXX-XXXX" from the first 4 bytes
     * of SHA-256(rawPublicKey). Shown on the phone and the PC; messages must
     * differ on either side to break the handshake.
     */
    fun fingerprint(rawPublicKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawPublicKey)
        val hex = digest.copyOfRange(0, 4).joinToString("") { "%02X".format(it) }
        return hex.substring(0, 4) + "-" + hex.substring(4)
    }

    /** "km4-z7q" → "KM4Z7Q". Null when not a valid share code. */
    fun normalizeCode(raw: String): String? {
        val cleaned = raw.uppercase().filter { it.isLetterOrDigit() }
        if (cleaned.length != CODE_LENGTH) return null
        if (cleaned.any { it !in CODE_ALPHABET }) return null
        return cleaned
    }
}
