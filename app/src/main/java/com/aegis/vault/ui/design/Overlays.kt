package com.aegis.vault.ui.design

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* ────────────────────────────────────────────────────────────────────────────
 * Bottom sheet (custom — no Material)
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ModalSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "sheetProgress",
    )
    LaunchedEffect(Unit) { visible = true }
    BackHandler(onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f * progress))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - progress) * size.height }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AegisColors.card)
                .pointerInput(Unit) { detectTapGestures { /* swallow */ } }
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 22.dp)
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AegisColors.inkTertiary)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    AText(text, style = AegisType.heading.copy(fontSize = 18.sp), modifier = modifier)
}

/* ────────────────────────────────────────────────────────────────────────────
 * Confirm dialog
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AegisColors.card)
                .pointerInput(Unit) { detectTapGestures { /* swallow */ } }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AText(title, style = AegisType.heading.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            AText(
                message,
                style = AegisType.small.copy(fontSize = 14.sp, lineHeight = 20.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            AegisButton(
                text = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = if (destructive) AegisButtonStyle.DANGER else AegisButtonStyle.PRIMARY,
            )
            Spacer(Modifier.height(4.dp))
            AegisButton(
                text = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AegisButtonStyle.GHOST,
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Toast & busy overlay
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun MessageToast(
    message: String?,
    onExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2400)
            onExpired()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xE61C1C1E))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            AText(
                message ?: "",
                style = AegisType.body.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
            )
        }
    }
}

@Composable
fun BusyOverlay(visible: Boolean, text: String) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(AegisColors.card)
                    .padding(horizontal = 30.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spinner()
                Spacer(Modifier.height(16.dp))
                AText(text, style = AegisType.small.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    color: Color = AegisColors.blue,
    size: androidx.compose.ui.unit.Dp = 28.dp,
) {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "spinAngle",
    )
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
    ) {
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
