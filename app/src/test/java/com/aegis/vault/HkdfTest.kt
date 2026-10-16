package com.aegis.vault

import com.aegis.vault.crypto.Hkdf
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** RFC 5869 test vectors — proves the hand-rolled HKDF matches the spec. */
class HkdfTest {

    private fun hex(text: String): ByteArray {
        val clean = text.replace(" ", "")
        require(clean.length % 2 == 0)
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    @Test
    fun rfc5869TestCase1() {
        val ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = hex("000102030405060708090a0b0c")
        val info = hex("f0f1f2f3f4f5f6f7f8f9")
        val expected = hex(
            "3cb25f25faacd57a90434f64d0362f2a" +
                "2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
                "34007208d5b887185865"
        )
        assertArrayEquals(expected, Hkdf.sha256(ikm, salt, info, 42))
    }

    @Test
    fun rfc5869TestCase3EmptySaltAndInfo() {
        val ikm = hex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val expected = hex(
            "8da4e775a563c18f715f802a063c5a31" +
                "b8a11f5c5ee1879ec3454e5f3c738d2d" +
                "9d201395faa4b61a96c8"
        )
        assertArrayEquals(expected, Hkdf.sha256(ikm, ByteArray(0), ByteArray(0), 42))
    }
}
