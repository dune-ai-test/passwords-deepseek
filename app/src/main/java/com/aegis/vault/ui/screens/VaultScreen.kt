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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.ItemType
import com.aegis.vault.ui.CategoryMode
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.AvatarBadge
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.SearchBarButton
import com.aegis.vault.ui.design.SectionHeader
import com.aegis.vault.ui.design.pressable

@Composable
fun VaultScreen(vm: AppViewModel, nav: Navigator) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    val audit by vm.audit.collectAsStateWithLifecycle()
    val vaultBytes by vm.vaultBytes.collectAsStateWithLifecycle()
    val data = vaultState ?: return

    val items = data.activeItems
    val logins = items.count { it.type == ItemType.LOGIN }
    val cards = items.count { it.type == ItemType.CARD }
    val notes = items.count { it.type == ItemType.NOTE }
    val identities = items.count { it.type == ItemType.IDENTITY }
    val recent = items
        .filter { it.lastUsedAt != null }
        .sortedByDescending { it.lastUsedAt }
        .take(3)
    val a = audit

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AText("Passwords", style = AegisType.title, modifier = Modifier.weight(1f))
            Box(
                Modifier.pressable(scaleDown = 0.92f) { nav.switchTab(Screen.Settings) }
            ) {
                AvatarBadge("A", AegisColors.violet, size = 40.dp, corner = 20.dp)
            }
        }

        SearchBarButton(
            text = "Search passwords, cards, notes",
            onClick = { nav.push(Screen.Search) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        R.drawable.ic_hard_drive,
                        AegisColors.blueSoft,
                        AegisColors.blue,
                        size = 40.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        AText("Local-only vault", style = AegisType.rowTitle)
                        AText(
                            "${Fmt.formatBytes(vaultBytes)} on this device",
                            style = AegisType.small,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AegisColors.greenSoft)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        AText("Offline", style = AegisType.smallSemibold.copy(color = AegisColors.greenDeep))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryTile(
                    R.drawable.ic_key_round, AegisColors.blue, AegisColors.blueSoft,
                    "Logins", logins, Modifier.weight(1f),
                ) { nav.push(Screen.Category(CategoryMode.ALL, ItemType.LOGIN)) }
                CategoryTile(
                    R.drawable.ic_credit_card, AegisColors.orange, AegisColors.orangeSoft,
                    "Cards", cards, Modifier.weight(1f),
                ) { nav.push(Screen.Category(CategoryMode.ALL, ItemType.CARD)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryTile(
                    R.drawable.ic_file_text, AegisColors.violetDeep, AegisColors.violetSoft,
                    "Notes", notes, Modifier.weight(1f),
                ) { nav.push(Screen.Category(CategoryMode.ALL, ItemType.NOTE)) }
                CategoryTile(
                    R.drawable.ic_user, AegisColors.greenDeep, AegisColors.greenSoft,
                    "Identities", identities, Modifier.weight(1f),
                ) { nav.push(Screen.Category(CategoryMode.ALL, ItemType.IDENTITY)) }
            }

            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Recently used",
                    action = "See all",
                    onAction = { nav.push(Screen.Category(CategoryMode.RECENT, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recent.forEach { item ->
                        ItemRow(
                            item = item,
                            onClick = {
                                vm.markUsed(item.id)
                                nav.push(Screen.Detail(item.id))
                            },
                        )
                    }
                }
            }

            if (a != null && (a.reuseGroups.isNotEmpty() || a.old.isNotEmpty() || a.weak.isNotEmpty())) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Needs attention",
                    action = "Fix all",
                    onAction = { nav.switchTab(Screen.Security) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (a.reuseGroups.isNotEmpty()) {
                        val group = a.reuseGroups.first()
                        AttentionRow(
                            icon = R.drawable.ic_copy,
                            tint = AegisColors.orange,
                            bg = AegisColors.orangeSoft,
                            title = "\"${group.password}\" reused",
                            subtitle = "${group.items.size} accounts share it",
                        ) { nav.push(Screen.Category(CategoryMode.REUSED, ItemType.LOGIN)) }
                    }
                    if (a.old.isNotEmpty()) {
                        val oldest = a.old.minBy { it.updatedAt }
                        val years = yearsSince(oldest.updatedAt)
                        AttentionRow(
                            icon = R.drawable.ic_clock,
                            tint = AegisColors.violetDeep,
                            bg = AegisColors.violetSoft,
                            title = "Unchanged for $years ${if (years == 1L) "year" else "years"}",
                            subtitle = listOfNotNull(
                                oldest.title.takeIf { it.isNotBlank() },
                                (oldest as? com.aegis.vault.data.LoginItem)?.username?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        ) { nav.push(Screen.Category(CategoryMode.OLD, ItemType.LOGIN)) }
                    }
                    if (a.weak.isNotEmpty()) {
                        val count = a.weak.size
                        AttentionRow(
                            icon = R.drawable.ic_triangle_alert,
                            tint = AegisColors.red,
                            bg = AegisColors.redSoft,
                            title = "$count weak ${if (count == 1) "password" else "passwords"}",
                            subtitle = "Easily guessed — update them soon",
                        ) { nav.push(Screen.Category(CategoryMode.WEAK, ItemType.LOGIN)) }
                    }
                }
            }

            if (items.isEmpty()) {
                Spacer(Modifier.height(28.dp))
                EmptyState(
                    icon = R.drawable.ic_key_round,
                    title = "Your vault is empty",
                    message = "Tap the blue + button to add your first login, card or secure note.",
                    cta = "Add a login",
                    onCta = { nav.push(Screen.Edit(presetType = ItemType.LOGIN)) },
                    footnote = "Everything stays encrypted on this device.",
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun yearsSince(millis: Long): Long =
    ((System.currentTimeMillis() - millis) / (365L * 24 * 60 * 60 * 1000)).coerceAtLeast(1L)

@Composable
private fun CategoryTile(
    icon: Int,
    tint: Color,
    bg: Color,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AegisCard(modifier = modifier, padding = 14.dp, onClick = onClick) {
        IconBadge(icon, bg, tint, size = 38.dp, corner = 12.dp, iconSize = 19.dp)
        Spacer(Modifier.height(10.dp))
        AText(title, style = AegisType.rowTitle)
        AText(
            if (count == 1) "1 item" else "$count items",
            style = AegisType.small,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun AttentionRow(
    icon: Int,
    tint: Color,
    bg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AegisColors.card)
            .pressable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, bg, tint, size = 38.dp, corner = 12.dp, iconSize = 19.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AText(title, style = AegisType.rowTitle, maxLines = 1)
            if (subtitle.isNotBlank()) {
                AText(subtitle, style = AegisType.small, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
            }
        }
        AegisIcon(R.drawable.ic_chevron_right, size = 16.dp, tint = AegisColors.inkTertiary)
    }
}
