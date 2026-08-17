package com.ghost.video.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.ghost.video.R
import com.ghost.video.SharedPlayer
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghost.video.data.DecoderPriority
import com.ghost.video.data.DialogThemePreference
import com.ghost.video.data.OrientationPreference
import com.ghost.video.data.SettingsRepository
import com.ghost.video.data.SubtitleFont
import com.ghost.video.data.ThemePreference
import com.ghost.video.ui.theme.getColorScheme
import com.ghost.video.ui.components.AppLoadingIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(url: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }

    // Lock state - using rememberSavedInstanceState to survive recomposition
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
    
    // PlayerView reference for controlling visibility
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Mirrors Media3 buffering so the official Material 3 circular loader is
    // shown directly over the central play/pause control while media is loading.
    var isBuffering by remember { mutableStateOf(true) }
    
    // Dialog states
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showAudioAdjustDialog by remember { mutableStateOf(false) }
    
    // Aspect ratio state
    var currentAspectRatio by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var audioSyncMs by remember { mutableFloatStateOf(0f) }
    var audioPitch by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var lastVolumeBeforeMute by remember { mutableFloatStateOf(1f) }
    
    // Collect settings
    val decoderPriority by settingsRepository.decoderPriority.collectAsState(initial = DecoderPriority.PREFER_DEVICE)
    val subtitleFont by settingsRepository.subtitleFont.collectAsState(initial = SubtitleFont.DEFAULT)
    val subtitleBold by settingsRepository.subtitleBold.collectAsState(initial = true)
    val subtitleSize by settingsRepository.subtitleSize.collectAsState(initial = 20)
    val subtitleBackground by settingsRepository.subtitleBackground.collectAsState(initial = false)
    val subtitleEmbeddedStyles by settingsRepository.subtitleEmbeddedStyles.collectAsState(initial = true)
    val dialogThemePreference by settingsRepository.dialogThemePreference.collectAsState(initial = DialogThemePreference.FOLLOW_SYSTEM)
    val appPalette by settingsRepository.appPalette.collectAsState(initial = com.ghost.video.data.AppPalette.MONOCHROME)
    val themePreference by settingsRepository.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
    val loadingIndicatorStyle by settingsRepository.loadingIndicatorStyle.collectAsState(
        initial = com.ghost.video.data.LoadingIndicatorStyle.ROUNDED_POLYGON
    )
    val pipMode by settingsRepository.pipMode.collectAsState(initial = true)
    val autoPipMode by settingsRepository.autoPipMode.collectAsState(initial = false)
    val backgroundPlay by settingsRepository.backgroundPlay.collectAsState(initial = false)
    val playerOrientation by settingsRepository.playerOrientation.collectAsState(initial = OrientationPreference.AUTO)
    val systemCaptionStyle by settingsRepository.systemCaptionStyle.collectAsState(initial = false)

    // Immersive Mode
    DisposableEffect(Unit) {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        onDispose {
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val params = window.attributes
                    params.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    window.attributes = params
                }
                
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Respect the Screen Orientation setting (Auto / Landscape / Portrait / Sensor).
    LaunchedEffect(playerOrientation) {
        val requested = when (playerOrientation) {
            OrientationPreference.AUTO -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationPreference.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationPreference.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationPreference.SENSOR_LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        activity?.requestedOrientation = requested
    }

    // Remember brightness: restore the saved level on open and, on exit, persist
    // the current level before handing brightness back to the system.
    DisposableEffect(Unit) {
        coroutineScope.launch {
            if (settingsRepository.rememberBrightness.first()) {
                val saved = settingsRepository.getBrightness().first()
                if (saved in 0f..1f) {
                    activity?.window?.let { w ->
                        w.attributes = w.attributes.apply { screenBrightness = saved }
                    }
                }
            }
        }
        onDispose {
            coroutineScope.launch {
                if (settingsRepository.rememberBrightness.first()) {
                    val current = activity?.window?.attributes?.screenBrightness ?: -1f
                    if (current in 0f..1f) settingsRepository.saveBrightness(current)
                }
            }
            activity?.window?.let { w ->
                w.attributes = w.attributes.apply {
                    screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
    
    // State for overlays
    var zoomPercent by remember { mutableIntStateOf(100) }
    var showZoom by remember { mutableStateOf(false) }
    var zoomTrigger by remember { mutableIntStateOf(0) }
    
    var volumePercent by remember { mutableIntStateOf(0) }
    var showVolume by remember { mutableStateOf(false) }
    var volumeTrigger by remember { mutableIntStateOf(0) }
    
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    DisposableEffect(Unit) {
        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
        } else {
            null
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }
    
    // Gesture settings
    val gestureSeekEnabled by settingsRepository.gestureSeekEnabled.collectAsState(initial = true)
    val gestureSeekSensitivity by settingsRepository.gestureSeekSensitivity.collectAsState(initial = 0.5f)
    val gestureBrightnessEnabled by settingsRepository.gestureBrightnessEnabled.collectAsState(initial = true)
    val gestureBrightnessSensitivity by settingsRepository.gestureBrightnessSensitivity.collectAsState(initial = 0.5f)
    val gestureVolumeEnabled by settingsRepository.gestureVolumeEnabled.collectAsState(initial = true)
    val gestureVolumeSensitivity by settingsRepository.gestureVolumeSensitivity.collectAsState(initial = 0.5f)
    val gestureZoomEnabled by settingsRepository.gestureZoomEnabled.collectAsState(initial = true)
    val gesturePanEnabled by settingsRepository.gesturePanEnabled.collectAsState(initial = false)
    val gestureDoubleTapEnabled by settingsRepository.gestureDoubleTapEnabled.collectAsState(initial = true)

    var brightnessPercent by remember { mutableIntStateOf(0) }
    var showBrightness by remember { mutableStateOf(false) }
    var brightnessTrigger by remember { mutableIntStateOf(0) }

    var seekMessage by remember { mutableStateOf("") }
    var showSeek by remember { mutableStateOf(false) }
    var seekTrigger by remember { mutableIntStateOf(0) }
    var isForwardSeek by remember { mutableStateOf(true) }
    
    // Auto-hide effects
    LaunchedEffect(zoomTrigger) {
        if (zoomTrigger > 0) {
            showZoom = true
            delay(1500)
            showZoom = false
        }
    }
    LaunchedEffect(volumeTrigger) {
        if (volumeTrigger > 0) {
            showVolume = true
            delay(1500)
            showVolume = false
        }
    }
    LaunchedEffect(brightnessTrigger) {
        if (brightnessTrigger > 0) {
            showBrightness = true
            delay(1500)
            showBrightness = false
        }
    }
    LaunchedEffect(seekTrigger) {
        if (seekTrigger > 0) {
            showSeek = true
            delay(800)
            showSeek = false
        }
    }
    
    // Show unlock button temporarily when tapped on locked screen
    LaunchedEffect(showUnlockButton) {
        if (showUnlockButton) {
            delay(3000)
            showUnlockButton = false
        }
    }
    
    val trackSelector = remember {
        DefaultTrackSelector(context)
    }
    
    val exoPlayer = remember(decoderPriority) {
        val extensionMode = when (decoderPriority) {
            DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        }
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(extensionMode)
            
        val extractorsFactory = DefaultExtractorsFactory()
        
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context, extractorsFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(url))
                
                coroutineScope.launch {
                    val resumePlayback = settingsRepository.resumePlayback.first()
                    val rememberSelections = settingsRepository.rememberSelections.first()
                    val savedPosition = settingsRepository.getPlaybackPosition(url).first()
                    val savedAudioId = settingsRepository.getAudioTrack(url).first()
                    val savedTextId = settingsRepository.getTextTrack(url).first()

                    if (resumePlayback && savedPosition > 0) {
                        seekTo(savedPosition)
                    }

                    if (rememberSelections) {
                        addListener(object : Player.Listener {
                            override fun onTracksChanged(tracks: Tracks) {
                                var paramsBuilder = trackSelectionParameters.buildUpon()
                                var changed = false

                                if (savedAudioId.isNotEmpty()) {
                                    for (group in tracks.groups) {
                                        if (group.type == C.TRACK_TYPE_AUDIO) {
                                            for (i in 0 until group.length) {
                                                val format = group.getTrackFormat(i)
                                                if (format.id == savedAudioId || format.language == savedAudioId) {
                                                    paramsBuilder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                    changed = true
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }

                                if (savedTextId.isNotEmpty()) {
                                    for (group in tracks.groups) {
                                        if (group.type == C.TRACK_TYPE_TEXT) {
                                            for (i in 0 until group.length) {
                                                val format = group.getTrackFormat(i)
                                                if (format.id == savedTextId || format.language == savedTextId) {
                                                    paramsBuilder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                                    changed = true
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }

                                if (changed) {
                                    trackSelectionParameters = paramsBuilder.build()
                                }
                                removeListener(this)
                            }
                        })
                    }

                    // Default playback speed, volume boost and autoplay toggles.
                    val defaultSpeed = settingsRepository.defaultPlaybackSpeed.first()
                    val boostEnabled = settingsRepository.volumeBoostEnabled.first()
                    val shouldAutoplay = settingsRepository.autoplay.first()

                    playbackParameters = PlaybackParameters(defaultSpeed.coerceIn(0.25f, 3f))
                    volume = if (boostEnabled) 2.0f else 1.0f

                    prepare()
                    playWhenReady = shouldAutoplay
                }
            }
    }

    // System Auto-PiP: keep the player reference (MainActivity checks it in
    // onUserLeaveHint) and enable Android 12+ auto-enter so leaving the app
    // while a video is playing floats it into system Picture-in-Picture.
    DisposableEffect(Unit) {
        SharedPlayer.player = exoPlayer
        SharedPlayer.autoPipEnabled = autoPipMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pipParams = android.app.PictureInPictureParams.Builder()
                .setAutoEnterEnabled(autoPipMode)
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            activity?.setPictureInPictureParams(pipParams)
        }
        onDispose {
            if (SharedPlayer.player === exoPlayer) SharedPlayer.player = null
            SharedPlayer.autoPipEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val pipParams = android.app.PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(false)
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                activity?.setPictureInPictureParams(pipParams)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch {
                    if (settingsRepository.resumePlayback.first()) {
                        settingsRepository.savePlaybackPosition(url, exoPlayer.currentPosition)
                    }
                    
                    val tracks = exoPlayer.currentTracks
                    val rememberSelections = settingsRepository.rememberSelections.first()
                    for (group in tracks.groups) {
                        if (group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    val idToSave = format.id ?: format.language ?: ""
                                    if (idToSave.isNotEmpty() && rememberSelections) {
                                        if (group.type == C.TRACK_TYPE_AUDIO) {
                                            settingsRepository.saveAudioTrack(url, idToSave)
                                        } else if (group.type == C.TRACK_TYPE_TEXT) {
                                            settingsRepository.saveTextTrack(url, idToSave)
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    DisposableEffect(exoPlayer) {
        val bufferingListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(bufferingListener)
        isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING

        onDispose {
            exoPlayer.removeListener(bufferingListener)
            if (!com.ghost.video.SharedPlayer.isFloatingMode) {
                coroutineScope.launch {
                    if (settingsRepository.resumePlayback.first()) {
                        settingsRepository.savePlaybackPosition(url, exoPlayer.currentPosition)
                    }
                    
                    val tracks = exoPlayer.currentTracks
                    val rememberSelections = settingsRepository.rememberSelections.first()
                    for (group in tracks.groups) {
                        if (group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    val idToSave = format.id ?: format.language ?: ""
                                    if (idToSave.isNotEmpty() && rememberSelections) {
                                        if (group.type == C.TRACK_TYPE_AUDIO) {
                                            settingsRepository.saveAudioTrack(url, idToSave)
                                        } else if (group.type == C.TRACK_TYPE_TEXT) {
                                            settingsRepository.saveTextTrack(url, idToSave)
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
                }
                exoPlayer.release()
            }
        }
    }
    
    // Get dialog colors based on preference
    val dialogColors = rememberDialogColors(dialogThemePreference, appPalette, themePreference)
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    clipChildren = false
                    (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_content_frame) as? android.view.ViewGroup)?.clipChildren = false
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setOnApplyWindowInsetsListener { _, _ -> android.view.WindowInsets.CONSUMED }
                    } else {
                        setOnApplyWindowInsetsListener { _, insets -> insets }
                    }
                    
                    setPadding(0, 0, 0, 0)
                    
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = !backgroundPlay

                    subtitleView?.apply {
                        if (systemCaptionStyle) {
                            // Use the device's captioning settings from Android's
                            // accessibility preferences.
                            setUserDefaultStyle()
                            setUserDefaultTextSize()
                        } else {
                            setApplyEmbeddedFontSizes(subtitleEmbeddedStyles)
                            setApplyEmbeddedStyles(subtitleEmbeddedStyles)

                            // Bundled variable fonts so each subtitle option is
                            // visibly different from the system default.
                            val typeface = when (subtitleFont) {
                                SubtitleFont.DEFAULT -> Typeface.DEFAULT
                                SubtitleFont.LORA -> ResourcesCompat.getFont(ctx, R.font.lora_variable)
                                SubtitleFont.JETBRAINS_MONO -> ResourcesCompat.getFont(ctx, R.font.jetbrains_mono_variable)
                                SubtitleFont.NUNITO -> ResourcesCompat.getFont(ctx, R.font.nunito_variable)
                            }

                            // Apply the user's Font, Bold and Background in BOTH
                            // cases. Previously these were skipped when "Styled
                            // Subtitles" was ON (the default), so the Font / Bold
                            // / Size settings appeared to do nothing.
                            val style = CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                if (subtitleBackground) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                if (subtitleBold) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
                                android.graphics.Color.BLACK,
                                typeface
                            )
                            setStyle(style)

                            // Fixed text size only when embedded styles are off;
                            // otherwise the subtitle file's own sizes win.
                            if (!subtitleEmbeddedStyles) {
                                setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.toFloat())
                            }
                        }
                    }
                    systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    
                    playerViewRef = this
                    
                    val basicControls = findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_basic_controls)
                    
                    fun ImageView.setClickFeedback(onClick: () -> Unit) {
                        setOnClickListener {
                            setColorFilter(android.graphics.Color.CYAN)
                            postDelayed({
                                setColorFilter(android.graphics.Color.WHITE)
                            }, 200)
                            onClick()
                        }
                    }
                    
                    // Quality Button
                    val qualityButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_hq)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback { showQualityDialog = true }
                    }
                    
                    // Mute / Unmute Button
                    val muteButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_volume_unmute)
                        setColorFilter(android.graphics.Color.WHITE)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setOnClickListener {
                            setColorFilter(android.graphics.Color.CYAN)
                            postDelayed({ setColorFilter(android.graphics.Color.WHITE) }, 200)
                            if (exoPlayer.volume > 0f) {
                                lastVolumeBeforeMute = exoPlayer.volume
                                exoPlayer.volume = 0f
                                isMuted = true
                                setImageResource(R.drawable.ic_volume_mute)
                            } else {
                                exoPlayer.volume = lastVolumeBeforeMute.coerceIn(0.1f, 1f)
                                isMuted = false
                                setImageResource(R.drawable.ic_volume_unmute)
                            }
                        }
                    }
                    
                    // Audio Sync / Pitch Button
                    val audioAdjustButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_audio_sync)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback { showAudioAdjustDialog = true }
                    }
                    
                    // Audio Track Button
                    val audioButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_aud)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback { showAudioDialog = true }
                    }
                    
                    val ccButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_cc)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback { showSubtitleDialog = true }
                    }
                    
                    // Aspect Ratio Button
                    val aspectButton = ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_aspect)
                        setColorFilter(android.graphics.Color.WHITE)
                        
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback { showAspectRatioDialog = true }
                    }
                    
                    // PiP Button
                    val pipButton = ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_pip)
                        setColorFilter(android.graphics.Color.WHITE)
                        
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = if (pipMode) View.VISIBLE else View.GONE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val params = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(16, 9))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                @Suppress("DEPRECATION")
                                activity?.enterPictureInPictureMode()
                            }
                        }
                    }
                    
                    // Lock Button
                    val lockButton = ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_lock)
                        setColorFilter(android.graphics.Color.WHITE)
                        
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setOnClickListener {
                            setColorFilter(android.graphics.Color.CYAN)
                            postDelayed({
                                setColorFilter(android.graphics.Color.WHITE)
                            }, 200)
                            
                            useController = false
                            hideController()
                            isLocked = true
                        }
                    }
                    
                    if (basicControls != null) {
                        val settingsButton = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                        val settingsIndex = basicControls.indexOfChild(settingsButton)
                        val insertIndex = if (settingsIndex >= 0) settingsIndex else basicControls.childCount
                        
                        basicControls.addView(lockButton, insertIndex)
                        basicControls.addView(pipButton, insertIndex + 1)
                        basicControls.addView(aspectButton, insertIndex + 2)
                        basicControls.addView(ccButton, insertIndex + 3)
                        basicControls.addView(audioButton, insertIndex + 4)
                        basicControls.addView(audioAdjustButton, insertIndex + 5)
                        basicControls.addView(muteButton, insertIndex + 6)
                        basicControls.addView(qualityButton, insertIndex + 7)
                    }
                    
                    // Gesture handling - Using a wrapper class to properly handle lock state
                    val gestureHandler = PlayerGestureHandler(
                        activity = activity,
                        playerView = this,
                        exoPlayer = exoPlayer,
                        audioManager = audioManager,
                        maxVolume = maxVolume,
                        gestureSeekEnabled = gestureSeekEnabled,
                        gestureSeekSensitivity = gestureSeekSensitivity,
                        gestureBrightnessEnabled = gestureBrightnessEnabled,
                        gestureBrightnessSensitivity = gestureBrightnessSensitivity,
                        gestureVolumeEnabled = gestureVolumeEnabled,
                        gestureVolumeSensitivity = gestureVolumeSensitivity,
                        gestureZoomEnabled = gestureZoomEnabled,
                        gesturePanEnabled = gesturePanEnabled,
                        gestureDoubleTapEnabled = gestureDoubleTapEnabled,
                        onZoomChanged = { percent, trigger ->
                            zoomPercent = percent
                            zoomTrigger = trigger
                        },
                        onVolumeChanged = { percent, trigger ->
                            volumePercent = percent
                            volumeTrigger = trigger
                        },
                        onBrightnessChanged = { percent, trigger ->
                            brightnessPercent = percent
                            brightnessTrigger = trigger
                        },
                        onSeekChanged = { message, forward, trigger ->
                            seekMessage = message
                            isForwardSeek = forward
                            seekTrigger = trigger
                        },
                        isLockedProvider = { isLocked },
                        onShowUnlock = { showUnlockButton = true }
                    )
                    
                    setOnTouchListener { _, event ->
                        gestureHandler.handleTouchEvent(event)
                    }
                }
            },
            update = { playerView ->
                isMuted = exoPlayer.volume == 0f
                if (isLocked) {
                    playerView.useController = false
                } else {
                    playerView.useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Keep the official Material 3 RoundedPolygon loading indicator centered
        // at the player controls while Media3 is buffering.
        AnimatedVisibility(
            visible = isBuffering && !isLocked,
            enter = fadeIn(animationSpec = tween(160)),
            exit = fadeOut(animationSpec = tween(160)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AppLoadingIndicator(
                style = loadingIndicatorStyle,
                size = 64.dp,
                shapeColor = Color.White,
                containerColor = Color.Black.copy(alpha = 0.56f)
            )
        }
        
        // Unlock button overlay when locked
        if (isLocked && showUnlockButton) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, start = 32.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    onClick = {
                        isLocked = false
                        showUnlockButton = false
                        playerViewRef?.useController = true
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Unlock",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Overlay UI
        if (!isLocked) {
            IndicatorOverlay(Icons.Default.ZoomIn, "$zoomPercent%", showZoom)
            IndicatorOverlay(Icons.Default.VolumeUp, "$volumePercent%", showVolume)
            IndicatorOverlay(Icons.Default.BrightnessMedium, "$brightnessPercent%", showBrightness)
        }
        
        // Seek Overlay
        AnimatedVisibility(
            visible = showSeek && !isLocked,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(if (isForwardSeek) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isForwardSeek) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = seekMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
    
    // Quality Dialog
    if (showQualityDialog) {
        TrackSelectionDialog(
            title = "Video Quality",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_VIDEO,
            onDismiss = { showQualityDialog = false },
            dialogColors = dialogColors
        )
    }
    
    // Audio Dialog
    if (showAudioDialog) {
        TrackSelectionDialog(
            title = "Audio Track",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_AUDIO,
            onDismiss = { showAudioDialog = false },
            dialogColors = dialogColors
        )
    }
    
    
    // Subtitle Dialog
    if (showSubtitleDialog) {
        TrackSelectionDialog(
            title = "Subtitles",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_TEXT,
            onDismiss = { showSubtitleDialog = false },
            dialogColors = dialogColors
        )
    }
    
    // Audio Sync / Pitch Dialog
    if (showAudioAdjustDialog) {
        AudioAdjustDialog(
            audioSyncMs = audioSyncMs,
            audioPitch = audioPitch,
            onAudioSyncChange = { value ->
                val deltaMs = value - audioSyncMs
                audioSyncMs = value
                exoPlayer.seekTo((exoPlayer.currentPosition + deltaMs.toLong()).coerceAtLeast(0L))
            },
            onAudioPitchChange = { value ->
                audioPitch = value
                exoPlayer.playbackParameters = PlaybackParameters(exoPlayer.playbackParameters.speed, value)
            },
            onReset = {
                audioSyncMs = 0f
                audioPitch = 1f
                exoPlayer.playbackParameters = PlaybackParameters(exoPlayer.playbackParameters.speed, 1f)
            },
            onDismiss = { showAudioAdjustDialog = false },
            dialogColors = dialogColors
        )
    }
    
    // Aspect Ratio Dialog
    if (showAspectRatioDialog) {
        AspectRatioDialog(
            currentAspectRatio = currentAspectRatio,
            onAspectRatioSelected = { ratio ->
                currentAspectRatio = ratio
                playerViewRef?.let { pv ->
                    when (ratio) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        AspectRatioFrameLayout.RESIZE_MODE_FILL,
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
                        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> {
                            pv.setAspectRatioListener(null)
                            pv.resizeMode = ratio
                        }
                        5 -> { // 19:9
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            pv.setAspectRatioListener { _, _, _ -> 19f / 9f }
                        }
                        6 -> { // 20:9
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            pv.setAspectRatioListener { _, _, _ -> 20f / 9f }
                        }
                        7 -> { // 21:9
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            pv.setAspectRatioListener { _, _, _ -> 21f / 9f }
                        }
                        8 -> { // 16:9
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            pv.setAspectRatioListener { _, _, _ -> 16f / 9f }
                        }
                        9 -> { // 18:9
                            pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            pv.setAspectRatioListener { _, _, _ -> 18f / 9f }
                        }
                    }
                }
                showAspectRatioDialog = false
            },
            onDismiss = { showAspectRatioDialog = false },
            dialogColors = dialogColors
        )
    }
}

// Separate class to handle gestures - this ensures lock state is properly checked
@UnstableApi
class PlayerGestureHandler(
    private val activity: Activity?,
    private val playerView: PlayerView,
    private val exoPlayer: ExoPlayer,
    private val audioManager: AudioManager,
    private val maxVolume: Int,
    private val gestureSeekEnabled: Boolean,
    private val gestureSeekSensitivity: Float,
    private val gestureBrightnessEnabled: Boolean,
    private val gestureBrightnessSensitivity: Float,
    private val gestureVolumeEnabled: Boolean,
    private val gestureVolumeSensitivity: Float,
    private val gestureZoomEnabled: Boolean,
    private val gesturePanEnabled: Boolean,
    private val gestureDoubleTapEnabled: Boolean,
    private val onZoomChanged: (Int, Int) -> Unit,
    private val onVolumeChanged: (Int, Int) -> Unit,
    private val onBrightnessChanged: (Int, Int) -> Unit,
    private val onSeekChanged: (String, Boolean, Int) -> Unit,
    private val isLockedProvider: () -> Boolean,
    private val onShowUnlock: () -> Unit
) {
    private var scale = 1f
    private var transX = 0f
    private var transY = 0f
    private var isBrightnessScroll = false
    private var isVolumeScroll = false
    private var accumulatedVolume = 0f
    private var zoomTriggerCount = 0
    private var volumeTriggerCount = 0
    private var brightnessTriggerCount = 0
    private var seekTriggerCount = 0
    private var normalSpeed = 1f
    
    private val scaleDetector: android.view.ScaleGestureDetector?
    private val gestureDetector: android.view.GestureDetector
    
    init {
        val ctx = playerView.context
        
        scaleDetector = if (gestureZoomEnabled) {
            android.view.ScaleGestureDetector(ctx, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    if (isLockedProvider()) return false
                    
                    scale *= detector.scaleFactor
                    scale = scale.coerceIn(1f, 5f)
                    
                    val surface = playerView.videoSurfaceView as? View
                    if (surface != null) {
                        val maxTransX = (surface.width * (scale - 1)) / 2f
                        val maxTransY = (surface.height * (scale - 1)) / 2f
                        transX = transX.coerceIn(-maxTransX, maxTransX)
                        transY = transY.coerceIn(-maxTransY, maxTransY)
                        
                        surface.scaleX = scale
                        surface.scaleY = scale
                        surface.translationX = transX
                        surface.translationY = transY
                    }
                    
                    onZoomChanged((scale * 100).toInt(), ++zoomTriggerCount)
                    return true
                }
            })
        } else null
        
        gestureDetector = android.view.GestureDetector(ctx, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: android.view.MotionEvent) {
                if (isLockedProvider()) return
                normalSpeed = exoPlayer.playbackParameters.speed
                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(2.0f)
                onSeekChanged("2x Speed", true, ++seekTriggerCount)
            }

            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                if (isLockedProvider()) return false
                if (!gestureDoubleTapEnabled) return false
                val surface = playerView.videoSurfaceView as? View ?: return false
                
                if (e.x > surface.width / 2f) {
                    exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                    onSeekChanged("+10s", true, ++seekTriggerCount)
                } else {
                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                    onSeekChanged("-10s", false, ++seekTriggerCount)
                }
                return true
            }

            override fun onDown(e: android.view.MotionEvent): Boolean {
                if (isLockedProvider()) return false
                isBrightnessScroll = false
                isVolumeScroll = false
                accumulatedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                return false
            }

            override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isLockedProvider()) return false
                
                val surface = playerView.videoSurfaceView as? View ?: return false
                val pointerCount = e2.pointerCount
                
                // Pan gesture: 2 fingers when zoomed in
                if (gesturePanEnabled && scale > 1f && pointerCount >= 2) {
                    transX -= distanceX
                    transY -= distanceY
                    
                    val maxTransX = (surface.width * (scale - 1)) / 2f
                    val maxTransY = (surface.height * (scale - 1)) / 2f
                    transX = transX.coerceIn(-maxTransX, maxTransX)
                    transY = transY.coerceIn(-maxTransY, maxTransY)
                    
                    surface.translationX = transX
                    surface.translationY = transY
                    return true
                }
                
                // Brightness/Volume/Seek gestures: 1 finger (works regardless of zoom)
                if (pointerCount == 1) {
                    if (e1 == null) return false
                    
                    if (!isBrightnessScroll && !isVolumeScroll) {
                        if (abs(distanceY) > abs(distanceX) + 10) {
                            if (e1.x < surface.width / 2f && gestureBrightnessEnabled) {
                                isBrightnessScroll = true
                            } else if (e1.x >= surface.width / 2f && gestureVolumeEnabled) {
                                isVolumeScroll = true
                            } else {
                                return false
                            }
                        } else if (abs(distanceX) > abs(distanceY) + 10 && gestureSeekEnabled) {
                            val seekAmount = (distanceX * 50 * gestureSeekSensitivity).toLong()
                            val newPos = (exoPlayer.currentPosition - seekAmount).coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
                            exoPlayer.seekTo(newPos)
                            // distanceX is positive when the finger moves LEFT, so a
                            // rightward swipe (forward seek) has distanceX < 0.
                            val forward = distanceX < 0
                            val msg = if (forward) "+${(kotlin.math.abs(seekAmount) / 1000).toInt()}s"
                            else "-${(kotlin.math.abs(seekAmount) / 1000).toInt()}s"
                            onSeekChanged(msg, forward, ++seekTriggerCount)
                            return true
                        } else {
                            return false
                        }
                    }

                    if (isBrightnessScroll && gestureBrightnessEnabled) {
                        activity?.window?.let { window ->
                            val lp = window.attributes
                            var currentBrightness = lp.screenBrightness
                            if (currentBrightness < 0f) currentBrightness = 0.5f
                            
                            val sensMultiplier = gestureBrightnessSensitivity * 1.5f
                            val newBrightness = (currentBrightness + distanceY / surface.height * sensMultiplier).coerceIn(0f, 1f)
                            lp.screenBrightness = newBrightness
                            window.attributes = lp
                            
                            onBrightnessChanged((newBrightness * 100).toInt(), ++brightnessTriggerCount)
                        }
                        return true
                    }
                    
                    if (isVolumeScroll && gestureVolumeEnabled) {
                        val sensMultiplier = gestureVolumeSensitivity * 1.5f
                        accumulatedVolume += (distanceY / surface.height) * maxVolume * sensMultiplier
                        val newVol = accumulatedVolume.coerceIn(0f, maxVolume.toFloat()).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        
                        onVolumeChanged(((newVol.toFloat() / maxVolume) * 100).toInt(), ++volumeTriggerCount)
                        return true
                    }
                }
                return false
            }
        })
    }
    
    fun handleTouchEvent(event: android.view.MotionEvent): Boolean {
        if (isLockedProvider()) {
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                onShowUnlock()
            }
            return true
        }
        
        if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) {
            if (exoPlayer.playbackParameters.speed == 2.0f) {
                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(normalSpeed)
            }
        }
        
        scaleDetector?.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return false
    }
}

