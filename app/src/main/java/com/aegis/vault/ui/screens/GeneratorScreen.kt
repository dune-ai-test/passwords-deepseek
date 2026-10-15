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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.AppViewModel
import com.aegis.vault.R
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.ui.Navigator
import com.aegis.vault.ui.design.AegisButton
import com.aegis.vault.ui.design.AegisCard
import com.aegis.vault.ui.design.AegisColors
import com.aegis.vault.ui.design.AegisFilterChip
import com.aegis.vault.ui.design.AegisIcon
import com.aegis.vault.ui.design.AegisSlider
import com.aegis.vault.ui.design.AegisStepper
import com.aegis.vault.ui.design.AegisType
import com.aegis.vault.ui.design.AText
import com.aegis.vault.ui.design.SegmentedControl
import com.aegis.vault.ui.design.StrengthMeter
import com.aegis.vault.ui.design.ToggleRow
import com.aegis.vault.ui.design.pressable
import kotlin.math.roundToInt

@Composable
fun GeneratorScreen(vm: AppViewModel, nav: Navigator) {
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
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 128.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AText("Generator", style = AegisType.title, modifier = Modifier.weight(1f))
            RoundIconButton(R.drawable.ic_rotate_ccw) { regenerate() }
            Spacer(Modifier.size(8.dp))
            RoundIconButton(R.drawable.ic_copy) { vm.copyToClipboard(password) }
        }

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

@Composable
private fun RoundIconButton(icon: Int, onClick: () -> Unit) {
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
