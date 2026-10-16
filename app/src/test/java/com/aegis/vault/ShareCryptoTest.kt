package com.aegis.vault

import com.aegis.vault.crypto.CloudKeys
import com.aegis.vault.crypto.Crypto
import com.aegis.vault.crypto.ShareCrypto
import java.security.interfaces.ECPublicKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-checks the "Send to PC" handshake against the browser-side rules in
 * `cloud/public/app.js`: same curve, same HKDF context, same fingerprint
 * format, same code alphabet.
 */
class ShareCryptoTest {

    @Test
    fun ecdhSharedSecretsMatch() {
        val phone = ShareCrypto.generateEphemeralKeyPair()
        val pc = ShareCrypto.generateEphemeralKeyPair()

        val phoneSecret = ShareCrypto.sharedSecret(phone.private, ShareCrypto.rawPublicKey(pc.public as ECPublicKey))
        val pcSecret = ShareCrypto.sharedSecret(pc.private, ShareCrypto.rawPublicKey(phone.public as ECPublicKey))

        assertNotNull(phoneSecret)
        assertNotNull(pcSecret)
        assertArrayEquals(phoneSecret, pcSecret)
        assertEquals(32, phoneSecret!!.size)
    }

    @Test
    fun publicKeyIsUncompressed65Bytes() {
        val raw = ShareCrypto.rawPublicKey(ShareCrypto.generateEphemeralKeyPair())
        assertEquals(65, raw.size)
        assertEquals(0x04.toByte(), raw[0])
    }

    @Test
    fun sealToPcRoundTrip() {
        val pc = ShareCrypto.generateEphemeralKeyPair()
        val payload = "{\"title\":\"Bank\",\"fields\":[]}".toByteArray(Charsets.UTF_8)

        val sealed = ShareCrypto.sealToPc(ShareCrypto.rawPublicKey(pc.public as ECPublicKey), payload)
        assertNotNull(sealed)
        assertEquals(65, sealed!!.senderPublicKey.size)
        assertEquals(Crypto.NONCE_BYTES, sealed.nonce.size)

        // PC side: ECDH with the sender's ephemeral key → same payload key.
        val shared = ShareCrypto.sharedSecret(pc.private, sealed.senderPublicKey)
        assertNotNull(shared)
        val key = ShareCrypto.payloadKey(shared!!)
        val decrypted = Crypto.decrypt(key, sealed.nonce, sealed.ciphertext)
        assertNotNull(decrypted)
        assertArrayEquals(payload, decrypted)
    }

    @Test
    fun sealToPcRejectsMalformedKey() {
        assertNull(ShareCrypto.sealToPc(ByteArray(10), "x".toByteArray()))
        assertNull(ShareCrypto.sealToPc(ByteArray(65), "x".toByteArray()))
    }

    @Test
    fun fingerprintIsDeterministicAndHumanReadable() {
        val raw = ShareCrypto.rawPublicKey(ShareCrypto.generateEphemeralKeyPair())
        val first = ShareCrypto.fingerprint(raw)
        assertEquals(first, ShareCrypto.fingerprint(raw))
        assertTrue(first.matches(Regex("^[0-9A-F]{4}-[0-9A-F]{4}$")))
    }

    @Test
    fun normalizeCodeAcceptsTypedCodes() {
        assertEquals("KM4Z7Q", ShareCrypto.normalizeCode("km4-z7q"))
        assertEquals("KM4Z7Q", ShareCrypto.normalizeCode("KM4 Z7Q"))
        assertEquals("KM4Z7Q", ShareCrypto.normalizeCode("km4z7q"))
    }

    @Test
    fun normalizeCodeRejectsInvalidCodes() {
        assertNull(ShareCrypto.normalizeCode("ABC12"))     // too short
        assertNull(ShareCrypto.normalizeCode("ABC1234"))   // too long
        assertNull(ShareCrypto.normalizeCode("ABC10O"))    // 0 and O are not in the alphabet
        assertNull(ShareCrypto.normalizeCode("ABCI1L"))    // I, 1 and L are not either
        assertNull(ShareCrypto.normalizeCode(""))
    }

    @Test
    fun cloudCredentialsAreDeterministicAndIndependent() {
        // Low iteration count: this only checks the derivation shape, not cost.
        val first = CloudKeys.derive("correct horse battery staple".toCharArray(), iterations = 1000)
        val second = CloudKeys.derive("correct horse battery staple".toCharArray(), iterations = 1000)
        assertEquals(first.accountId, second.accountId)
        assertEquals(first.authKey, second.authKey)

        val other = CloudKeys.derive("different password entirely".toCharArray(), iterations = 1000)
        assertFalse(first.accountId == other.accountId)
        assertFalse(first.authKey == other.authKey)

        // 16-byte account id in unpadded base64url = 22 chars; 32-byte auth key = 43.
        assertEquals(22, first.accountId.length)
        assertEquals(43, first.authKey.length)
        assertTrue(first.accountId.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun cloudDeriveWipesThePasswordArray() {
        val password = "wipe me".toCharArray()
        CloudKeys.derive(password, iterations = 1000)
        assertTrue(password.all { it == '\u0000' })
    }
}
