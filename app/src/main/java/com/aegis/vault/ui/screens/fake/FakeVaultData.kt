package com.aegis.vault.ui.screens.fake

import androidx.compose.runtime.mutableStateListOf
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.PasswordHistoryEntry
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultData
import com.aegis.vault.data.VaultItem

/**
 * Demo content for the decoy vault.
 *
 * The decoy opens instead of the real vault whenever an unlock attempt fails.
 * It is pure presentation: everything lives in memory, nothing is ever written
 * to disk, and no screen here can read or change the real vault.
 *
 * The content is deliberately mundane — everyday logins, two cards, a few
 * notes — and includes a couple of weak/reused/old passwords so the Security
 * tab has believable findings.
 */
object FakeVaultData {

    /** Roughly the on-disk size the storage card would report for this many items. */
    const val APPROX_BYTES = 4712L

    fun items(now: Long = System.currentTimeMillis()): List<VaultItem> {
        val day = 24L * 60 * 60 * 1000
        fun ago(days: Long) = now - days * day
        return listOf(
            LoginItem(
                id = "fx-login-netflix",
                title = "Netflix",
                username = "jordan.marsh@outlook.com",
                password = "Ne7f!ixMarsh22",
                website = "netflix.com",
                notes = "Family plan — 4 screens.",
                tags = listOf("streaming"),
                createdAt = ago(795),
                updatedAt = ago(142),
                lastUsedAt = ago(2),
                history = listOf(PasswordHistoryEntry("MarshNetflix19", ago(142))),
            ),
            LoginItem(
                id = "fx-login-amazon",
                title = "Amazon",
                username = "jordan.marsh@outlook.com",
                password = "Am@zonPrime24!",
                website = "amazon.com",
                createdAt = ago(910),
                updatedAt = ago(95),
                lastUsedAt = ago(1),
            ),
            LoginItem(
                id = "fx-login-gmail",
                title = "Gmail",
                username = "jordan.marsh@gmail.com",
                password = "J0rdan#K9mq!xL",
                website = "gmail.com",
                createdAt = ago(1200),
                updatedAt = ago(64),
                lastUsedAt = ago(3),
                history = listOf(
                    PasswordHistoryEntry("ilovegoogle22", ago(64)),
                    PasswordHistoryEntry("Gmail*2019!marsh", ago(384)),
                ),
            ),
            LoginItem(
                id = "fx-login-spotify",
                title = "Spotify",
                username = "jordan.marsh",
                password = "Rover2019!",
                website = "spotify.com",
                notes = "Student discount applied.",
                tags = listOf("music"),
                createdAt = ago(640),
                updatedAt = ago(431),
                lastUsedAt = ago(5),
            ),
            LoginItem(
                id = "fx-login-reddit",
                title = "Reddit",
                username = "j_marsh_tx",
                password = "Rover2019!",
                website = "reddit.com",
                createdAt = ago(520),
                updatedAt = ago(340),
            ),
            LoginItem(
                id = "fx-login-instagram",
                title = "Instagram",
                username = "jordan.marsh",
                password = "sunshine1",
                website = "instagram.com",
                createdAt = ago(730),
                updatedAt = ago(612),
                lastUsedAt = ago(12),
            ),
            LoginItem(
                id = "fx-login-pinterest",
                title = "Pinterest",
                username = "jordan.marsh@outlook.com",
                password = "welcome1",
                website = "pinterest.com",
                createdAt = ago(300),
                updatedAt = ago(160),
            ),
            LoginItem(
                id = "fx-login-github",
                title = "GitHub",
                username = "jmarsh-dev",
                password = "gh_T0ken*Marsh!",
                website = "github.com",
                createdAt = ago(840),
                updatedAt = ago(34),
                lastUsedAt = ago(8),
            ),
            LoginItem(
                id = "fx-login-steam",
                title = "Steam",
                username = "j_marsh_84",
                password = "Steam#Guard88",
                website = "store.steampowered.com",
                createdAt = ago(1500),
                updatedAt = ago(211),
                lastUsedAt = ago(20),
            ),
            LoginItem(
                id = "fx-login-chase",
                title = "Chase",
                username = "jmarsh29",
                password = "W3lcome@Marsh!",
                website = "chase.com",
                notes = "Alerts on for purchases over $100.",
                tags = listOf("banking"),
                createdAt = ago(1000),
                updatedAt = ago(150),
                lastUsedAt = ago(9),
            ),
            CardItem(
                id = "fx-card-visa",
                title = "Everyday Visa",
                cardholder = "Jordan Marsh",
                number = "4751880234176629",
                expiry = "09/29",
                securityCode = "412",
                billingAddress = "2214 Willow Creek Dr, Apt 12, Austin, TX 78704",
                createdAt = ago(880),
                updatedAt = ago(120),
                lastUsedAt = ago(6),
            ),
            CardItem(
                id = "fx-card-mc",
                title = "Travel card",
                cardholder = "Jordan Marsh",
                number = "5521774399021186",
                expiry = "01/28",
                securityCode = "738",
                createdAt = ago(430),
                updatedAt = ago(200),
            ),
            NoteItem(
                id = "fx-note-wifi",
                title = "Home Wi-Fi",
                body = "Network: WillowNet-5G\nPassword: Map13&Creek2021\nRouter admin: 192.168.0.1 — admin / c0ttageR0uter",
                tags = listOf("home"),
                createdAt = ago(410),
                updatedAt = ago(58),
            ),
            NoteItem(
                id = "fx-note-keypad",
                title = "Front door keypad",
                body = "Entry code: 4172\nSpare key: kitchen drawer, blue tin.",
                createdAt = ago(270),
                updatedAt = ago(130),
            ),
            NoteItem(
                id = "fx-note-insurance",
                title = "Car insurance",
                body = "Policy 84-291-773 — Northgate Auto\nRenews Mar 14\nClaims: (512) 555-0177",
                tags = listOf("car"),
                createdAt = ago(350),
                updatedAt = ago(96),
            ),
            IdentityItem(
                id = "fx-id-jordan",
                title = "Jordan Marsh",
                fullName = "Jordan Marsh",
                email = "jordan.marsh@outlook.com",
                phone = "(512) 555-0134",
                address = "2214 Willow Creek Dr, Apt 12, Austin, TX 78704",
                createdAt = ago(770),
                updatedAt = ago(210),
            ),
        )
    }
}

