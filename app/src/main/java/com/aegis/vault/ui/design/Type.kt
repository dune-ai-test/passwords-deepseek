package com.aegis.vault.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aegis.vault.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Type scale lifted from the design mockups (Inter, 393dp design width). */
object AegisType {
    val title = TextStyle(
        fontFamily = InterFamily, fontSize = 30.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.Bold, color = AegisColors.ink
    )
    val title2 = TextStyle(
        fontFamily = InterFamily, fontSize = 26.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Bold, color = AegisColors.ink
    )
    val score = TextStyle(
        fontFamily = InterFamily, fontSize = 40.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Bold, color = AegisColors.ink
    )
    val big = TextStyle(
        fontFamily = InterFamily, fontSize = 22.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold, color = AegisColors.ink
    )
    val heading = TextStyle(
        fontFamily = InterFamily, fontSize = 17.sp, lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold, color = AegisColors.ink
    )
    val rowTitle = TextStyle(
        fontFamily = InterFamily, fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, color = AegisColors.ink
    )
    val rowTitleRegular = TextStyle(
        fontFamily = InterFamily, fontSize = 15.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Normal, color = AegisColors.ink
    )
    val body = TextStyle(
        fontFamily = InterFamily, fontSize = 15.sp, lineHeight = 22.sp,
        fontWeight = FontWeight.Normal, color = AegisColors.ink
    )
    val bodySemibold = body.copy(fontWeight = FontWeight.SemiBold)
    val small = TextStyle(
        fontFamily = InterFamily, fontSize = 13.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.Normal, color = AegisColors.inkSecondary
    )
    val smallSemibold = small.copy(fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(
        fontFamily = InterFamily, fontSize = 11.sp, lineHeight = 14.sp,
        fontWeight = FontWeight.Normal, color = AegisColors.inkTertiary
    )
    val button = TextStyle(
        fontFamily = InterFamily, fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = Color.White
    )
    val label = TextStyle(
        fontFamily = InterFamily, fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, color = AegisColors.inkSecondary
    )
    val tinyBold = TextStyle(
        fontFamily = InterFamily, fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold, color = AegisColors.ink
    )
}
