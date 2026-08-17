package com.ghost.video.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.R
import com.ghost.video.data.AppPalette
import com.ghost.video.data.AppTextStyle
import com.ghost.video.ui.theme.paletteScheme
import com.ghost.video.data.DialogThemePreference
import com.ghost.video.data.ThemePreference
import com.ghost.video.data.ViewLayout
import com.ghost.video.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentTheme by viewModel.themePreference.collectAsState()
    val currentPalette by viewModel.appPalette.collectAsState()
    val currentTextStyle by viewModel.appTextStyle.collectAsState()
    val boldText by viewModel.boldText.collectAsState()
    val currentLayout by viewModel.viewLayout.collectAsState()
    val currentDialogTheme by viewModel.dialogThemePreference.collectAsState()
    val highContrastDark by viewModel.highContrastDark.collectAsState()
    val glowEffect by viewModel.glowEffect.collectAsState()
    val loadingIndicatorStyle by viewModel.loadingIndicatorStyle.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    var showPalettePicker by remember { mutableStateOf(false) }
    var showTextStylePicker by remember { mutableStateOf(false) }
    var showLayoutPicker by remember { mutableStateOf(false) }

    // AMOLED removed — only System, Light, Dark
    val themeOptions = listOf(
        ThemeOption("System", Icons.Default.BrightnessAuto, ThemePreference.SYSTEM),
        ThemeOption("Light", Icons.Default.Brightness7, ThemePreference.LIGHT),
        ThemeOption("Dark", Icons.Default.Brightness4, ThemePreference.DARK)
    )

    val dialogThemeOptions = listOf(
        DialogThemeOption("Follow System", Icons.Default.BrightnessAuto, DialogThemePreference.FOLLOW_SYSTEM),
        DialogThemeOption("Dark", Icons.Default.Brightness4, DialogThemePreference.DARK),
        DialogThemeOption("Light", Icons.Default.Brightness7, DialogThemePreference.LIGHT),
        DialogThemeOption("Custom (App Color)", Icons.Default.Palette, DialogThemePreference.CUSTOM)
    )

    val viewLayoutOptions = listOf(
        ViewLayoutOption(
            title = "List",
            subtitle = "",
            icon = Icons.Default.ViewAgenda,
            layout = ViewLayout.LIST
        ),
        ViewLayoutOption(
            title = "Grid",
            subtitle = "",
            icon = Icons.Default.GridView,
            layout = ViewLayout.GRID
        ),
        ViewLayoutOption(
            title = "Compact",
            subtitle = "Fast 3-column browsing",
            icon = Icons.Default.Apps,
            layout = ViewLayout.COMPACT_GRID
        ),
        ViewLayoutOption(
            title = "Cinema",
            subtitle = "Featured video first",
            icon = Icons.Default.ViewAgenda,
            layout = ViewLayout.CINEMA
        )
    )

    // 5 curated Material 3 palettes (multi-hue combinations).
    val palettes = remember { appPalettes() }
    val selectedPalette = palettes.firstOrNull { it.palette == currentPalette } ?: palettes.first()
    val textStyles = remember {
        listOf(
            TextStyleOption(AppTextStyle.DEFAULT, "Default", "Android system text"),
            TextStyleOption(AppTextStyle.MANROPE, "Manrope", "Clean and modern"),
            TextStyleOption(AppTextStyle.NUNITO, "Nunito", "Soft and friendly"),
            TextStyleOption(AppTextStyle.LORA, "Lora", "Elegant serif"),
            TextStyleOption(AppTextStyle.JETBRAINS_MONO, "JetBrains Mono", "Technical monospace"),
            TextStyleOption(AppTextStyle.INTER, "Inter", "Sharp screen readability"),
            TextStyleOption(AppTextStyle.CABARET, "Cabaret", "Decorative display"),
            TextStyleOption(AppTextStyle.GRAZING_MACE, "Grazing Mace", "Distinctive display"),
            TextStyleOption(AppTextStyle.ABRAHAM_STAMP, "Abraham Stamp", "Bold stamped look")
        )
    }
    val selectedTextStyle = textStyles.firstOrNull { it.style == currentTextStyle }
        ?: textStyles.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // App Palette — minimal row
            item {
                PalettePickerCard(
                    selectedOption = selectedPalette,
                    onClick = { showPalettePicker = true }
                )
            }

            item {
                TextStylePickerCard(
                    selectedOption = selectedTextStyle,
                    onClick = { showTextStylePicker = true }
                )
            }

            item {
                BoldTextToggleCard(
                    checked = boldText,
                    onCheckedChange = viewModel::setBoldText
                )
            }

            // Glow effect — adds a soft glow to every toggle in the app.
            item {
                GlowEffectToggleCard(
                    checked = glowEffect,
                    onCheckedChange = viewModel::setGlowEffect
                )
            }

            // View Layout — simple "Choose Layout" row that opens a picker dialog.
            item {
                val selectedLayoutOption = viewLayoutOptions.firstOrNull { it.layout == currentLayout }
                    ?: viewLayoutOptions.first()
                ViewLayoutPickerCard(
                    selectedOption = selectedLayoutOption,
                    onClick = { showLayoutPicker = true }
                )
            }

            // Theme — native segmented switcher (Auto / Light / Dark)
            item {
                ThemeModeSwitcherCard(
                    currentTheme = currentTheme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
            }

            // High contrast Dark toggle — minimal row.
            // Enabled when the app is actually showing a dark UI: either Dark theme
            // is selected, OR System theme is selected and the phone is in dark mode.
            item {
                val darkActive = currentTheme == ThemePreference.DARK ||
                        (currentTheme == ThemePreference.SYSTEM && systemInDark)
                HighContrastDarkCard(
                    enabled = darkActive,
                    checked = highContrastDark,
                    onCheckedChange = { viewModel.setHighContrastDark(it) }
                )
            }

            // Dialog Theme — native segmented switcher (4 options)
            item {
                DialogThemeModeSwitcherCard(
                    currentDialogTheme = currentDialogTheme,
                    onDialogThemeSelected = { viewModel.setDialogTheme(it) }
                )
            }
        }
    }

    if (showPalettePicker) {
        PalettePickerDialog(
            palettes = palettes,
            selectedPalette = currentPalette,
            onDismiss = { showPalettePicker = false },
            onPaletteSelected = { palette ->
                viewModel.setAppPalette(palette)
                showPalettePicker = false
            }
        )
    }

    if (showTextStylePicker) {
        TextStylePickerDialog(
            options = textStyles,
            selectedStyle = currentTextStyle,
            onDismiss = { showTextStylePicker = false },
            onTextStyleSelected = { style ->
                viewModel.setAppTextStyle(style)
                showTextStylePicker = false
            }
        )
    }

    if (showLayoutPicker) {
        ViewLayoutPickerDialog(
            options = viewLayoutOptions,
            selectedLayout = currentLayout,
            onDismiss = { showLayoutPicker = false },
            onViewLayoutSelected = { layout ->
                viewModel.setViewLayout(layout)
                showLayoutPicker = false
            }
        )
    }

}

