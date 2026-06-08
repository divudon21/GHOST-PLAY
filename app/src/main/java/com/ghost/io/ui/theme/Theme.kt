package com.ghost.io.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ghost.io.data.AppColorPreference
import com.ghost.io.data.ThemePreference

// Base Purple Theme (Default)
private val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
)
private val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625b71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
)

// Blue Theme
private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAECBFA),
    onPrimary = Color(0xFF173F92),
    primaryContainer = Color(0xFF2E5CB8),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF8AB4F8),
    onSecondary = Color(0xFF0D327B),
    secondaryContainer = Color(0xFF224CA0),
    onSecondaryContainer = Color(0xFFC7DAFF),
    tertiary = Color(0xFF82C8A0),
    onTertiary = Color(0xFF00381C),
    tertiaryContainer = Color(0xFF00522B),
    onTertiaryContainer = Color(0xFFADF2C8),
)
private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001946),
    secondary = Color(0xFF1967D2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC7DAFF),
    onSecondaryContainer = Color(0xFF00153D),
    tertiary = Color(0xFF1E8E3E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFADF2C8),
    onTertiaryContainer = Color(0xFF00210E),
)

// Green Theme
private val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8DAB5),
    onPrimary = Color(0xFF0F5223),
    primaryContainer = Color(0xFF276D38),
    onPrimaryContainer = Color(0xFFC4F7D0),
    secondary = Color(0xFF81C995),
    onSecondary = Color(0xFF003915),
    secondaryContainer = Color(0xFF005221),
    onSecondaryContainer = Color(0xFF9DF6B0),
    tertiary = Color(0xFFFDE293),
    onTertiary = Color(0xFF423000),
    tertiaryContainer = Color(0xFF5E4500),
    onTertiaryContainer = Color(0xFFFFF0C4),
)
private val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF1E8E3E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4F7D0),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF188038),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9DF6B0),
    onSecondaryContainer = Color(0xFF00210A),
    tertiary = Color(0xFFF9AB00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF0C4),
    onTertiaryContainer = Color(0xFF261A00),
)

// Orange Theme
private val OrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFDC69C),
    onPrimary = Color(0xFF4C2700),
    primaryContainer = Color(0xFF6D3A00),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFFCAD70),
    onSecondary = Color(0xFF4B2300),
    secondaryContainer = Color(0xFF6C3500),
    onSecondaryContainer = Color(0xFFFFDCC0),
    tertiary = Color(0xFFF28B82),
    onTertiary = Color(0xFF4B120E),
    tertiaryContainer = Color(0xFF68211A),
    onTertiaryContainer = Color(0xFFFFDAD5),
)
private val OrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC0),
    onPrimaryContainer = Color(0xFF2E1300),
    secondary = Color(0xFFF57C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC0),
    onSecondaryContainer = Color(0xFF2D1200),
    tertiary = Color(0xFFD32F2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD5),
    onTertiaryContainer = Color(0xFF410001),
)

// Red Theme
private val RedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF28B82),
    onPrimary = Color(0xFF4B120E),
    primaryContainer = Color(0xFF68211A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFEE675C),
    onSecondary = Color(0xFF4B100B),
    secondaryContainer = Color(0xFF681E17),
    onSecondaryContainer = Color(0xFFFFDAD5),
    tertiary = Color(0xFFFDC69C),
    onTertiary = Color(0xFF4C2700),
    tertiaryContainer = Color(0xFF6D3A00),
    onTertiaryContainer = Color(0xFFFFDCC0),
)
private val RedLightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFFC62828),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF410001),
    tertiary = Color(0xFFE65100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF2E1300),
)

// Pink Theme
private val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF4C102A),
    primaryContainer = Color(0xFF682140),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFF06292),
    onSecondary = Color(0xFF4A102A),
    secondaryContainer = Color(0xFF651F3F),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFE91E63),
    onTertiary = Color(0xFF46001B),
    tertiaryContainer = Color(0xFF650029),
    onTertiaryContainer = Color(0xFFFFD9E2),
)
private val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFFD81B60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001A),
    secondary = Color(0xFFC2185B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF3E001A),
    tertiary = Color(0xFFAD1457),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001A),
)

// Teal Theme
private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504B),
    onPrimaryContainer = Color(0xFF9CF2EA),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFF6FF8ED),
    tertiary = Color(0xFF26A69A),
    onTertiary = Color(0xFF003734),
    tertiaryContainer = Color(0xFF00504B),
    onTertiaryContainer = Color(0xFF4DF9E9),
)
private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF2EA),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF00796B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF6FF8ED),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF00695C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF4DF9E9),
    onTertiaryContainer = Color(0xFF00201E),
)

// Yellow Theme
private val YellowDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFF59D),
    onPrimary = Color(0xFF3B3100),
    primaryContainer = Color(0xFF554800),
    onPrimaryContainer = Color(0xFFFFE063),
    secondary = Color(0xFFFFF176),
    onSecondary = Color(0xFF383000),
    secondaryContainer = Color(0xFF514700),
    onSecondaryContainer = Color(0xFFFFE05C),
    tertiary = Color(0xFFFFEE58),
    onTertiary = Color(0xFF352E00),
    tertiaryContainer = Color(0xFF4E4400),
    onTertiaryContainer = Color(0xFFFFE052),
)
private val YellowLightColorScheme = lightColorScheme(
    primary = Color(0xFFFBC02D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE063),
    onPrimaryContainer = Color(0xFF231B00),
    secondary = Color(0xFFF9A825),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE05C),
    onSecondaryContainer = Color(0xFF211A00),
    tertiary = Color(0xFFF57F17),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE052),
    onTertiaryContainer = Color(0xFF1F1900),
)

