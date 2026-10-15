package com.aegis.vault.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.vault.R
import com.aegis.vault.data.Brands
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultItem

/* ────────────────────────────────────────────────────────────────────────────
 * Building blocks
 * ──────────────────────────────────────────────────────────────────────────── */

/** BasicText wrapper used across the whole app (no Material). */
@Composable
fun AText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AegisType.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    val base = if (color == Color.Unspecified) style else style.copy(color = color)
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) base.copy(textAlign = textAlign) else base,
        overflow = overflow,
        maxLines = maxLines,
    )
}

/** iOS-style press feedback: subtle scale-down, no ripple. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
            } else {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
            }
        )
}

@Composable
fun AegisIcon(
    id: Int,
    contentDescription: String? = null,
    size: Dp = 20.dp,
    tint: Color = AegisColors.ink,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/* ────────────────────────────────────────────────────────────────────────────
 * Buttons
 * ──────────────────────────────────────────────────────────────────────────── */

enum class AegisButtonStyle { PRIMARY, SECONDARY, DANGER, GHOST }

@Composable
fun AegisButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AegisButtonStyle = AegisButtonStyle.PRIMARY,
    enabled: Boolean = true,
    icon: Int? = null,
) {
    val bg = when (style) {
        AegisButtonStyle.PRIMARY -> AegisColors.blue
        AegisButtonStyle.SECONDARY -> AegisColors.card
        AegisButtonStyle.DANGER -> AegisColors.red
        AegisButtonStyle.GHOST -> Color.Transparent
    }
    val fg = when (style) {
        AegisButtonStyle.PRIMARY -> Color.White
        AegisButtonStyle.SECONDARY -> AegisColors.blue
        AegisButtonStyle.DANGER -> Color.White
        AegisButtonStyle.GHOST -> AegisColors.blue
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(if (style == AegisButtonStyle.SECONDARY) Modifier.shadow(1.dp, shape) else Modifier)
            .clip(shape)
            .background(if (enabled) bg else bg.copy(alpha = 0.45f))
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                AegisIcon(icon, size = 18.dp, tint = fg)
                Spacer(Modifier.width(8.dp))
            }
            AText(text, style = AegisType.button, color = fg)
        }
    }
}

/** Compact blue text button — used for "Edit", "Cancel", "See all". */
@Composable
fun AegisLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AegisColors.blue,
) {
    AText(
        text = text,
        modifier = modifier
            .pressable(scaleDown = 0.94f, onClick = onClick)
            .padding(vertical = 6.dp),
        style = AegisType.rowTitle.copy(fontWeight = FontWeight.SemiBold),
        color = color,
    )
}

/* ────────────────────────────────────────────────────────────────────────────
 * Containers & rows
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun AegisCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(AegisColors.card)
            .then(if (onClick != null) Modifier.pressable { onClick() } else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun AegisDivider(startPadding: Dp = 60.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(1.dp)
            .background(AegisColors.divider)
    )
}

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AText(title, style = AegisType.heading, modifier = Modifier.weight(1f), maxLines = 1)
        if (action != null && onAction != null) {
            AegisLinkText(action, onClick = onAction)
        }
    }
}

/** Screen header: round back button + title block + trailing actions. */
@Composable
fun NavHeader(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AegisColors.card)
                    .pressable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_chevron_left, size = 20.dp, tint = AegisColors.ink)
            }
            Spacer(Modifier.width(14.dp))
        }
        if (title != null) {
            Column(Modifier.weight(1f)) {
                AText(title, style = AegisType.title2, maxLines = 1)
                if (subtitle != null) {
                    AText(subtitle, style = AegisType.small, modifier = Modifier.padding(top = 2.dp))
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (actions != null) actions()
    }
}

@Composable
fun AvatarBadge(
    letter: String,
    color: Color,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    textColor: Color = Color.White,
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        AText(
            text = letter.take(1).uppercase(),
            style = AegisType.heading.copy(fontSize = (size.value * 0.44f).sp, color = textColor),
            color = textColor,
        )
    }
}

