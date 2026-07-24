package com.ghost.video

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.ghost.video.R
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ghost.video.viewmodel.AudioViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.ghost.video.ui.screens.AudioPlayerScreen
import com.ghost.video.ui.screens.AudioScreen
import com.ghost.video.ui.screens.AudioSettingsScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
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
import com.ghost.video.ui.screens.AppearanceSettingsScreen
import com.ghost.video.ui.screens.DecoderSettingsScreen
import com.ghost.video.ui.screens.GeneralSettingsScreen
import com.ghost.video.ui.screens.GesturesSettingsScreen
import com.ghost.video.ui.screens.PlayerScreen
import com.ghost.video.ui.screens.PlayerSettingsScreen
import com.ghost.video.ui.screens.SettingsScreen
import com.ghost.video.ui.screens.SubtitleSettingsScreen
import com.ghost.video.ui.screens.ThumbnailSettingsScreen
import com.ghost.video.ui.theme.AgonAppTheme
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.screens.HomeScreen
import com.ghost.video.viewmodel.SettingsViewModel
import java.net.URLDecoder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// Animation durations tuned for quick, native-feeling navigation.
private const val ANIM_DURATION = 220
private const val MINI_PLAYER_IN = 260
private const val MINI_PLAYER_OUT = 200

private fun navTween(): FiniteAnimationSpec<IntOffset> = tween(durationMillis = ANIM_DURATION, easing = FastOutSlowInEasing)
private fun alphaTween() = tween<Float>(durationMillis = ANIM_DURATION, easing = FastOutSlowInEasing)

// Shared-axis transitions: shorter travel keeps settings pages feeling smooth even on low-end devices.
private val TAB_SLIDE_IN_RIGHT = slideInHorizontally(
    initialOffsetX = { it / 4 },
    animationSpec = navTween()
) + fadeIn(animationSpec = alphaTween())

private val TAB_SLIDE_OUT_LEFT = slideOutHorizontally(
    targetOffsetX = { -it / 6 },
    animationSpec = navTween()
) + fadeOut(animationSpec = alphaTween())

private val TAB_SLIDE_IN_LEFT = slideInHorizontally(
    initialOffsetX = { -it / 4 },
    animationSpec = navTween()
) + fadeIn(animationSpec = alphaTween())

private val TAB_SLIDE_OUT_RIGHT = slideOutHorizontally(
    targetOffsetX = { it / 6 },
    animationSpec = navTween()
) + fadeOut(animationSpec = alphaTween())

private val FADE_IN = fadeIn(animationSpec = alphaTween())
private val FADE_OUT = fadeOut(animationSpec = alphaTween())

// Fade-through transition for the bottom-nav tabs (Home / Audio / Settings).
// This is the Material "fade through" pattern used by many top apps: the
// outgoing screen fades out while slightly shrinking, and the incoming screen
// fades in while gently scaling up — no sliding. Feels calm and premium.
private const val TAB_FADE_DURATION = 160

// A simple, light cross-fade for bottom-nav tabs. No scale/delay — those caused a
// visible pause + extra GPU work that felt like jitter when switching Home/Audio/
// Settings. A plain fade is the smoothest and cheapest option.
private val TAB_ENTER =
    fadeIn(animationSpec = tween(durationMillis = TAB_FADE_DURATION, easing = FastOutSlowInEasing))

private val TAB_EXIT =
    fadeOut(animationSpec = tween(durationMillis = TAB_FADE_DURATION, easing = FastOutSlowInEasing))

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
            val appPalette by settingsViewModel.appPalette.collectAsState()
            val highContrastDark by settingsViewModel.highContrastDark.collectAsState()
            
            AgonAppTheme(
                themePreference = themePreference,
                palette = appPalette,
                highContrastDark = highContrastDark
            ) {
                MainApp(externalVideoUrl = externalVideoUrl)
            }
        }
    }
}

