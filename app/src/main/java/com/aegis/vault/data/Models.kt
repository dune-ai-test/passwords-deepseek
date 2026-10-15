package com.aegis.vault.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ItemType { LOGIN, CARD, NOTE, IDENTITY }

val ItemType.displayName: String
    get() = when (this) {
        ItemType.LOGIN -> "Login"
        ItemType.CARD -> "Card"
        ItemType.NOTE -> "Secure Note"
        ItemType.IDENTITY -> "Identity"
    }

val ItemType.pluralName: String
    get() = when (this) {
        ItemType.LOGIN -> "Logins"
        ItemType.CARD -> "Cards"
        ItemType.NOTE -> "Secure Notes"
        ItemType.IDENTITY -> "Identities"
    }

@Serializable
sealed class VaultItem {
    abstract val id: String
    abstract val title: String
    abstract val tags: List<String>
    abstract val notes: String
    abstract val createdAt: Long
    abstract val updatedAt: Long
    abstract val lastUsedAt: Long?
    abstract val trashedAt: Long?

    val isTrashed: Boolean get() = trashedAt != null

    val type: ItemType
        get() = when (this) {
            is LoginItem -> ItemType.LOGIN
            is CardItem -> ItemType.CARD
            is NoteItem -> ItemType.NOTE
            is IdentityItem -> ItemType.IDENTITY
        }
}

@Serializable
data class PasswordHistoryEntry(
    val value: String,
    val changedAt: Long,
)

@Serializable
@SerialName("login")
data class LoginItem(
    override val id: String,
    override val title: String = "",
    override val tags: List<String> = emptyList(),
    override val notes: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val lastUsedAt: Long? = null,
    override val trashedAt: Long? = null,
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val history: List<PasswordHistoryEntry> = emptyList(),
) : VaultItem()

@Serializable
@SerialName("card")
data class CardItem(
    override val id: String,
    override val title: String = "",
    override val tags: List<String> = emptyList(),
    override val notes: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val lastUsedAt: Long? = null,
    override val trashedAt: Long? = null,
    val cardholder: String = "",
    val number: String = "",
    val expiry: String = "",
    val securityCode: String = "",
    val billingAddress: String = "",
) : VaultItem()

@Serializable
@SerialName("note")
data class NoteItem(
    override val id: String,
    override val title: String = "",
    override val tags: List<String> = emptyList(),
    override val notes: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val lastUsedAt: Long? = null,
    override val trashedAt: Long? = null,
    val body: String = "",
) : VaultItem()

@Serializable
@SerialName("identity")
data class IdentityItem(
    override val id: String,
    override val title: String = "",
    override val tags: List<String> = emptyList(),
    override val notes: String = "",
    override val createdAt: Long = 0L,
    override val updatedAt: Long = 0L,
    override val lastUsedAt: Long? = null,
    override val trashedAt: Long? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
) : VaultItem()

@Serializable
data class VaultSettings(
    val autoLockMinutes: Int = 1,
    val biometricEnabled: Boolean = false,
    val screenshotProtection: Boolean = false,
    val lastBackupAt: Long? = null,
)

@Serializable
data class VaultData(
    val schemaVersion: Int = 1,
    val items: List<VaultItem> = emptyList(),
    val settings: VaultSettings = VaultSettings(),
) {
    val activeItems: List<VaultItem> get() = items.filter { !it.isTrashed }
    val trashedItems: List<VaultItem> get() = items.filter { it.isTrashed }.sortedByDescending { it.trashedAt ?: 0L }
    fun countOf(type: ItemType): Int = activeItems.count { it.type == type }
}