@Composable
fun rememberDialogColors(
    dialogTheme: DialogThemePreference,
    palette: com.ghost.video.data.AppPalette,
    themePreference: ThemePreference
): DialogColors {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    return remember(dialogTheme, palette, themePreference, isSystemDark) {
        when (dialogTheme) {
            DialogThemePreference.FOLLOW_SYSTEM -> {
                if (isSystemDark) DialogColors.Dark else DialogColors.Light
            }
            DialogThemePreference.DARK -> DialogColors.Dark
            DialogThemePreference.LIGHT -> DialogColors.Light
            DialogThemePreference.CUSTOM -> {
                val colorScheme = getColorScheme(palette, true)
                DialogColors.Custom(
                    backgroundColor = colorScheme.surface,
                    textColor = colorScheme.onSurface,
                    selectedColor = colorScheme.primary,
                    selectedTextColor = colorScheme.onPrimary
                )
            }
        }
    }
}

data class DialogColors(
    val backgroundColor: Color,
    val textColor: Color,
    val selectedColor: Color,
    val selectedTextColor: Color
) {
    companion object {
        val Dark = DialogColors(
            backgroundColor = Color(0xFF1E1E1E),
            textColor = Color.White,
            selectedColor = Color(0xFFBB86FC),
            selectedTextColor = Color.Black
        )
        
        val Light = DialogColors(
            backgroundColor = Color.White,
            textColor = Color.Black,
            selectedColor = Color(0xFF6200EE),
            selectedTextColor = Color.White
        )
        
        fun Custom(backgroundColor: Color, textColor: Color, selectedColor: Color, selectedTextColor: Color) = DialogColors(
            backgroundColor = backgroundColor,
            textColor = textColor,
            selectedColor = selectedColor,
            selectedTextColor = selectedTextColor
        )
    }
}

