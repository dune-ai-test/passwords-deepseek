package com.aegis.vault.ui.screens.fake

import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.BuildConfig
import com.aegis.vault.R
import com.aegis.vault.data.Duress
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.ItemType
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.CategoryMode
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisSlider
import com.aegis.vault.ui.design.AegisStepper
import com.aegis.vault.ui.design.AegisTextField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.AvatarBadge
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.EmptyState
import com.aegis.vault.ui.design.IOSSwitch
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ItemBadge
import com.aegis.vault.ui.design.ItemRow
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.ProgressRing
import com.aegis.vault.ui.design.SectionHeader
import com.aegis.vault.ui.design.SegmentedControl
import com.aegis.vault.ui.design.SettingsRow
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.SmallButton
import com.aegis.vault.ui.design.StrengthMeter
import com.aegis.vault.ui.design.ToggleRow
import com.aegis.vault.ui.design.pressable
import com.aegis.vault.ui.screens.AboutSheet
import com.aegis.vault.ui.screens.AutoLockSheet
import com.aegis.vault.ui.screens.BackupSheet
import com.aegis.vault.ui.screens.HelpSheet
import java.security.SecureRandom
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ────────────────────────────────────────────────────────────────────────────
 * Generator
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun FakeGeneratorScreen(vm: AppViewModel) {
    var mode by remember { mutableIntStateOf(0) } // 0 Random · 1 Memorable
    var length by remember { mutableIntStateOf(20) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var avoidLookalikes by remember { mutableStateOf(false) }
    var words by remember { mutableIntStateOf(4) }
    var separatorIndex by remember { mutableIntStateOf(0) }
    var capitalize by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }

    val separators = listOf("-", " ", ".")

    fun regenerate() {
        password = if (mode == 0) {
            vm.generateRandom(length, upper, lower, digits, symbols, avoidLookalikes)
        } else {
            vm.generateMemorable(words, separators[separatorIndex], capitalize)
        }
    }

    LaunchedEffect(mode, length, upper, lower, digits, symbols, avoidLookalikes, words, separatorIndex, capitalize) {
        regenerate()
    }

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
            AText("Generator", style = AegisType.title, modifier = Modifier.weight(1f))
            FakeRoundIconButton(R.drawable.ic_rotate_ccw) { regenerate() }
            Spacer(Modifier.size(8.dp))
            FakeRoundIconButton(R.drawable.ic_copy) { vm.copyToClipboard(password) }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 18.dp) {
                AText(
                    password.ifEmpty { "…" },
                    style = AegisType.big.copy(letterSpacing = 0.5.sp),
                )
                Spacer(Modifier.height(16.dp))
                StrengthMeter(PasswordTools.estimate(password))
            }

            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                selectedIndex = mode,
                options = listOf("Random", "Memorable"),
                onSelect = { mode = it },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(16.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                if (mode == 0) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AText("Length", style = AegisType.rowTitle, modifier = Modifier.weight(1f))
                        AegisStepper(
                            value = "$length",
                            onMinus = { length = (length - 1).coerceAtLeast(8) },
                            onPlus = { length = (length + 1).coerceAtMost(64) },
                            minusEnabled = length > 8,
                            plusEnabled = length < 64,
                        )
                    }
                    AegisSlider(
                        value = length.toFloat(),
                        onValueChange = { length = it.roundToInt() },
                        valueRange = 8f..64f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    ToggleRow(title = "Uppercase letters", checked = upper, onCheckedChange = { upper = it })
                    ToggleRow(title = "Lowercase letters", checked = lower, onCheckedChange = { lower = it })
                    ToggleRow(title = "Numbers", checked = digits, onCheckedChange = { digits = it })
                    ToggleRow(title = "Symbols", checked = symbols, onCheckedChange = { symbols = it })
                    ToggleRow(
                        title = "Avoid look-alikes",
                        checked = avoidLookalikes,
                        onCheckedChange = { avoidLookalikes = it },
                        subtitle = "Excludes i, l, 1, O and 0",
                        divider = false,
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AText("Words", style = AegisType.rowTitle, modifier = Modifier.weight(1f))
                        AegisStepper(
                            value = "$words",
                            onMinus = { words = (words - 1).coerceAtLeast(2) },
                            onPlus = { words = (words + 1).coerceAtMost(6) },
                            minusEnabled = words > 2,
                            plusEnabled = words < 6,
                        )
                    }
                    ToggleRow(title = "Capitalise words", checked = capitalize, onCheckedChange = { capitalize = it })
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AText("Separator", style = AegisType.rowTitle, modifier = Modifier.weight(1f))
                        listOf("-", "space", ".").forEachIndexed { index, label ->
                            AegisFilterChip(label = label, selected = separatorIndex == index, onClick = { separatorIndex = index })
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            AegisButton(
                text = "Generate new password",
                onClick = { regenerate() },
                icon = R.drawable.ic_refresh_cw,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            AText(
                "Generated on this device with a cryptographically secure RNG.",
                style = AegisType.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FakeRoundIconButton(icon: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AegisColors.blueSoft)
            .pressable(scaleDown = 0.9f) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = 18.dp, tint = AegisColors.blue)
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Security (audit of the decoy's own demo items)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun FakeSecurityScreen(nav: FakeNavigator, state: FakeVaultState) {
    val a = state.audit

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
            AText("Security", style = AegisType.title, modifier = Modifier.weight(1f))
            IconBadge(
                R.drawable.ic_shield_check,
                AegisColors.greenSoft,
                AegisColors.greenDeep,
                size = 40.dp,
                corner = 20.dp,
                iconSize = 20.dp,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AText("Security score", style = AegisType.label)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AText("${a.score}", style = AegisType.score)
                            Spacer(Modifier.width(10.dp))
                            FakeScoreChip(a.scoreLabel, a.score)
                        }
                        Spacer(Modifier.height(8.dp))
                        AText(
                            if (a.fixesNeeded == 0) {
                                "No issues found — your vault looks great."
                            } else {
                                "${a.fixesNeeded} ${if (a.fixesNeeded == 1) "issue" else "issues"} could be improved."
                            },
                            style = AegisType.small,
                        )
                        Spacer(Modifier.height(2.dp))
                        AText("Checked locally just now", style = AegisType.caption)
                    }
                    Spacer(Modifier.width(14.dp))
                    ProgressRing(
                        progress = a.score / 100f,
                        color = fakeScoreColor(a.score),
                        size = 74.dp,
                        strokeWidth = 8.dp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FakeStatTile(
                    count = a.weak.size,
                    label = "Weak",
                    icon = R.drawable.ic_triangle_alert,
                    tint = AegisColors.orange,
                    bg = AegisColors.orangeSoft,
                    modifier = Modifier.weight(1f),
                )
                FakeStatTile(
                    count = a.reusedAccountCount,
                    label = "Reused",
                    icon = R.drawable.ic_copy,
                    tint = AegisColors.red,
                    bg = AegisColors.redSoft,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FakeStatTile(
                    count = a.old.size,
                    label = "Old",
                    icon = R.drawable.ic_clock,
                    tint = AegisColors.violetDeep,
                    bg = AegisColors.violetSoft,
                    modifier = Modifier.weight(1f),
                )
                FakeStatTile(
                    count = a.strongCount,
                    label = "Strong",
                    icon = R.drawable.ic_shield_check,
                    tint = AegisColors.greenDeep,
                    bg = AegisColors.greenSoft,
                    modifier = Modifier.weight(1f),
                )
            }

            if (a.reuseGroups.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                val group = a.reuseGroups.first()
                AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            R.drawable.ic_copy,
                            AegisColors.redSoft,
                            AegisColors.red,
                            size = 40.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            AText("Reuse detected", style = AegisType.rowTitle)
                            AText(
                                "\"${group.password}\" is shared by ${group.items.size} accounts",
                                style = AegisType.small,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        SmallButton("Fix") {
                            nav.push(FakeScreen.Category(CategoryMode.REUSED, ItemType.LOGIN))
                        }
                    }
                }
            }

            if (a.weak.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Weak passwords",
                    action = "Review",
                    onAction = { nav.push(FakeScreen.Category(CategoryMode.WEAK, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    a.weak.take(5).forEach { item ->
                        ItemRow(
                            item = item,
                            subtitle = "Password is too simple",
                            onClick = { nav.push(FakeScreen.Detail(item.id)) },
                        )
                    }
                }
            }

            if (a.old.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = "Old passwords",
                    action = "Review",
                    onAction = { nav.push(FakeScreen.Category(CategoryMode.OLD, ItemType.LOGIN)) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    a.old.take(5).forEach { item ->
                        ItemRow(
                            item = item,
                            subtitle = fakeOldSubtitle(item.updatedAt),
                            onClick = { nav.push(FakeScreen.Detail(item.id)) },
                        )
                    }
                }
            }

            if (a.fixesNeeded == 0) {
                Spacer(Modifier.height(24.dp))
                InfoNote(
                    text = "All clear — no weak, reused or old passwords were found.",
                    icon = R.drawable.ic_check,
                    tint = AegisColors.greenDeep,
                    bg = AegisColors.greenSoft,
                    textColor = AegisColors.greenDeep,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            InfoNote(
                text = "The audit runs entirely on this device — your passwords are never sent anywhere.",
                icon = R.drawable.ic_lock,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun fakeOldSubtitle(updatedAt: Long): String {
    val years = ((System.currentTimeMillis() - updatedAt) / (365L * 24 * 60 * 60 * 1000)).coerceAtLeast(1L)
    return "Unchanged for $years ${if (years == 1L) "year" else "years"}"
}

private fun fakeScoreColor(score: Int): Color = when {
    score >= 90 -> AegisColors.green
    score >= 75 -> AegisColors.greenDeep
    score >= 50 -> AegisColors.orange
    else -> AegisColors.red
}

@Composable
private fun FakeScoreChip(label: String, score: Int) {
    val color = fakeScoreColor(score)
    Box(
        Modifier
            .padding(bottom = 7.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        AText(label, style = AegisType.smallSemibold.copy(color = color))
    }
}

@Composable
private fun FakeStatTile(
    count: Int,
    label: String,
    icon: Int,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    AegisCard(modifier = modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, bg, tint, size = 36.dp, corner = 11.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                AText("$count", style = AegisType.big.copy(fontSize = 20.sp))
                AText(label, style = AegisType.small)
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Trash
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun FakeTrashScreen(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState) {
    val items = state.trashedItems
    var confirmEmpty by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<VaultItem?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 148.dp)
    ) {
        NavHeader(
            title = "Trash",
            subtitle = if (items.isEmpty()) {
                "Empty"
            } else {
                "${items.size} ${if (items.size == 1) "item" else "items"}"
            },
            onBack = { nav.pop() },
            actions = {
                if (items.isNotEmpty()) {
                    AegisLinkText(
                        "Empty",
                        onClick = { confirmEmpty = true },
                        color = AegisColors.red,
                    )
                }
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (items.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_trash_2,
                    title = "Trash is empty",
                    message = "Deleted items appear here before they're gone for good.",
                )
            } else {
                items.forEach { item ->
                    FakeTrashRow(
                        item = item,
                        onRestore = {
                            state.restore(item.id)
                            vm.toast("Restored")
                        },
                        onDelete = { pendingDelete = item },
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    val victim = pendingDelete
    if (victim != null) {
        ConfirmDialog(
            title = "Delete forever?",
            message = "\"${victim.title.ifBlank { "Untitled" }}\" will be permanently deleted. This can't be undone.",
            confirmLabel = "Delete forever",
            destructive = true,
            onConfirm = {
                pendingDelete = null
                state.deleteForever(victim.id)
                vm.toast("Deleted")
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Empty the trash?",
            message = "${items.size} ${if (items.size == 1) "item" else "items"} will be permanently deleted.",
            confirmLabel = "Delete forever",
            destructive = true,
            onConfirm = {
                confirmEmpty = false
                state.emptyTrash()
                vm.toast("Trash emptied")
            },
            onDismiss = { confirmEmpty = false },
        )
    }
}

@Composable
private fun FakeTrashRow(
    item: VaultItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AegisColors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemBadge(item)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AText(item.title.ifBlank { "Untitled" }, style = AegisType.rowTitle, maxLines = 1)
            AText(
                "Deleted ${Fmt.relativeTime(item.trashedAt ?: item.updatedAt)}",
                style = AegisType.small,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        FakeRoundAction(R.drawable.ic_rotate_ccw, AegisColors.blue, onRestore)
        Spacer(Modifier.width(6.dp))
        FakeRoundAction(R.drawable.ic_trash_2, AegisColors.red, onDelete)
    }
}

@Composable
private fun FakeRoundAction(icon: Int, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .pressable(scaleDown = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = 16.dp, tint = tint)
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Settings
 * ──────────────────────────────────────────────────────────────────────────── */

private enum class FakeSheet { AUTOLOCK, BACKUP, CLOUD, ABOUT, HELP, MASTER, DURESS, DECOY_LOG, IMPORT_PASSWORD }

@Composable
fun FakeSettingsScreen(vm: AppViewModel, nav: FakeNavigator, state: FakeVaultState) {
    val settings by vm.decoySettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sheet by remember { mutableStateOf<FakeSheet?>(null) }
    var confirmErase by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = try {
                    val bytes = ByteArray(3200).also { SecureRandom().nextBytes(it) }
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    true
                } catch (e: Exception) {
                    false
                }
                if (ok) {
                    vm.updateDecoySettings { it.copy(lastBackupAt = System.currentTimeMillis()) }
                    vm.toast("Encrypted backup saved")
                } else {
                    vm.toast("Export failed")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            sheet = FakeSheet.IMPORT_PASSWORD
        }
    }

    val autofillManager = remember { context.getSystemService(AutofillManager::class.java) }
    val autofillOn = autofillManager?.hasEnabledAutofillServices() == true
    val autofillSupported = autofillManager?.isAutofillSupported == true

    fun launchExport() {
        exportLauncher.launch("aegis-backup-${System.currentTimeMillis()}.aegis")
    }

    fun toggleBiometric(target: Boolean) {
        if (target && !vm.canUseBiometrics()) {
            vm.toast("No biometrics enrolled on this device")
            return
        }
        vm.updateDecoySettings { it.copy(biometricEnabled = target) }
        vm.toast(if (target) "Biometric unlock enabled" else "Biometric unlock disabled")
    }

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
            AText("Settings", style = AegisType.title, modifier = Modifier.weight(1f))
            AvatarBadge("A", AegisColors.violet, size = 40.dp, corner = 20.dp)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 148.dp)
        ) {
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge("A", AegisColors.violet, size = 48.dp, corner = 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        AText("Local profile", style = AegisType.rowTitle)
                        AText(
                            "No account needed — your data stays on this device",
                            style = AegisType.small,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_clock,
                    title = "Auto-lock",
                    value = Fmt.autoLockLabel(settings.autoLockMinutes),
                    onClick = { sheet = FakeSheet.AUTOLOCK },
                )
                SettingsRow(
                    icon = R.drawable.ic_scan_face,
                    title = "Biometric unlock",
                    subtitle = if (vm.canUseBiometrics()) null else "No biometrics enrolled on this device",
                    onClick = { toggleBiometric(!settings.biometricEnabled) },
                    trailing = {
                        IOSSwitch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { checked -> toggleBiometric(checked) },
                        )
                    },
                )
                SettingsRow(
                    icon = R.drawable.ic_lock,
                    title = "Master password",
                    subtitle = "Change it regularly",
                    onClick = { sheet = FakeSheet.MASTER },
                )
                SettingsRow(
                    icon = R.drawable.ic_eye_off,
                    title = "Screenshot protection",
                    subtitle = "Hide contents in the app switcher",
                    onClick = {
                        vm.updateDecoySettings { it.copy(screenshotProtection = !settings.screenshotProtection) }
                    },
                    trailing = {
                        IOSSwitch(
                            checked = settings.screenshotProtection,
                            onCheckedChange = { enabled ->
                                vm.updateDecoySettings { it.copy(screenshotProtection = enabled) }
                            },
                        )
                    },
                    divider = autofillSupported,
                )
                if (autofillSupported) {
                    SettingsRow(
                        icon = R.drawable.ic_pencil,
                        title = "Autofill",
                        value = if (autofillOn) "On" else "Off",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE))
                            } catch (e: Exception) {
                                vm.toast("Autofill settings aren't available here")
                            }
                        },
                        divider = false,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_shield,
                    title = "Duress code",
                    subtitle = "A second code that opens the decoy vault",
                    value = if (state.duressConfigured) "Set" else "Off",
                    onClick = { sheet = FakeSheet.DURESS },
                )
                SettingsRow(
                    icon = R.drawable.ic_triangle_alert,
                    title = "Decoy activity",
                    value = "Never",
                    onClick = { sheet = FakeSheet.DECOY_LOG },
                    divider = false,
                )
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_share_2,
                    title = "Export to file",
                    subtitle = "Save an encrypted backup",
                    onClick = { launchExport() },
                )
                SettingsRow(
                    icon = R.drawable.ic_clock,
                    title = "Local backup",
                    value = settings.lastBackupAt?.let { Fmt.relativeTime(it) } ?: "Never",
                    onClick = { sheet = FakeSheet.BACKUP },
                )
                SettingsRow(
                    icon = R.drawable.ic_cloud,
                    title = "Cloud backup",
                    subtitle = "End-to-end encrypted",
                    value = "Off",
                    onClick = { sheet = FakeSheet.CLOUD },
                )
                SettingsRow(
                    icon = R.drawable.ic_hard_drive,
                    title = "Storage",
                    subtitle = "This device · encrypted",
                    value = Fmt.formatBytes(FakeVaultData.APPROX_BYTES),
                )
                SettingsRow(
                    icon = R.drawable.ic_trash_2,
                    title = "Trash",
                    subtitle = if (state.trashedItems.isEmpty()) "Empty" else "${state.trashedItems.size} ${if (state.trashedItems.size == 1) "item" else "items"}",
                    onClick = { nav.push(FakeScreen.Trash) },
                    divider = false,
                )
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_bell,
                    title = "Help & support",
                    onClick = { sheet = FakeSheet.HELP },
                )
                SettingsRow(
                    icon = R.drawable.ic_shield_check,
                    title = "About Aegis",
                    value = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = { sheet = FakeSheet.ABOUT },
                    divider = false,
                )
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_trash_2,
                    title = "Erase vault",
                    subtitle = "Delete everything from this device",
                    destructive = true,
                    onClick = { confirmErase = true },
                    divider = false,
                )
            }

            Spacer(Modifier.height(16.dp))
            AText(
                "Aegis ${BuildConfig.VERSION_NAME} · Built with OpenCode CLI and DeepSeek V4.1 Flash",
                style = AegisType.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    when (sheet) {
        FakeSheet.AUTOLOCK -> AutoLockSheet(
            selected = settings.autoLockMinutes,
            onSelect = { minutes ->
                vm.updateDecoySettings { it.copy(autoLockMinutes = minutes) }
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        FakeSheet.BACKUP -> BackupSheet(
            lastBackupAt = settings.lastBackupAt,
            onBackupNow = {
                sheet = null
                launchExport()
            },
            onRestore = {
                sheet = null
                importLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { sheet = null },
        )
        FakeSheet.CLOUD -> FakeCloudSheet(vm = vm, onDismiss = { sheet = null })
        FakeSheet.ABOUT -> AboutSheet(onDismiss = { sheet = null })
        FakeSheet.HELP -> HelpSheet(onDismiss = { sheet = null })
        FakeSheet.MASTER -> FakeChangeMasterSheet(vm = vm, onDismiss = { sheet = null })
        FakeSheet.DURESS -> FakeDuressSheet(vm = vm, state = state, onDismiss = { sheet = null })
        FakeSheet.DECOY_LOG -> FakeDecoyLogSheet(onDismiss = { sheet = null })
        FakeSheet.IMPORT_PASSWORD -> FakeImportPasswordSheet(onDismiss = { sheet = null })
        null -> {}
    }

    if (confirmErase) {
        ConfirmDialog(
            title = "Erase this vault?",
            message = "All logins, cards and notes will be permanently deleted from this device. This cannot be undone.",
            confirmLabel = "Erase everything",
            destructive = true,
            onConfirm = {
                confirmErase = false
                // Mirrors the real erase flow. The decoy just locks — the real
                // vault is untouched and still opens with the master password.
                vm.toast("Vault erased from this device")
                vm.lock()
            },
            onDismiss = { confirmErase = false },
        )
    }
}

@Composable
private fun FakeChangeMasterSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val strength = remember(next) { PasswordTools.estimate(next) }
    val valid = current.isNotEmpty() && next.length >= 12 && next == confirm

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Change master password")
        Spacer(Modifier.height(6.dp))
        AText("Your vault stays the same — only the way it's locked changes.", style = AegisType.small)
        Spacer(Modifier.height(14.dp))
        AegisPasswordField(
            value = current,
            onValueChange = { current = it },
            placeholder = "Current password",
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = next,
            onValueChange = { next = it },
            placeholder = "New password",
            leadingIcon = R.drawable.ic_lock,
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "Confirm new password",
            leadingIcon = R.drawable.ic_shield,
        )
        Spacer(Modifier.height(14.dp))
        StrengthMeter(strength)
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = "Update master password",
            onClick = {
                vm.toast("Master password updated")
                onDismiss()
            },
            enabled = valid,
        )
    }
}

@Composable
private fun FakeImportPasswordSheet(onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Restore backup")
        Spacer(Modifier.height(6.dp))
        AText(
            "Enter the master password that was active when this backup was created.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(14.dp))
        AegisPasswordField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            placeholder = "Backup password",
        )
        error?.let { message ->
            Spacer(Modifier.height(10.dp))
            AText(
                message,
                style = AegisType.smallSemibold.copy(color = AegisColors.red),
            )
        }
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = "Restore backup",
            onClick = { error = "Wrong password for this backup — or the file is damaged" },
            enabled = password.isNotEmpty(),
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "Restoring replaces the vault currently on this device.",
            icon = R.drawable.ic_triangle_alert,
        )
    }
}

/**
 * Decoy twin of the real cloud setup. Identical look and wording, but the
 * "server" is the phone itself: after a short pause the attempt fails the way
 * a dead connection would — nothing is ever registered or uploaded.
 */
@Composable
private fun FakeCloudSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("https://aegis-cloud.pages.dev") }
    var connecting by remember { mutableStateOf(false) }

    if (connecting) {
        LaunchedEffect(Unit) {
            delay(1200)
            connecting = false
            vm.toast("Couldn't reach the server. Check your connection and try again.")
        }
    }

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Cloud backup")
        Spacer(Modifier.height(8.dp))
        AText(
            "Your vault is encrypted on this device before it is uploaded — the server only ever stores ciphertext and cannot read your data.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(16.dp))
        AegisPasswordField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Master password",
        )
        Spacer(Modifier.height(12.dp))
        AegisTextField(
            value = server,
            onValueChange = { server = it },
            label = "Server",
            leadingIcon = R.drawable.ic_cloud,
            placeholder = "https://aegis-cloud.pages.dev",
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
        )
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = if (connecting) "Connecting…" else "Turn on cloud backup",
            enabled = password.isNotEmpty() && server.isNotBlank() && !connecting,
            onClick = { connecting = true },
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "The first upload can take a minute — your password is being stretched into keys on this device. This never changes your master password.",
            icon = R.drawable.ic_lock,
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Duress code (parity UI — nothing persists, no unlock consults it)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeDuressSheet(vm: AppViewModel, state: FakeVaultState, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val valid = code.length >= Duress.MIN_LENGTH && code == confirm && !confirming

    if (confirming) {
        LaunchedEffect(Unit) {
            delay(900)
            confirming = false
            val replacing = state.duressConfigured
            state.duressConfigured = true
            vm.toast(if (replacing) "Duress code updated" else "Duress code set")
            onDismiss()
        }
    }

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Duress code")
        Spacer(Modifier.height(6.dp))
        AText(
            "If you're ever forced to unlock the vault, enter this code instead of your master password. It opens the decoy vault — your real data stays locked, and every use is recorded under Decoy activity.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(14.dp))
        AegisPasswordField(
            value = code,
            onValueChange = { code = it },
            placeholder = if (state.duressConfigured) "New duress code" else "Duress code",
            leadingIcon = R.drawable.ic_shield,
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "Confirm duress code",
        )
        Spacer(Modifier.height(8.dp))
        AText(
            "At least ${Duress.MIN_LENGTH} characters — choose something you can't confuse with your master password.",
            style = AegisType.caption,
        )
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = when {
                confirming -> "Saving…"
                state.duressConfigured -> "Update duress code"
                else -> "Set duress code"
            },
            onClick = { confirming = true },
            enabled = valid,
        )
        if (state.duressConfigured) {
            Spacer(Modifier.height(10.dp))
            AegisButton(
                text = "Remove duress code",
                style = AegisButtonStyle.GHOST,
                onClick = { confirmRemove = true },
            )
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove the duress code?",
            message = "A wrong password will still open the decoy vault afterwards — only the registered code stops working.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = {
                confirmRemove = false
                state.duressConfigured = false
                vm.toast("Duress code removed")
                onDismiss()
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Decoy activity (parity UI — the decoy never shows its own log)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FakeDecoyLogSheet(onDismiss: () -> Unit) {
    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Decoy activity")
        Spacer(Modifier.height(6.dp))
        AText(
            "Every time the decoy vault is opened — with your duress code or a wrong password — it's recorded here. This list exists only in the real vault.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(10.dp))
        AText(
            "Nothing yet — the decoy has never been opened.",
            style = AegisType.small.copy(color = AegisColors.inkSecondary),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        AegisButton(text = "Close", style = AegisButtonStyle.GHOST, onClick = onDismiss)
    }
}
