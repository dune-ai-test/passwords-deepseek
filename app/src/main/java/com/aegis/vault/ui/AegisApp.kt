package com.aegis.vault.ui

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.BusyOverlay
import com.aegis.vault.ui.design.MessageToast
import com.aegis.vault.ui.design.pressable
import com.aegis.vault.ui.screens.CategoryScreen
import com.aegis.vault.ui.screens.CreateMasterScreen
import com.aegis.vault.ui.screens.DetailScreen
import com.aegis.vault.ui.screens.DevGalleryScreen
import com.aegis.vault.ui.screens.EditItemScreen
import com.aegis.vault.ui.screens.GeneratorScreen
import com.aegis.vault.ui.screens.OnboardingScreen
import com.aegis.vault.ui.screens.SearchScreen
import com.aegis.vault.ui.screens.SecurityScreen
import com.aegis.vault.ui.screens.SettingsScreen
import com.aegis.vault.ui.screens.TrashScreen
import com.aegis.vault.ui.screens.UnlockScreen
import com.aegis.vault.ui.screens.VaultScreen
import kotlinx.coroutines.delay

@Composable
fun AegisApp(vm: AppViewModel, activity: FragmentActivity) {
    val stage by vm.stage.collectAsStateWithLifecycle()
    val vault by vm.vault.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val busyText by vm.busyText.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    // Screenshot protection follows the setting (default: on).
    val secure = vault?.settings?.screenshotProtection ?: true
    LaunchedEffect(secure) {
        if (secure) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Inactivity heartbeat while unlocked.
    LaunchedEffect(stage) {
        if (stage == AppViewModel.Stage.UNLOCKED) {
            while (true) {
                delay(5_000)
                vm.tick()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(AegisColors.bg)) {
        when (stage) {
            AppViewModel.Stage.FIRST_RUN -> FirstRunFlow(vm, activity)
            AppViewModel.Stage.LOCKED -> UnlockScreen(vm, activity)
            AppViewModel.Stage.UNLOCKED -> MainShell(vm, activity)
        }
        MessageToast(
            message = message,
            onExpired = vm::clearMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (stage == AppViewModel.Stage.UNLOCKED) 110.dp else 40.dp),
        )
        BusyOverlay(visible = busy, text = busyText)
    }
}

@Composable
private fun FirstRunFlow(vm: AppViewModel, activity: FragmentActivity) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        CreateMasterScreen(vm, activity, onBack = { creating = false })
    } else {
        OnboardingScreen(
            onCreate = { creating = true },
            onUnlock = {
                if (vm.vaultExists()) {
                    vm.showUnlock()
                } else {
                    vm.toast("No vault found on this device")
                }
            },
        )
    }
}

@Composable
private fun MainShell(vm: AppViewModel, activity: FragmentActivity) {
    val nav = remember { Navigator() }
    val current = nav.current

    BackHandler(enabled = nav.depth > 1) { nav.pop() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        vm.noteInteraction()
                    }
                }
            }
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                when (nav.lastDirection) {
                    1 -> (slideInHorizontally(animationSpec = tween(240)) { it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240)) { -it / 8 } + fadeOut(tween(180)))
                    -1 -> (slideInHorizontally(animationSpec = tween(240)) { -it / 8 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(240)) { it } + fadeOut(tween(180)))
                    else -> fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                }
            },
            label = "aegisScreen",
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            when (screen) {
                Screen.Vault -> VaultScreen(vm, nav)
                is Screen.Category -> CategoryScreen(vm, nav, screen)
                is Screen.Detail -> DetailScreen(vm, nav, screen.id)
                is Screen.Edit -> EditItemScreen(vm, nav, screen)
                Screen.Generator -> GeneratorScreen(vm, nav)
                Screen.Security -> SecurityScreen(vm, nav)
                Screen.Settings -> SettingsScreen(vm, nav, activity)
                Screen.Search -> SearchScreen(vm, nav)
                Screen.Trash -> TrashScreen(vm, nav)
                Screen.DevGallery -> DevGalleryScreen(nav)
                else -> Box(Modifier.fillMaxSize())
            }
        }

        val tab = tabOf(current)
        if (tab != null) {
            AegisDock(
                current = tab,
                onTab = { target -> nav.switchTab(rootFor(target)) },
                onAdd = { nav.push(Screen.Edit()) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Floating dock: 4 tabs + blue circular add button
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
private fun AegisDock(
    current: Tab,
    onTab: (Tab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(29.dp),
                    clip = false,
                    ambientColor = Color(0x1F0B1B3A),
                    spotColor = Color(0x1F0B1B3A),
                )
                .clip(RoundedCornerShape(29.dp))
                .background(Color(0xF2FFFFFF))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockTab(Tab.PASSWORDS, R.drawable.ic_key_round, "Passwords", current, onTab, Modifier.weight(1f))
            DockTab(Tab.GENERATOR, R.drawable.ic_refresh_cw, "Generator", current, onTab, Modifier.weight(1f))
            DockTab(Tab.SECURITY, R.drawable.ic_shield_check, "Security", current, onTab, Modifier.weight(1f))
            DockTab(Tab.SETTINGS, R.drawable.ic_settings, "Settings", current, onTab, Modifier.weight(1f))
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x59007AFF),
                        spotColor = Color(0x59007AFF),
                    )
                    .clip(CircleShape)
                    .background(AegisColors.blue)
                    .pressable(scaleDown = 0.92f) { onAdd() },
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_plus, size = 20.dp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun DockTab(
    tab: Tab,
    icon: Int,
    label: String,
    current: Tab,
    onTab: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = tab == current
    Column(
        modifier = modifier
            .pressable(scaleDown = 0.95f) { onTab(tab) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(46.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) AegisColors.blueSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            AegisIcon(
                icon,
                size = 18.dp,
                tint = if (selected) AegisColors.blue else AegisColors.dockInactive,
            )
        }
        Spacer(Modifier.height(2.dp))
        AText(
            label,
            style = AegisType.tinyBold,
            color = if (selected) AegisColors.blue else AegisColors.dockInactive,
            maxLines = 1,
        )
    }
}
