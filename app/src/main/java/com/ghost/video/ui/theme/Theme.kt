package com.ghost.video.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.ghost.video.data.AppColorPreference
import com.ghost.video.data.AppPalette
import com.ghost.video.data.ThemePreference
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min

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

// ═══════════════════════════════════════════════════════════════════════════
//  Curated Material 3 palettes — each a full multi-hue colour combination
//  (primary + secondary + tertiary), not a single static colour.
// ═══════════════════════════════════════════════════════════════════════════

// ── 1. Monochrome — pure black, white and neutral grey ──
private val MonochromeDark = darkColorScheme(
    primary = Color(0xFFF1F1F1), onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3F3F3F), onPrimaryContainer = Color(0xFFF1F1F1),
    secondary = Color(0xFFD4D4D4), onSecondary = Color(0xFF292929),
    secondaryContainer = Color(0xFF484848), onSecondaryContainer = Color(0xFFE7E7E7),
    tertiary = Color(0xFFBDBDBD), onTertiary = Color(0xFF242424),
    tertiaryContainer = Color(0xFF555555), onTertiaryContainer = Color(0xFFE8E8E8),
    background = Color(0xFF111111), onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF111111), onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF454545), onSurfaceVariant = Color(0xFFC9C9C9),
    outline = Color(0xFF919191)
)
private val MonochromeLight = lightColorScheme(
    primary = Color(0xFF292929), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E3E3), onPrimaryContainer = Color(0xFF181818),
    secondary = Color(0xFF4B4B4B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E6E6), onSecondaryContainer = Color(0xFF1C1C1C),
    tertiary = Color(0xFF686868), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDADADA), onTertiaryContainer = Color(0xFF202020),
    background = Color(0xFFFCFCFC), onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFCFCFC), onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE5E5E5), onSurfaceVariant = Color(0xFF474747),
    outline = Color(0xFF777777)
)

// ── 2. Aurora — indigo / violet with teal-green accents ──
private val AuroraDark = darkColorScheme(
    primary = Color(0xFFB9C3FF), onPrimary = Color(0xFF002586),
    primaryContainer = Color(0xFF1A3BBB), onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFD6BAFF), onSecondary = Color(0xFF3D1D74),
    secondaryContainer = Color(0xFF54388C), onSecondaryContainer = Color(0xFFEDDCFF),
    tertiary = Color(0xFF6ED4C7), onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048), onTertiaryContainer = Color(0xFF8DF1E3),
    background = Color(0xFF12131A), onBackground = Color(0xFFE3E1EC),
    surface = Color(0xFF12131A), onSurface = Color(0xFFE3E1EC),
    surfaceVariant = Color(0xFF45464F), onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A)
)
private val AuroraLight = lightColorScheme(
    primary = Color(0xFF3B4CCC), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF6D3FB5), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDDCFF), onSecondaryContainer = Color(0xFF260059),
    tertiary = Color(0xFF006A5E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF8DF1E3), onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC), onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680)
)

// ── 3. Sunset — warm coral / amber with rose accents ──
private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB59B), onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF7F2C0D), onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFFFD9A0), onSecondary = Color(0xFF442B00),
    secondaryContainer = Color(0xFF624000), onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary = Color(0xFFF7B0C7), onTertiary = Color(0xFF4E1130),
    tertiaryContainer = Color(0xFF6A2947), onTertiaryContainer = Color(0xFFFFD9E4),
    background = Color(0xFF1A120F), onBackground = Color(0xFFF1DED7),
    surface = Color(0xFF1A120F), onSurface = Color(0xFFF1DED7),
    surfaceVariant = Color(0xFF53433D), onSurfaceVariant = Color(0xFFD8C2B9),
    outline = Color(0xFFA08D85)
)
private val SunsetLight = lightColorScheme(
    primary = Color(0xFFA23716), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF), onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF855400), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB0), onSecondaryContainer = Color(0xFF2A1700),
    tertiary = Color(0xFF8C4A61), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E4), onTertiaryContainer = Color(0xFF3A071F),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED4), onSurfaceVariant = Color(0xFF53443D),
    outline = Color(0xFF85736C)
)

