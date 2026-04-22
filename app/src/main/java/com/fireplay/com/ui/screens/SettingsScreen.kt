package com.fireplay.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fireplay.com.data.AppColorPreference
import com.fireplay.com.data.ThemePreference
import com.fireplay.com.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val currentTheme by viewModel.themePreference.collectAsState()
    val currentColor by viewModel.colorPreference.collectAsState()

    val themeOptions = listOf(
        ThemeOption("System Default", Icons.Default.BrightnessAuto, ThemePreference.SYSTEM),
        ThemeOption("Light", Icons.Default.Brightness7, ThemePreference.LIGHT),
        ThemeOption("Dark", Icons.Default.Brightness4, ThemePreference.DARK),
        ThemeOption("AMOLED Dark", Icons.Default.NightlightRound, ThemePreference.AMOLED)
    )

    val colorOptions = listOf(
        ColorOption("Purple", Color(0xFF6650a4), AppColorPreference.PURPLE),
        ColorOption("Blue", Color(0xFF1A73E8), AppColorPreference.BLUE),
        ColorOption("Green", Color(0xFF1E8E3E), AppColorPreference.GREEN),
        ColorOption("Orange", Color(0xFFE65100), AppColorPreference.ORANGE),
        ColorOption("Red", Color(0xFFD32F2F), AppColorPreference.RED),
        ColorOption("Pink", Color(0xFFD81B60), AppColorPreference.PINK),
        ColorOption("Teal", Color(0xFF00897B), AppColorPreference.TEAL),
        ColorOption("Yellow", Color(0xFFFBC02D), AppColorPreference.YELLOW),
        ColorOption("Cyan", Color(0xFF00ACC1), AppColorPreference.CYAN),
        ColorOption("Indigo", Color(0xFF3949AB), AppColorPreference.INDIGO)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
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
                    text = "App Color",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colorOptions.take(5).forEach { option ->
                            ColorCircle(
                                color = option.colorValue,
                                isSelected = currentColor == option.preference,
                                onClick = { viewModel.setColor(option.preference) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colorOptions.drop(5).forEach { option ->
                            ColorCircle(
                                color = option.colorValue,
                                isSelected = currentColor == option.preference,
                                onClick = { viewModel.setColor(option.preference) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(themeOptions) { option ->
                ThemeOptionCard(
                    option = option,
                    isSelected = currentTheme == option.preference,
                    onClick = { viewModel.setTheme(option.preference) }
                )
            }
        }
    }
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = color, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
            )
        }
    }
}

@Composable
fun ThemeOptionCard(
    option: ThemeOption,
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
                .padding(20.dp), // Large padding for big tap target
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp), // Big icon
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleLarge, // Big text
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ThemeOption(
    val title: String,
    val icon: ImageVector,
    val preference: ThemePreference
)

data class ColorOption(
    val name: String,
    val colorValue: Color,
    val preference: AppColorPreference
)