/**
 * Session-only state of the decoy vault: search, edit, trash and "recently
 * used" all work against this, and it resets when the app locks.
 */
class FakeVaultState {

    val items = mutableStateListOf<VaultItem>().apply { addAll(FakeVaultData.items()) }

    val activeItems: List<VaultItem> get() = items.filter { !it.isTrashed }

    val trashedItems: List<VaultItem>
        get() = items.filter { it.isTrashed }.sortedByDescending { it.trashedAt ?: 0L }

    val audit: PasswordTools.Audit get() = PasswordTools.audit(VaultData(items = items))

    fun item(id: String): VaultItem? = items.firstOrNull { it.id == id }

    fun markUsed(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = items[index]
        val now = System.currentTimeMillis()
        val last = item.lastUsedAt
        if (last != null && now - last < 60_000L) return
        items[index] = item.withLastUsed(now)
    }

    fun upsert(item: VaultItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item else items.add(item)
    }

    fun moveToTrash(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        items[index] = items[index].withTrashed(System.currentTimeMillis())
    }

    fun restore(id: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        items[index] = items[index].withTrashed(null)
    }

    fun deleteForever(id: String) {
        items.removeAll { it.id == id }
    }

    fun emptyTrash() {
        items.removeAll { it.isTrashed }
    }
}

private fun VaultItem.withLastUsed(now: Long): VaultItem = when (this) {
    is LoginItem -> copy(lastUsedAt = now)
    is CardItem -> copy(lastUsedAt = now)
    is NoteItem -> copy(lastUsedAt = now)
    is IdentityItem -> copy(lastUsedAt = now)
}

private fun VaultItem.withTrashed(at: Long?): VaultItem = when (this) {
    is LoginItem -> copy(trashedAt = at)
    is CardItem -> copy(trashedAt = at)
    is NoteItem -> copy(trashedAt = at)
    is IdentityItem -> copy(trashedAt = at)
}
