package com.aegis.vault.data

import android.content.Context
import android.content.SharedPreferences
import com.aegis.vault.crypto.Crypto
import java.security.MessageDigest

/**
 * Duress code + decoy-open log.
 *
 * The duress code is a second secret the owner can enter at the unlock screen.
 * It never opens the real vault: a failed master-password attempt already
 * routes to the decoy, and the duress code only *labels* that event, so a
 * coerced unlock looks identical whether a typo or the code was used.
 *
 * The verifier is derived with a deliberately cheap PBKDF2 cost: cracking it
 * would only prove "this string opens the decoy" — a fact with no value to an
 * attacker — and it runs on every failed unlock attempt, where the full
 * 600k-iteration cost would be wasted work on slow devices.
 *
 * The log stores timestamps and a duress/label flag only — never content —
 * in app-private preferences, the same exposure class as the vault file.
 */
object Duress {

    const val KDF_ITERATIONS = 20_000
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 64
    const val MAX_EVENTS = 100

    /** One decoy open: when it happened and whether the duress code was used. */
    data class Event(val at: Long, val duress: Boolean)

    fun deriveVerifier(code: CharArray, salt: ByteArray): ByteArray =
        Crypto.deriveKey(code, salt, KDF_ITERATIONS)

    fun verifierMatches(code: CharArray, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(deriveVerifier(code, salt), expected)

    /** Compact durable format: `at:flag;at:flag` (flag 1 = duress code). */
    fun encodeLog(events: List<Event>): String =
        events.takeLast(MAX_EVENTS).joinToString(";") { "${it.at}:${if (it.duress) 1 else 0}" }

    fun decodeLog(raw: String?): List<Event> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(';').mapNotNull { part ->
            val at = part.substringBefore(':').toLongOrNull()
            val flag = part.substringAfter(':', "")
            if (at == null || at <= 0L) null else Event(at, flag == "1")
        }.takeLast(MAX_EVENTS)
    }
}

/** Preferences-backed storage for the duress verifier and the decoy log. */
class DuressStore(context: Context) {

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_LOG = "log"
        private const val KEY_NOTIFIED_AT = "notifiedAt"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aegis_duress", Context.MODE_PRIVATE)

    val hasCode: Boolean
        get() = prefs.getString(KEY_VERIFIER, null) != null && prefs.getString(KEY_SALT, null) != null

    /** Register [code] as the duress code. Callers validate it first. */
    fun setCode(code: CharArray) {
        val salt = Crypto.randomBytes(Crypto.SALT_BYTES)
        val verifier = Duress.deriveVerifier(code, salt)
        prefs.edit()
            .putString(KEY_SALT, Crypto.b64(salt))
            .putString(KEY_VERIFIER, Crypto.b64(verifier))
            .apply()
    }

    fun clearCode() {
        prefs.edit().remove(KEY_SALT).remove(KEY_VERIFIER).apply()
    }

    /** True when [code] is the registered duress code. False with no code set. */
    fun verify(code: CharArray): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { Crypto.unb64(it) } ?: return false
        val verifier = prefs.getString(KEY_VERIFIER, null)?.let { Crypto.unb64(it) } ?: return false
        return Duress.verifierMatches(code, salt, verifier)
    }

    fun addEvent(duress: Boolean) {
        val events = Duress.decodeLog(prefs.getString(KEY_LOG, null))
        val updated = events + Duress.Event(System.currentTimeMillis(), duress)
        prefs.edit().putString(KEY_LOG, Duress.encodeLog(updated)).apply()
    }

    fun events(): List<Duress.Event> = Duress.decodeLog(prefs.getString(KEY_LOG, null))

    fun clearEvents() {
        prefs.edit().remove(KEY_LOG).remove(KEY_NOTIFIED_AT).apply()
    }

    /** Events newer than the last one announced to the owner. */
    fun unseenEvents(): List<Duress.Event> {
        val notifiedAt = prefs.getLong(KEY_NOTIFIED_AT, 0L)
        return events().filter { it.at > notifiedAt }
    }

    fun markEventsSeen() {
        val newest = events().maxOfOrNull { it.at } ?: return
        prefs.edit().putLong(KEY_NOTIFIED_AT, newest).apply()
    }
}
