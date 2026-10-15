package com.aegis.vault.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────────────────
 * iOS switch
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun IOSSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val width = 51.dp
    val height = 31.dp
    val knob = 27.dp
    val knobOffset by animateDpAsState(
        targetValue = if (checked) width - knob - 2.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "knob",
    )
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(height / 2))
            .background(
                when {
                    !enabled -> AegisColors.track.copy(alpha = 0.6f)
                    checked -> AegisColors.green
                    else -> AegisColors.track
                }
            )
            .pressable(enabled = enabled, scaleDown = 1f) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(knob)
                .shadow(2.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Segmented controls
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun SegmentedControl(
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AegisColors.chipBg)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) AegisColors.card else Color.Transparent)
                    .pressable(scaleDown = 0.98f) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                AText(
                    label,
                    style = AegisType.body.copy(
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold
                        else androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    color = if (selected) AegisColors.ink else AegisColors.inkSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

data class IconSegment(val icon: Int, val label: String)

@Composable
fun IconSegmentedControl(
    selectedIndex: Int,
    options: List<IconSegment>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisColors.chipBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Column(
                Modifier
                    .weight(1f)
                    .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) AegisColors.card else Color.Transparent)
                    .pressable(scaleDown = 0.97f) { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AegisIcon(
                    option.icon,
                    size = 18.dp,
                    tint = if (selected) AegisColors.blue else AegisColors.inkSecondary,
                )
                Spacer(Modifier.height(5.dp))
                AText(
                    option.label,
                    style = AegisType.caption.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = if (selected) AegisColors.ink else AegisColors.inkSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
fun AegisFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) AegisColors.blue else AegisColors.card)
            .pressable(scaleDown = 0.96f) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AText(
            label,
            style = AegisType.rowTitle.copy(fontSize = 14.sp),
            color = if (selected) Color.White else AegisColors.ink,
            maxLines = 1,
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Text fields
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AegisTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    leadingIcon: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 8,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    textStyle: TextStyle = AegisType.body,
) {
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            AText(label, style = AegisType.label, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AegisColors.card)
                .defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                AegisIcon(leadingIcon, size = 18.dp, tint = AegisColors.inkSecondary)
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    AText(placeholder, style = textStyle.copy(color = AegisColors.inkTertiary), maxLines = maxLines)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(AegisColors.blue),
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onNext = { onImeAction?.invoke() },
                        onDone = { onImeAction?.invoke() },
                        onGo = { onImeAction?.invoke() },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun AegisPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Master password",
    leadingIcon: Int? = R.drawable.ic_lock,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    trailingExtra: (@Composable () -> Unit)? = null,
) {
    var revealed by remember { mutableStateOf(false) }
    AegisTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingExtra != null) {
                    trailingExtra()
                    Spacer(Modifier.width(4.dp))
                }
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .pressable(scaleDown = 0.9f) { revealed = !revealed },
                    contentAlignment = Alignment.Center,
                ) {
                    AegisIcon(
                        if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
                        size = 18.dp,
                        tint = AegisColors.inkSecondary,
                    )
                }
            }
        },
    )
}

/** Non-editable search bar (tap → Search screen). */
@Composable
fun SearchBarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AegisColors.card)
            .pressable(scaleDown = 0.98f) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AegisIcon(R.drawable.ic_search, size = 17.dp, tint = AegisColors.inkSecondary)
        Spacer(Modifier.width(8.dp))
        AText(text, style = AegisType.body, color = AegisColors.inkTertiary)
    }
}

/** Real search input with auto-focus (Search screen). */
@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    onClear: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autoFocus) {
            delay(180)
            focusRequester.requestFocus()
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AegisColors.card)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AegisIcon(R.drawable.ic_search, size = 17.dp, tint = AegisColors.inkSecondary)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                AText(placeholder, style = AegisType.body, color = AegisColors.inkTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AegisType.body,
                cursorBrush = SolidColor(AegisColors.blue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(24.dp)
                    .pressable(scaleDown = 0.9f) { onClear?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_x, size = 14.dp, tint = AegisColors.inkSecondary)
            }
        }
    }
}

/** Requirement check row (used on the master-password screen). */
@Composable
fun CheckItem(label: String, checked: Boolean, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(
        targetValue = if (checked) AegisColors.green else AegisColors.chipBg,
        label = "checkBg",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            AegisIcon(
                R.drawable.ic_check,
                size = 12.dp,
                tint = if (checked) Color.White else AegisColors.inkTertiary,
            )
        }
        Spacer(Modifier.width(10.dp))
        AText(
            label,
            style = AegisType.small.copy(fontSize = 14.sp, color = if (checked) AegisColors.ink else AegisColors.inkSecondary),
        )
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Slider & stepper
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AegisSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val range = valueRange.endInclusive - valueRange.start
    var widthPx by remember { mutableFloatStateOf(1f) }
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange, steps) {
                fun snap(raw: Float): Float {
                    var v = valueRange.start + (raw.coerceIn(0f, 1f)) * range
                    if (steps > 0) {
                        val stepCount = steps + 1
                        val snapped = ((v - valueRange.start) / range * stepCount).roundToInt().toFloat() / stepCount
                        v = valueRange.start + snapped * range
                    }
                    return v.coerceIn(valueRange.start, valueRange.endInclusive)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onValueChange(snap(down.position.x / widthPx))
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        onValueChange(snap(change.position.x / widthPx))
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AegisColors.chipBg)
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AegisColors.blue)
        )
        Box(
            Modifier
                .offset(x = (widthDp - 24.dp) * fraction)
                .size(24.dp)
                .shadow(2.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun StepperButton(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(AegisColors.chipBg)
            .pressable(enabled = enabled, scaleDown = 0.9f) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = 16.dp, tint = if (enabled) AegisColors.ink else AegisColors.inkTertiary)
    }
}

@Composable
fun AegisStepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepperButton(R.drawable.ic_minus, minusEnabled, onMinus)
        AText(
            value,
            style = AegisType.rowTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 48.dp)
                .padding(horizontal = 6.dp),
        )
        StepperButton(R.drawable.ic_plus, plusEnabled, onPlus)
    }
}
