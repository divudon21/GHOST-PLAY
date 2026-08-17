package com.ghost.video

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.ghost.video.R
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ghost.video.viewmodel.AudioViewModel
import com.ghost.video.ui.screens.AudioPlayerScreen
import com.ghost.video.ui.screens.AudioScreen
import com.ghost.video.ui.screens.AudioSettingsScreen
import com.ghost.video.ui.screens.BatterySaverScreen
import com.ghost.video.ui.screens.AppUpdateScreen
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
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
import com.ghost.video.data.AppPalette
import com.ghost.video.data.AppTextStyle
import com.ghost.video.ui.components.LocalGlowEffect
import com.ghost.video.ui.screens.HomeScreen
import com.ghost.video.viewmodel.SettingsViewModel
import java.net.URLDecoder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// Settings pages use a restrained shared-axis transition: gentle enough for a
// smooth return to the settings list without a noticeable jump or heavy motion.
private const val ANIM_DURATION = 260

private fun navTween(): FiniteAnimationSpec<IntOffset> = tween(durationMillis = ANIM_DURATION, easing = FastOutSlowInEasing)
private fun alphaTween() = tween<Float>(durationMillis = ANIM_DURATION, easing = FastOutSlowInEasing)

// Small shared-axis travel prevents the settings list and child pages from
// visually pulling against each other when the user presses Back.
private val TAB_SLIDE_IN_RIGHT = slideInHorizontally(
    initialOffsetX = { it / 8 },
    animationSpec = navTween()
) + fadeIn(animationSpec = alphaTween())

private val TAB_SLIDE_OUT_LEFT = slideOutHorizontally(
    targetOffsetX = { -it / 10 },
    animationSpec = navTween()
) + fadeOut(animationSpec = alphaTween())

private val TAB_SLIDE_IN_LEFT = slideInHorizontally(
    initialOffsetX = { -it / 8 },
    animationSpec = navTween()
) + fadeIn(animationSpec = alphaTween())

private val TAB_SLIDE_OUT_RIGHT = slideOutHorizontally(
    targetOffsetX = { it / 10 },
    animationSpec = navTween()
) + fadeOut(animationSpec = alphaTween())

private val FADE_IN = fadeIn(animationSpec = alphaTween())
private val FADE_OUT = fadeOut(animationSpec = alphaTween())

// Tab switch transition: a short, cheap cross-fade. The full-screen horizontal
// slide was removed — tabs are switched by a Telegram-style swipe gesture
// (TabSwipe modifier) or by tapping the bottom nav. A fade has zero layout cost,
// so it can't jitter the heavy video grid while it slides.
private const val TAB_FADE_DURATION = 180

private val TAB_ENTER =
    fadeIn(animationSpec = tween(durationMillis = TAB_FADE_DURATION, easing = FastOutSlowInEasing))

private val TAB_EXIT =
    fadeOut(animationSpec = tween(durationMillis = TAB_FADE_DURATION, easing = FastOutSlowInEasing))

