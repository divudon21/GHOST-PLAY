package com.ghost.io

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.ui.res.painterResource
import com.ghost.io.R
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.io.viewmodel.AudioViewModel
import com.ghost.io.ui.screens.AudioScreen
import com.ghost.io.ui.screens.AudioSettingsScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.navigation.NavHostController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.composable
import java.net.URLEncoder
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import com.ghost.io.ui.screens.AppearanceSettingsScreen
import com.ghost.io.ui.screens.DecoderSettingsScreen
import com.ghost.io.ui.screens.GeneralSettingsScreen
import com.ghost.io.ui.screens.GesturesSettingsScreen
import com.ghost.io.ui.screens.PlayerScreen
import com.ghost.io.ui.screens.PlayerSettingsScreen
import com.ghost.io.ui.screens.SettingsScreen
import com.ghost.io.ui.screens.SubtitleSettingsScreen
import com.ghost.io.ui.screens.ThumbnailSettingsScreen
import com.ghost.io.ui.theme.AgonAppTheme
import com.ghost.io.data.ThemePreference
import com.ghost.io.ui.screens.HomeScreen
import com.ghost.io.viewmodel.SettingsViewModel
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Check if app was opened via an external intent (like a file manager)
        var externalVideoUrl: String? = null
        if (intent?.action == Intent.ACTION_VIEW) {
            externalVideoUrl = intent.data?.toString()
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themePreference by settingsViewModel.themePreference.collectAsState()
            val colorPreference by settingsViewModel.colorPreference.collectAsState()
            
            AgonAppTheme(
                themePreference = themePreference,
                colorPreference = colorPreference
            ) {
                MainApp(externalVideoUrl = externalVideoUrl)
            }
        }
    }
}

@Composable
fun MainApp(audioViewModel: AudioViewModel = viewModel(), externalVideoUrl: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNavRoutes = listOf("home", "audio", "settings")
    
    // Automatically navigate to player if opened from an external source
    LaunchedEffect(externalVideoUrl) {
        if (externalVideoUrl != null) {
            val encodedUrl = URLEncoder.encode(externalVideoUrl, "UTF-8")
            navController.navigate("player/$encodedUrl") {
                popUpTo("home")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { 
            Column {
                // Mini Audio Player
                val currentAudio by audioViewModel.currentAudio.collectAsState()
                val isPlaying by audioViewModel.isPlaying.collectAsState()
                
                if (currentAudio != null && currentRoute != "player/{url}") { // Hide mini player when watching video
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(id = R.drawable.ic_music), contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentAudio!!.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                if (!currentAudio!!.artist.isNullOrEmpty() && currentAudio!!.artist != "<unknown>") {
                                    Text(currentAudio!!.artist!!, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { audioViewModel.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause"
                                )
                            }
                        }
                    }
                }
                
                if (currentRoute in showBottomNavRoutes) {
                    BottomNav(navController, currentRoute)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (currentRoute in showBottomNavRoutes) innerPadding else PaddingValues(0.dp)),
        ) {
            composable("home") {
                HomeScreen(onPlayUrl = { url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate("player/$encodedUrl")
                })
            }
            composable("audio") {
                AudioScreen(viewModel = audioViewModel)
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToAppearance = {
                        navController.navigate("appearance_settings")
                    },
                    onNavigateToPlayer = {
                        navController.navigate("player_settings")
                    },
                    onNavigateToThumbnail = {
                        navController.navigate("thumbnail_settings")
                    },
                    onNavigateToGestures = {
                        navController.navigate("gestures_settings")
                    },
                    onNavigateToDecoder = { navController.navigate("decoder_settings") },
                    onNavigateToAudio = { navController.navigate("audio_settings") },
                    onNavigateToSubtitle = { navController.navigate("subtitle_settings") },
                    onNavigateToGeneral = { navController.navigate("general_settings") }
                )
            }
            composable("appearance_settings") {
                AppearanceSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("player_settings") {
                PlayerSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("thumbnail_settings") {
                ThumbnailSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("gestures_settings") {
                GesturesSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("decoder_settings") {
                DecoderSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("subtitle_settings") {
                SubtitleSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("general_settings") {
                GeneralSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("audio_settings") {
                AudioSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "player/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(url, "UTF-8")
                PlayerScreen(url = decodedUrl)
            }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.ic_music), contentDescription = "Audio") },
            label = { Text("Audio") },
            selected = currentRoute == "audio",
            onClick = {
                navController.navigate("audio") {
                    popUpTo("home")
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = {
                navController.navigate("settings") {
                    popUpTo("home")
                }
            },
        )
    }
}
