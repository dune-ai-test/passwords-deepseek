package com.aegis.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aegis.vault.R
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisButtonStyle
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisLinkText
import com.aegis.vault.ui.design.AegisPasswordField
import com.aegis.vault.ui.design.AegisSlider
import com.aegis.vault.ui.design.AegisStepper
import com.aegis.vault.ui.design.AegisTextField
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.AvatarBadge
import com.aegis.vault.ui.design.CheckItem
import com.aegis.vault.ui.design.ConfirmDialog
import com.aegis.vault.ui.design.IconBadge
import com.aegis.vault.ui.design.InfoNote
import com.aegis.vault.ui.design.ModalSheet
import com.aegis.vault.ui.design.NavHeader
import com.aegis.vault.ui.design.ProgressRing
import com.aegis.vault.ui.design.SearchInput
import com.aegis.vault.ui.design.SegmentedControl
import com.aegis.vault.ui.design.SheetTitle
import com.aegis.vault.ui.design.SmallButton
import com.aegis.vault.ui.design.Spinner
import com.aegis.vault.ui.design.StrengthMeter
import com.aegis.vault.ui.design.ToggleRow

/** Internal design QA screen — reachable via long-press on "About Aegis" (debug builds only). */
@Composable
fun DevGalleryScreen(nav: Navigator) {
    var sheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var switchOn by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(true) }
    var slider by remember { mutableStateOf(20f) }
    var segment by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 40.dp)
        ) {
            NavHeader(
                title = "Design gallery",
                subtitle = "Internal · QA only",
                onBack = { nav.pop() },
            )

            GallerySection("Buttons") {
                AegisButton("Primary action", onClick = { })
                Spacer(Modifier.height(10.dp))
                AegisButton("Secondary action", onClick = { }, style = AegisButtonStyle.SECONDARY)
                Spacer(Modifier.height(10.dp))
                AegisButton("Destructive action", onClick = { }, style = AegisButtonStyle.DANGER)
                Spacer(Modifier.height(10.dp))
                AegisButton("Disabled action", onClick = { }, enabled = false)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallButton("Small") { }
                    AegisFilterChip(label = "Chip on", selected = true, onClick = { })
                    AegisFilterChip(label = "Chip off", selected = false, onClick = { })
                }
                Spacer(Modifier.height(10.dp))
                AegisLinkText("Aegis link text", onClick = { })
            }

            GallerySection("Inputs") {
                AegisTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Standard field",
                    leadingIcon = R.drawable.ic_user,
                )
                Spacer(Modifier.height(10.dp))
                AegisPasswordField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Password field",
                )
                Spacer(Modifier.height(10.dp))
                SearchInput(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Search field",
                    autoFocus = false,
                )
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    selectedIndex = segment,
                    options = listOf("One", "Two", "Three"),
                    onSelect = { segment = it },
                )
                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = "Toggle row",
                    checked = switchOn,
                    onCheckedChange = { switchOn = it },
                    subtitle = "With a subtitle",
                    divider = false,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AText("Stepper", style = AegisType.rowTitle, modifier = Modifier.weight(1f))
                    AegisStepper(
                        value = slider.toInt().toString(),
                        onMinus = { slider -= 1f },
                        onPlus = { slider += 1f },
                    )
                }
                Spacer(Modifier.height(6.dp))
                AegisSlider(value = slider, onValueChange = { slider = it }, valueRange = 8f..64f)
            }

            GallerySection("Feedback") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallButton("Sheet") { sheet = true }
                    SmallButton("Dialog", color = AegisColors.red) { dialog = true }
                    Spinner()
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CheckItem("Checked", checked)
                    CheckItem("Unchecked", !checked)
                    SmallButton(if (checked) "Flip" else "Flip back") { checked = !checked }
                }
                Spacer(Modifier.height(14.dp))
                InfoNote("This is an info note with a soft background and an icon.")
                Spacer(Modifier.height(14.dp))
                StrengthMeter(PasswordTools.estimate("abc123"))
                Spacer(Modifier.height(10.dp))
                StrengthMeter(PasswordTools.estimate("correct-horse-battery-staple-42!"))
            }

            GallerySection("Badges & swatches") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBadge("A", AegisColors.blue)
                    AvatarBadge("B", AegisColors.violet)
                    IconBadge(R.drawable.ic_clock, AegisColors.orangeSoft, AegisColors.orange)
                    ProgressRing(
                        progress = 0.84f,
                        color = AegisColors.green,
                        size = 44.dp,
                        strokeWidth = 6.dp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AegisColors.letterPalette.forEach { color ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (sheet) {
        ModalSheet(onDismiss = { sheet = false }) {
            SheetTitle("Example sheet")
            Spacer(Modifier.height(8.dp))
            AText("Sheets slide up from the bottom with a dimmed backdrop.", style = AegisType.small)
            Spacer(Modifier.height(16.dp))
            AegisButton("Dismiss", onClick = { sheet = false })
        }
    }

    if (dialog) {
        ConfirmDialog(
            title = "Delete item?",
            message = "This is an example confirmation dialog with a destructive action.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { dialog = false },
            onDismiss = { dialog = false },
        )
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        AText(title, style = AegisType.label, modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}
