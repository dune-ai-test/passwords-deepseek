package com.aegis.vault.ui.screens.fake

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.Brands
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisDivider
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.ActionRow
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ItemBadge
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.TagChip
import com.aegis.vault.ui.design.darken
import com.aegis.vault.ui.design.pressable
import com.aegis.vault.ui.openUrl
import com.aegis.vault.ui.safeFileName
import com.aegis.vault.ui.screens.SendToPcAction
import com.aegis.vault.ui.screens.ShareToPcSheet
import com.aegis.vault.ui.shareText
import com.aegis.vault.ui.shareTextFile
import com.aegis.vault.ui.writeTextToUri

@Composable
fun FakeDetailScreen(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState, id: String) {
    val item = state.item(id)
    if (item == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    LaunchedEffect(id) { state.markUsed(id) }
    when (item) {
        is LoginItem -> FakeLoginDetail(vm, nav, state, item)
        is CardItem -> FakeCardDetail(vm, nav, state, item)
        is NoteItem -> FakeNoteDetail(vm, nav, state, item)
        is IdentityItem -> FakeIdentityDetail(vm, nav, state, item)
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Shared bits
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeFieldIconButton(
    icon: Int,
    tint: Color = AegisColors.blue,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .pressable(scaleDown = 0.88f) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = 16.dp, tint = tint)
    }
}

@Composable
private fun FakeDetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = AegisType.body,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            AText(label, style = AegisType.label)
            Spacer(Modifier.height(3.dp))
            AText(value.ifBlank { "—" }, style = valueStyle, maxLines = 3)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun FakeHeroRow(item: VaultItem, badgeSize: Dp = 56.dp) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemBadge(item, size = badgeSize)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            AText(item.title.ifBlank { "Untitled" }, style = AegisType.big, maxLines = 1)
            AText(
                "Edited ${Fmt.relativeTime(item.updatedAt)}",
                style = AegisType.small,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun FakeTrashActionRow(text: String, onClick: () -> Unit) {
    ActionRow(
        icon = R.drawable.ic_trash_2,
        label = text,
        onClick = onClick,
        tint = AegisColors.red,
        bg = AegisColors.redSoft,
    )
}

/* ────────────────────────────────────────────────────────────────────────────
 * Login
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeLoginDetail(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState, item: LoginItem) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var confirmShare by remember { mutableStateOf(false) }
    var sendToPc by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (historyOpen) 180f else 0f,
        label = "historyChevron",
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NavHeader(
            onBack = { nav.pop() },
            actions = {
                AegisLinkText("Edit", onClick = { nav.push(FakeScreen.Edit(item.id)) })
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            FakeHeroRow(item)

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                FakeDetailField(
                    label = "Username",
                    value = item.username,
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(item.username)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Password",
                    value = if (revealed) {
                        item.password
                    } else {
                        "•".repeat(item.password.length.coerceIn(6, 20))
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FakeFieldIconButton(
                                icon = if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
                                tint = AegisColors.inkSecondary,
                            ) { revealed = !revealed }
                            FakeFieldIconButton(R.drawable.ic_copy) {
                                vm.copyToClipboard(item.password, sensitive = true)
                            }
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Website",
                    value = item.website,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.website.isNotBlank()) {
                                FakeFieldIconButton(R.drawable.ic_globe) {
                                    openUrl(context, item.website)
                                }
                            }
                            FakeFieldIconButton(R.drawable.ic_copy) {
                                vm.copyToClipboard(item.website)
                            }
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Last changed",
                    value = Fmt.relativeTime(item.updatedAt),
                )
                if (item.notes.isNotBlank()) {
                    AegisDivider(startPadding = 16.dp)
                    FakeDetailField(label = "Notes", value = item.notes)
                }
                if (item.tags.isNotEmpty()) {
                    AegisDivider(startPadding = 16.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item.tags.forEach { tag -> TagChip(tag) }
                    }
                }
            }

            if (item.password.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                AegisButton(
                    text = "Copy password",
                    onClick = { vm.copyToClipboard(item.password, sensitive = true) },
                    icon = R.drawable.ic_copy,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SendToPcAction { sendToPc = true }
                AegisDivider(startPadding = 62.dp)
                ActionRow(
                    icon = R.drawable.ic_share_2,
                    label = "Share login",
                    onClick = { confirmShare = true },
                )
                AegisDivider(startPadding = 62.dp)
                FakeTrashActionRow("Move to trash") {
                    state.moveToTrash(item.id)
                    vm.toast("Moved to trash")
                    nav.pop()
                }
            }

            if (item.history.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable(scaleDown = 0.99f) { historyOpen = !historyOpen }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AText("Password history", style = AegisType.rowTitle, modifier = Modifier.weight(1f))
                        AegisIcon(
                            R.drawable.ic_chevron_down,
                            size = 16.dp,
                            tint = AegisColors.inkTertiary,
                            modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                        )
                    }
                    AnimatedVisibility(visible = historyOpen) {
                        Column {
                            item.history.forEachIndexed { index, entry ->
                                AegisDivider(startPadding = 16.dp)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        AText(
                                            if (index == 0) "Previous" else "Older",
                                            style = AegisType.smallSemibold.copy(color = AegisColors.ink),
                                        )
                                        AText(
                                            "•".repeat(entry.value.length.coerceIn(8, 16)),
                                            style = AegisType.body,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                    AText(
                                        Fmt.relativeTime(entry.changedAt),
                                        style = AegisType.small,
                                        modifier = Modifier.padding(end = 10.dp),
                                    )
                                    FakeFieldIconButton(R.drawable.ic_copy) {
                                        vm.copyToClipboard(entry.value, sensitive = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmShare) {
        ConfirmDialog(
            title = "Share password?",
            message = "The shared text will include your password in plain text. Only share it with apps and people you trust.",
            confirmLabel = "Share anyway",
            onConfirm = {
                confirmShare = false
                val body = buildString {
                    appendLine("Username: ${item.username}")
                    appendLine("Password: ${item.password}")
                    if (item.website.isNotBlank()) appendLine("Website: ${item.website}")
                }.trim()
                shareText(context, item.title, body)
            },
            onDismiss = { confirmShare = false },
        )
    }

    if (sendToPc) {
        ShareToPcSheet(vm = vm, item = item, fake = true, onDismiss = { sendToPc = false })
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Card
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeCardDetail(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState, item: CardItem) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(false) }
    var confirmShare by remember { mutableStateOf(false) }
    var sendToPc by remember { mutableStateOf(false) }

    val brand = Brands.cardBrand(item.number)
    val digits = item.number.filter { it.isDigit() }
    val masked = if (digits.length >= 4) {
        "••••  ••••  ••••  ${digits.takeLast(4)}"
    } else {
        "••••  ••••  ••••  ••••"
    }
    val full = if (digits.isEmpty()) "••••  ••••  ••••  ••••" else digits.chunked(4).joinToString("  ")

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NavHeader(
            onBack = { nav.pop() },
            actions = {
                AegisLinkText("Edit", onClick = { nav.push(FakeScreen.Edit(item.id)) })
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            FakeHeroRow(item)

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(196.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(22.dp),
                        clip = false,
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x33000000),
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(brand.color, brand.color.darken(0.35f))
                        )
                    )
                    .pressable(scaleDown = 0.99f) { revealed = !revealed }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AText(
                            "AEGIS",
                            style = AegisType.label.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                letterSpacing = 1.5.sp,
                            ),
                        )
                        Spacer(Modifier.weight(1f))
                        AText(
                            brand.name,
                            style = AegisType.rowTitle.copy(
                                color = Color.White.copy(alpha = 0.95f),
                                letterSpacing = 0.5.sp,
                            ),
                        )
                    }
                    AText(
                        if (revealed) full else masked,
                        style = AegisType.big.copy(color = Color.White, letterSpacing = 2.sp),
                        maxLines = 1,
                    )
                    Row {
                        Column(Modifier.weight(1f)) {
                            AText(
                                "CARDHOLDER",
                                style = AegisType.caption.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp,
                                ),
                            )
                            AText(
                                item.cardholder.ifBlank { "—" }.uppercase(),
                                style = AegisType.rowTitle.copy(color = Color.White),
                                maxLines = 1,
                            )
                        }
                        Column {
                            AText(
                                "EXPIRES",
                                style = AegisType.caption.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp,
                                ),
                            )
                            AText(
                                item.expiry.ifBlank { "••/••" },
                                style = AegisType.rowTitle.copy(color = Color.White),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                FakeDetailField(
                    label = "Card number",
                    value = if (revealed) full else masked,
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(digits, sensitive = true)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Security code",
                    value = if (revealed) item.securityCode else "•••",
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(item.securityCode, sensitive = true)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(label = "Expires", value = item.expiry)
                if (item.billingAddress.isNotBlank()) {
                    AegisDivider(startPadding = 16.dp)
                    FakeDetailField(label = "Billing address", value = item.billingAddress)
                }
                if (item.notes.isNotBlank()) {
                    AegisDivider(startPadding = 16.dp)
                    FakeDetailField(label = "Notes", value = item.notes)
                }
            }

            Spacer(Modifier.height(16.dp))
            AegisButton(
                text = "Copy card number",
                onClick = { vm.copyToClipboard(digits, sensitive = true) },
                icon = R.drawable.ic_copy,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SendToPcAction { sendToPc = true }
                AegisDivider(startPadding = 62.dp)
                ActionRow(
                    icon = R.drawable.ic_share_2,
                    label = "Share card details",
                    onClick = { confirmShare = true },
                )
                AegisDivider(startPadding = 62.dp)
                FakeTrashActionRow("Move to trash") {
                    state.moveToTrash(item.id)
                    vm.toast("Moved to trash")
                    nav.pop()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmShare) {
        ConfirmDialog(
            title = "Share card details?",
            message = "The shared file will include your full card number in plain text. Only share it with people you trust.",
            confirmLabel = "Share anyway",
            onConfirm = {
                confirmShare = false
                val text = buildString {
                    appendLine("Card: ${item.title}")
                    appendLine("Number: $full")
                    if (item.expiry.isNotBlank()) appendLine("Expires: ${item.expiry}")
                    if (item.cardholder.isNotBlank()) appendLine("Cardholder: ${item.cardholder}")
                }.trim()
                shareTextFile(context, safeFileName(item.title) + ".txt", "text/plain", text)
            },
            onDismiss = { confirmShare = false },
        )
    }

    if (sendToPc) {
        ShareToPcSheet(vm = vm, item = item, fake = true, onDismiss = { sendToPc = false })
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Secure note
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeNoteDetail(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState, item: NoteItem) {
    val context = LocalContext.current
    var sendToPc by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) writeTextToUri(context, uri, item.body)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NavHeader(
            onBack = { nav.pop() },
            actions = {
                AegisLinkText("Edit", onClick = { nav.push(FakeScreen.Edit(item.id)) })
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            FakeHeroRow(item)

            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.tags.forEach { tag -> TagChip(tag) }
                }
            }

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 18.dp) {
                SelectionContainer {
                    AText(
                        text = item.body.ifBlank { "No content yet." },
                        style = AegisType.body.copy(lineHeight = 24.sp),
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 16.dp) {
                    AText("Notes", style = AegisType.label)
                    Spacer(Modifier.height(6.dp))
                    AText(item.notes, style = AegisType.body)
                }
            }

            Spacer(Modifier.height(16.dp))
            AegisButton(
                text = "Copy note",
                onClick = { vm.copyToClipboard(item.body) },
                icon = R.drawable.ic_copy,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SendToPcAction { sendToPc = true }
                AegisDivider(startPadding = 62.dp)
                ActionRow(
                    icon = R.drawable.ic_file_text,
                    label = "Export to file",
                    onClick = { exportLauncher.launch(safeFileName(item.title) + ".txt") },
                )
                AegisDivider(startPadding = 62.dp)
                FakeTrashActionRow("Move to trash") {
                    state.moveToTrash(item.id)
                    vm.toast("Moved to trash")
                    nav.pop()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (sendToPc) {
        ShareToPcSheet(vm = vm, item = item, fake = true, onDismiss = { sendToPc = false })
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Identity
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeIdentityDetail(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState, item: IdentityItem) {
    val context = LocalContext.current
    var sendToPc by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NavHeader(
            onBack = { nav.pop() },
            actions = {
                AegisLinkText("Edit", onClick = { nav.push(FakeScreen.Edit(item.id)) })
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            FakeHeroRow(item)

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                FakeDetailField(
                    label = "Full name",
                    value = item.fullName,
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(item.fullName)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Email",
                    value = item.email,
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(item.email)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(
                    label = "Phone",
                    value = item.phone,
                    trailing = {
                        FakeFieldIconButton(R.drawable.ic_copy) {
                            vm.copyToClipboard(item.phone)
                        }
                    },
                )
                AegisDivider(startPadding = 16.dp)
                FakeDetailField(label = "Address", value = item.address)
                if (item.notes.isNotBlank()) {
                    AegisDivider(startPadding = 16.dp)
                    FakeDetailField(label = "Notes", value = item.notes)
                }
            }

            Spacer(Modifier.height(16.dp))
            AegisButton(
                text = "Copy all details",
                onClick = {
                    val text = buildString {
                        if (item.fullName.isNotBlank()) appendLine("Name: ${item.fullName}")
                        if (item.email.isNotBlank()) appendLine("Email: ${item.email}")
                        if (item.phone.isNotBlank()) appendLine("Phone: ${item.phone}")
                        if (item.address.isNotBlank()) appendLine("Address: ${item.address}")
                    }.trim()
                    vm.copyToClipboard(text)
                },
                icon = R.drawable.ic_copy,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(14.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SendToPcAction { sendToPc = true }
                AegisDivider(startPadding = 62.dp)
                ActionRow(
                    icon = R.drawable.ic_share_2,
                    label = "Share identity",
                    onClick = {
                        val text = buildString {
                            if (item.fullName.isNotBlank()) appendLine("Name: ${item.fullName}")
                            if (item.email.isNotBlank()) appendLine("Email: ${item.email}")
                            if (item.phone.isNotBlank()) appendLine("Phone: ${item.phone}")
                            if (item.address.isNotBlank()) appendLine("Address: ${item.address}")
                        }.trim()
                        shareText(context, item.title, text)
                    },
                )
                AegisDivider(startPadding = 62.dp)
                FakeTrashActionRow("Move to trash") {
                    state.moveToTrash(item.id)
                    vm.toast("Moved to trash")
                    nav.pop()
                }
            }

            Spacer(Modifier.height(14.dp))
            InfoNote(
                "Stored encrypted on this device. With cloud backup on, only the encrypted vault ever leaves your phone.",
                icon = R.drawable.ic_lock,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (sendToPc) {
        ShareToPcSheet(vm = vm, item = item, fake = true, onDismiss = { sendToPc = false })
    }
}
