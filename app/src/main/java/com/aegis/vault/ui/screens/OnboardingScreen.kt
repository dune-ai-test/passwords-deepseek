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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.CheckItem
import com.aegis.vault.ui.design.IOSSwitch
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.StrengthMeter
import kotlinx.coroutines.delay

/* ────────────────────────────────────────────────────────────────────────────
 * First launch — welcome + feature highlights
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun OnboardingScreen(onCreate: () -> Unit, onUnlock: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(42.dp))
        Box(
            Modifier
                .size(88.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false,
                    ambientColor = Color(0x40007AFF),
                    spotColor = Color(0x40007AFF),
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5E5CE6)))),
            contentAlignment = Alignment.Center,
        ) {
            AegisIcon(R.drawable.ic_key_round, size = 42.dp, tint = Color.White)
        }
        Spacer(Modifier.height(26.dp))
        AText(
            "Every password,\none tap away",
            style = AegisType.title,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        AText(
            "Aegis keeps your logins, cards and notes encrypted on this device — no accounts, no cloud.",
            style = AegisType.body.copy(color = AegisColors.inkSecondary),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        FeatureCard(
            icon = R.drawable.ic_scan_face,
            tint = AegisColors.blue,
            bg = AegisColors.blueSoft,
            title = "Biometric unlock",
            text = "Open your vault with your fingerprint or face.",
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = R.drawable.ic_lock,
            tint = AegisColors.greenDeep,
            bg = AegisColors.greenSoft,
            title = "Encrypted on device",
            text = "An AES-256 vault that never leaves your phone.",
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = R.drawable.ic_shield_check,
            tint = AegisColors.violetDeep,
            bg = AegisColors.violetSoft,
            title = "Security audit",
            text = "Spot weak, reused and old passwords instantly.",
        )
        Spacer(Modifier.height(32.dp))
        AegisButton("Create master password", onClick = onCreate)
        Spacer(Modifier.height(8.dp))
        AegisLinkText("I already have a vault", onClick = onUnlock)
        Spacer(Modifier.height(6.dp))
        AText(
            "Your master password is never stored or transmitted.",
            style = AegisType.caption,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun FeatureCard(icon: Int, tint: Color, bg: Color, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AegisColors.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, bg, tint, size = 42.dp, corner = 13.dp, iconSize = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            AText(title, style = AegisType.rowTitle)
            AText(text, style = AegisType.small, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Create the master password
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun CreateMasterScreen(vm: AppViewModel, activity: FragmentActivity, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(true) }

    val strength = remember(password) { PasswordTools.estimate(password) }
    val hasLength = password.length >= 12
    val hasCase = password.any { it.isLowerCase() } && password.any { it.isUpperCase() }
    val hasNumberOrSymbol = password.any { it.isDigit() } || password.any { !it.isLetterOrDigit() }
    val valid = hasLength && hasCase && hasNumberOrSymbol && password == confirm && password.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
    ) {
        NavHeader(onBack = onBack)
        Column(
            Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(
                R.drawable.ic_lock,
                AegisColors.blueSoft,
                AegisColors.blue,
                size = 64.dp,
                corner = 20.dp,
                iconSize = 30.dp,
            )
            Spacer(Modifier.height(16.dp))
            AText("Set your master password", style = AegisType.title2, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            AText(
                "This is the only password you'll need. Make it long and memorable.",
                style = AegisType.body.copy(color = AegisColors.inkSecondary),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            AegisPasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Master password",
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(12.dp))
            AegisPasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                placeholder = "Confirm master password",
                leadingIcon = R.drawable.ic_shield,
                imeAction = ImeAction.Done,
            )
            Spacer(Modifier.height(16.dp))
            StrengthMeter(strength)
            Spacer(Modifier.height(18.dp))
            AegisCard(padding = 16.dp) {
                CheckItem("At least 12 characters", hasLength)
                Spacer(Modifier.height(10.dp))
                CheckItem("Upper and lower case letters", hasCase)
                Spacer(Modifier.height(10.dp))
                CheckItem("A number or symbol", hasNumberOrSymbol)
                if (confirm.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    CheckItem("Passwords match", password == confirm)
                }
            }
            Spacer(Modifier.height(14.dp))
            InfoNote(
                "If you forget your master password, no one can restore your vault — the encryption has no reset.",
                icon = R.drawable.ic_bell,
            )
            Spacer(Modifier.height(18.dp))
            AegisCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        R.drawable.ic_scan_face,
                        AegisColors.blueSoft,
                        AegisColors.blue,
                        size = 40.dp,
                        corner = 12.dp,
                        iconSize = 20.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        AText("Enable biometric unlock", style = AegisType.rowTitle)
                        AText(
                            "Unlock with your fingerprint or face",
                            style = AegisType.small,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IOSSwitch(checked = biometric, onCheckedChange = { biometric = it })
                }
            }
            Spacer(Modifier.height(24.dp))
            AegisButton(
                text = "Create vault",
                onClick = { if (valid) vm.createVault(password, biometric, activity) { } },
                enabled = valid,
            )
            Spacer(Modifier.height(10.dp))
            AText("You can change this later in Settings.", style = AegisType.caption)
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Unlock an existing vault
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun UnlockScreen(vm: AppViewModel, activity: FragmentActivity) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    val bioReady = remember { vm.biometricReady() }

    fun attempt() {
        if (password.isEmpty()) return
        vm.unlock(password) { ok -> if (!ok) error = true }
    }

    LaunchedEffect(Unit) {
        if (bioReady) {
            delay(450)
            vm.beginBiometricUnlock(activity) { }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        IconBadge(
            R.drawable.ic_key_round,
            AegisColors.blueSoft,
            AegisColors.blue,
            size = 64.dp,
            corner = 20.dp,
            iconSize = 30.dp,
        )
        Spacer(Modifier.height(18.dp))
        AText("Welcome back", style = AegisType.title2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        AText(
            "Enter your master password to unlock your vault.",
            style = AegisType.body.copy(color = AegisColors.inkSecondary),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        AegisPasswordField(
            value = password,
            onValueChange = {
                password = it
                error = false
            },
            placeholder = "Master password",
            onImeAction = { attempt() },
        )
        if (error) {
            Spacer(Modifier.height(10.dp))
            AText(
                "Wrong master password — try again.",
                style = AegisType.smallSemibold.copy(color = AegisColors.red),
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        AegisButton(
            text = "Unlock",
            onClick = { attempt() },
            enabled = password.isNotEmpty(),
        )
        if (bioReady) {
            Spacer(Modifier.height(12.dp))
            AegisButton(
                text = "Unlock with biometrics",
                onClick = { vm.beginBiometricUnlock(activity) { } },
                style = AegisButtonStyle.SECONDARY,
                icon = R.drawable.ic_scan_face,
            )
        }
        Spacer(Modifier.height(14.dp))
        AegisLinkText("Forgot master password?", onClick = { showForgot = true })
        Spacer(Modifier.height(30.dp))
    }

    if (showForgot) {
        ForgotPasswordSheet(vm = vm, onDismiss = { showForgot = false })
    }
}

/* Unused import guard — Arrangement/sp are referenced above via layout wildcard usage. */