class MainActivity : ComponentActivity() {
    // Auto Picture-in-Picture (system): when the user leaves the app while a
    // video is playing, enter system PiP. Android 12+ (S) handles this through
    // setAutoEnterEnabled(), so this covers Android 7–11 (back/home button).
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            SharedPlayer.autoPipEnabled
        ) {
            val p = SharedPlayer.player
            if (p != null && p.isPlaying && !isInPictureInPictureMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } else {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Check if app was opened via an external intent (like a file manager)
        var externalVideoUrl: String? = null
        if (intent?.action == Intent.ACTION_VIEW) {
            externalVideoUrl = intent.data?.toString()
        }

        // If the user previously enabled update notifications, make sure the daily
        // background check is (re)scheduled after an app restart / reboot.
        lifecycleScope.launch {
            val repo = com.ghost.video.data.SettingsRepository(applicationContext)
            // Remove legacy per-URL keys once, so DataStore reads stay fast.
            repo.cleanupLegacyPerUrlKeys()
            if (repo.updateNotifications.first()) {
                com.ghost.video.data.UpdateWorker.schedule(applicationContext)
            }
        }

        // Previously this gate blocked the first frame until DataStore returned
        // the saved theme, which froze the app for 1-2s on cold starts (blank
        // screen). The UI now composes immediately with fallback defaults, so the
        // gate only skips frames until the first composition is ready, and a short
        // timeout acts as a safety net so drawing can never stay blocked.
        var isInitialThemeReady = false
        val contentView = window.decorView
        val firstDrawGate = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!isInitialThemeReady) return false
                if (contentView.viewTreeObserver.isAlive) {
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                }
                return true
            }
        }
        contentView.viewTreeObserver.addOnPreDrawListener(firstDrawGate)
        // Safety net: never block the first frame for more than a moment.
        contentView.postDelayed({ isInitialThemeReady = true }, 350L)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val startupTheme by settingsViewModel.startupThemeSettings.collectAsState()

            // Render immediately with app defaults; the persisted theme swaps in
            // the moment DataStore emits it. This keeps startup instant instead of
            // waiting on the disk read.
            AgonAppTheme(
                themePreference = startupTheme?.theme ?: ThemePreference.SYSTEM,
                palette = startupTheme?.palette ?: AppPalette.MONOCHROME,
                textStyle = startupTheme?.textStyle ?: AppTextStyle.DEFAULT,
                boldText = startupTheme?.boldText ?: false,
                highContrastDark = startupTheme?.highContrastDark ?: false
            ) {
                MainApp(externalVideoUrl = externalVideoUrl)
            }
            SideEffect { isInitialThemeReady = true }
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
    val glowEffect by sharedSettingsViewModel.glowEffect.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNavRoutes = listOf("home", "audio", "settings")
    val context = androidx.compose.ui.platform.LocalContext.current

    // Telegram-style tab swipe: moves to the adjacent tab (index + delta) without
    // growing the back stack — same navigation the bottom nav uses.
    fun swipeToAdjacentTab(delta: Int) {
        val idx = showBottomNavRoutes.indexOf(currentRoute).coerceAtLeast(0)
        val target = idx + delta
        if (target in showBottomNavRoutes.indices) {
            navController.navigate(showBottomNavRoutes[target]) {
                popUpTo("home")
                launchSingleTop = true
            }
        }
    }
    
    // Automatically navigate to player if opened from an external source
    LaunchedEffect(externalVideoUrl) {
        if (externalVideoUrl != null) {
            val encodedUrl = URLEncoder.encode(externalVideoUrl, "UTF-8")
            navController.navigate("player/$encodedUrl") {
                popUpTo("home")
            }
        }
    }

    CompositionLocalProvider(LocalGlowEffect provides glowEffect) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { 
            Column {
                // Compact mini audio player.
                val currentAudio by audioViewModel.currentAudio.collectAsState()
                val isPlaying by audioViewModel.isPlaying.collectAsState()
                val miniPlayerAudio = currentAudio

                val showMiniPlayer = miniPlayerAudio != null &&
                        currentRoute != "player/{url}" &&
                        currentRoute != "audio_player"

                // A flat, compact mini player: no glass shader, entrance animation,
                // stacked gesture detectors, elevation, or extra settings observer.
                if (showMiniPlayer && miniPlayerAudio != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clickable {
                                    navController.navigate("audio_player") { launchSingleTop = true }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 7.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Music icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        miniPlayerAudio.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!miniPlayerAudio.artist.isNullOrEmpty() && miniPlayerAudio.artist != "<unknown>") {
                                        Text(
                                            miniPlayerAudio.artist,
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

                // Slide + fade the bottom nav in/out when moving between the tab
                // screens and the deeper pages. A hard cut here is what makes Back
                // feel like a jump instead of a smooth return.
                AnimatedVisibility(
                    visible = currentRoute in showBottomNavRoutes,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)),
                ) {
                    BottomNav(navController, currentRoute)
                }
            }
        },
    ) { innerPadding ->
        // Instant padding (no animation): animating the padding re-laid-out the
        // whole content every frame, which caused jitter on the heavy video grid.
        // The bottom bar already slides via AnimatedVisibility and the content
        // cross-fades, so the bar appear/disappear is still smooth.
        val isTabRoute = currentRoute in showBottomNavRoutes
        val layoutDirection = LocalLayoutDirection.current
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(
                top = if (isTabRoute) innerPadding.calculateTopPadding() else 0.dp,
                bottom = if (isTabRoute) innerPadding.calculateBottomPadding() else 0.dp,
                start = if (isTabRoute) innerPadding.calculateStartPadding(layoutDirection) else 0.dp,
                end = if (isTabRoute) innerPadding.calculateEndPadding(layoutDirection) else 0.dp
            ),
        ) {
            composable(
                "home",
                enterTransition = { TAB_ENTER },
                exitTransition = { TAB_EXIT },
                popEnterTransition = { TAB_ENTER },
                popExitTransition = { TAB_EXIT }
            ) {
                HomeScreen(
                    onPlayUrl = { url ->
                        val encodedUrl = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("player/$encodedUrl")
                    },
                    onSwipeNext = { swipeToAdjacentTab(1) },
                    onSwipePrevious = { swipeToAdjacentTab(-1) }
                )
            }
            composable(
                "audio",
                enterTransition = { TAB_ENTER },
                exitTransition = { TAB_EXIT },
                popEnterTransition = { TAB_ENTER },
                popExitTransition = { TAB_EXIT }
            ) {
                AudioScreen(
                    viewModel = audioViewModel,
                    onSwipeNext = { swipeToAdjacentTab(1) },
                    onSwipePrevious = { swipeToAdjacentTab(-1) }
                )
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
                enterTransition = { TAB_ENTER },
                exitTransition = { TAB_EXIT },
                popEnterTransition = { TAB_ENTER },
                popExitTransition = { TAB_EXIT }
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
                    onNavigateToGeneral = { navController.navigate("general_settings") { launchSingleTop = true } },
                    onNavigateToBatterySaver = { navController.navigate("battery_saver") { launchSingleTop = true } },
                    onNavigateToAppUpdate = { navController.navigate("app_update") { launchSingleTop = true } },
                    onSwipeNext = { swipeToAdjacentTab(1) },
                    onSwipePrevious = { swipeToAdjacentTab(-1) }
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
                "battery_saver",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                BatterySaverScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = sharedSettingsViewModel
                )
            }
            composable(
                "app_update",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                AppUpdateScreen(
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
}

@Composable
fun BottomNav(navController: NavHostController, currentRoute: String?) {
    val routes = listOf("home", "audio", "settings")
    val labels = listOf("Home", "Audio", "Settings")
    val icons = listOf(
        painterResource(id = R.drawable.ic_home),
        painterResource(id = R.drawable.ic_music),
        painterResource(id = R.drawable.ic_settings)
    )
    val currentIndex = routes.indexOf(currentRoute).coerceAtLeast(0)
    val density = LocalDensity.current

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Keep the exact existing capsule geometry, but tint the moving selected
    // segment with the active app colour from Appearance settings.
    val trackBg = if (isDark) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.035f)
    val trackBorder = if (isDark) Color.White.copy(alpha = 0.075f) else Color.Black.copy(alpha = 0.06f)
    val thumbBg = MaterialTheme.colorScheme.primary
    val activeColor = MaterialTheme.colorScheme.onPrimary
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.38f)

    fun navigateTo(idx: Int) {
        navController.navigate(routes[idx]) {
            popUpTo("home")
            launchSingleTop = true
        }
    }

    val trackHeight = 56.dp
    val pad = 5.dp

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
            // Professional spring-driven pill (the standard used by polished
            // animated bottom-nav libraries): spring physics accelerates and
            // settles naturally, unlike a mechanical tween. Still a single-value
            // animateDpAsState — only the pill's offset recomposes per frame, so
            // performance stays flat.
            val thumbOffset by animateDpAsState(
                targetValue = segWidth * currentIndex,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                label = "bottomNavThumb"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { translationX = with(density) { thumbOffset.toPx() } }
                    .width(segWidth)
                    .fillMaxHeight()
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
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "bottomNavTint"
    )

    // Subtle scale "pop" on the active icon — springy, no layout cost
    // (graphicsLayer transform only, not a recomposition-heavy size change).
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.14f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "bottomNavIconScale"
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
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            } else {
                Icon(
                    painter = icon as androidx.compose.ui.graphics.painter.Painter,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
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

