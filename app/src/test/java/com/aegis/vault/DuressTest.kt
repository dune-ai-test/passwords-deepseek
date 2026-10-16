package com.aegis.vault

import com.aegis.vault.data.Duress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuressTest {

    private val salt = ByteArray(16) { (it + 1).toByte() }

    @Test
    fun verifierMatchesTheRegisteredCodeOnly() {
        val code = "harbor-crocodile-17".toCharArray()
        val verifier = Duress.deriveVerifier(code, salt)

        assertTrue(Duress.verifierMatches(code, salt, verifier))
        assertFalse(Duress.verifierMatches("harbor-crocodile-18".toCharArray(), salt, verifier))
        assertFalse(Duress.verifierMatches("Harbor-Crocodile-17".toCharArray(), salt, verifier))
    }

    @Test
    fun verifierIsBoundToItsSalt() {
        val code = "quiet-lantern".toCharArray()
        val otherSalt = ByteArray(16) { (it + 40).toByte() }
        val verifier = Duress.deriveVerifier(code, salt)

        assertFalse(Duress.verifierMatches(code, otherSalt, verifier))
    }

    @Test
    fun logRoundTrips() {
        val events = listOf(
            Duress.Event(at = 1_700_000_000_000L, duress = false),
            Duress.Event(at = 1_700_000_060_000L, duress = true),
        )
        assertEquals(events, Duress.decodeLog(Duress.encodeLog(events)))
    }

    @Test
    fun logDecodesEmptyAndGarbage() {
        assertEquals(emptyList<Duress.Event>(), Duress.decodeLog(null))
        assertEquals(emptyList<Duress.Event>(), Duress.decodeLog(""))
        assertEquals(emptyList<Duress.Event>(), Duress.decodeLog("not-a-number:1;0:1;-5:1"))
    }

    @Test
    fun logKeepsOnlyTheNewestEvents() {
        val events = (1..(Duress.MAX_EVENTS + 25)).map {
            Duress.Event(at = it.toLong(), duress = it % 2 == 0)
        }
        val decoded = Duress.decodeLog(Duress.encodeLog(events))

        assertEquals(Duress.MAX_EVENTS, decoded.size)
        assertEquals(events.takeLast(Duress.MAX_EVENTS), decoded)
    }
}
