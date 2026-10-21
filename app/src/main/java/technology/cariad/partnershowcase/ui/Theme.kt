package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import technology.cariad.partnershowcase.DemoConfig

/** CARIAD's palette, and the few roles this screen needs. */
@Immutable
data class ShowcaseColors(
    val background: Color,
    val card: Color,
    val cardRaised: Color,
    val text: Color,
    val muted: Color,
    val outline: Color,
    val accent: Color,
    /** The accent as text: on a light background, mint alone is unreadable. */
    val accentText: Color,
    val onAccent: Color,
    val driving: Color,
    val warning: Color,
)

private val Night =
    ShowcaseColors(
        background = Color(0xFF12032A),
        card = Color(0xFF1D0638),
        cardRaised = Color(0xFF2C1752),
        text = Color.White,
        muted = Color(0xFFBDB6D0),
        outline = Color(0xFF3D2A63),
        accent = DemoConfig.ACCENT,
        accentText = DemoConfig.ACCENT,
        onAccent = Color(0xFF1D0638),
        driving = Color(0xFFEE4C40),
        warning = Color(0xFFFEF04A),
    )

private val Day =
    ShowcaseColors(
        background = Color(0xFFECEBF2),
        card = Color.White,
        cardRaised = Color(0xFFF4F3F9),
        text = Color(0xFF1D0638),
        muted = Color(0xFF575170),
        outline = Color(0xFFD6D3E2),
        accent = DemoConfig.ACCENT,
        accentText = lerp(DemoConfig.ACCENT, Color.Black, 0.5f),
        onAccent = Color(0xFF1D0638),
        driving = Color(0xFFD63A2F),
        warning = Color(0xFF7A6F00),
    )

val LocalShowcaseColors = staticCompositionLocalOf { Night }

/**
 * Car typography: larger than on a phone, because the screen is an arm's length away and every
 * glance while driving counts. Body text never below 24 sp on the driving surfaces.
 */
private val CarTypography =
    Typography(
        displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
        bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
        labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    )

/** Day and night follow the car (headlights, ambient light), not a setting in the app. */
@Composable
fun ShowcaseTheme(content: @Composable () -> Unit) {
    val night = isSystemInDarkTheme()
    val colors = if (night) Night else Day
    val scheme =
        if (night)
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = colors.background,
                surface = colors.card,
                onSurface = colors.text,
                outline = colors.outline,
            )
        else
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.onAccent,
                background = colors.background,
                surface = colors.card,
                onSurface = colors.text,
                outline = colors.outline,
            )
    CompositionLocalProvider(LocalShowcaseColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = CarTypography, content = content)
    }
}