@Composable
fun IconBadge(
    icon: Int,
    bg: Color,
    tint: Color,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    iconSize: Dp = 20.dp,
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        AegisIcon(icon, size = iconSize, tint = tint)
    }
}

/** Brand-aware badge for a vault item. */
@Composable
fun ItemBadge(item: VaultItem, size: Dp = 40.dp) {
    when (item) {
        is LoginItem -> {
            val brand = Brands.forDomain(item.website.ifBlank { item.title })
            if (brand != null) {
                AvatarBadge(brand.letter, brand.color, size = size, corner = size * 0.3f)
            } else {
                IconBadge(R.drawable.ic_key_round, AegisColors.blueSoft, AegisColors.blue, size = size, corner = size * 0.3f, iconSize = size * 0.5f)
            }
        }
        is CardItem -> IconBadge(R.drawable.ic_credit_card, AegisColors.orangeSoft, AegisColors.orange, size = size, corner = size * 0.3f, iconSize = size * 0.5f)
        is NoteItem -> IconBadge(R.drawable.ic_file_text, AegisColors.violetSoft, AegisColors.violetDeep, size = size, corner = size * 0.3f, iconSize = size * 0.5f)
        is IdentityItem -> IconBadge(R.drawable.ic_user, AegisColors.greenSoft, AegisColors.greenDeep, size = size, corner = size * 0.3f, iconSize = size * 0.5f)
    }
}

fun defaultSubtitle(item: VaultItem): String = when (item) {
    is LoginItem -> item.username.ifBlank { item.website }
    is CardItem -> if (item.number.length >= 4) "•••• ${item.number.takeLast(4)}" else item.cardholder
    is NoteItem -> item.body.lineSequence().firstOrNull { it.isNotBlank() }?.take(70) ?: ""
    is IdentityItem -> item.email.ifBlank { item.fullName }
}

@Composable
fun ItemRow(
    item: VaultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AegisColors.card)
            .pressable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemBadge(item)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AText(item.title.ifBlank { "Untitled" }, style = AegisType.rowTitle, maxLines = 1)
            val sub = subtitle ?: defaultSubtitle(item)
            if (sub.isNotBlank()) {
                AText(sub, style = AegisType.small, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            AegisIcon(R.drawable.ic_chevron_right, size = 16.dp, tint = AegisColors.inkTertiary)
        }
    }
}

/** Action row (copy / share / trash style), usually inside an AegisCard. */
@Composable
fun ActionRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = AegisColors.blue,
    bg: Color = AegisColors.blueSoft,
    showChevron: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, bg, tint, size = 34.dp, corner = 10.dp, iconSize = 18.dp)
        Spacer(Modifier.width(12.dp))
        AText(
            label,
            style = AegisType.rowTitleRegular,
            color = if (tint == AegisColors.red) AegisColors.red else AegisColors.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (showChevron) {
            AegisIcon(R.drawable.ic_chevron_right, size = 16.dp, tint = AegisColors.inkTertiary)
        }
    }
}

/** Settings-style row: icon badge + title/subtitle + value + trailing. */
@Composable
fun SettingsRow(
    icon: Int,
    title: String,
    onClick: (() -> Unit)? = null,
    value: String? = null,
    subtitle: String? = null,
    tint: Color = AegisColors.blue,
    bg: Color = AegisColors.blueSoft,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
    destructive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        val rowModifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onLongClick = onLongClick) { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 11.dp)
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = icon,
                bg = if (destructive) AegisColors.redSoft else bg,
                tint = if (destructive) AegisColors.red else tint,
                size = 34.dp,
                corner = 10.dp,
                iconSize = 18.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AText(
                    title,
                    style = AegisType.rowTitle,
                    color = if (destructive) AegisColors.red else AegisColors.ink,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    AText(subtitle, style = AegisType.small, maxLines = 1)
                }
            }
            if (value != null) {
                AText(
                    value,
                    style = AegisType.body.copy(color = AegisColors.inkSecondary),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            } else if (onClick != null) {
                Spacer(Modifier.width(6.dp))
                AegisIcon(R.drawable.ic_chevron_right, size = 15.dp, tint = AegisColors.inkTertiary)
            }
        }
        if (divider) AegisDivider()
    }
}