@Composable
fun AudioAdjustDialog(
    audioSyncMs: Float,
    audioPitch: Float,
    onAudioSyncChange: (Float) -> Unit,
    onAudioPitchChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    dialogColors: DialogColors
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(460.dp)
                .height(400.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = dialogColors.backgroundColor,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = dialogColors.selectedColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Audio Sync And Pitch",
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogColors.textColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    color = dialogColors.textColor.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    PlayerAdjustSlider(
                        title = "Audio sync",
                        valueText = "${audioSyncMs.toInt()} ms",
                        description = "Adjust if audio is early or late",
                        value = audioSyncMs,
                        onValueChange = onAudioSyncChange,
                        valueRange = -1000f..1000f,
                        steps = 39,
                        dialogColors = dialogColors
                    )

                    PlayerAdjustSlider(
                        title = "Audio pitch",
                        valueText = "${String.format("%.2f", audioPitch)}x",
                        description = "Fine tune voice/music pitch",
                        value = audioPitch,
                        onValueChange = onAudioPitchChange,
                        valueRange = 0.50f..2.00f,
                        steps = 29,
                        dialogColors = dialogColors
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(dialogColors.selectedColor.copy(alpha = 0.12f))
                            .clickable(onClick = onReset)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = dialogColors.selectedColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(dialogColors.selectedColor)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = dialogColors.selectedTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerAdjustSlider(
    title: String,
    valueText: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    dialogColors: DialogColors
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = dialogColors.textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = dialogColors.textColor.copy(alpha = 0.68f)
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = dialogColors.selectedColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = dialogColors.selectedColor,
                activeTrackColor = dialogColors.selectedColor,
                inactiveTrackColor = dialogColors.textColor.copy(alpha = 0.22f),
                activeTickColor = dialogColors.selectedTextColor.copy(alpha = 0.65f),
                inactiveTickColor = dialogColors.textColor.copy(alpha = 0.38f)
            )
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TrackSelectionDialog(
    title: String,
    exoPlayer: ExoPlayer,
    trackType: Int,
    onDismiss: () -> Unit,
    dialogColors: DialogColors
) {
    val tracks = exoPlayer.currentTracks
    
    // Find the track group for this type
    val trackGroups = remember(tracks) {
        val groups = mutableListOf<Pair<Tracks.Group, Int>>()
        for (group in tracks.groups) {
            if (group.type == trackType) {
                for (i in 0 until group.length) {
                    groups.add(group to i)
                }
            }
        }
        groups
    }
    
    // FIXED size for ALL dialogs - same width and height regardless of track count
    val dialogWidth = 460.dp
    val dialogHeight = 400.dp
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(dialogWidth)
                .height(dialogHeight)
                .clip(RoundedCornerShape(20.dp)),
            color = dialogColors.backgroundColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogColors.textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                HorizontalDivider(
                    color = dialogColors.textColor.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                // Scrollable list - fills remaining space
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    if (trackType == C.TRACK_TYPE_TEXT) {
                        item {
                            val isSelected = trackGroups.isEmpty() || !tracks.groups.any { 
                                it.type == C.TRACK_TYPE_TEXT && it.isSelected 
                            }
                            TrackOption(
                                label = "None",
                                subtitle = "Disable subtitles",
                                isSelected = isSelected,
                                dialogColors = dialogColors,
                                onClick = {
                                    val params = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .build()
                                    exoPlayer.trackSelectionParameters = params
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    items(trackGroups) { (group, trackIndex) ->
                        val format = group.getTrackFormat(trackIndex)
                        val isSelected = group.isTrackSelected(trackIndex)
                        
                        when (trackType) {
                            C.TRACK_TYPE_VIDEO -> {
                                val width = format.width
                                val height = format.height
                                val bitrate = if (format.bitrate > 0) " • ${format.bitrate / 1000} kbps" else ""
                                val frameRate = if (format.frameRate > 0) " • ${format.frameRate.toInt()} fps" else ""
                                val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                                val label = if (width > 0 && height > 0) "${width}x${height}$bitrate$frameRate" else format.label ?: "Video"
                                val subtitle = if (codec.isNotEmpty()) "Codec: $codec" else null
                                
                                TrackOption(
                                    label = label,
                                    subtitle = subtitle,
                                    isSelected = isSelected,
                                    dialogColors = dialogColors,
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon().setOverrideForType(override).build()
                                        onDismiss()
                                    }
                                )
                            }
                            C.TRACK_TYPE_AUDIO -> {
                                val lang = format.language?.uppercase() ?: "Unknown"
                                val labelStr = format.label ?: ""
                                val channels = when (format.channelCount) {
                                    1 -> "Mono"
                                    2 -> "Stereo"
                                    6 -> "5.1 Surround"
                                    8 -> "7.1 Surround"
                                    in 3..5 -> "${format.channelCount}ch"
                                    else -> ""
                                }
                                val bitrate = if (format.bitrate > 0) "${format.bitrate / 1000} kbps" else ""
                                val sampleRate = if (format.sampleRate > 0) "${format.sampleRate / 1000} kHz" else ""
                                val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                                
                                val mainLabel = buildString {
                                    append(lang)
                                    if (labelStr.isNotEmpty()) append(" • $labelStr")
                                }
                                val subInfo = buildList {
                                    if (channels.isNotEmpty()) add(channels)
                                    if (bitrate.isNotEmpty()) add(bitrate)
                                    if (sampleRate.isNotEmpty()) add(sampleRate)
                                    if (codec.isNotEmpty()) add(codec)
                                }.joinToString(" • ")
                                
                                TrackOption(
                                    label = mainLabel,
                                    subtitle = subInfo.ifEmpty { null },
                                    isSelected = isSelected,
                                    dialogColors = dialogColors,
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon().setOverrideForType(override).build()
                                        onDismiss()
                                    }
                                )
                            }
                            C.TRACK_TYPE_TEXT -> {
                                val lang = format.language?.uppercase() ?: "Unknown"
                                val labelStr = format.label ?: ""
                                val mimeType = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                                
                                val flags = buildList {
                                    if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) add("Forced")
                                    if ((format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0) add("SDH")
                                    if ((format.roleFlags and C.ROLE_FLAG_CAPTION) != 0) add("Caption")
                                    if ((format.roleFlags and C.ROLE_FLAG_SUBTITLE) != 0) add("Subtitle")
                                    if ((format.roleFlags and C.ROLE_FLAG_DUB) != 0) add("Dub")
                                    if ((format.roleFlags and C.ROLE_FLAG_COMMENTARY) != 0) add("Commentary")
                                    if ((format.roleFlags and C.ROLE_FLAG_EASY_TO_READ) != 0) add("Easy Read")
                                }
                                
                                val mainLabel = buildString {
                                    append(lang)
                                    if (labelStr.isNotEmpty() && labelStr != lang) append(" • $labelStr")
                                }
                                val subInfo = buildList {
                                    addAll(flags)
                                    if (mimeType.isNotEmpty()) add(mimeType)
                                }.joinToString(" • ")
                                
                                TrackOption(
                                    label = mainLabel,
                                    subtitle = subInfo.ifEmpty { null },
                                    isSelected = isSelected,
                                    dialogColors = dialogColors,
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon().setOverrideForType(override).build()
                                        onDismiss()
                                    }
                                )
                            }
                            else -> {
                                TrackOption(
                                    label = format.label ?: "Track $trackIndex",
                                    subtitle = null,
                                    isSelected = isSelected,
                                    dialogColors = dialogColors,
                                    onClick = {
                                        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon().setOverrideForType(override).build()
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                    
                    if (trackGroups.isEmpty() && trackType != C.TRACK_TYPE_TEXT) {
                        item {
                            Text(
                                text = "No tracks available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = dialogColors.textColor.copy(alpha = 0.6f),
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackOption(
    label: String,
    subtitle: String?,
    isSelected: Boolean,
    dialogColors: DialogColors,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = if (subtitle != null) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = dialogColors.selectedColor,
                    unselectedColor = dialogColors.textColor.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) dialogColors.selectedColor else dialogColors.textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) dialogColors.selectedColor.copy(alpha = 0.7f) else dialogColors.textColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Minimal thin separator between options.
        HorizontalDivider(
            color = dialogColors.textColor.copy(alpha = 0.08f),
            thickness = 0.6.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Composable
fun IndicatorOverlay(icon: ImageVector, text: String, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -50 }, animationSpec = tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 64.dp)
                    .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.large)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun AspectRatioDialog(
    currentAspectRatio: Int,
    onAspectRatioSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    dialogColors: DialogColors
) {
    val aspectOptions = listOf(
        AspectRatioOption("Original (Fit)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
        AspectRatioOption("Stretch (Fill)", AspectRatioFrameLayout.RESIZE_MODE_FILL),
        AspectRatioOption("Crop (Zoom)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
        AspectRatioOption("16:9", 8),
        AspectRatioOption("18:9", 9),
        AspectRatioOption("19:9", 5),
        AspectRatioOption("20:9", 6),
        AspectRatioOption("21:9", 7)
    )
    
    // FIXED size - same as other dialogs
    val dialogWidth = 460.dp
    val dialogHeight = 400.dp
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(dialogWidth)
                .height(dialogHeight)
                .clip(RoundedCornerShape(20.dp)),
            color = dialogColors.backgroundColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aspect Ratio",
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogColors.textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                HorizontalDivider(
                    color = dialogColors.textColor.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                // Options list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    items(aspectOptions) { option ->
                        TrackOption(
                            label = option.label,
                            subtitle = null,
                            isSelected = currentAspectRatio == option.value,
                            dialogColors = dialogColors,
                            onClick = { onAspectRatioSelected(option.value) }
                        )
                    }
                }
            }
        }
    }
}

data class AspectRatioOption(
    val label: String,
    val value: Int
)
