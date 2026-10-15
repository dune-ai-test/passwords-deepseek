package com.aegis.vault

import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordToolsTest {

    @Test
    fun commonPasswordsAreWeak() {
        val strength = PasswordTools.estimate("password")
        assertTrue(strength.score < 50)
        assertEquals(PasswordTools.StrengthLabel.WEAK, strength.label)

        assertTrue(PasswordTools.estimate("123456").score < 50)
        assertTrue(PasswordTools.estimate("").score == 0)
    }

    @Test
    fun longMixedPasswordIsStrong() {
        assertTrue(PasswordTools.estimate("9F\$kLm#Qz2!vB7@p").score >= 75)
    }

    @Test
    fun randomGeneratorHonoursLengthAndClassGuarantees() {
        val password = PasswordTools.generateRandom(24)
        assertEquals(24, password.length)
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun randomGeneratorAvoidsLookalikesWhenAsked() {
        val password = PasswordTools.generateRandom(64, avoidLookalikes = true)
        assertTrue(password.none { it in "il1Lo0O" })
    }

    @Test
    fun memorableGeneratorJoinsWordsWithSeparator() {
        val words = listOf("apple", "anchor", "arrow", "aztec")
        val password = PasswordTools.generateMemorable(words, count = 4, separator = "-", capitalize = true)
        val parts = password.split("-")
        assertEquals(4, parts.size)
        assertTrue(parts.all { part -> words.contains(part.lowercase()) })
    }

    @Test
    fun auditFlagsWeakReusedAndOldLogins() {
        val now = System.currentTimeMillis()
        val year = 365L * 24 * 60 * 60 * 1000
        val shared = "S3cure-Shared-Pass!"
        val items = listOf(
            LoginItem(id = "1", title = "A", password = shared, updatedAt = now),
            LoginItem(id = "2", title = "B", password = shared, updatedAt = now),
            LoginItem(
                id = "3",
                title = "C",
                password = "123456",
                createdAt = now - 2 * year,
                updatedAt = now - 2 * year,
            ),
        )
        val audit = PasswordTools.audit(VaultData(items = items), now)
        assertEquals(1, audit.reuseGroups.size)
        assertEquals(2, audit.reusedAccountCount)
        assertTrue(audit.weak.isNotEmpty())
        assertEquals(listOf("3"), audit.old.map { it.id })
        assertTrue(audit.score in 10..100)
        assertTrue(audit.fixesNeeded >= 2)
    }

    @Test
    fun auditOfCleanVaultScoresPerfectly() {
        val now = System.currentTimeMillis()
        val items = listOf(
            LoginItem(id = "1", title = "A", password = "9F\$kLm#Qz2!vB7@p", updatedAt = now),
            LoginItem(id = "2", title = "B", password = "xW7#mQ2@zR5!bN8(", updatedAt = now),
        )
        val audit = PasswordTools.audit(VaultData(items = items), now)
        assertEquals(0, audit.weak.size)
        assertEquals(0, audit.reuseGroups.size)
        assertEquals(0, audit.old.size)
        assertEquals(100, audit.score)
        assertEquals(2, audit.strongCount)
    }
}