@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    icon: Int = R.drawable.ic_triangle_alert,
    tint: Color = AegisColors.orange,
    bg: Color = AegisColors.orangeSoft,
    textColor: Color = AegisColors.orangeDeep,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AegisIcon(icon, size = 18.dp, tint = tint, modifier = Modifier.padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        AText(
            text,
            style = AegisType.small.copy(color = textColor, lineHeight = 19.sp),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = AegisColors.chipBg,
    textColor: Color = AegisColors.ink,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (onClick != null) Modifier.pressable(scaleDown = 0.95f) { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AText(text, style = AegisType.small.copy(fontWeight = FontWeight.Medium, color = textColor))
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.12f))
                    .pressable(scaleDown = 0.85f) { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                AegisIcon(R.drawable.ic_x, size = 10.dp, tint = textColor)
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Strength meter
 * ──────────────────────────────────────────────────────────────────────────── */

fun strengthColor(label: PasswordTools.StrengthLabel): Color = when (label) {
    PasswordTools.StrengthLabel.WEAK -> AegisColors.red
    PasswordTools.StrengthLabel.FAIR -> AegisColors.orange
    PasswordTools.StrengthLabel.GOOD -> AegisColors.yellow
    PasswordTools.StrengthLabel.STRONG -> AegisColors.green
    PasswordTools.StrengthLabel.EXCELLENT -> AegisColors.green
}

@Composable
fun StrengthMeter(
    strength: PasswordTools.Strength,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val color = strengthColor(strength.label)
    Column(modifier.fillMaxWidth()) {
        if (showLabel) {
            Row(Modifier.fillMaxWidth()) {
                AText("Password strength", style = AegisType.label, modifier = Modifier.weight(1f))
                AText(strength.label.display, style = AegisType.label.copy(color = color))
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < strength.label.segments) color else AegisColors.chipBg)
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Empty state
 * ──────────────────────────────────────────────────────────────────────────── */

@Composable
fun EmptyState(
    icon: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    cta: String? = null,
    onCta: (() -> Unit)? = null,
    footnote: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        IconBadge(icon, AegisColors.blueSoft, AegisColors.blue, size = 64.dp, corner = 20.dp, iconSize = 30.dp)
        Spacer(Modifier.height(18.dp))
        AText(title, style = AegisType.title2.copy(fontSize = 20.sp), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        AText(
            message,
            style = AegisType.small.copy(fontSize = 14.sp, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
        )
        if (cta != null && onCta != null) {
            Spacer(Modifier.height(20.dp))
            AegisButton(cta, onClick = onCta, modifier = Modifier.width(200.dp))
        }
        if (footnote != null) {
            Spacer(Modifier.height(14.dp))
            AText(footnote, style = AegisType.caption, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/* ────────────────────────────────────────────────────────────────────────────
 * Misc
 * ──────────────────────────────────────────────────────────────────────────── */

/** Label + iOS switch row (generator options, settings). */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    divider: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AText(title, style = AegisType.rowTitle)
                if (subtitle != null) {
                    AText(subtitle, style = AegisType.small, modifier = Modifier.padding(top = 1.dp))
                }
            }
            IOSSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (divider) AegisDivider(startPadding = 16.dp)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 7.dp,
    trackColor: Color = AegisColors.chipBg,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke,
        )
    }
}

@Composable
fun SmallButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AegisColors.blue,
    fg: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .pressable(scaleDown = 0.95f) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        AText(text, style = AegisType.rowTitle.copy(fontSize = 14.sp), color = fg)
    }
}
