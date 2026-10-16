package com.aegis.vault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.VaultItem
import com.aegis.vault.ui.design.ActionRow
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisTextField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.Spinner
import kotlinx.coroutines.delay

/* ────────────────────────────────────────────────────────────────────────────
 * "Send to PC" — one-time, end-to-end encrypted handoff to the browser page
 * ──────────────────────────────────────────────────────────────────────────── */

/** Action row for the detail screens; the sheet itself is hosted by the screen. */
@Composable
fun SendToPcAction(onOpen: () -> Unit) {
    ActionRow(
        icon = R.drawable.ic_send,
        label = "Send to PC",
        onClick = onOpen,
    )
}

private enum class ShareStep { INPUT, CONNECTING, CONFIRM, SENDING, DONE }

/**
 * The whole handoff: enter the code the PC shows → compare the short key
 * fingerprint on both screens → encrypt just this item and send it.
 *
 * [fake] renders the decoy variant: identical UI, but the "server" answers
 * are simulated locally and nothing ever leaves the app.
 */
@Composable
fun ShareToPcSheet(
    vm: AppViewModel,
    item: VaultItem,
    onDismiss: () -> Unit,
    fake: Boolean = false,
) {
    var step by remember { mutableStateOf(ShareStep.INPUT) }
    var code by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }

    if (fake && step == ShareStep.CONNECTING) {
        LaunchedEffect(ShareStep.CONNECTING) {
            delay(700)
            fingerprint = fakeFingerprint()
            step = ShareStep.CONFIRM
        }
    }
    if (fake && step == ShareStep.SENDING) {
        LaunchedEffect(ShareStep.SENDING) {
            delay(900)
            step = ShareStep.DONE
        }
    }

    fun startLookup() {
        step = ShareStep.CONNECTING
        if (fake) return
        vm.shareToPcLookup(code) { fp, err ->
            if (fp != null) {
                fingerprint = fp
                step = ShareStep.CONFIRM
            } else {
                step = ShareStep.INPUT
                if (err != null) vm.toast(err)
            }
        }
    }

    fun startSend() {
        step = ShareStep.SENDING
        if (fake) return
        vm.shareToPcSend(item) { err ->
            if (err == null) {
                step = ShareStep.DONE
            } else {
                step = ShareStep.INPUT
                vm.toast(err)
            }
        }
    }

    ModalSheet(
        onDismiss = {
            vm.shareCancel()
            onDismiss()
        }
    ) {
        when (step) {
            ShareStep.INPUT -> {
                SheetTitle("Send to PC")
                Spacer(Modifier.height(8.dp))
                AText(
                    "Open the Aegis share page on your computer, then type the 6-character code it shows.",
                    style = AegisType.small,
                )
                Spacer(Modifier.height(16.dp))
                AegisTextField(
                    value = code,
                    onValueChange = { raw ->
                        code = raw.filter { it.isLetterOrDigit() }.uppercase().take(6)
                    },
                    placeholder = "ABC123",
                    leadingIcon = R.drawable.ic_send,
                    textStyle = AegisType.body.copy(letterSpacing = 3.sp),
                    imeAction = ImeAction.Done,
                    onImeAction = { if (code.length == 6) startLookup() },
                )
                Spacer(Modifier.height(16.dp))
                AegisButton(
                    text = "Connect",
                    enabled = code.length == 6,
                    onClick = { startLookup() },
                )
                Spacer(Modifier.height(12.dp))
                InfoNote(
                    "Only this one item is sent, encrypted for your PC. The code works once and expires after 3 minutes.",
                    icon = R.drawable.ic_lock,
                )
            }

            ShareStep.CONNECTING -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spinner()
                    Spacer(Modifier.height(14.dp))
                    AText("Looking up the code…", style = AegisType.small)
                }
            }

            ShareStep.CONFIRM -> {
                SheetTitle("Confirm the code")
                Spacer(Modifier.height(8.dp))
                AText("Does your PC show this same key?", style = AegisType.small)
                Spacer(Modifier.height(12.dp))
                AegisCard(padding = 18.dp) {
                    AText(
                        fingerprint,
                        style = AegisType.title2.copy(letterSpacing = 3.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                InfoNote(
                    "If the key on your PC is different, cancel — someone may be trying to intercept the handoff.",
                )
                Spacer(Modifier.height(16.dp))
                AegisButton(text = "Send securely", onClick = { startSend() })
                Spacer(Modifier.height(8.dp))
                AegisButton(
                    text = "Cancel",
                    style = AegisButtonStyle.GHOST,
                    onClick = {
                        vm.shareCancel()
                        onDismiss()
                    },
                )
            }

            ShareStep.SENDING -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spinner()
                    Spacer(Modifier.height(14.dp))
                    AText("Encrypting & sending…", style = AegisType.small)
                }
            }

            ShareStep.DONE -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconBadge(
                        R.drawable.ic_check,
                        AegisColors.greenSoft,
                        AegisColors.greenDeep,
                        size = 56.dp,
                        corner = 18.dp,
                        iconSize = 26.dp,
                    )
                    Spacer(Modifier.height(14.dp))
                    AText("Sent", style = AegisType.heading)
                    Spacer(Modifier.height(6.dp))
                    AText(
                        "Your PC can fetch it once — it expires in 3 minutes and clears itself after 60 seconds.",
                        style = AegisType.small,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    AegisButton(text = "Done", onClick = { onDismiss() })
                }
            }
        }
    }
}

/** Decoy path only: a plausible-looking key that never existed. */
private fun fakeFingerprint(): String {
    val hex = "0123456789ABCDEF"
    val builder = StringBuilder("XXXX-XXXX")
    repeat(4) { i -> builder.setCharAt(i, hex[kotlin.random.Random.nextInt(16)]) }
    repeat(4) { i -> builder.setCharAt(i + 5, hex[kotlin.random.Random.nextInt(16)]) }
    return builder.toString()
}