// Cyan Theme
private val CyanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF80DEEA),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9DF0FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF6FF0FF),
    tertiary = Color(0xFF26C6DA),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF4DF0FF),
)
private val CyanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00ACC1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DF0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF0097A7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF6FF0FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF00838F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF4DF0FF),
    onTertiaryContainer = Color(0xFF001F24),
)

// Indigo Theme
private val IndigoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF15225E),
    primaryContainer = Color(0xFF2E3B77),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFF7986CB),
    onSecondary = Color(0xFF00155A),
    secondaryContainer = Color(0xFF1A2D73),
    onSecondaryContainer = Color(0xFFDCE1FF),
    tertiary = Color(0xFF5C6BC0),
    onTertiary = Color(0xFF001254),
    tertiaryContainer = Color(0xFF002287),
    onTertiaryContainer = Color(0xFFDCE1FF),
)
private val IndigoLightColorScheme = lightColorScheme(
    primary = Color(0xFF3949AB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF000F43),
    secondary = Color(0xFF303F9F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE1FF),
    onSecondaryContainer = Color(0xFF00155A),
    tertiary = Color(0xFF283593),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE1FF),
    onTertiaryContainer = Color(0xFF001254),
)

/**
 * Get color scheme for use outside Composable context (e.g., for dialogs)
 */
fun getColorScheme(
    colorPreference: AppColorPreference,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    return if (isDark) {
        when (colorPreference) {
            AppColorPreference.PURPLE -> PurpleDarkColorScheme
            AppColorPreference.BLUE -> BlueDarkColorScheme
            AppColorPreference.GREEN -> GreenDarkColorScheme
            AppColorPreference.ORANGE -> OrangeDarkColorScheme
            AppColorPreference.RED -> RedDarkColorScheme
            AppColorPreference.PINK -> PinkDarkColorScheme
            AppColorPreference.TEAL -> TealDarkColorScheme
            AppColorPreference.YELLOW -> YellowDarkColorScheme
            AppColorPreference.CYAN -> CyanDarkColorScheme
            AppColorPreference.INDIGO -> IndigoDarkColorScheme
        }
    } else {
        when (colorPreference) {
            AppColorPreference.PURPLE -> PurpleLightColorScheme
            AppColorPreference.BLUE -> BlueLightColorScheme
            AppColorPreference.GREEN -> GreenLightColorScheme
            AppColorPreference.ORANGE -> OrangeLightColorScheme
            AppColorPreference.RED -> RedLightColorScheme
            AppColorPreference.PINK -> PinkLightColorScheme
            AppColorPreference.TEAL -> TealLightColorScheme
            AppColorPreference.YELLOW -> YellowLightColorScheme
            AppColorPreference.CYAN -> CyanLightColorScheme
            AppColorPreference.INDIGO -> IndigoLightColorScheme
        }
    }
}

@Composable
fun AgonAppTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    colorPreference: AppColorPreference = AppColorPreference.PURPLE,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val baseDarkScheme = when (colorPreference) {
        AppColorPreference.PURPLE -> PurpleDarkColorScheme
        AppColorPreference.BLUE -> BlueDarkColorScheme
        AppColorPreference.GREEN -> GreenDarkColorScheme
        AppColorPreference.ORANGE -> OrangeDarkColorScheme
        AppColorPreference.RED -> RedDarkColorScheme
        AppColorPreference.PINK -> PinkDarkColorScheme
        AppColorPreference.TEAL -> TealDarkColorScheme
        AppColorPreference.YELLOW -> YellowDarkColorScheme
        AppColorPreference.CYAN -> CyanDarkColorScheme
        AppColorPreference.INDIGO -> IndigoDarkColorScheme
    }

    val baseLightScheme = when (colorPreference) {
        AppColorPreference.PURPLE -> PurpleLightColorScheme
        AppColorPreference.BLUE -> BlueLightColorScheme
        AppColorPreference.GREEN -> GreenLightColorScheme
        AppColorPreference.ORANGE -> OrangeLightColorScheme
        AppColorPreference.RED -> RedLightColorScheme
        AppColorPreference.PINK -> PinkLightColorScheme
        AppColorPreference.TEAL -> TealLightColorScheme
        AppColorPreference.YELLOW -> YellowLightColorScheme
        AppColorPreference.CYAN -> CyanLightColorScheme
        AppColorPreference.INDIGO -> IndigoLightColorScheme
    }

    val amoledScheme = baseDarkScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212)
    )

    val colorScheme = when (themePreference) {
        ThemePreference.LIGHT -> baseLightScheme
        ThemePreference.DARK -> baseDarkScheme
        ThemePreference.AMOLED -> amoledScheme
        ThemePreference.SYSTEM -> {
            when {
                // Only use dynamic colors if the user kept the default PURPLE preference
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && colorPreference == AppColorPreference.PURPLE -> {
                    val context = LocalContext.current
                    if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                isSystemDark -> baseDarkScheme
                else -> baseLightScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}