/**
 * Generic capsule switcher option — icon + label.
 */
data class CapsuleSwitcherOption(
    val label: String,
    val icon: ImageVector
)

// ═══════════════════════════════════════════════════════════════════════════
//  Lightweight capsule switcher
//
//  Uses the exact visual treatment of the Home / Audio / Settings capsule:
//   • Same 56dp track, 5dp inner padding and circular geometry.
//   • Active app-palette colour with matching on-primary content.
//   • No slide animation, shadow, physics, drag, haptics or gradient work.
// ═══════════════════════════════════════════════════════════════════════════

/** Palette of flat colours shared by every capsule card. */
private data class CapsuleColors(
    val cardBg: Color,
    val iconBadgeBg: Color,
    val iconBadgeColor: Color,
    val labelColor: Color,
    val trackBg: Color,
    val trackBorder: Color,
    val thumbBg: Color,
    val activeColor: Color,
    val inactiveColor: Color
)

@Composable
private fun rememberCapsuleColors(): CapsuleColors {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = MaterialTheme.colorScheme
    return CapsuleColors(
        // Use Material theme roles instead of fixed white/grey values so the
        // complete capsule card follows the selected app palette.
        cardBg = scheme.surface,
        iconBadgeBg = scheme.primaryContainer,
        iconBadgeColor = scheme.onPrimaryContainer,
        labelColor = scheme.onSurface,
        trackBg = scheme.primaryContainer.copy(alpha = if (isDark) 0.32f else 0.48f),
        trackBorder = scheme.primary.copy(alpha = if (isDark) 0.22f else 0.18f),
        thumbBg = scheme.primary,
        activeColor = scheme.onPrimary,
        inactiveColor = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.62f else 0.58f)
    )
}