// ── 4. Oceanic — deep blue / cyan with sky accents ──
private val OceanicDark = darkColorScheme(
    primary = Color(0xFF7FD1F0), onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C69), onPrimaryContainer = Color(0xFFC3E8FF),
    secondary = Color(0xFF6FDDDB), onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF00504E), onSecondaryContainer = Color(0xFF8CF8F5),
    tertiary = Color(0xFFAEC6FF), onTertiary = Color(0xFF002B75),
    tertiaryContainer = Color(0xFF0F429A), onTertiaryContainer = Color(0xFFD9E2FF),
    background = Color(0xFF0F1416), onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1416), onSurface = Color(0xFFDEE3E6),
    surfaceVariant = Color(0xFF40484C), onSurfaceVariant = Color(0xFFC0C8CC),
    outline = Color(0xFF8A9296)
)
private val OceanicLight = lightColorScheme(
    primary = Color(0xFF00668A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC3E8FF), onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF006A68), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF8CF8F5), onSecondaryContainer = Color(0xFF00201F),
    tertiary = Color(0xFF2C57C0), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9E2FF), onTertiaryContainer = Color(0xFF001947),
    background = Color(0xFFFBFCFE), onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFE), onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDCE4E8), onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787C)
)

// ── 5. Verdant — fresh green with lime & warm accents ──
private val VerdantDark = darkColorScheme(
    primary = Color(0xFF8CD98A), onPrimary = Color(0xFF00390C),
    primaryContainer = Color(0xFF115316), onPrimaryContainer = Color(0xFFA8F5A4),
    secondary = Color(0xFFBBCBB1), onSecondary = Color(0xFF263422),
    secondaryContainer = Color(0xFF3C4B37), onSecondaryContainer = Color(0xFFD7E7CC),
    tertiary = Color(0xFFCFC98C), onTertiary = Color(0xFF333205),
    tertiaryContainer = Color(0xFF4A491A), onTertiaryContainer = Color(0xFFEBE5A6),
    background = Color(0xFF101510), onBackground = Color(0xFFE0E4DB),
    surface = Color(0xFF101510), onSurface = Color(0xFFE0E4DB),
    surfaceVariant = Color(0xFF424940), onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9388)
)
private val VerdantLight = lightColorScheme(
    primary = Color(0xFF276A26), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F5A4), onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF53634D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E7CC), onSecondaryContainer = Color(0xFF111F0E),
    tertiary = Color(0xFF626131), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBE5A6), onTertiaryContainer = Color(0xFF1D1D00),
    background = Color(0xFFFCFDF6), onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFCFDF6), onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDEE5D8), onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796E)
)

// ── 6. Midnight — deep slate blue with cyan & soft violet accents ──
private val MidnightDark = darkColorScheme(
    primary = Color(0xFF8FB8FF), onPrimary = Color(0xFF00305F),
    primaryContainer = Color(0xFF124585), onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFF7FE0E4), onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF004F54), onSecondaryContainer = Color(0xFF9CF0F5),
    tertiary = Color(0xFFC6BEFF), onTertiary = Color(0xFF2E1D6C),
    tertiaryContainer = Color(0xFF453889), onTertiaryContainer = Color(0xFFE5DEFF),
    background = Color(0xFF0D1017), onBackground = Color(0xFFDDE2EC),
    surface = Color(0xFF0D1017), onSurface = Color(0xFFDDE2EC),
    surfaceVariant = Color(0xFF42474F), onSurfaceVariant = Color(0xFFC2C7D0),
    outline = Color(0xFF8C919A)
)
private val MidnightLight = lightColorScheme(
    primary = Color(0xFF1B5EA8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF), onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF00696F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9CF0F5), onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF5A50A2), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF), onTertiaryContainer = Color(0xFF160659),
    background = Color(0xFFFAFAFF), onBackground = Color(0xFF191C21),
    surface = Color(0xFFFAFAFF), onSurface = Color(0xFF191C21),
    surfaceVariant = Color(0xFFDEE2EC), onSurfaceVariant = Color(0xFF42474F),
    outline = Color(0xFF72777F)
)

// ── 7. Rose Gold — warm pink / blush with gold & mauve accents ──
private val RoseGoldDark = darkColorScheme(
    primary = Color(0xFFFFB0C8), onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7C2949), onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFF6C56D), onSecondary = Color(0xFF412D00),
    secondaryContainer = Color(0xFF5D4200), onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFDDBFD8), onTertiary = Color(0xFF3F2A3E),
    tertiaryContainer = Color(0xFF574056), onTertiaryContainer = Color(0xFFFADBF4),
    background = Color(0xFF19110F), onBackground = Color(0xFFEFDFDC),
    surface = Color(0xFF19110F), onSurface = Color(0xFFEFDFDC),
    surfaceVariant = Color(0xFF524345), onSurfaceVariant = Color(0xFFD7C1C3),
    outline = Color(0xFF9F8C8E)
)
private val RoseGoldLight = lightColorScheme(
    primary = Color(0xFF9C4062), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3E001F),
    secondary = Color(0xFF7C5800), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDF9E), onSecondaryContainer = Color(0xFF271900),
    tertiary = Color(0xFF71586E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFADBF4), onTertiaryContainer = Color(0xFF291628),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF3DDDF), onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF847375)
)

