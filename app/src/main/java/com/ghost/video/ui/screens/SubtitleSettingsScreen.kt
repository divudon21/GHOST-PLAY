package com.ghost.video.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.TextRotationNone
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Rectangle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.data.SubtitleFont
import com.ghost.video.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentFont by viewModel.subtitleFont.collectAsState()
    val subtitleBold by viewModel.subtitleBold.collectAsState()
    val subtitleSize by viewModel.subtitleSize.collectAsState()
    val subtitleBackground by viewModel.subtitleBackground.collectAsState()
    val subtitleEmbeddedStyles by viewModel.subtitleEmbeddedStyles.collectAsState()
    val systemCaptionStyle by viewModel.systemCaptionStyle.collectAsState()

    val fontLabel = when (currentFont) {
        SubtitleFont.DEFAULT -> "Default"
        SubtitleFont.MONOSPACE -> "Monospace"
        SubtitleFont.SANS_SERIF -> "Sans Serif"
        SubtitleFont.SERIF -> "Serif"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subtitle") },
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
            // System caption style
            item {
                SettingsToggleCard(
                    title = "System caption style",
                    subtitle = "Open system captioning preferences",
                    icon = Icons.Rounded.ClosedCaption,
                    checked = systemCaptionStyle,
                    showTopDivider = true,
                    onCheckedChange = { enabled ->
                        viewModel.setSystemCaptionStyle(enabled)
                        if (enabled) {
                            val intent = Intent(Settings.ACTION_CAPTIONING_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Subtitle font
            item {
                CapsuleSwitcherCard(
                    title = "Subtitle Font",
                    titleIcon = Icons.Rounded.FontDownload,
                    options = listOf(
                        CapsuleSwitcherOption("Default", Icons.Rounded.TextFields),
                        CapsuleSwitcherOption("Sans", Icons.Rounded.TextRotationNone),
                        CapsuleSwitcherOption("Serif", Icons.Rounded.Title),
                        CapsuleSwitcherOption("Mono", Icons.Rounded.TextFields)
                    ),
                    selectedIndex = currentFont.ordinal,
                    onSelected = { viewModel.setSubtitleFont(SubtitleFont.entries[it]) }
                )
            }

            // Bold subtitle text
            item {
                SettingsToggleCard(
                    title = "Bold subtitle text",
                    subtitle = "Use bold text for subtitle",
                    icon = Icons.Rounded.FormatBold,
                    checked = subtitleBold,
                    onCheckedChange = { viewModel.setSubtitleBold(it) }
                )
            }

            // Subtitle text size
            item {
                val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatSize,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subtitle text size",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$subtitleSize",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setSubtitleSize(20) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset to default",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Slider(
                        value = subtitleSize.toFloat(),
                        onValueChange = { viewModel.setSubtitleSize(it.toInt()) },
                        valueRange = 10f..40f,
                        steps = 29,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            inactiveTickColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    HorizontalDivider(thickness = 0.6.dp, color = lineColor)
                }
            }

            // Subtitle background
            item {
                SettingsToggleCard(
                    title = "Subtitle background",
                    subtitle = "Enable background to subtitle text",
                    icon = Icons.Rounded.Rectangle,
                    checked = subtitleBackground,
                    onCheckedChange = { viewModel.setSubtitleBackground(it) }
                )
            }

            // Embedded styles
            item {
                SettingsToggleCard(
                    title = "Styled Subtitles",
                    subtitle = "Show colors, fonts & effects from subtitle files",
                    icon = Icons.Rounded.Style,
                    checked = subtitleEmbeddedStyles,
                    onCheckedChange = { viewModel.setSubtitleEmbeddedStyles(it) }
                )
            }

        }
    }

    }

@Composable
fun FontOptionRow(
    font: SubtitleFont,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fontFamily = when (font) {
        SubtitleFont.MONOSPACE -> FontFamily.Monospace
        SubtitleFont.SANS_SERIF -> FontFamily.SansSerif
        SubtitleFont.SERIF -> FontFamily.Serif
        SubtitleFont.DEFAULT -> FontFamily.Default
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The font name rendered in its own typeface — a clean live preview.
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = fontFamily,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showTopDivider: Boolean = false
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(thickness = 0.6.dp, color = lineColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            com.ghost.video.ui.components.SmoothSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}

@Composable
fun SettingsClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(thickness = 0.6.dp, color = lineColor)
    }
}
