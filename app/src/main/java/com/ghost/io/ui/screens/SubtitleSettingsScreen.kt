package com.ghost.io.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.io.data.SubtitleFont
import com.ghost.io.viewmodel.SettingsViewModel

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

    var showFontDialog by remember { mutableStateOf(false) }

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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            // System caption style
            item {
                SettingsToggleCard(
                    title = "System caption style",
                    subtitle = "Click to open system captioning preferences",
                    icon = Icons.Default.Subtitles,
                    checked = systemCaptionStyle,
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
                SettingsClickableCard(
                    title = "Subtitle font",
                    subtitle = fontLabel,
                    icon = Icons.Default.TextFields,
                    onClick = { showFontDialog = true }
                )
            }

            // Bold subtitle text
            item {
                SettingsToggleCard(
                    title = "Bold subtitle text",
                    subtitle = "Use bold text for subtitle",
                    icon = Icons.Default.FormatBold,
                    checked = subtitleBold,
                    onCheckedChange = { viewModel.setSubtitleBold(it) }
                )
            }

            // Subtitle text size
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Subtitle text size",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$subtitleSize",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setSubtitleSize(20) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset to default",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = subtitleSize.toFloat(),
                            onValueChange = { viewModel.setSubtitleSize(it.toInt()) },
                            valueRange = 10f..40f,
                            steps = 29,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Subtitle background
            item {
                SettingsToggleCard(
                    title = "Subtitle background",
                    subtitle = "Enable background to subtitle text",
                    icon = Icons.Default.Title,
                    checked = subtitleBackground,
                    onCheckedChange = { viewModel.setSubtitleBackground(it) }
                )
            }

            // Embedded styles
            item {
                SettingsToggleCard(
                    title = "Styled Subtitles",
                    subtitle = "Show colors, fonts & effects from ASS/SSA, WebVTT files. Disable for plain subtitles.",
                    icon = Icons.Default.Style,
                    checked = subtitleEmbeddedStyles,
                    onCheckedChange = { viewModel.setSubtitleEmbeddedStyles(it) }
                )
            }
            
            // Supported formats info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Supported Subtitle Formats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• SRT - Basic subtitles\n• WebVTT - Styled with colors\n• ASS/SSA - Full styling support\n• VTT, TTML, TX3G - Various formats",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Font selection dialog
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Subtitle font") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontOptionRow(
                        label = "Default",
                        isSelected = currentFont == SubtitleFont.DEFAULT,
                        onClick = {
                            viewModel.setSubtitleFont(SubtitleFont.DEFAULT)
                            showFontDialog = false
                        }
                    )
                    FontOptionRow(
                        label = "Monospace",
                        isSelected = currentFont == SubtitleFont.MONOSPACE,
                        onClick = {
                            viewModel.setSubtitleFont(SubtitleFont.MONOSPACE)
                            showFontDialog = false
                        }
                    )
                    FontOptionRow(
                        label = "Sans Serif",
                        isSelected = currentFont == SubtitleFont.SANS_SERIF,
                        onClick = {
                            viewModel.setSubtitleFont(SubtitleFont.SANS_SERIF)
                            showFontDialog = false
                        }
                    )
                    FontOptionRow(
                        label = "Serif",
                        isSelected = currentFont == SubtitleFont.SERIF,
                        onClick = {
                            viewModel.setSubtitleFont(SubtitleFont.SERIF)
                            showFontDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FontOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimary,
                            CircleShape
                        )
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