// ── 8. Emerald — rich jade green with mint & teal accents ──
private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF6DDB9C), onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF005230), onPrimaryContainer = Color(0xFF8AF8B6),
    secondary = Color(0xFF7CD9D4), onSecondary = Color(0xFF003734),
    secondaryContainer = Color(0xFF00504C), onSecondaryContainer = Color(0xFF98F5EF),
    tertiary = Color(0xFFA9CDF4), onTertiary = Color(0xFF0B3050),
    tertiaryContainer = Color(0xFF294768), onTertiaryContainer = Color(0xFFCFE4FF),
    background = Color(0xFF0D1512), onBackground = Color(0xFFDCE5DE),
    surface = Color(0xFF0D1512), onSurface = Color(0xFFDCE5DE),
    surfaceVariant = Color(0xFF3F4943), onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF89938B)
)
private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF006C43), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8AF8B6), onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF006A66), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF98F5EF), onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF415F80), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFE4FF), onTertiaryContainer = Color(0xFF001C38),
    background = Color(0xFFF6FCF6), onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF6FCF6), onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DD), onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707972)
)

// ── 9. Lavender — soft purple with lilac & periwinkle accents ──
private val LavenderDark = darkColorScheme(
    primary = Color(0xFFD3BBFF), onPrimary = Color(0xFF3C1D71),
    primaryContainer = Color(0xFF533789), onPrimaryContainer = Color(0xFFEBDCFF),
    secondary = Color(0xFFCBC2DB), onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF494458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFAFC6FF), onTertiary = Color(0xFF002E6B),
    tertiaryContainer = Color(0xFF204584), onTertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFF14121A), onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF14121A), onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454E), onSurfaceVariant = Color(0xFFC9C4D0),
    outline = Color(0xFF938F99)
)
private val LavenderLight = lightColorScheme(
    primary = Color(0xFF6A3EAC), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDCFF), onPrimaryContainer = Color(0xFF250057),
    secondary = Color(0xFF615B71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF34588E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E2FF), onTertiaryContainer = Color(0xFF001B3D),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A757F)
)

// ── 10. Ember — bold red / crimson with orange & gold accents ──
private val EmberDark = darkColorScheme(
    primary = Color(0xFFFFB4A8), onPrimary = Color(0xFF690004),
    primaryContainer = Color(0xFF930009), onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFFFB68A), onSecondary = Color(0xFF522300),
    secondaryContainer = Color(0xFF743500), onSecondaryContainer = Color(0xFFFFDBC7),
    tertiary = Color(0xFFEBC248), onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = Color(0xFF584400), onTertiaryContainer = Color(0xFFFFDF90),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF1DEDB),
    surface = Color(0xFF1A1110), onSurface = Color(0xFFF1DEDB),
    surfaceVariant = Color(0xFF534341), onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C89)
)
private val EmberLight = lightColorScheme(
    primary = Color(0xFFBC000F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4), onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF984700), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC7), onSecondaryContainer = Color(0xFF321300),
    tertiary = Color(0xFF735B00), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF90), onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFFFBFF), onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF), onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDD9), onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857370)
)

/**
 * Resolve a palette to its light/dark Material 3 [ColorScheme].
 *
 * A restrained accent wash lets the chosen palette remain visible on cards and
 * backgrounds in both themes. It is deliberately subtle: controls retain clear
 * contrast and the app never turns into a full-colour surface.
 */