/**
 * Generic capsule switcher card. Label + icon on top, a solid segmented pill below
 * that works with any number of options. Used for View Layout, Orientation,
 * Decoder, Thumbnail strategy, Subtitle font, etc.
 */
@Composable
fun CapsuleSwitcherCard(
    title: String,
    titleIcon: ImageVector,
    options: List<CapsuleSwitcherOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val c = rememberCapsuleColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.cardBg)
            .border(1.dp, c.trackBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CapsuleHeader(title = title, icon = titleIcon, colors = c)
            Spacer(modifier = Modifier.height(16.dp))
            CapsuleTrack(
                itemCount = options.size,
                selectedIndex = selectedIndex,
                colors = c,
                onSelected = onSelected
            ) { index ->
                CapsuleSegmentContent(
                    icon = options[index].icon,
                    label = options[index].label,
                    isActive = selectedIndex == index,
                    colors = c
                )
            }
        }
    }
}

/** Shared label row (icon badge + title) used by every capsule card. */
@Composable
private fun CapsuleHeader(title: String, icon: ImageVector, colors: CapsuleColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.iconBadgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.iconBadgeColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = colors.labelColor,
            letterSpacing = (-0.01).em
        )
    }
}

/**
 * Solid segmented track matching the main bottom capsule. Selection switches
 * immediately, avoiding the same uneven slide motion removed from bottom nav.
 */
@Composable
private fun CapsuleTrack(
    itemCount: Int,
    selectedIndex: Int,
    colors: CapsuleColors,
    onSelected: (Int) -> Unit,
    segment: @Composable (index: Int) -> Unit
) {
    val trackHeight = 56.dp
    val pad = 5.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(CircleShape)
            .background(colors.trackBg)
            .border(1.dp, colors.trackBorder, CircleShape)
            .padding(pad)
    ) {
        val segWidth = maxWidth / itemCount
        val safeIndex = selectedIndex.coerceIn(0, itemCount - 1)
        val thumbOffset = segWidth * safeIndex

        // Flat selected segment with no slide or elevation.
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.thumbBg)
        )

        // Segments on top.
        Row(modifier = Modifier.matchParentSize()) {
            for (i in 0 until itemCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (selectedIndex != i) onSelected(i) },
                    contentAlignment = Alignment.Center
                ) {
                    segment(i)
                }
            }
        }
    }
}

