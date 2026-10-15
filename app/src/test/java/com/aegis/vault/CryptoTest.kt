package com.aegis.vault

import com.aegis.vault.crypto.Crypto
import com.aegis.vault.crypto.VaultEnvelope
import com.aegis.vault.crypto.VaultFileCodec
import com.aegis.vault.crypto.VaultMeta
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoTest {

    @Test
    fun encryptDecryptRoundTrip() {
        val key = Crypto.randomBytes(Crypto.KEY_BYTES)
        val plain = "hello aegis — correct horse battery staple".toByteArray(Charsets.UTF_8)
        val (nonce, ciphertext) = Crypto.encrypt(key, plain)
        assertEquals(Crypto.NONCE_BYTES, nonce.size)
        assertTrue(!plain.contentEquals(ciphertext))
        val decrypted = Crypto.decrypt(key, nonce, ciphertext)
        assertNotNull(decrypted)
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun decryptWithWrongKeyFails() {
        val key = Crypto.randomBytes(Crypto.KEY_BYTES)
        val other = Crypto.randomBytes(Crypto.KEY_BYTES)
        val (nonce, ciphertext) = Crypto.encrypt(key, "secret".toByteArray())
        assertNull(Crypto.decrypt(other, nonce, ciphertext))
    }

    @Test
    fun decryptionDetectsTampering() {
        val key = Crypto.randomBytes(Crypto.KEY_BYTES)
        val (nonce, ciphertext) = Crypto.encrypt(key, "secret".toByteArray())
        ciphertext[0] = (ciphertext[0] + 1).toByte()
        assertNull(Crypto.decrypt(key, nonce, ciphertext))
    }

    @Test
    fun deriveKeyIsDeterministicAndSaltDependent() {
        val salt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val first = Crypto.deriveKey("master".toCharArray(), salt, iterations = 1000)
        val second = Crypto.deriveKey("master".toCharArray(), salt, iterations = 1000)
        assertArrayEquals(first, second)
        assertEquals(Crypto.KEY_BYTES, first.size)

        val different = Crypto.deriveKey(
            "master".toCharArray(),
            Crypto.randomBytes(Crypto.SALT_BYTES),
            iterations = 1000,
        )
        assertTrue(!first.contentEquals(different))
    }

    @Test
    fun envelopeSerializationRoundTrip() {
        val envelope = VaultEnvelope(
            salt = "c2FsdA==",
            wrappedKeyNonce = "bm9uY2U=",
            wrappedKey = "d3JhcHBlZA==",
            payloadNonce = "cGF5bm9uY2U=",
            payload = "cGF5bG9hZA==",
            createdAt = 42L,
        )
        val parsed = VaultFileCodec.parse(VaultFileCodec.encode(envelope))
        assertNotNull(parsed)
        assertEquals("c2FsdA==", parsed!!.salt)
        assertEquals(42L, parsed.createdAt)
        assertEquals(Crypto.KDF_ITERATIONS, parsed.iterations)
    }

    @Test
    fun envelopeParsingRejectsForeignFormats() {
        assertNull(VaultFileCodec.parse("{\"format\":\"something-else\"}"))
        assertNull(VaultFileCodec.parse("not json at all"))
    }

    @Test
    fun metaRoundTrip() {
        val meta = VaultMeta(itemCount = 12, updatedAt = 1234L)
        val parsed = VaultFileCodec.parseMeta(VaultFileCodec.encodeMeta(meta))
        assertNotNull(parsed)
        assertEquals(12, parsed!!.itemCount)
        assertEquals(1234L, parsed.updatedAt)
    }

    @Test
    fun base64RoundTrip() {
        val bytes = Crypto.randomBytes(48)
        val decoded = Crypto.unb64(Crypto.b64(bytes))
        assertNotNull(decoded)
        assertArrayEquals(bytes, decoded)
        assertNull(Crypto.unb64("!!! not base64 !!!"))
    }
}