fun paletteScheme(palette: AppPalette, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val base = when (palette) {
        AppPalette.MONOCHROME -> if (isDark) MonochromeDark else MonochromeLight
        AppPalette.AURORA -> if (isDark) AuroraDark else AuroraLight
        AppPalette.SUNSET -> if (isDark) SunsetDark else SunsetLight
        AppPalette.OCEANIC -> if (isDark) OceanicDark else OceanicLight
        AppPalette.VERDANT -> if (isDark) VerdantDark else VerdantLight
        AppPalette.MIDNIGHT -> if (isDark) MidnightDark else MidnightLight
        AppPalette.ROSEGOLD -> if (isDark) RoseGoldDark else RoseGoldLight
        AppPalette.EMERALD -> if (isDark) EmeraldDark else EmeraldLight
        AppPalette.LAVENDER -> if (isDark) LavenderDark else LavenderLight
        AppPalette.EMBER -> if (isDark) EmberDark else EmberLight
    }
    val backgroundWash = base.primaryContainer.copy(alpha = if (isDark) 0.12f else 0.075f)
        .compositeOver(base.background)
    val cardWash = base.primaryContainer.copy(alpha = if (isDark) 0.34f else 0.20f)
        .compositeOver(base.surface)
    return base.copy(
        background = backgroundWash,
        surface = backgroundWash,
        surfaceVariant = cardWash
    )
}

/**
 * Generate a dark ColorScheme from a custom seed color.
 */
fun generateDarkScheme(seed: Color): androidx.compose.material3.ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seed.red * 255).toInt(), (seed.green * 255).toInt(), (seed.blue * 255).toInt(), hsl
    )
    val h = hsl[0]
    val s = max(hsl[1], 0.4f)

    fun hslColor(hue: Float, sat: Float, light: Float): Color = Color(
        ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f)))
    )

    val primary = hslColor(h, s, 0.80f)
    val onPrimary = hslColor(h, s, 0.20f)
    val primaryContainer = hslColor(h, s, 0.30f)
    val onPrimaryContainer = hslColor(h, s, 0.92f)
    val secondary = hslColor((h + 30) % 360, s * 0.7f, 0.75f)
    val onSecondary = hslColor((h + 30) % 360, s * 0.7f, 0.18f)
    val secondaryContainer = hslColor((h + 30) % 360, s * 0.7f, 0.25f)
    val onSecondaryContainer = hslColor((h + 30) % 360, s * 0.7f, 0.90f)
    val tertiary = hslColor((h + 60) % 360, s * 0.8f, 0.75f)
    val onTertiary = hslColor((h + 60) % 360, s * 0.8f, 0.18f)
    val tertiaryContainer = hslColor((h + 60) % 360, s * 0.8f, 0.28f)
    val onTertiaryContainer = hslColor((h + 60) % 360, s * 0.8f, 0.90f)

    return darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    )
}

/**
 * Generate a light ColorScheme from a custom seed color.
 */
fun generateLightScheme(seed: Color): androidx.compose.material3.ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(
        (seed.red * 255).toInt(), (seed.green * 255).toInt(), (seed.blue * 255).toInt(), hsl
    )
    val h = hsl[0]
    val s = max(hsl[1], 0.4f)

    fun hslColor(hue: Float, sat: Float, light: Float): Color = Color(
        ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f)))
    )

    val primary = hslColor(h, s, 0.40f)
    val onPrimary = Color.White
    val primaryContainer = hslColor(h, s * 0.7f, 0.90f)
    val onPrimaryContainer = hslColor(h, s, 0.10f)
    val secondary = hslColor((h + 30) % 360, s * 0.7f, 0.40f)
    val onSecondary = Color.White
    val secondaryContainer = hslColor((h + 30) % 360, s * 0.7f, 0.90f)
    val onSecondaryContainer = hslColor((h + 30) % 360, s * 0.7f, 0.10f)
    val tertiary = hslColor((h + 60) % 360, s * 0.8f, 0.40f)
    val onTertiary = Color.White
    val tertiaryContainer = hslColor((h + 60) % 360, s * 0.8f, 0.90f)
    val onTertiaryContainer = hslColor((h + 60) % 360, s * 0.8f, 0.10f)

    return lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    )
}

/**
 * Get color scheme for use outside Composable context (e.g., for dialogs).
 * Now driven by the curated [AppPalette].
 */
fun getColorScheme(
    palette: AppPalette,
    isDark: Boolean
): androidx.compose.material3.ColorScheme = paletteScheme(palette, isDark)

@Composable
fun AgonAppTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    palette: AppPalette = AppPalette.MONOCHROME,
    highContrastDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()

    val darkScheme = paletteScheme(palette, isDark = true)
    val lightScheme = paletteScheme(palette, isDark = false)

    val amoledScheme = darkScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212)
    )

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
        ThemePreference.AMOLED -> amoledScheme
        ThemePreference.SYSTEM -> {
            when {
                isSystemDark && highContrastDark -> highContrastDarkScheme
                isSystemDark -> darkScheme
                else -> lightScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
