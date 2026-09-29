package com.mackson.delivery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Bold, rounded headlines echo the big friendly display text ("Your cart is empty", "Choose
// delivery location") in the Part 1 prototype. Body text stays >= 16sp so it's legible for
// low-vision users without manual zoom — the WCAG 2.2 AA target from Part 1 section 1.6.
val MacksonTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MacksonFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp
    )
)
