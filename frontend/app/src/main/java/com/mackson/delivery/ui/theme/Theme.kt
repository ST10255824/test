package com.mackson.delivery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colour palette sampled directly (pixel-picked) from the Part 1 prototype's own screenshots —
// deep navy + gold on a soft blue-white surface, plus the vivid sky-blue "hero" background used
// on its onboarding/auth/store-select screens specifically (Login, OTP, Choose Nearest Store).
val MacksonNavy = Color(0xFF1E3A5F)        // headings, primary buttons, logo mark
val MacksonNavyDeep = Color(0xFF0B1C38)    // darkest buttons (e.g. "Continue", "Add New Address")
val MacksonGold = Color(0xFFD4A017)        // primary CTA accent ("Confirm", "Checkout", ratings)
val MacksonGoldLight = Color(0xFFF3C969)   // badges, highlighted/selected chip backgrounds
val MacksonSkyBlue = Color(0xFF9FD0F8)     // full-bleed hero background (auth + store-select)
val MacksonMidBlue = Color(0xFF4C84CA)     // secondary action buttons on the sky-blue screens
val SurfaceBlueTint = Color(0xFFEFF4FB)    // app background (post-login screens)
val SurfaceWhite = Color(0xFFFFFFFF)       // cards
val SurfaceDarkNavy = Color(0xFF0B1626)    // dark-theme background

val StatusSuccessGreen = Color(0xFF17A34A)
val StatusInfoBlue = Color(0xFF2563EB)
val StatusWarningAmber = Color(0xFFD97706)
val StatusNeutralGray = Color(0xFF6B7280)
val StatusErrorRed = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = MacksonNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE6F5),
    onPrimaryContainer = MacksonNavyDeep,
    secondary = MacksonGold,
    onSecondary = MacksonNavyDeep,
    secondaryContainer = Color(0xFFFBEFD1),
    onSecondaryContainer = Color(0xFF7A5B06),
    tertiary = StatusInfoBlue,
    error = StatusErrorRed,
    errorContainer = Color(0xFFFBDCDA),
    onErrorContainer = Color(0xFF7A130C),
    background = SurfaceBlueTint,
    onBackground = MacksonNavy,
    surface = SurfaceWhite,
    onSurface = MacksonNavy,
    surfaceVariant = Color(0xFFE3EAF4),
    onSurfaceVariant = Color(0xFF4B5768),
    outline = Color(0xFFC6D1E3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB8E3),
    onPrimary = MacksonNavyDeep,
    primaryContainer = Color(0xFF223A61),
    onPrimaryContainer = Color(0xFFDDE6F5),
    secondary = MacksonGoldLight,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF57420A),
    onSecondaryContainer = Color(0xFFFBEFD1),
    tertiary = Color(0xFF9CBBFB),
    error = Color(0xFFF2B8B5),
    background = SurfaceDarkNavy,
    onBackground = Color(0xFFE3E9F5),
    surface = Color(0xFF13233F),
    onSurface = Color(0xFFE3E9F5),
    surfaceVariant = Color(0xFF223354),
    onSurfaceVariant = Color(0xFFC2CEE3),
    outline = Color(0xFF3C4F73)
)

@Composable
fun MacksonDeliveryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MacksonTypography,
        shapes = MacksonShapes,
        content = content
    )
}