@Composable
fun MainApp(audioViewModel: AudioViewModel = viewModel(), externalVideoUrl: String? = null) {
    // ONE shared, activity-scoped SettingsViewModel for every settings screen.
    // Previously each screen created its own VM, so on open the toggles/capsules
    // briefly showed default values before DataStore emitted the saved ones,
    // causing a visible "reset/refresh" flash. Sharing a single warmed-up VM
    // (its flows are collected Eagerly) means saved values are ready instantly.
    val sharedSettingsViewModel: SettingsViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNavRoutes = listOf("home", "audio", "settings")
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                // Mini Audio Player with slide-up/down animation
                val currentAudio by audioViewModel.currentAudio.collectAsState()
                val isPlaying by audioViewModel.isPlaying.collectAsState()
                
                val showMiniPlayer = currentAudio != null &&
                        currentRoute != "player/{url}" &&
                        currentRoute != "audio_player"

                // Glass effect toggle from Audio settings.
                val settingsViewModel: com.ghost.video.viewmodel.SettingsViewModel = viewModel()
                val glassMiniPlayer by settingsViewModel.glassMiniPlayerEnabled.collectAsState()

                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(MINI_PLAYER_IN)
                    ) + fadeIn(animationSpec = tween(MINI_PLAYER_IN)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(MINI_PLAYER_OUT)
                    ) + fadeOut(animationSpec = tween(MINI_PLAYER_OUT))
                ) {
                    if (currentAudio != null) {
                        // Mini player uses NEUTRAL surface colours so the selected
                        // app colour does NOT apply to it.
                        val containerColor = if (glassMiniPlayer)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant

                        val glassModifier = if (glassMiniPlayer)
                            Modifier.background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ).border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        else Modifier

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .then(glassModifier)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        navController.navigate("audio_player") { launchSingleTop = true }
                                    })
                                }
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        // Swipe up (negative drag) opens the full player.
                                        if (dragAmount < -8f) {
                                            navController.navigate("audio_player") { launchSingleTop = true }
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Music icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(id = R.drawable.ic_music),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        currentAudio!!.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!currentAudio!!.artist.isNullOrEmpty() && currentAudio!!.artist != "<unknown>") {
                                        Text(
                                            currentAudio!!.artist!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                // Previous
                                IconButton(onClick = { audioViewModel.skipToPrevious() }) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Play/Pause
                                IconButton(onClick = { audioViewModel.togglePlayPause() }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Next
                                IconButton(onClick = { audioViewModel.skipToNext() }) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Close/Stop
                                IconButton(onClick = { audioViewModel.stop(context) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
            composable(
                "home",
                enterTransition = { null },
                exitTransition = { null },
                popEnterTransition = { null },
                popExitTransition = { null }
            ) {
                HomeScreen(onPlayUrl = { url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate("player/$encodedUrl")
                })
            }
            composable(
                "audio",
                enterTransition = { null },
                exitTransition = { null },
                popEnterTransition = { null },
                popExitTransition = { null }
            ) {
                AudioScreen(viewModel = audioViewModel)
            }
            composable(
                "audio_player",
                enterTransition = {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
                },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))
                }
            ) {
                AudioPlayerScreen(
                    audioViewModel = audioViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "settings",
                enterTransition = { null },
                exitTransition = { null },
                popEnterTransition = { null },
                popExitTransition = { null }
            ) {
                SettingsScreen(
                    viewModel = sharedSettingsViewModel,
                    onNavigateToAppearance = {
                        navController.navigate("appearance_settings") { launchSingleTop = true }
                    },
                    onNavigateToPlayer = {
                        navController.navigate("player_settings") { launchSingleTop = true }
                    },
                    onNavigateToThumbnail = {
                        navController.navigate("thumbnail_settings") { launchSingleTop = true }
                    },
                    onNavigateToGestures = {
                        navController.navigate("gestures_settings") { launchSingleTop = true }
                    },
                    onNavigateToDecoder = { navController.navigate("decoder_settings") { launchSingleTop = true } },
                    onNavigateToAudio = { navController.navigate("audio_settings") { launchSingleTop = true } },
                    onNavigateToSubtitle = { navController.navigate("subtitle_settings") { launchSingleTop = true } },
                    onNavigateToGeneral = { navController.navigate("general_settings") { launchSingleTop = true } }
                )
            }
            composable(
                "appearance_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                AppearanceSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "player_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                PlayerSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "thumbnail_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                ThumbnailSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "gestures_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                GesturesSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "decoder_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                DecoderSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "subtitle_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                SubtitleSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "general_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                GeneralSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "audio_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                AudioSettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                route = "player/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
                enterTransition = { FADE_IN },
                exitTransition = { FADE_OUT },
                popEnterTransition = { FADE_IN },
                popExitTransition = { FADE_OUT }
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
    val routes = listOf("home", "audio", "settings")
    val labels = listOf("Home", "Audio", "Settings")
    val icons = listOf(
        painterResource(id = R.drawable.ic_home),
        painterResource(id = R.drawable.ic_music),
        Icons.Default.Settings
    )
    val currentIndex = routes.indexOf(currentRoute).coerceAtLeast(0)

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val trackBg = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    val trackBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val thumbBg = if (isDark) Color(0xFF3A3A3E) else Color(0xFFFFFFFF)
    val activeColor = if (isDark) Color.White else Color(0xFF111111)
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.38f)

    fun navigateTo(idx: Int) {
        navController.navigate(routes[idx]) {
            popUpTo("home")
            launchSingleTop = true
        }
    }

    val trackHeight = 58.dp
    val pad = 6.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(trackBg)
                .border(1.dp, trackBorder, CircleShape)
                .padding(pad)
        ) {
            val segWidth = maxWidth / routes.size
            // Single cheap tween — no physics loop, drag, haptic or squash/stretch.
            val thumbOffset by animateDpAsState(
                targetValue = segWidth * currentIndex,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "bottomNavThumb"
            )

            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(segWidth)
                    .fillMaxHeight()
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(thumbBg)
            )

            Row(modifier = Modifier.matchParentSize()) {
                for (i in routes.indices) {
                    BottomNavSegment(
                        icon = icons[i],
                        label = labels[i],
                        isActive = currentIndex == i,
                        activeColor = activeColor,
                        inactiveColor = inactiveColor,
                        onClick = { if (currentIndex != i) navigateTo(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavSegment(
    icon: Any,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "bottomNavTint"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    painter = icon as androidx.compose.ui.graphics.painter.Painter,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
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
}
