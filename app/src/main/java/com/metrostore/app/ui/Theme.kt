package com.metrostore.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrostore.app.R

val LocalAccent = compositionLocalOf { Color(0xFF1E9E1E) }

val MetroAccents = listOf(
    Color(0xFF1E9E1E), Color(0xFF1BA1E2), Color(0xFFE51400), Color(0xFFA200FF),
    Color(0xFFF09609), Color(0xFFE671B8), Color(0xFF00ABA9), Color(0xFF647687)
)
val MetroGray = Color(0xFF9A9A9A)
val MetroDim = Color(0xFF3A3A3A)

/** Provided fonts: Segoe WP Light for all text, Segoe UI Symbol for glyphs (stars, check). */
val SegoeLight = FontFamily(
    Font(R.font.segoe_wp_light, FontWeight.Light),
    Font(R.font.segoe_wp_light, FontWeight.Normal),
    Font(R.font.segoe_wp_light, FontWeight.SemiBold)
)
val SegoeSymbol = FontFamily(Font(R.font.seguisym))

/** Single typographic hierarchy used by the whole app. */
object MetroText {
    private fun s(size: Int, weight: FontWeight = FontWeight.Light, color: Color = Color.White) =
        TextStyle(fontFamily = SegoeLight, fontWeight = weight, fontSize = size.sp, color = color)

    val pageTitle = s(72).copy(letterSpacing = (-1).sp)
    val pivotTitle = s(42)
    val menuItem = s(28)
    val listItem = s(24)
    val listTitle = s(22)
    val tile = s(22)
    val body = s(16, color = Color(0xFFD0D0D0))
    val caption = s(14, color = MetroGray)
    val section = s(14, FontWeight.SemiBold)
    val small = s(13, FontWeight.SemiBold)
}
