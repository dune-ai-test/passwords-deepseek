package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.ItemType
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.PasswordHistoryEntry
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisTextField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.IconSegment
import com.aegis.vault.ui.design.IconSegmentedControl
import com.aegis.vault.ui.design.SmallButton
import com.aegis.vault.ui.design.pressable
import java.util.UUID

@Composable
fun EditItemScreen(vm: AppViewModel, nav: Navigator, screen: Screen.Edit) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    val existing = screen.id?.let { id -> vaultState?.items?.firstOrNull { it.id == id } }

    var type by remember { mutableStateOf(existing?.type ?: screen.presetType ?: ItemType.LOGIN) }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var tagsText by remember { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }

    var username by remember { mutableStateOf((existing as? LoginItem)?.username ?: "") }
    var password by remember { mutableStateOf((existing as? LoginItem)?.password ?: "") }
    var website by remember { mutableStateOf((existing as? LoginItem)?.website ?: "") }
    var showPassword by remember { mutableStateOf(true) }
    var suggestion by remember { mutableStateOf("") }

    var cardholder by remember { mutableStateOf((existing as? CardItem)?.cardholder ?: "") }
    var number by remember { mutableStateOf((existing as? CardItem)?.number ?: "") }
    var expiry by remember { mutableStateOf((existing as? CardItem)?.expiry ?: "") }
    var securityCode by remember { mutableStateOf((existing as? CardItem)?.securityCode ?: "") }
    var billingAddress by remember { mutableStateOf((existing as? CardItem)?.billingAddress ?: "") }

    var noteBody by remember { mutableStateOf((existing as? NoteItem)?.body ?: "") }

    var fullName by remember { mutableStateOf((existing as? IdentityItem)?.fullName ?: "") }
    var email by remember { mutableStateOf((existing as? IdentityItem)?.email ?: "") }
    var phone by remember { mutableStateOf((existing as? IdentityItem)?.phone ?: "") }
    var address by remember { mutableStateOf((existing as? IdentityItem)?.address ?: "") }

    LaunchedEffect(type) {
        if (type == ItemType.LOGIN && password.isEmpty() && suggestion.isEmpty()) {
            suggestion = vm.generateRandom(20)
        }
    }

    fun parseTags(): List<String> =
        tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(8)

    fun save() {
        val now = System.currentTimeMillis()
        val id = existing?.id ?: UUID.randomUUID().toString()
        val createdAt = existing?.createdAt ?: now
        val item: VaultItem = when (type) {
            ItemType.LOGIN -> {
                val old = existing as? LoginItem
                val history = if (old != null && old.password.isNotBlank() && old.password != password) {
                    (listOf(PasswordHistoryEntry(old.password, old.updatedAt)) + old.history).take(8)
                } else {
                    old?.history ?: emptyList()
                }
                LoginItem(
                    id = id,
                    title = title.ifBlank { website.ifBlank { username.ifBlank { "Untitled login" } } },
                    tags = parseTags(),
                    notes = notes,
                    createdAt = createdAt,
                    updatedAt = now,
                    lastUsedAt = old?.lastUsedAt,
                    trashedAt = null,
                    username = username,
                    password = password,
                    website = website,
                    history = history,
                )
            }
            ItemType.CARD -> CardItem(
                id = id,
                title = title.ifBlank { cardholder.ifBlank { "Untitled card" } },
                tags = parseTags(),
                notes = notes,
                createdAt = createdAt,
                updatedAt = now,
                lastUsedAt = (existing as? CardItem)?.lastUsedAt,
                trashedAt = null,
                cardholder = cardholder,
                number = number,
                expiry = expiry,
                securityCode = securityCode,
                billingAddress = billingAddress,
            )
            ItemType.NOTE -> NoteItem(
                id = id,
                title = title.ifBlank { "Untitled note" },
                tags = parseTags(),
                notes = notes,
                createdAt = createdAt,
                updatedAt = now,
                lastUsedAt = (existing as? NoteItem)?.lastUsedAt,
                trashedAt = null,
                body = noteBody,
            )
            ItemType.IDENTITY -> IdentityItem(
                id = id,
                title = title.ifBlank { fullName.ifBlank { "Untitled identity" } },
                tags = parseTags(),
                notes = notes,
                createdAt = createdAt,
                updatedAt = now,
                lastUsedAt = (existing as? IdentityItem)?.lastUsedAt,
                trashedAt = null,
                fullName = fullName,
                email = email,
                phone = phone,
                address = address,
            )
        }
        vm.upsert(item)
        vm.toast(if (existing == null) "Saved" else "Changes saved")
        nav.pop()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(72.dp)) {
                AegisLinkText("Cancel", onClick = { nav.pop() }, color = AegisColors.inkSecondary)
            }
            AText(
                if (existing == null) "New item" else "Edit item",
                style = AegisType.heading,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                AegisLinkText("Save", onClick = { save() })
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existing == null) {
                IconSegmentedControl(
                    selectedIndex = type.ordinal,
                    options = listOf(
                        IconSegment(R.drawable.ic_key_round, "Login"),
                        IconSegment(R.drawable.ic_credit_card, "Card"),
                        IconSegment(R.drawable.ic_file_text, "Note"),
                        IconSegment(R.drawable.ic_user, "Identity"),
                    ),
                    onSelect = { index -> type = ItemType.entries[index] },
                )
                Spacer(Modifier.height(2.dp))
            }

            AegisTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = when (type) {
                    ItemType.LOGIN -> "Title — e.g. GitHub"
                    ItemType.CARD -> "Card name — e.g. Travel card"
                    ItemType.NOTE -> "Note title"
                    ItemType.IDENTITY -> "Label — e.g. My identity"
                },
            )

            when (type) {
                ItemType.LOGIN -> {
                    AegisTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username or email",
                        leadingIcon = R.drawable.ic_user,
                        keyboardType = KeyboardType.Email,
                    )
                    AegisTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = R.drawable.ic_lock,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .pressable(scaleDown = 0.9f) { showPassword = !showPassword },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AegisIcon(
                                        if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye,
                                        size = 18.dp,
                                        tint = AegisColors.inkSecondary,
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                GeneratePill {
                                    suggestion = vm.generateRandom(20)
                                    if (password.isBlank()) password = suggestion
                                }
                            }
                        },
                    )
                    if (password.isBlank() && suggestion.isNotBlank()) {
                        AegisCard(padding = 14.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconBadge(
                                    R.drawable.ic_refresh_cw,
                                    AegisColors.greenSoft,
                                    AegisColors.greenDeep,
                                    size = 34.dp,
                                    corner = 10.dp,
                                    iconSize = 16.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    AText(
                                        "Suggested password",
                                        style = AegisType.label.copy(color = AegisColors.greenDeep),
                                    )
                                    AText(suggestion, style = AegisType.rowTitle, maxLines = 1)
                                }
                                SmallButton("Use") { password = suggestion }
                            }
                        }
                    }
                    AegisTextField(
                        value = website,
                        onValueChange = { website = it },
                        placeholder = "Website — e.g. github.com",
                        leadingIcon = R.drawable.ic_globe,
                        keyboardType = KeyboardType.Uri,
                    )
                }
                ItemType.CARD -> {
                    AegisTextField(
                        value = cardholder,
                        onValueChange = { cardholder = it },
                        placeholder = "Cardholder name",
                        leadingIcon = R.drawable.ic_user,
                    )
                    AegisTextField(
                        value = number,
                        onValueChange = { number = it },
                        placeholder = "Card number",
                        leadingIcon = R.drawable.ic_credit_card,
                        keyboardType = KeyboardType.Number,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AegisTextField(
                            value = expiry,
                            onValueChange = { expiry = it },
                            placeholder = "MM/YY",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        AegisTextField(
                            value = securityCode,
                            onValueChange = { securityCode = it },
                            placeholder = "CVV",
                            keyboardType = KeyboardType.Number,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AegisTextField(
                        value = billingAddress,
                        onValueChange = { billingAddress = it },
                        placeholder = "Billing address",
                        singleLine = false,
                        maxLines = 3,
                    )
                }
                ItemType.NOTE -> {
                    AegisTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        placeholder = "Write your note…",
                        singleLine = false,
                        maxLines = 12,
                    )
                }
                ItemType.IDENTITY -> {
                    AegisTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "Full name",
                        leadingIcon = R.drawable.ic_user,
                    )
                    AegisTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = R.drawable.ic_file_text,
                        keyboardType = KeyboardType.Email,
                    )
                    AegisTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "Phone",
                        keyboardType = KeyboardType.Phone,
                    )
                    AegisTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = "Address",
                        singleLine = false,
                        maxLines = 3,
                    )
                }
            }

            AegisTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                placeholder = "Tags — comma separated",
            )

            if (type != ItemType.NOTE) {
                AegisTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Notes",
                    singleLine = false,
                    maxLines = 4,
                )
            }

            Spacer(Modifier.height(4.dp))
            AegisButton("Save", onClick = { save() })
            Spacer(Modifier.height(2.dp))
            AText(
                "Everything is encrypted with your master password.",
                style = AegisType.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun GeneratePill(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AegisColors.greenSoft)
            .pressable(scaleDown = 0.93f) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        AText(
            "Generate",
            style = AegisType.smallSemibold.copy(color = AegisColors.greenDeep),
        )
    }
}
