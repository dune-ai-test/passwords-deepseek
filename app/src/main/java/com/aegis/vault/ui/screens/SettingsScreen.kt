package com.aegis.vault.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.BuildConfig
import com.aegis.vault.R
import com.aegis.vault.data.Fmt
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.Screen
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.AvatarBadge
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.IOSSwitch
import com.aegis.vault.ui.design.SettingsRow

private enum class SettingsSheet { AUTOLOCK, BACKUP, ABOUT, HELP, MASTER, IMPORT_PASSWORD, CLOUD_SETUP, CLOUD }

@Composable
fun SettingsScreen(vm: AppViewModel, nav: Navigator, activity: FragmentActivity) {
    val vaultState by vm.vault.collectAsStateWithLifecycle()
    val data = vaultState ?: return
    val settings = data.settings
    val vaultBytes by vm.vaultBytes.collectAsStateWithLifecycle()
    val cloudState by vm.cloudState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) vm.exportTo(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImport = uri
            sheet = SettingsSheet.IMPORT_PASSWORD
        }
    }

    val autofillManager = remember { context.getSystemService(AutofillManager::class.java) }
    val autofillOn = autofillManager?.hasEnabledAutofillServices() == true
    val autofillSupported = autofillManager?.isAutofillSupported == true

    fun launchExport() {
        exportLauncher.launch("aegis-backup-${System.currentTimeMillis()}.aegis")
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
                            if (cloudState.enabled) "No account needed — end-to-end encrypted"
                            else "No account needed — your data stays on this device",
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
                    onClick = { sheet = SettingsSheet.AUTOLOCK },
                )
                SettingsRow(
                    icon = R.drawable.ic_scan_face,
                    title = "Biometric unlock",
                    subtitle = if (vm.canUseBiometrics()) null else "No biometrics enrolled on this device",
                    onClick = {
                        if (settings.biometricEnabled) vm.disableBiometric()
                        else vm.enableBiometric(activity) { }
                    },
                    trailing = {
                        IOSSwitch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { checked ->
                                if (checked) vm.enableBiometric(activity) { } else vm.disableBiometric()
                            },
                        )
                    },
                )
                SettingsRow(
                    icon = R.drawable.ic_lock,
                    title = "Master password",
                    subtitle = "Change it regularly",
                    onClick = { sheet = SettingsSheet.MASTER },
                )
                SettingsRow(
                    icon = R.drawable.ic_eye_off,
                    title = "Screenshot protection",
                    subtitle = "Hide contents in the app switcher",
                    onClick = {
                        vm.updateSettings { it.copy(screenshotProtection = !settings.screenshotProtection) }
                    },
                    trailing = {
                        IOSSwitch(
                            checked = settings.screenshotProtection,
                            onCheckedChange = { enabled ->
                                vm.updateSettings { it.copy(screenshotProtection = enabled) }
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
                    icon = R.drawable.ic_share_2,
                    title = "Export to file",
                    subtitle = "Save an encrypted backup",
                    onClick = { launchExport() },
                )
                SettingsRow(
                    icon = R.drawable.ic_clock,
                    title = "Local backup",
                    value = settings.lastBackupAt?.let { Fmt.relativeTime(it) } ?: "Never",
                    onClick = { sheet = SettingsSheet.BACKUP },
                )
                SettingsRow(
                    icon = R.drawable.ic_cloud,
                    title = "Cloud backup",
                    subtitle = "End-to-end encrypted",
                    value = if (cloudState.enabled) "On" else "Off",
                    onClick = {
                        sheet = if (cloudState.enabled) SettingsSheet.CLOUD else SettingsSheet.CLOUD_SETUP
                    },
                )
                SettingsRow(
                    icon = R.drawable.ic_hard_drive,
                    title = "Storage",
                    subtitle = "This device · encrypted",
                    value = Fmt.formatBytes(vaultBytes),
                )
                SettingsRow(
                    icon = R.drawable.ic_trash_2,
                    title = "Trash",
                    subtitle = if (data.trashedItems.isEmpty()) "Empty" else "${data.trashedItems.size} ${if (data.trashedItems.size == 1) "item" else "items"}",
                    onClick = { nav.push(Screen.Trash) },
                    divider = false,
                )
            }

            Spacer(Modifier.height(20.dp))
            AegisCard(modifier = Modifier.padding(horizontal = 20.dp), padding = 0.dp) {
                SettingsRow(
                    icon = R.drawable.ic_bell,
                    title = "Help & support",
                    onClick = { sheet = SettingsSheet.HELP },
                )
                SettingsRow(
                    icon = R.drawable.ic_shield_check,
                    title = "About Aegis",
                    value = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = { sheet = SettingsSheet.ABOUT },
                    onLongClick = { nav.push(Screen.DevGallery) },
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
        SettingsSheet.AUTOLOCK -> AutoLockSheet(
            selected = settings.autoLockMinutes,
            onSelect = { minutes ->
                vm.updateSettings { it.copy(autoLockMinutes = minutes) }
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        SettingsSheet.BACKUP -> BackupSheet(
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
        SettingsSheet.ABOUT -> AboutSheet(onDismiss = { sheet = null })
        SettingsSheet.HELP -> HelpSheet(onDismiss = { sheet = null })
        SettingsSheet.MASTER -> ChangeMasterSheet(vm = vm, onDismiss = { sheet = null })
        SettingsSheet.CLOUD_SETUP -> CloudSetupSheet(vm = vm, onDismiss = { sheet = null })
        SettingsSheet.CLOUD -> CloudSheet(vm = vm, onDismiss = { sheet = null })
        SettingsSheet.IMPORT_PASSWORD -> {
            val uri = pendingImport
            if (uri != null) {
                ImportPasswordSheet(
                    vm = vm,
                    uri = uri,
                    onImported = {
                        sheet = null
                        pendingImport = null
                        nav.switchTab(Screen.Vault)
                    },
                    onDismiss = {
                        sheet = null
                        pendingImport = null
                    },
                )
            }
        }
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
                vm.wipeVault()
            },
            onDismiss = { confirmErase = false },
        )
    }
}
