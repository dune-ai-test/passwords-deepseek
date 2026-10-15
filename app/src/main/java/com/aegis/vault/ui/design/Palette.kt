package com.aegis.vault.ui.design

import androidx.compose.ui.graphics.Color

/** The iOS-style palette from the original design mockups. */
object AegisColors {
    val blue = Color(0xFF007AFF)
    val blueSoft = Color(0xFFE8F1FE)
    val violet = Color(0xFF5E5CE6)
    val violetSoft = Color(0xFFF3EAFB)
    val violetDeep = Color(0xFF6E56CF)

    val green = Color(0xFF34C759)
    val greenSoft = Color(0xFFE4F8EB)
    val greenDeep = Color(0xFF1B7A3D)

    val orange = Color(0xFFFF9500)
    val orangeSoft = Color(0xFFFFF3E2)
    val orangeDeep = Color(0xFF8A4B00)

    val red = Color(0xFFFF3B30)
    val redSoft = Color(0xFFFFE9E7)
    val pink = Color(0xFFFF2D55)
    val teal = Color(0xFF00C7BE)
    val yellow = Color(0xFFFFCC00)

    val ink = Color(0xFF1C1C1E)
    val inkSecondary = Color(0xFF8E8E93)
    val inkTertiary = Color(0xFFC7C7CC)

    val bg = Color(0xFFF2F2F7)
    val card = Color(0xFFFFFFFF)
    val divider = Color(0xFFEDEDF0)
    val chipBg = Color(0xFFEDEDF0)
    val track = Color(0xFFE5E5EA)
    val dockInactive = Color(0xFF6E6980)
    val dockOutline = Color(0xFFE4DFEF)

    val letterPalette = listOf(
        blue, violet, orange, green, pink, teal, Color(0xFF5856D6), Color(0xFFAF52DE)
    )

    val infoSoft = blueSoft
    val info = blue
    val warnSoft = orangeSoft
    val warn = orange
    val dangerSoft = redSoft
    val danger = red
    val successSoft = greenSoft
    val success = green
}

/** Multiply RGB channels toward black to make a darker shade of a color. */
fun Color.darken(fraction: Float): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha
)
