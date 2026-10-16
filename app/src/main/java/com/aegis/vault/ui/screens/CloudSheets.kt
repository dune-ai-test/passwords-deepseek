package com.aegis.vault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.Fmt
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisDivider
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisTextField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.ActionRow
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.Spinner

/* ────────────────────────────────────────────────────────────────────────────
 * Cloud backup — setup sheet (cloud off)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun CloudSetupSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(vm.cloudServerDefault()) }
    val state by vm.cloudState.collectAsStateWithLifecycle()
    val enableConfirm by vm.cloudEnableConfirm.collectAsStateWithLifecycle()

    // Auto-close once the setup completed (toast tells the story).
    LaunchedEffect(state.enabled) {
        if (state.enabled) onDismiss()
    }

    ModalSheet(
        onDismiss = {
            vm.answerCloudEnable(false)
            onDismiss()
        }
    ) {
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
            text = "Turn on cloud backup",
            enabled = password.isNotEmpty() && server.isNotBlank(),
            onClick = { vm.cloudEnable(password, server) },
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(
            "The first upload can take a minute — your password is being stretched into keys on this device. This never changes your master password.",
            icon = R.drawable.ic_lock,
        )
    }

    if (enableConfirm) {
        ConfirmDialog(
            title = "Replace the existing backup?",
            message = "This account already has an encrypted vault in the cloud. Replace it with this device's vault? The previous copy stays in cloud history for a while.",
            confirmLabel = "Replace backup",
            onConfirm = { vm.answerCloudEnable(true) },
            onDismiss = { vm.answerCloudEnable(false) },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Cloud backup — status sheet (cloud on)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun CloudSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val state by vm.cloudState.collectAsStateWithLifecycle()
    val replaceConfirm by vm.cloudReplaceConfirm.collectAsStateWithLifecycle()
    var confirmDisable by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Closes itself when the delete flow turns cloud backup off.
    LaunchedEffect(state.enabled) {
        if (!state.enabled) onDismiss()
    }

    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Cloud backup")
        Spacer(Modifier.height(12.dp))

        AegisCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    R.drawable.ic_cloud,
                    AegisColors.blueSoft,
                    AegisColors.blue,
                    size = 42.dp,
                    corner = 13.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    AText(
                        if (state.busy) "Backing up…" else "On · end-to-end encrypted",
                        style = AegisType.rowTitle,
                    )
                    AText(
                        state.lastBackupAt?.let { "Last backup ${Fmt.relativeTime(it)}" } ?: "No backup yet",
                        style = AegisType.small,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    AText(
                        state.serverUrl,
                        style = AegisType.caption,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (state.busy) Spinner(size = 20.dp)
            }
            val error = state.error
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AegisIcon(R.drawable.ic_triangle_alert, size = 16.dp, tint = AegisColors.orange)
                    Spacer(Modifier.width(8.dp))
                    AText(
                        error,
                        style = AegisType.small,
                        color = AegisColors.orangeDeep,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AegisCard(padding = 0.dp) {
            ActionRow(
                icon = R.drawable.ic_refresh_cw,
                label = "Back up now",
                onClick = { if (!state.busy) vm.cloudBackupNow() },
            )
            AegisDivider(startPadding = 62.dp)
            ActionRow(
                icon = R.drawable.ic_x,
                label = "Turn off cloud backup",
                onClick = { confirmDisable = true },
                tint = AegisColors.orange,
                bg = AegisColors.orangeSoft,
            )
            AegisDivider(startPadding = 62.dp)
            ActionRow(
                icon = R.drawable.ic_trash_2,
                label = "Delete the cloud copy",
                onClick = { confirmDelete = true },
                tint = AegisColors.red,
                bg = AegisColors.redSoft,
            )
        }
        Spacer(Modifier.height(14.dp))
        InfoNote(
            "The server stores only encrypted data. Your master password never leaves this device.",
            icon = R.drawable.ic_lock,
        )
    }

    if (replaceConfirm) {
        ConfirmDialog(
            title = "A newer backup exists",
            message = "Another device backed up newer changes. Replace it with this device's vault? The previous version stays in cloud history.",
            confirmLabel = "Replace anyway",
            onConfirm = { vm.answerCloudReplace(true) },
            onDismiss = { vm.answerCloudReplace(false) },
        )
    }
    if (confirmDisable) {
        ConfirmDialog(
            title = "Turn off cloud backup?",
            message = "This device stops backing up. The encrypted copy stays on the server — you can still restore from it later.",
            confirmLabel = "Turn off",
            onConfirm = {
                confirmDisable = false
                vm.cloudDisable(deleteRemote = false)
                onDismiss()
            },
            onDismiss = { confirmDisable = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete the cloud copy?",
            message = "The encrypted backup is permanently deleted from the server and cloud backup turns off. This cannot be undone.",
            confirmLabel = "Delete everything",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                vm.cloudDisable(deleteRemote = true)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Restore from cloud (unlock screen)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun RestoreCloudScreen(vm: AppViewModel, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(vm.cloudServerDefault()) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NavHeader(onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(6.dp))
            IconBadge(
                R.drawable.ic_cloud,
                AegisColors.blueSoft,
                AegisColors.blue,
                size = 64.dp,
                corner = 20.dp,
                iconSize = 30.dp,
            )
            Spacer(Modifier.height(16.dp))
            AText("Restore from cloud", style = AegisType.title2, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            AText(
                "Enter the master password that was used to turn on cloud backup. The encrypted backup is downloaded and decrypted on this device.",
                style = AegisType.body.copy(color = AegisColors.inkSecondary),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            AegisPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                placeholder = "Master password",
            )
            Spacer(Modifier.height(12.dp))
            AegisTextField(
                value = server,
                onValueChange = {
                    server = it
                    error = null
                },
                label = "Server",
                leadingIcon = R.drawable.ic_cloud,
                placeholder = "https://aegis-cloud.pages.dev",
                keyboardType = KeyboardType.Uri,
            )
            val message = error
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                AText(message, style = AegisType.small, color = AegisColors.red, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            AegisButton(
                text = "Download & unlock",
                enabled = password.isNotEmpty() && server.isNotBlank(),
                onClick = { vm.restoreFromCloud(password, server) { err -> error = err } },
            )
            Spacer(Modifier.height(14.dp))
            InfoNote(
                "Restoring runs two key derivations, so it can take a minute or two on older phones. If this device already has a vault, the backup replaces it.",
                icon = R.drawable.ic_lock,
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
