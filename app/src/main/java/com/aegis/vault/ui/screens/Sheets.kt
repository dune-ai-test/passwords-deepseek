package com.aegis.vault.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aegis.vault.AppViewModel
import com.aegis.vault.BuildConfig
import com.aegis.vault.R
import com.aegis.vault.data.Fmt
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisDivider
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.ActionRow
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.StrengthMeter
import com.aegis.vault.ui.design.pressable

/* ────────────────────────────────────────────────────────────────────────────
 * Auto-lock
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AutoLockSheet(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Auto-lock")
        Spacer(Modifier.height(4.dp))
        AText("Lock the vault after a period of inactivity.", style = AegisType.small)
        Spacer(Modifier.height(8.dp))
        listOf(1, 2, 5, 15, 30, 60, -1).forEach { minutes ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scaleDown = 0.98f, onClick = { onSelect(minutes) })
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AText(
                    Fmt.autoLockLabel(minutes),
                    style = AegisType.body,
                    modifier = Modifier.weight(1f),
                )
                if (selected == minutes) {
                    AegisIcon(R.drawable.ic_check, size = 18.dp, tint = AegisColors.blue)
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Backup
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun BackupSheet(
    lastBackupAt: Long?,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Backup")
        Spacer(Modifier.height(6.dp))
        AText(
            if (lastBackupAt != null) {
                "Last backup ${Fmt.relativeTime(lastBackupAt)}."
            } else {
                "Backups are encrypted copies of your vault file."
            },
            style = AegisType.small,
        )
        Spacer(Modifier.height(14.dp))
        AegisCard(padding = 0.dp) {
            ActionRow(
                icon = R.drawable.ic_share_2,
                label = "Back up now",
                onClick = onBackupNow,
            )
            AegisDivider(startPadding = 62.dp)
            ActionRow(
                icon = R.drawable.ic_rotate_ccw,
                label = "Restore from file",
                onClick = onRestore,
                tint = AegisColors.orange,
                bg = AegisColors.orangeSoft,
            )
        }
        Spacer(Modifier.height(14.dp))
        InfoNote(
            "Aegis cannot read your backups. Restoring requires the master password that was active when the backup was made.",
            icon = R.drawable.ic_lock,
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * About & help
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    ModalSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5E5CE6)))),
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_key_round, size = 30.dp, tint = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            AText("Aegis", style = AegisType.title2)
            Spacer(Modifier.height(4.dp))
            AText(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = AegisType.small,
            )
            Spacer(Modifier.height(12.dp))
            AText(
                "A zero-knowledge password manager. Everything is encrypted on this device — with cloud backup on, the server only ever stores ciphertext.",
                style = AegisType.small,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            AText(
                "Designed and built with OpenCode CLI and DeepSeek V4.1 Flash.",
                style = AegisType.caption,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            AText(
                "Inter (SIL OFL) · Lucide icons (ISC) · EFF wordlist (CC0)",
                style = AegisType.caption,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            AegisButton(
                text = "Done",
                onClick = onDismiss,
                style = AegisButtonStyle.SECONDARY,
            )
        }
    }
}

@Composable
fun HelpSheet(onDismiss: () -> Unit) {
    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Help & support")
        Spacer(Modifier.height(10.dp))
        HelpItem(
            "Can't unlock",
            "Try biometrics first. If that fails, the master password is the only way in — check caps lock and keyboard language.",
        )
        HelpItem(
            "Master password lost",
            "Aegis is zero-knowledge, so a lost master password cannot be recovered. Erase the vault and start fresh from the unlock screen.",
        )
        HelpItem(
            "Backups",
            "Settings → Export to file writes an encrypted copy. Restore it any time from Settings → Local backup.",
        )
        HelpItem(
            "Cloud backup",
            "Turn it on under Settings → Cloud backup. Your vault is encrypted before upload — even the server can't read it. Restore it on a new phone from the unlock screen.",
        )
        HelpItem(
            "Send to PC",
            "Open the Aegis share page in a browser on your computer, type the 6-character code it shows, then check that both screens display the same key. One item only, encrypted end to end.",
        )
        HelpItem(
            "Auto-lock",
            "One minute by default. Change it under Settings → Auto-lock, or disable it with Never.",
        )
        HelpItem(
            "Clipboard",
            "Passwords copied from a detail screen clear themselves from the clipboard after 60 seconds.",
        )
        Spacer(Modifier.height(10.dp))
        AegisButton(text = "Close", onClick = onDismiss, style = AegisButtonStyle.SECONDARY)
    }
}

@Composable
private fun HelpItem(title: String, text: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        AText(title, style = AegisType.rowTitle)
        AText(text, style = AegisType.small, modifier = Modifier.padding(top = 2.dp))
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Change master password
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ChangeMasterSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val strength = remember(next) { PasswordTools.estimate(next) }
    val valid = current.isNotEmpty() && next.length >= 12 && next == confirm

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Change master password")
        Spacer(Modifier.height(6.dp))
        AText("Your vault stays the same — only the way it's locked changes.", style = AegisType.small)
        Spacer(Modifier.height(14.dp))
        AegisPasswordField(
            value = current,
            onValueChange = {
                current = it
                error = null
            },
            placeholder = "Current password",
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = next,
            onValueChange = {
                next = it
                error = null
            },
            placeholder = "New password",
            leadingIcon = R.drawable.ic_lock,
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = confirm,
            onValueChange = {
                confirm = it
                error = null
            },
            placeholder = "Confirm new password",
            leadingIcon = R.drawable.ic_shield,
        )
        Spacer(Modifier.height(14.dp))
        StrengthMeter(strength)
        error?.let { message ->
            Spacer(Modifier.height(10.dp))
            AText(
                message,
                style = AegisType.smallSemibold.copy(color = AegisColors.red),
            )
        }
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = "Update master password",
            onClick = {
                vm.changeMasterPassword(current, next) { err ->
                    if (err == null) onDismiss() else error = err
                }
            },
            enabled = valid,
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Forgot master password (from the unlock screen)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ForgotPasswordSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var confirmErase by remember { mutableStateOf(false) }

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Forgot master password?")
        Spacer(Modifier.height(8.dp))
        AText(
            "Aegis is zero-knowledge: your master password is never stored, so it cannot be recovered or reset. If it's lost, the only option is to erase this vault and start over.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(16.dp))
        AegisButton(
            text = "Erase vault and start over",
            onClick = { confirmErase = true },
            style = AegisButtonStyle.DANGER,
        )
        Spacer(Modifier.height(4.dp))
        AegisButton(
            text = "Back",
            onClick = onDismiss,
            style = AegisButtonStyle.GHOST,
        )
    }

    if (confirmErase) {
        ConfirmDialog(
            title = "Erase this vault?",
            message = "All data on this device will be permanently deleted. This cannot be undone.",
            confirmLabel = "Erase everything",
            destructive = true,
            onConfirm = {
                confirmErase = false
                vm.wipeVault()
                onDismiss()
            },
            onDismiss = { confirmErase = false },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Restore backup password
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ImportPasswordSheet(
    vm: AppViewModel,
    uri: Uri,
    onImported: () -> Unit,
    onDismiss: () -> Unit,
) {
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
            onClick = {
                vm.importFrom(uri, password) { err ->
                    if (err == null) onImported() else error = err
                }
            },
            enabled = password.isNotEmpty(),
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "Restoring replaces the vault currently on this device.",
            icon = R.drawable.ic_triangle_alert,
        )
    }
}
