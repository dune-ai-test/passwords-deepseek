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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.ItemType
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.CategoryMode
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.defaultSubtitle
import com.aegis.vault.ui.design.pressable

enum class SortMode(val menu: String, val subtitle: String) {
    AZ("Name A–Z", "sorted A–Z"),
    ZA("Name Z–A", "sorted Z–A"),
    RECENT("Recently used", "by recent use"),
    UPDATED("Recently updated", "by last update"),
}

@Composable
fun CategoryScreen(vm: AppViewModel, nav: Navigator, screen: Screen.Category) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    val audit by vm.audit.collectAsStateWithLifecycle()
    val data = vaultState ?: return

    var mode by remember(screen) { mutableStateOf(screen.mode) }
    var sort by remember {
        mutableStateOf(
            if (screen.mode == CategoryMode.RECENT) SortMode.RECENT else SortMode.AZ
        )
    }
    var showSort by remember { mutableStateOf(false) }
    val a = audit

    val baseList: List<VaultItem> = when (mode) {
        CategoryMode.ALL -> data.activeItems.filter { it.type == screen.type }
        CategoryMode.RECENT -> data.activeItems.filter { it.lastUsedAt != null }
        CategoryMode.WEAK -> a?.weak ?: emptyList()
        CategoryMode.REUSED -> a?.reuseGroups?.flatMap { it.items }?.distinctBy { it.id } ?: emptyList()
        CategoryMode.OLD -> a?.old ?: emptyList()
    }
    val list = when (sort) {
        SortMode.AZ -> baseList.sortedBy { it.title.lowercase() }
        SortMode.ZA -> baseList.sortedByDescending { it.title.lowercase() }
        SortMode.RECENT -> baseList.sortedByDescending { it.lastUsedAt ?: 0L }
        SortMode.UPDATED -> baseList.sortedByDescending { it.updatedAt }
    }

    val title = when (mode) {
        CategoryMode.ALL -> when (screen.type) {
            ItemType.LOGIN -> "Logins"
            ItemType.CARD -> "Cards"
            ItemType.NOTE -> "Secure Notes"
            ItemType.IDENTITY -> "Identities"
        }
        CategoryMode.RECENT -> "Recently used"
        CategoryMode.WEAK -> "Weak passwords"
        CategoryMode.REUSED -> "Reused passwords"
        CategoryMode.OLD -> "Old passwords"
    }
    val noun = when (mode) {
        CategoryMode.ALL -> when (screen.type) {
            ItemType.LOGIN -> if (list.size == 1) "login" else "logins"
            ItemType.CARD -> if (list.size == 1) "card" else "cards"
            ItemType.NOTE -> if (list.size == 1) "note" else "notes"
            ItemType.IDENTITY -> if (list.size == 1) "identity" else "identities"
        }
        else -> if (list.size == 1) "account" else "accounts"
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 148.dp)
    ) {
        NavHeader(
            title = title,
            subtitle = "${list.size} $noun · ${sort.subtitle}",
            onBack = { nav.pop() },
            actions = {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AegisColors.blueSoft)
                        .pressable(scaleDown = 0.9f) { showSort = true },
                    contentAlignment = Alignment.Center,
                ) {
                    AegisIcon(R.drawable.ic_sliders_horizontal, size = 18.dp, tint = AegisColors.blue)
                }
            },
        )

        if (mode != CategoryMode.RECENT && screen.type == ItemType.LOGIN) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AegisFilterChip(label = "All", selected = mode == CategoryMode.ALL, onClick = { mode = CategoryMode.ALL })
                AegisFilterChip(label = "Weak", selected = mode == CategoryMode.WEAK, onClick = { mode = CategoryMode.WEAK })
                AegisFilterChip(label = "Reused", selected = mode == CategoryMode.REUSED, onClick = { mode = CategoryMode.REUSED })
                AegisFilterChip(label = "Old", selected = mode == CategoryMode.OLD, onClick = { mode = CategoryMode.OLD })
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (list.isEmpty()) {
                when (mode) {
                    CategoryMode.ALL -> when (screen.type) {
                        ItemType.LOGIN -> EmptyState(
                            icon = R.drawable.ic_key_round,
                            title = "No logins yet",
                            message = "Save the passwords you use every day — they'll be one tap away.",
                            cta = "Add a login",
                            onCta = { nav.push(Screen.Edit(presetType = ItemType.LOGIN)) },
                        )
                        ItemType.CARD -> EmptyState(
                            icon = R.drawable.ic_credit_card,
                            title = "No cards yet",
                            message = "Keep card details handy for checkout without reaching for your wallet.",
                            cta = "Add a card",
                            onCta = { nav.push(Screen.Edit(presetType = ItemType.CARD)) },
                        )
                        ItemType.NOTE -> EmptyState(
                            icon = R.drawable.ic_file_text,
                            title = "No secure notes yet",
                            message = "Store passport details, software keys and Wi-Fi passwords — encrypted on this device.",
                            cta = "Create a note",
                            onCta = { nav.push(Screen.Edit(presetType = ItemType.NOTE)) },
                        )
                        ItemType.IDENTITY -> EmptyState(
                            icon = R.drawable.ic_user,
                            title = "No identities yet",
                            message = "Keep names, emails and addresses ready to paste into forms.",
                            cta = "Add an identity",
                            onCta = { nav.push(Screen.Edit(presetType = ItemType.IDENTITY)) },
                        )
                    }
                    CategoryMode.RECENT -> EmptyState(
                        icon = R.drawable.ic_clock,
                        title = "Nothing used recently",
                        message = "Items you open will show up here for quick access.",
                    )
                    CategoryMode.WEAK -> EmptyState(
                        icon = R.drawable.ic_shield_check,
                        title = "No weak passwords",
                        message = "Nice — every password passes the strength check.",
                    )
                    CategoryMode.REUSED -> EmptyState(
                        icon = R.drawable.ic_copy,
                        title = "No reused passwords",
                        message = "Every login has a password of its own. Well done.",
                    )
                    CategoryMode.OLD -> EmptyState(
                        icon = R.drawable.ic_clock,
                        title = "No old passwords",
                        message = "Everything has been updated within the last year.",
                    )
                }
            } else {
                list.forEach { item ->
                    ItemRow(
                        item = item,
                        onClick = {
                            vm.markUsed(item.id)
                            nav.push(Screen.Detail(item.id))
                        },
                        subtitle = subtitleFor(item, mode, a),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSort) {
        ModalSheet(onDismiss = { showSort = false }) {
            SheetTitle("Sort")
            Spacer(Modifier.height(8.dp))
            SortMode.entries.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable(scaleDown = 0.98f) {
                            sort = option
                            showSort = false
                        }
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText(option.menu, style = AegisType.body, modifier = Modifier.weight(1f))
                    if (sort == option) {
                        AegisIcon(R.drawable.ic_check, size = 18.dp, tint = AegisColors.blue)
                    }
                }
            }
        }
    }
}

private fun subtitleFor(item: VaultItem, mode: CategoryMode, audit: PasswordTools.Audit?): String =
    when (mode) {
        CategoryMode.WEAK -> "Password is too simple"
        CategoryMode.REUSED -> {
            val count = (item as? LoginItem)?.let { login ->
                audit?.reuseGroups?.firstOrNull { it.password == login.password }?.items?.size
            } ?: 0
            if (count > 1) "Reused by $count accounts" else "Reused password"
        }
        CategoryMode.OLD -> {
            val years = ((System.currentTimeMillis() - item.updatedAt) / (365L * 24 * 60 * 60 * 1000))
                .coerceAtLeast(1L)
            "Unchanged for $years ${if (years == 1L) "year" else "years"}"
        }
        else -> defaultSubtitle(item)
    }
