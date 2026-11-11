package com.ghost.video.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.ghost.video.R
import com.ghost.video.data.AppColor
import com.ghost.video.data.AppTextStyle
import com.ghost.video.data.ThemePreference
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

// ── App colour system ───────────────────────────────────────────────────────
// The old curated multi-hue palette set was removed. A single seed colour per
// [AppColor] now generates the complete Material 3 scheme (primary, secondary,
// tertiary and every surface role), so one "App colour" choice tints the whole
// app consistently in both light and dark.

private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(
        ColorUtils.HSLToColor(
            floatArrayOf(h, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
        )
    )
}

/** Hue (0..360) and a saturation floor that keeps every seed usable. */
private fun seedHueSaturation(seed: Color): Pair<Float, Float> {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seed.red * 255f).toInt().coerceIn(0, 255),
        (seed.green * 255f).toInt().coerceIn(0, 255),
        (seed.blue * 255f).toInt().coerceIn(0, 255),
        hsl
    )
    return hsl[0] to hsl[1].coerceAtLeast(0.42f)
}

/** Colour scheme lookup for use outside a composable (player dialogs). */
fun getColorScheme(appColor: AppColor, isDark: Boolean): ColorScheme =
    appColorScheme(appColor, isDark)

/** Complete Material 3 scheme generated from one app colour. */
fun appColorScheme(appColor: AppColor, isDark: Boolean): ColorScheme {
    val (hue, saturation) = seedHueSaturation(Color(appColor.seed))
    val s = saturation

    return if (isDark) {
        darkColorScheme(
            primary = hslColor(hue, s * 0.72f, 0.80f),
            onPrimary = hslColor(hue, s * 0.85f, 0.18f),
            primaryContainer = hslColor(hue, s * 0.85f, 0.32f),
            onPrimaryContainer = hslColor(hue, s * 0.55f, 0.93f),
            inversePrimary = hslColor(hue, s * 0.80f, 0.42f),
            secondary = hslColor(hue + 18f, s * 0.38f, 0.79f),
            onSecondary = hslColor(hue + 18f, s * 0.40f, 0.18f),
            secondaryContainer = hslColor(hue + 18f, s * 0.32f, 0.31f),
            onSecondaryContainer = hslColor(hue + 18f, s * 0.22f, 0.92f),
            tertiary = hslColor(hue + 55f, s * 0.42f, 0.81f),
            onTertiary = hslColor(hue + 55f, s * 0.45f, 0.18f),
            tertiaryContainer = hslColor(hue + 55f, s * 0.36f, 0.33f),
            onTertiaryContainer = hslColor(hue + 55f, s * 0.25f, 0.93f),
            background = hslColor(hue, 0.16f, 0.07f),
            onBackground = hslColor(hue, 0.08f, 0.93f),
            surface = hslColor(hue, 0.16f, 0.07f),
            onSurface = hslColor(hue, 0.08f, 0.93f),
            surfaceVariant = hslColor(hue, 0.16f, 0.22f),
            onSurfaceVariant = hslColor(hue, 0.10f, 0.83f),
            surfaceContainerLowest = hslColor(hue, 0.18f, 0.04f),
            surfaceContainerLow = hslColor(hue, 0.16f, 0.09f),
            surfaceContainer = hslColor(hue, 0.16f, 0.12f),
            surfaceContainerHigh = hslColor(hue, 0.15f, 0.17f),
            surfaceContainerHighest = hslColor(hue, 0.14f, 0.22f),
            outline = hslColor(hue, 0.10f, 0.56f),
            outlineVariant = hslColor(hue, 0.10f, 0.32f),
            inverseSurface = hslColor(hue, 0.08f, 0.92f),
            inverseOnSurface = hslColor(hue, 0.12f, 0.20f),
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = hslColor(hue, s * 0.85f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslColor(hue, s * 0.55f, 0.90f),
            onPrimaryContainer = hslColor(hue, s * 0.85f, 0.12f),
            inversePrimary = hslColor(hue, s * 0.72f, 0.80f),
            secondary = hslColor(hue + 18f, s * 0.40f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(hue + 18f, s * 0.25f, 0.91f),
            onSecondaryContainer = hslColor(hue + 18f, s * 0.38f, 0.12f),
            tertiary = hslColor(hue + 55f, s * 0.45f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(hue + 55f, s * 0.28f, 0.91f),
            onTertiaryContainer = hslColor(hue + 55f, s * 0.42f, 0.12f),
            background = hslColor(hue, 0.30f, 0.99f),
            onBackground = hslColor(hue, 0.14f, 0.11f),
            surface = hslColor(hue, 0.30f, 0.99f),
            onSurface = hslColor(hue, 0.14f, 0.11f),
            surfaceVariant = hslColor(hue, 0.30f, 0.90f),
            onSurfaceVariant = hslColor(hue, 0.12f, 0.31f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(hue, 0.35f, 0.96f),
            surfaceContainer = hslColor(hue, 0.34f, 0.94f),
            surfaceContainerHigh = hslColor(hue, 0.32f, 0.92f),
            surfaceContainerHighest = hslColor(hue, 0.30f, 0.90f),
            outline = hslColor(hue, 0.10f, 0.50f),
            outlineVariant = hslColor(hue, 0.16f, 0.78f),
            inverseSurface = hslColor(hue, 0.12f, 0.20f),
            inverseOnSurface = hslColor(hue, 0.20f, 0.95f),
            scrim = Color.Black,
        )
    }
}

private val DefaultAppTypography = Typography()

// Bundled OFL fonts from the Google Fonts GitHub repository. Using packaged
// files instead of generic Android family aliases makes every option visibly
// different and consistent on every supported device.
// Each bundled font is a VARIABLE font. Registering the same file four times
// (one entry per weight) did NOT produce a real bold: Android loads the variable
// font's default instance for every declared weight, so the Bold Text toggle only
// visibly worked for the system Default style. Registering a single entry instead
// lets Compose apply synthetic bold for FontWeight.Bold, so Bold Text now works
// with every text style (Manrope / Nunito / Lora / JetBrains Mono / Inter).
// Bundled fonts are shipped as real static weights instead of variable files: the
// Nunito/Manrope variable fonts report a *default* weight of 200 (ExtraLight), so
// Compose — unlike Flutter, which picks the axis value from the TextStyle —
// rendered all of their text extra-light and fake-bolded the rest, which is why
// "Soft and rounded" looked wrong. Static instances make every weight render the
// way DownList shows it.
private fun weightedFontFamily(
    regularRes: Int,
    mediumRes: Int,
    boldRes: Int,
): FontFamily = FontFamily(
    Font(regularRes, weight = FontWeight.Normal),
    Font(mediumRes, weight = FontWeight.Medium),
    Font(boldRes, weight = FontWeight.Bold),
)

private val ManropeFontFamily = weightedFontFamily(
    R.font.manrope_regular,
    R.font.manrope_medium,
    R.font.manrope_bold,
)
private val NunitoFontFamily = weightedFontFamily(
    R.font.nunito_regular,
    R.font.nunito_medium,
    R.font.nunito_bold,
)
private val LoraFontFamily = weightedFontFamily(
    R.font.lora_regular,
    R.font.lora_medium,
    R.font.lora_bold,
)
private val JetBrainsMonoFontFamily = weightedFontFamily(
    R.font.jetbrains_mono_regular,
    R.font.jetbrains_mono_medium,
    R.font.jetbrains_mono_bold,
)
private val InterFontFamily = weightedFontFamily(
    R.font.inter_regular,
    R.font.inter_medium,
    R.font.inter_bold,
)
private val CabaretFontFamily = FontFamily(Font(R.font.cabaret))
private val GrazingMaceFontFamily = FontFamily(Font(R.font.grazing_mace))
private val AbrahamStampFontFamily = FontFamily(Font(R.font.abraham_stamp))
private val LatoFontFamily = FontFamily(
    Font(R.font.lato_regular),
    Font(R.font.lato_bold, weight = androidx.compose.ui.text.font.FontWeight.Bold),
)

private fun typographyWithFamily(fontFamily: FontFamily): Typography {
    val base = DefaultAppTypography
    fun TextStyle.withFamily() = copy(fontFamily = fontFamily)
    return base.copy(
        displayLarge = base.displayLarge.withFamily(),
        displayMedium = base.displayMedium.withFamily(),
        displaySmall = base.displaySmall.withFamily(),
        headlineLarge = base.headlineLarge.withFamily(),
        headlineMedium = base.headlineMedium.withFamily(),
        headlineSmall = base.headlineSmall.withFamily(),
        titleLarge = base.titleLarge.withFamily(),
        titleMedium = base.titleMedium.withFamily(),
        titleSmall = base.titleSmall.withFamily(),
        bodyLarge = base.bodyLarge.withFamily(),
        bodyMedium = base.bodyMedium.withFamily(),
        bodySmall = base.bodySmall.withFamily(),
        labelLarge = base.labelLarge.withFamily(),
        labelMedium = base.labelMedium.withFamily(),
        labelSmall = base.labelSmall.withFamily()
    )
}

private val ManropeAppTypography = typographyWithFamily(ManropeFontFamily)
private val NunitoAppTypography = typographyWithFamily(NunitoFontFamily)
private val LoraAppTypography = typographyWithFamily(LoraFontFamily)
private val JetBrainsMonoAppTypography = typographyWithFamily(JetBrainsMonoFontFamily)
private val InterAppTypography = typographyWithFamily(InterFontFamily)
private val CabaretAppTypography = typographyWithFamily(CabaretFontFamily)
private val GrazingMaceAppTypography = typographyWithFamily(GrazingMaceFontFamily)
private val AbrahamStampAppTypography = typographyWithFamily(AbrahamStampFontFamily)
private val LatoAppTypography = typographyWithFamily(LatoFontFamily)

private fun boldTypography(base: Typography): Typography {
    fun TextStyle.bold() = copy(fontWeight = FontWeight.Bold)
    return base.copy(
        displayLarge = base.displayLarge.bold(),
        displayMedium = base.displayMedium.bold(),
        displaySmall = base.displaySmall.bold(),
        headlineLarge = base.headlineLarge.bold(),
        headlineMedium = base.headlineMedium.bold(),
        headlineSmall = base.headlineSmall.bold(),
        titleLarge = base.titleLarge.bold(),
        titleMedium = base.titleMedium.bold(),
        titleSmall = base.titleSmall.bold(),
        bodyLarge = base.bodyLarge.bold(),
        bodyMedium = base.bodyMedium.bold(),
        bodySmall = base.bodySmall.bold(),
        labelLarge = base.labelLarge.bold(),
        labelMedium = base.labelMedium.bold(),
        labelSmall = base.labelSmall.bold()
    )
}

// Precomputed once: toggling bold text only swaps an immutable Typography object.
private val DefaultBoldTypography = boldTypography(DefaultAppTypography)
private val ManropeBoldTypography = boldTypography(ManropeAppTypography)
private val NunitoBoldTypography = boldTypography(NunitoAppTypography)
private val LoraBoldTypography = boldTypography(LoraAppTypography)
private val JetBrainsMonoBoldTypography = boldTypography(JetBrainsMonoAppTypography)
private val InterBoldTypography = boldTypography(InterAppTypography)
private val CabaretBoldTypography = boldTypography(CabaretAppTypography)
private val GrazingMaceBoldTypography = boldTypography(GrazingMaceAppTypography)
private val AbrahamStampBoldTypography = boldTypography(AbrahamStampAppTypography)
private val LatoBoldTypography = boldTypography(LatoAppTypography)

/**
 * Font family used to preview an app text style inside the Appearance screen, so
 * every option is rendered in the font it selects. `null` means the system font.
 */
fun appTextStyleFontFamily(textStyle: AppTextStyle): FontFamily? = when (textStyle) {
    AppTextStyle.DEFAULT -> null
    AppTextStyle.MANROPE -> ManropeFontFamily
    AppTextStyle.NUNITO -> NunitoFontFamily
    AppTextStyle.LORA -> LoraFontFamily
    AppTextStyle.JETBRAINS_MONO -> JetBrainsMonoFontFamily
    AppTextStyle.INTER -> InterFontFamily
    AppTextStyle.CABARET -> CabaretFontFamily
    AppTextStyle.GRAZING_MACE -> GrazingMaceFontFamily
    AppTextStyle.ABRAHAM_STAMP -> AbrahamStampFontFamily
    AppTextStyle.LATO -> LatoFontFamily
}

@Composable
fun AgonAppTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    appColor: AppColor = AppColor.PURPLE,
    textStyle: AppTextStyle = AppTextStyle.DEFAULT,
    boldText: Boolean = false,
    highContrastDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()

    val darkScheme = appColorScheme(appColor, isDark = true)
    val lightScheme = appColorScheme(appColor, isDark = false)

    // High contrast variant for DARK theme
    val highContrastDarkScheme = darkScheme.copy(
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF0A0A0A),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = Color(0xFFEEEEEE),
        outline = Color(0xFF888888),
        outlineVariant = Color(0xFF444444)
    )

    val colorScheme = when (themePreference) {
        ThemePreference.LIGHT -> lightScheme
        ThemePreference.DARK -> if (highContrastDark) highContrastDarkScheme else darkScheme
        ThemePreference.SYSTEM -> {
            when {
                isSystemDark && highContrastDark -> highContrastDarkScheme
                isSystemDark -> darkScheme
                else -> lightScheme
            }
        }
    }

    val typography = when (textStyle) {
        AppTextStyle.DEFAULT -> if (boldText) DefaultBoldTypography else DefaultAppTypography
        AppTextStyle.MANROPE -> if (boldText) ManropeBoldTypography else ManropeAppTypography
        AppTextStyle.NUNITO -> if (boldText) NunitoBoldTypography else NunitoAppTypography
        AppTextStyle.LORA -> if (boldText) LoraBoldTypography else LoraAppTypography
        AppTextStyle.JETBRAINS_MONO -> if (boldText) JetBrainsMonoBoldTypography else JetBrainsMonoAppTypography
        AppTextStyle.INTER -> if (boldText) InterBoldTypography else InterAppTypography
        AppTextStyle.CABARET -> if (boldText) CabaretBoldTypography else CabaretAppTypography
        AppTextStyle.GRAZING_MACE -> if (boldText) GrazingMaceBoldTypography else GrazingMaceAppTypography
        AppTextStyle.ABRAHAM_STAMP -> if (boldText) AbrahamStampBoldTypography else AbrahamStampAppTypography
        AppTextStyle.LATO -> if (boldText) LatoBoldTypography else LatoAppTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
