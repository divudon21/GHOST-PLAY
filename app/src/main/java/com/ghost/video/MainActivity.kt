package com.ghost.video

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ghost.video.viewmodel.AudioViewModel
import com.ghost.video.ui.screens.AudioPlayerScreen
import com.ghost.video.ui.screens.AudioScreen
import com.ghost.video.ui.screens.AudioSettingsScreen
import com.ghost.video.ui.screens.UpdatesScreen
import com.ghost.video.viewmodel.UpdatesViewModel
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
import androidx.compose.runtime.DisposableEffect
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
import com.ghost.video.ui.screens.SearchSettingsScreen
import com.ghost.video.ui.screens.SubtitleSettingsScreen
import com.ghost.video.ui.screens.ThumbnailSettingsScreen
import com.ghost.video.ui.theme.AgonAppTheme
import com.ghost.video.data.ThemePreference
import com.ghost.video.data.AppColor
import com.ghost.video.data.AppTextStyle
import com.ghost.video.ui.screens.HomeScreen
import com.ghost.video.viewmodel.SettingsViewModel
import com.ghost.video.navigation.NextNavigationBar
import com.ghost.video.navigation.GhostTopLevelDestination
import com.ghost.video.ui.screens.SearchScreen
import androidx.compose.foundation.layout.navigationBarsPadding
import java.net.URLDecoder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// Settings pages use a restrained shared-axis transition: gentle enough for a
// smooth return to the settings list without a noticeable jump or heavy motion.
private const val ANIM_DURATION = 260

// Navigation transitions are NextPlayer v0.17.5's own specs: every navigation
// animation runs for 200 ms with linear easing, top-level tab switches cross-fade
// and deeper screens slide in from the edge while the outgoing screen only drifts
// by 30% of its width.
private const val NAV_DURATION = 200

private fun navTween(): FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = NAV_DURATION, easing = LinearEasing)

private fun navAlphaTween(): FiniteAnimationSpec<Float> =
    tween(durationMillis = NAV_DURATION, easing = LinearEasing)

private val TAB_SLIDE_IN_RIGHT = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = navTween()
)

private val TAB_SLIDE_OUT_LEFT = slideOutHorizontally(
    targetOffsetX = { -(it * 0.3f).toInt() },
    animationSpec = navTween()
)

private val TAB_SLIDE_IN_LEFT = slideInHorizontally(
    initialOffsetX = { -(it * 0.3f).toInt() },
    animationSpec = navTween()
)

private val TAB_SLIDE_OUT_RIGHT = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = navTween()
)

private val FADE_IN = fadeIn(animationSpec = navAlphaTween())
private val FADE_OUT = fadeOut(animationSpec = navAlphaTween())

// Top-level tabs cross-fade, exactly like NextPlayer's NavDisplay.
private val TAB_ENTER = fadeIn(animationSpec = navAlphaTween())

private val TAB_EXIT = fadeOut(animationSpec = navAlphaTween())

class MainActivity : ComponentActivity() {
    // Latest external video handed to us through an ACTION_VIEW intent. Kept as
    // state so a second "Open with Ghost Play" while the app is already running
    // (which arrives on onNewIntent, not onCreate) is opened too.
    private var pendingExternalUrl by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* No UI depends on the answer; the media notification simply shows or not. */ }

    private fun readExternalVideoUrl(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.toString()?.let { pendingExternalUrl = it }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Without this, an ACTION_VIEW intent delivered while the activity was
        // already alive was silently dropped: the player only ever read the
        // launch intent inside onCreate.
        readExternalVideoUrl(intent)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        SharedPlayer.isInPictureInPicture.value = isInPictureInPictureMode
    }

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
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() must run after super.onCreate(); calling it before
        // was harmless but wrong (it touches the window the superclass sets up).
        enableEdgeToEdge()

        SharedPlayer.appContext = applicationContext
        // Check if app was opened via an external intent (like a file manager)
        readExternalVideoUrl(intent)

        // Android 13+: the media notification (and its play/pause controls) is
        // invisible without POST_NOTIFICATIONS. The permission was declared but
        // only ever requested from the App-update settings page, so music
        // controls never appeared for most users.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Legacy per-URL keys are removed once so DataStore reads stay fast. Update
        // checks are no longer scheduled through WorkManager: DownList's updater
        // runs when the app opens instead (see the Updates gate in setContent).
        lifecycleScope.launch {
            val repo = com.ghost.video.data.SettingsRepository(applicationContext)
            repo.cleanupLegacyPerUrlKeys()
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
                appColor = startupTheme?.appColor ?: AppColor.PURPLE,
                textStyle = startupTheme?.textStyle ?: AppTextStyle.DEFAULT,
                boldText = startupTheme?.boldText ?: false,
                highContrastDark = startupTheme?.highContrastDark ?: false
            ) {
                val updatesViewModel: UpdatesViewModel = viewModel()
                UpdatesGate(viewModel = updatesViewModel)
                MainApp(
                    externalVideoUrl = pendingExternalUrl,
                    onExternalVideoConsumed = { pendingExternalUrl = null }
                )
            }
            SideEffect { isInitialThemeReady = true }
        }
    }
}

