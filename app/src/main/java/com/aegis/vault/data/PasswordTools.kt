package com.aegis.vault.data

import java.security.SecureRandom
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Password generation, strength estimation and the local security audit.
 * Pure-Kotlin so it is fully unit-testable on the JVM.
 */
object PasswordTools {

    enum class StrengthLabel(val display: String, val segments: Int) {
        WEAK("Weak", 1),
        FAIR("Fair", 2),
        GOOD("Good", 3),
        STRONG("Strong", 3),
        EXCELLENT("Excellent", 4),
    }

    data class Strength(val score: Int, val label: StrengthLabel)

    data class ReuseGroup(val password: String, val items: List<LoginItem>)

    data class Audit(
        val totalLogins: Int,
        val weak: List<LoginItem>,
        val reuseGroups: List<ReuseGroup>,
        val reusedAccountCount: Int,
        val old: List<LoginItem>,
        val strongCount: Int,
        val score: Int,
        val scoreLabel: String,
    ) {
        val fixesNeeded: Int
            get() = (if (weak.isNotEmpty()) 1 else 0) +
                (if (reuseGroups.isNotEmpty()) 1 else 0) +
                (if (old.isNotEmpty()) 1 else 0)
    }

    private val random = SecureRandom()

    private const val LOOKALIKES = "il1Lo0O"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?/~"

    private val COMMON_PASSWORDS = setOf(
        "password", "123456", "123456789", "qwerty", "111111", "12345678", "abc123",
        "1234567", "password1", "12345", "1234567890", "letmein", "welcome", "monkey",
        "dragon", "iloveyou", "admin", "football", "master", "sunshine", "princess",
        "login", "passw0rd", "starwars", "whatever", "trustno1", "zaq12wsx",
        "summer21", "summer2021", "winter2021", "spring2021", "summer2022", "qwerty123",
        "1q2w3e4r", "qazwsx", "superman", "batman", "shadow", "michael", "jennifer",
        "buddy", "hunter", "ranger", "buster", "soccer", "hockey", "killer",
    )

    /* ── Strength ─────────────────────────────────────────────────────────── */

    fun estimate(password: String): Strength {
        if (password.isEmpty()) return Strength(0, StrengthLabel.WEAK)
        val lowered = password.lowercase()
        if (lowered in COMMON_PASSWORDS) return Strength(8, StrengthLabel.WEAK)

        var pool = 0
        if (password.any { it in 'a'..'z' }) pool += 26
        if (password.any { it in 'A'..'Z' }) pool += 26
        if (password.any { it in '0'..'9' }) pool += 10
        if (password.any { !it.isLetterOrDigit() }) pool += 32
        if (pool == 0) pool = 26

        var score = password.length * (ln(pool.toDouble()) / ln(2.0))

        // Penalise keyboard/number/letter sequences.
        val sequences = listOf("1234567890", "abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm")
        var index = 0
        while (index + 3 <= lowered.length) {
            val chunk = lowered.substring(index, index + 3)
            if (sequences.any { it.contains(chunk) }) score -= 2.0
            index++
        }

        // Penalise low character variety.
        val distinct = password.toSet().size.toDouble() / password.length
        if (distinct < 0.5) score *= 0.8
        if (password.all { it.isDigit() }) score *= 0.75

        val finalScore = score.coerceIn(0.0, 100.0).roundToInt()
        val label = when {
            finalScore < 30 -> StrengthLabel.WEAK
            finalScore < 50 -> StrengthLabel.FAIR
            finalScore < 75 -> StrengthLabel.GOOD
            finalScore < 90 -> StrengthLabel.STRONG
            else -> StrengthLabel.EXCELLENT
        }
        return Strength(finalScore, label)
    }

    /* ── Generation ───────────────────────────────────────────────────────── */

    fun generateRandom(
        length: Int,
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true,
        avoidLookalikes: Boolean = false,
    ): String {
        fun pool(base: String): String =
            if (avoidLookalikes) base.filter { it !in LOOKALIKES } else base

        val pools = mutableListOf<String>()
        if (upper) pools += pool(UPPER)
        if (lower) pools += pool(LOWER)
        if (digits) pools += pool(DIGITS)
        if (symbols) pools += pool(SYMBOLS)
        if (pools.isEmpty()) pools += pool(LOWER)

        val everything = pools.joinToString("")
        val builder = StringBuilder()
        if (length >= pools.size) {
            pools.forEach { builder.append(it[random.nextInt(it.length)]) }
        }
        while (builder.length < length) {
            builder.append(everything[random.nextInt(everything.length)])
        }
        val chars = builder.toString().toCharArray()
        for (i in chars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return String(chars)
    }

    fun generateMemorable(
        words: List<String>,
        count: Int = 4,
        separator: String = "-",
        capitalize: Boolean = true,
    ): String {
        if (words.isEmpty()) return ""
        return (0 until count)
            .map { words[random.nextInt(words.size)] }
            .joinToString(separator) { word ->
                if (capitalize) word.replaceFirstChar { it.uppercaseChar() } else word
            }
    }

    /* ── Audit ────────────────────────────────────────────────────────────── */

    fun audit(data: VaultData, now: Long = System.currentTimeMillis()): Audit {
        val logins = data.items.filterIsInstance<LoginItem>().filter { !it.isTrashed }
        val withPassword = logins.filter { it.password.isNotBlank() }

        val weak = withPassword.filter { estimate(it.password).score < 50 }

        val reuseGroups = withPassword
            .groupBy { it.password }
            .filter { it.value.size > 1 }
            .map { ReuseGroup(it.key, it.value) }
            .sortedByDescending { it.items.size }

        val old = logins.filter { it.updatedAt > 0L && now - it.updatedAt > 365L * 24 * 60 * 60 * 1000 }

        val strongCount = withPassword.count { estimate(it.password).score >= 75 && it.password !in reuseGroups.flatMap { g -> listOf(g.password) } }

        val reusedExtra = reuseGroups.sumOf { it.items.size - 1 }
        val penalty = weak.size * 8 + reusedExtra * 6 + old.size * 2
        val score = if (logins.isEmpty()) 100 else (100 - penalty).coerceIn(10, 100)
        val label = when {
            score >= 90 -> "Excellent"
            score >= 75 -> "Good"
            score >= 50 -> "Fair"
            else -> "Weak"
        }
        return Audit(
            totalLogins = logins.size,
            weak = weak,
            reuseGroups = reuseGroups,
            reusedAccountCount = reuseGroups.sumOf { it.items.size },
            old = old,
            strongCount = strongCount,
            score = score,
            scoreLabel = label,
        )
    }
}
