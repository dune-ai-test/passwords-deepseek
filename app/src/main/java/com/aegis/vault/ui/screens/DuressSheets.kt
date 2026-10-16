package com.aegis.vault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.Duress
import com.aegis.vault.data.Fmt
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisDivider
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.SheetTitle

/* ────────────────────────────────────────────────────────────────────────────
 * Duress code
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun DuressCodeSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val enabled = remember { vm.duressEnabled() }
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }
    val valid = code.length >= Duress.MIN_LENGTH && code == confirm

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
            onValueChange = {
                code = it
                error = null
            },
            placeholder = if (enabled) "New duress code" else "Duress code",
            leadingIcon = R.drawable.ic_shield,
        )
        Spacer(Modifier.height(10.dp))
        AegisPasswordField(
            value = confirm,
            onValueChange = {
                confirm = it
                error = null
            },
            placeholder = "Confirm duress code",
        )
        Spacer(Modifier.height(8.dp))
        AText(
            "At least ${Duress.MIN_LENGTH} characters — choose something you can't confuse with your master password.",
            style = AegisType.caption,
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
            text = if (enabled) "Update duress code" else "Set duress code",
            onClick = {
                vm.setDuressCode(code) { err ->
                    if (err == null) onDismiss() else error = err
                }
            },
            enabled = valid,
        )
        if (enabled) {
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
                vm.removeDuressCode()
                onDismiss()
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Decoy activity log
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun DecoyLogSheet(events: List<Duress.Event>, onClear: () -> Unit, onDismiss: () -> Unit) {
    ModalSheet(onDismiss = onDismiss) {
        SheetTitle("Decoy activity")
        Spacer(Modifier.height(6.dp))
        AText(
            "Every time the decoy vault is opened — with your duress code or a wrong password — it's recorded here. This list exists only in the real vault.",
            style = AegisType.small,
        )
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) {
            AText(
                "Nothing yet — the decoy has never been opened.",
                style = AegisType.small.copy(color = AegisColors.inkSecondary),
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            events.asReversed().forEachIndexed { index, event ->
                if (index > 0) AegisDivider(startPadding = 0.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        AText(
                            if (event.duress) "Duress code used" else "Wrong password",
                            style = AegisType.rowTitle,
                        )
                        AText(
                            "${Fmt.relativeTime(event.at)} · ${Fmt.date(event.at)}",
                            style = AegisType.small,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (events.isNotEmpty()) {
            AegisButton(
                text = "Clear log",
                style = AegisButtonStyle.SECONDARY,
                onClick = {
                    onClear()
                    onDismiss()
                },
            )
            Spacer(Modifier.height(10.dp))
        }
        AegisButton(text = "Close", style = AegisButtonStyle.GHOST, onClick = onDismiss)
    }
}