@Composable
fun MainApp(
    audioViewModel: AudioViewModel = viewModel(),
    externalVideoUrl: String? = null,
    onExternalVideoConsumed: () -> Unit = {}
) {
    // ONE shared, activity-scoped SettingsViewModel for every settings screen.
    // Previously each screen created its own VM, so on open the toggles/capsules
    // briefly showed default values before DataStore emitted the saved ones,
    // causing a visible "reset/refresh" flash. Sharing a single warmed-up VM
    // (its flows are collected Eagerly) means saved values are ready instantly.
    val sharedSettingsViewModel: SettingsViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNavRoutes = GhostTopLevelDestination.entries.map { it.route }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun selectTab(destination: GhostTopLevelDestination) {
        if (currentRoute == destination.route) return
        navController.navigate(destination.route) {
            popUpTo("home") { saveState = false }
            launchSingleTop = true
        }
    }

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
            onExternalVideoConsumed()
        }
    }

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
                        animationSpec = tween(NAV_DURATION, easing = LinearEasing)
                    ) + fadeIn(animationSpec = navAlphaTween()),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(NAV_DURATION, easing = LinearEasing)
                    ) + fadeOut(animationSpec = navAlphaTween()),
                ) {
                    NextNavigationBar(
                        isSelected = { destination -> destination.route == currentRoute },
                        onDestinationClick = { destination -> selectTab(destination) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // Instant padding (no animation): animating the padding re-laid-out the
        // whole content every frame, which caused jitter on the heavy video grid.
        // The bottom bar already slides via AnimatedVisibility and the content
        // cross-fades, so the bar appear/disappear is still smooth.
        val isTabRoute = currentRoute in showBottomNavRoutes
        // Home and Settings draw edge-to-edge: their own Scaffolds fill the status
        // bar / notch area with the screen background and the shared top app bar
        // applies the status bar inset itself. Without this the notch strip stayed
        // bare above those screens.
        val edgeToEdgeTop = currentRoute == "settings" || currentRoute == "home" ||
            currentRoute == "audio" || currentRoute == "search"
        val layoutDirection = LocalLayoutDirection.current
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(
                    top = if (isTabRoute && !edgeToEdgeTop) innerPadding.calculateTopPadding() else 0.dp,
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
                    onNavigateToSearch = {
                        navController.navigate("search") {
                            popUpTo("home") { saveState = false }
                            launchSingleTop = true
                        }
                    },
                    onSwipeNext = { swipeToAdjacentTab(1) },
                    onSwipePrevious = { swipeToAdjacentTab(-1) }
                )
            }
            composable(
                "search",
                enterTransition = { TAB_ENTER },
                exitTransition = { TAB_EXIT },
                popEnterTransition = { TAB_ENTER },
                popExitTransition = { TAB_EXIT }
            ) {
                SearchScreen(
                    settingsViewModel = sharedSettingsViewModel,
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
                    onBack = { navController.popBackStack() },
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
                    onNavigateToSearch = { navController.navigate("search_settings") { launchSingleTop = true } },
                    onNavigateToGeneral = { navController.navigate("general_settings") { launchSingleTop = true } },
                    onNavigateToUpdates = { navController.navigate("updates") { launchSingleTop = true } },
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
                "search_settings",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                SearchSettingsScreen(
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
                "updates",
                enterTransition = { TAB_SLIDE_IN_RIGHT },
                exitTransition = { TAB_SLIDE_OUT_LEFT },
                popEnterTransition = { TAB_SLIDE_IN_LEFT },
                popExitTransition = { TAB_SLIDE_OUT_RIGHT }
            ) {
                UpdatesScreen(onBack = { navController.popBackStack() })
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
                PlayerScreen(url = normalizeRouteUrl(url))
            }
        }
    }
}

/**
 * Navigation arguments arrive in the form the caller put them in: the app
 * navigates with [URLEncoder], and navigation decodes path arguments itself on
 * newer versions of the library. Decoding blindly therefore risked a double
 * decode (a `%2B` in a stream URL turning into a space, breaking playback), so
 * the string is only decoded when it is still percent-encoded.
 */
internal fun normalizeRouteUrl(raw: String): String {
    if (raw.isEmpty()) return raw
    // A real URI (content://, file://, https://, ...) means it is already decoded.
    if (android.net.Uri.parse(raw).scheme != null) return raw
    return try {
        URLDecoder.decode(raw, "UTF-8")
    } catch (_: Exception) {
        raw
    }
}

/**
 * DownList-style update gate: when the app opens (and when it comes back from the
 * background after at least ten seconds) the newest release is looked up, a
 * notification is posted at most once per build, and — if the user kept the
 * "Show pop-up" preference on — an "Update available" dialog is shown.
 */
@Composable
private fun UpdatesGate(viewModel: UpdatesViewModel) {
    val state by viewModel.state.collectAsState()
    var dialogRelease by remember { mutableStateOf<com.ghost.video.data.UpdateRelease?>(null) }

    val showPopupIfWanted: (com.ghost.video.data.UpdateRelease?) -> Unit = { release ->
        if (release != null && viewModel.state.value.popup && dialogRelease == null) {
            dialogRelease = release
        }
    }

    // Automatic check once the stored preferences are ready.
    LaunchedEffect(state.ready) {
        if (state.ready) viewModel.checkOnOpen(showPopupIfWanted)
    }

    // Re-check after a longer stay in the background, like DownList does.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var pausedAt: Long? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> pausedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_RESUME -> {
                    val since = pausedAt
                    pausedAt = null
                    if (since != null && System.currentTimeMillis() - since >= 10_000L) {
                        viewModel.checkOnOpen(showPopupIfWanted)
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val release = dialogRelease
    if (release != null) {
        val compatible = state.installed?.let { release.isCompatibleWith(it) } ?: true
        AlertDialog(
            onDismissRequest = { dialogRelease = null },
            title = { Text("Update available") },
            text = {
                Text(
                    text = "${release.label} is available." + if (compatible) "" else
                        "\n\nThis release has a different signing key. Android may require " +
                        "uninstalling the current app first, which removes local settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogRelease = null
                        viewModel.openRelease(release)
                    },
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { dialogRelease = null }) { Text("Cancel") }
            },
        )
    }
}