/** Icon + label content for a segment; only the tint animates (cheap). */
@Composable
private fun CapsuleSegmentContent(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    colors: CapsuleColors
) {
    val tint by animateColorAsState(
        targetValue = if (isActive) colors.activeColor else colors.inactiveColor,
        animationSpec = tween(180),
        label = "segTint"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * A curated Material 3 palette option — a multi-hue colour combination shown as a
 * three-swatch preview (primary / secondary / tertiary).
 */
data class PaletteOption(
    val name: String,
    val description: String,
    val palette: AppPalette,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

/**
 * The 5 curated Material 3 palettes. Preview colours are pulled from each palette's
 * dark scheme so the swatches match exactly what the app will render.
 */
fun appPalettes(): List<PaletteOption> {
    fun option(name: String, desc: String, p: AppPalette): PaletteOption {
        val s = paletteScheme(p, isDark = true)
        return PaletteOption(name, desc, p, s.primary, s.secondary, s.tertiary)
    }
    return listOf(
        option("Monochrome", "Pure black · white · neutral grey", AppPalette.MONOCHROME),
        option("Aurora", "Indigo · violet · teal", AppPalette.AURORA),
        option("Sunset", "Coral · amber · rose", AppPalette.SUNSET),
        option("Oceanic", "Deep blue · cyan · sky", AppPalette.OCEANIC),
        option("Verdant", "Green · lime · sand", AppPalette.VERDANT),
        option("Midnight", "Slate blue · cyan · violet", AppPalette.MIDNIGHT),
        option("Rose Gold", "Blush · gold · mauve", AppPalette.ROSEGOLD),
        option("Emerald", "Jade · mint · teal", AppPalette.EMERALD),
        option("Lavender", "Purple · lilac · periwinkle", AppPalette.LAVENDER),
        option("Ember", "Crimson · orange · gold", AppPalette.EMBER)
    )
}

/**
 * Shared minimal setting row — a plain leading icon (no box, no circle), a title,
 * a subtitle (selected value) and a trailing arrow. A thin line is drawn above
 * (only on the first row) and below each row so options look separated.
 */
@Composable
fun MinimalSettingRow(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    onClick: (() -> Unit)? = null,
    showTopDivider: Boolean = false,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plain leading icon — no box, no background.
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

@Composable
private fun BoldTextToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MinimalSettingRow(
        title = "Bold text",
        subtitle = "Make the selected text style bold",
        leadingIcon = Icons.Default.FormatBold,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun GlowEffectToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MinimalSettingRow(
        title = "Glow effect",
        subtitle = "Add a soft glow to all toggles",
        leadingIcon = Icons.Rounded.AutoAwesome,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

data class TextStyleOption(
    val style: AppTextStyle,
    val title: String,
    val description: String
)

private val ManropePreviewFamily = FontFamily(Font(R.font.manrope_variable))
private val NunitoPreviewFamily = FontFamily(Font(R.font.nunito_variable))
private val LoraPreviewFamily = FontFamily(Font(R.font.lora_variable))
private val JetBrainsPreviewFamily = FontFamily(Font(R.font.jetbrains_mono_variable))
private val InterPreviewFamily = FontFamily(Font(R.font.inter_variable))
private val CabaretPreviewFamily = FontFamily(Font(R.font.cabaret))
private val GrazingMacePreviewFamily = FontFamily(Font(R.font.grazing_mace))
private val AbrahamStampPreviewFamily = FontFamily(Font(R.font.abraham_stamp))

private fun previewFontFamily(style: AppTextStyle): FontFamily = when (style) {
    AppTextStyle.DEFAULT -> FontFamily.Default
    AppTextStyle.MANROPE -> ManropePreviewFamily
    AppTextStyle.NUNITO -> NunitoPreviewFamily
    AppTextStyle.LORA -> LoraPreviewFamily
    AppTextStyle.JETBRAINS_MONO -> JetBrainsPreviewFamily
    AppTextStyle.INTER -> InterPreviewFamily
    AppTextStyle.CABARET -> CabaretPreviewFamily
    AppTextStyle.GRAZING_MACE -> GrazingMacePreviewFamily
    AppTextStyle.ABRAHAM_STAMP -> AbrahamStampPreviewFamily
}

@Composable
fun TextStylePickerCard(
    selectedOption: TextStyleOption,
    onClick: () -> Unit
) {
    MinimalSettingRow(
        title = "Choose Text Style",
        subtitle = selectedOption.title,
        leadingIcon = Icons.Default.TextFields,
        onClick = onClick
    )
}

@Composable
fun TextStylePickerDialog(
    options: List<TextStyleOption>,
    selectedStyle: AppTextStyle,
    onDismiss: () -> Unit,
    onTextStyleSelected: (AppTextStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Text Style") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 440.dp)
            ) {
                items(options) { option ->
                    val selected = option.style == selectedStyle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { onTextStyleSelected(option.style) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                fontFamily = previewFontFamily(option.style),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.description,
                                fontFamily = previewFontFamily(option.style),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // One clean rounded box for the Cancel action, so the bottom of the
            // dialog looks intentional instead of a bare text button.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@Composable
fun PalettePickerCard(
    selectedOption: PaletteOption,
    onClick: () -> Unit
) {
    MinimalSettingRow(
        title = "App Palette",
        subtitle = selectedOption.name,
        leadingIcon = Icons.Default.Palette,
        onClick = onClick,
        showTopDivider = true,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaletteDots(selectedOption, size = 16.dp)
            }
        }
    )
}

/** Three overlapping dots previewing a palette's primary/secondary/tertiary. */
@Composable
private fun PaletteDots(option: PaletteOption, size: Dp) {
    listOf(option.primary, option.secondary, option.tertiary).forEach { c ->
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(c)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
        )
    }
}

@Composable
fun PalettePickerDialog(
    palettes: List<PaletteOption>,
    selectedPalette: AppPalette,
    onDismiss: () -> Unit,
    onPaletteSelected: (AppPalette) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Palette") },
        text = {
            // Scrollable so all 10 palettes are reachable on any screen size.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 440.dp)
            ) {
                items(palettes) { option ->
                    PaletteRow(
                        option = option,
                        isSelected = option.palette == selectedPalette,
                        onClick = { onPaletteSelected(option.palette) }
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@Composable
fun PaletteRow(
    option: PaletteOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(180),
        label = "paletteBorder"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Combined swatch bar previewing the multi-hue combination.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(9.dp))
        ) {
            Box(modifier = Modifier.size(width = 22.dp, height = 34.dp).background(option.primary))
            Box(modifier = Modifier.size(width = 22.dp, height = 34.dp).background(option.secondary))
            Box(modifier = Modifier.size(width = 22.dp, height = 34.dp).background(option.tertiary))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class ViewLayoutOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val layout: ViewLayout
)

@Composable
fun ViewLayoutPickerCard(
    selectedOption: ViewLayoutOption,
    onClick: () -> Unit
) {
    MinimalSettingRow(
        title = "Choose View Layout",
        subtitle = selectedOption.title,
        leadingIcon = selectedOption.icon,
        onClick = onClick
    )
}

@Composable
fun ViewLayoutPickerDialog(
    options: List<ViewLayoutOption>,
    selectedLayout: ViewLayout,
    onDismiss: () -> Unit,
    onViewLayoutSelected: (ViewLayout) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("View Layout") },
        text = {
            // Two columns keep all four layout choices readable and easy to tap.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { option ->
                    ViewLayoutTile(
                        option = option,
                        isSelected = selectedLayout == option.layout,
                        onClick = { onViewLayoutSelected(option.layout) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@Composable
fun ViewLayoutTile(
    option: ViewLayoutOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(180),
        label = "layoutTileBorder"
    )
    val accent = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mini preview of the layout, drawn with simple blocks.
        LayoutPreview(layout = option.layout, accent = accent)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = option.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Tiny block diagram that previews each layout style. */
@Composable
fun LayoutPreview(layout: ViewLayout, accent: Color) {
    val block: @Composable (Modifier) -> Unit = { m ->
        Box(
            modifier = m
                .clip(RoundedCornerShape(3.dp))
                .background(accent.copy(alpha = 0.85f))
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        when (layout) {
            ViewLayout.LIST -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(3) {
                        block(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
            ViewLayout.GRID -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            block(Modifier.weight(1f).fillMaxHeight())
                            block(Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            ViewLayout.COMPACT_GRID -> {
                // Intentional compact mosaic: clean tiles with enough breathing
                // room to read at a glance, rather than a cramped dense grid.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1.15f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        block(Modifier.weight(1.35f).fillMaxHeight())
                        block(Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(0.85f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(3) { block(Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
            }
            ViewLayout.CINEMA -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    block(Modifier.fillMaxWidth().weight(1.35f))
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(0.65f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        block(Modifier.weight(1f).fillMaxHeight())
                        block(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
fun ThemePickerCard(
    selectedOption: ThemeOption,
    onClick: () -> Unit
) {
    MinimalSettingRow(
        title = "Choose Theme",
        subtitle = selectedOption.title,
        leadingIcon = selectedOption.icon,
        onClick = onClick
    )
}

/**
 * Native segmented theme-mode switcher — replicates the HTML reference exactly:
 *  • Card with gradient background + shadow, rounded 26dp
 *  • Icon badge (palette) + “Theme Mode” label on the left
 *  • Pill-shaped switcher with 3 segments (Auto / Light / Dark)
 *  • Sliding thumb with spring physics (stiffness 0.22, damping 0.76)
 *  • Horizontal drag with rubber-banding at edges
 *  • Squash & stretch based on velocity
 *  • Press-scale on segments (0.90)
 *  • Colour transitions (300ms ease)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeModeSwitcherCard(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit
) {
    val themes = listOf(ThemePreference.SYSTEM, ThemePreference.LIGHT, ThemePreference.DARK)
    val options = listOf(
        CapsuleSwitcherOption("Auto", Icons.Default.BrightnessAuto),
        CapsuleSwitcherOption("Light", Icons.Default.Brightness7),
        CapsuleSwitcherOption("Dark", Icons.Default.Brightness4)
    )
    val currentIndex = themes.indexOf(currentTheme).coerceAtLeast(0)
    CapsuleSwitcherCard(
        title = "Theme Mode",
        titleIcon = Icons.Default.Palette,
        options = options,
        selectedIndex = currentIndex,
        onSelected = { onThemeSelected(themes[it]) }
    )
}

@Composable
fun DialogThemeModeSwitcherCard(
    currentDialogTheme: DialogThemePreference,
    onDialogThemeSelected: (DialogThemePreference) -> Unit
) {
    val themes = listOf(
        DialogThemePreference.FOLLOW_SYSTEM,
        DialogThemePreference.DARK,
        DialogThemePreference.LIGHT,
        DialogThemePreference.CUSTOM
    )
    val options = listOf(
        CapsuleSwitcherOption("System", Icons.Default.BrightnessAuto),
        CapsuleSwitcherOption("Dark", Icons.Default.Brightness4),
        CapsuleSwitcherOption("Light", Icons.Default.Brightness7),
        CapsuleSwitcherOption("Custom", Icons.Default.Palette)
    )
    val currentIndex = themes.indexOf(currentDialogTheme).coerceAtLeast(0)
    CapsuleSwitcherCard(
        title = "Dialog Theme",
        titleIcon = Icons.Default.Palette,
        options = options,
        selectedIndex = currentIndex,
        onSelected = { onDialogThemeSelected(themes[it]) }
    )
}

@Composable
fun ThemePickerDialog(
    options: List<ThemeOption>,
    selectedPreference: ThemePreference,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemePreference) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    val isSelected = selectedPreference == option.preference
                    ThemeDialogOptionRow(
                        option = option,
                        isSelected = isSelected,
                        onClick = { onThemeSelected(option.preference) }
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@Composable
fun ThemeDialogOptionRow(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "themeDialogContainer"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Wide theme preview card — kept for future use; current UI uses the simple Choose Theme popup.
 * Arranged 3-in-a-row horizontally. Selected card gets primary border + checkmark.
 */
@Composable
fun ThemePreviewCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Theme preview colors
    val previewBg = when (option.preference) {
        ThemePreference.SYSTEM -> Color(0xFFF3EDF7)
        ThemePreference.LIGHT -> Color(0xFFFDF8FD)
        ThemePreference.DARK -> Color(0xFF141218)
    }
    val previewSurface = when (option.preference) {
        ThemePreference.SYSTEM -> Color(0xFFFFFFFF)
        ThemePreference.LIGHT -> Color(0xFFFFFFFF)
        ThemePreference.DARK -> Color(0xFF211F26)
    }
    val previewPrimary = when (option.preference) {
        ThemePreference.SYSTEM -> Color(0xFF6750A4)
        ThemePreference.LIGHT -> Color(0xFF6650A4)
        ThemePreference.DARK -> Color(0xFFD0BCFF)
    }
    val previewText = when (option.preference) {
        ThemePreference.SYSTEM -> Color(0xFF1D1B20)
        ThemePreference.LIGHT -> Color(0xFF1D1B20)
        ThemePreference.DARK -> Color(0xFFE6E0E9)
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "themeBorder"
    )

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Mini screen preview — wide rectangle showing theme colors
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewBg)
            ) {
                // Mock app bar at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .align(Alignment.TopCenter)
                        .background(previewPrimary)
                )

                // Mock content — two card rows
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp, start = 6.dp, end = 6.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Card row 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(previewSurface),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(previewPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(previewText.copy(alpha = 0.4f))
                        )
                    }
                    // Card row 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(previewSurface),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(previewPrimary.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(previewText.copy(alpha = 0.3f))
                        )
                    }
                }

                // Checkmark badge on selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Theme name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * High contrast Dark toggle — only enabled when Dark theme is selected.
 */
@Composable
fun HighContrastDarkCard(
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MinimalSettingRow(
        title = "High contrast Dark",
        subtitle = if (enabled) "Increase contrast with brighter text and vivid colors"
        else "Available when the app is in dark mode",
        leadingIcon = Icons.Default.Contrast,
        enabled = enabled,
        trailing = {
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked && enabled,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

data class DialogThemeOption(
    val title: String,
    val icon: ImageVector,
    val preference: DialogThemePreference
)

@Composable
fun DialogThemePickerCard(
    selectedOption: DialogThemeOption,
    onClick: () -> Unit
) {
    MinimalSettingRow(
        title = "Choose Dialog Theme",
        subtitle = selectedOption.title,
        leadingIcon = selectedOption.icon,
        onClick = onClick
    )
}

@Composable
fun DialogThemePickerDialog(
    options: List<DialogThemeOption>,
    selectedPreference: DialogThemePreference,
    onDismiss: () -> Unit,
    onDialogThemeSelected: (DialogThemePreference) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Dialog Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    DialogThemeDialogOptionRow(
                        option = option,
                        isSelected = selectedPreference == option.preference,
                        onClick = { onDialogThemeSelected(option.preference) }
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    )
}

@Composable
fun DialogThemeDialogOptionRow(
    option: DialogThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "dialogThemeContainer"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120))
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DialogThemeOptionCard(
    option: DialogThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LayoutOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
