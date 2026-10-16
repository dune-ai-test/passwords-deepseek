package com.aegis.vault.sync

import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.VaultItem
import com.aegis.vault.data.displayName
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The plaintext payload of a "Send to PC" handoff. Kept deliberately small
 * (title, the item's fields, notes) — it is encrypted on-device before it
 * leaves and only ever rendered on the PC's share page, which wipes itself
 * after 60 seconds.
 */
object SharePayload {

    fun forItem(item: VaultItem): String {
        val fields = buildJsonArray {
            when (item) {
                is LoginItem -> {
                    if (item.username.isNotBlank()) add(field("Username", item.username, mono = false))
                    if (item.password.isNotBlank()) add(field("Password", item.password, mono = true))
                    if (item.website.isNotBlank()) add(field("Website", item.website, mono = false))
                }

                is CardItem -> {
                    val digits = item.number.filter { it.isDigit() }
                    val pretty = if (digits.length >= 8) digits.chunked(4).joinToString(" ") else item.number
                    if (item.cardholder.isNotBlank()) add(field("Cardholder", item.cardholder, mono = false))
                    if (pretty.isNotBlank()) add(field("Card number", pretty, mono = true))
                    if (item.expiry.isNotBlank()) add(field("Expiry", item.expiry, mono = false))
                    if (item.securityCode.isNotBlank()) add(field("Security code", item.securityCode, mono = true))
                }

                is NoteItem -> {
                    if (item.body.isNotBlank()) add(field("Note", item.body, mono = false))
                }

                is IdentityItem -> {
                    if (item.fullName.isNotBlank()) add(field("Name", item.fullName, mono = false))
                    if (item.email.isNotBlank()) add(field("Email", item.email, mono = false))
                    if (item.phone.isNotBlank()) add(field("Phone", item.phone, mono = false))
                    if (item.address.isNotBlank()) add(field("Address", item.address, mono = false))
                }
            }
        }

        return buildJsonObject {
            put("version", 1)
            put("type", item.type.displayName)
            put("title", item.title.ifBlank { "Untitled" })
            put("fields", fields)
            if (item.notes.isNotBlank()) put("notes", item.notes)
        }.toString()
    }

    private fun field(label: String, value: String, mono: Boolean): JsonObject = buildJsonObject {
        put("label", label)
        put("value", value)
        put("mono", mono)
    }
}
