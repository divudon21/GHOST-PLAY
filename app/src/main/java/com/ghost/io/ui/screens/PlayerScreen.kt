package com.ghost.io.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.ghost.io.R
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghost.io.data.AppColorPreference
import com.ghost.io.data.DecoderPriority
import com.ghost.io.data.DialogThemePreference
import com.ghost.io.data.SettingsRepository
import com.ghost.io.data.SubtitleFont
import com.ghost.io.data.ThemePreference
import com.ghost.io.ui.theme.getColorScheme
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

    // Lock state
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockButton by remember { mutableStateOf(false) }
    
    // PlayerView reference for controlling visibility
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    
    // Dialog states
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    
    // Collect settings
    val decoderPriority by settingsRepository.decoderPriority.collectAsState(initial = DecoderPriority.PREFER_DEVICE)
    val subtitleFont by settingsRepository.subtitleFont.collectAsState(initial = SubtitleFont.DEFAULT)
    val subtitleBold by settingsRepository.subtitleBold.collectAsState(initial = true)
    val subtitleSize by settingsRepository.subtitleSize.collectAsState(initial = 20)
    val subtitleBackground by settingsRepository.subtitleBackground.collectAsState(initial = false)
    val subtitleEmbeddedStyles by settingsRepository.subtitleEmbeddedStyles.collectAsState(initial = true)
    val dialogThemePreference by settingsRepository.dialogThemePreference.collectAsState(initial = DialogThemePreference.FOLLOW_SYSTEM)
    val appColorPreference by settingsRepository.colorPreference.collectAsState(initial = AppColorPreference.PURPLE)
    val themePreference by settingsRepository.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
    val volumeBoostEnabled by settingsRepository.volumeBoostEnabled.collectAsState(initial = false)

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
            
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
    
    // Handle lock state changes
    LaunchedEffect(isLocked) {
        playerViewRef?.let { playerView ->
            if (isLocked) {
                playerView.useController = false
                playerView.hideController()
            } else {
                playerView.useController = true
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
                    val savedPosition = settingsRepository.getPlaybackPosition(url).first()
                    val savedAudioId = settingsRepository.getAudioTrack(url).first()
                    val savedTextId = settingsRepository.getTextTrack(url).first()
                    
                    if (savedPosition > 0) {
                        seekTo(savedPosition)
                    }
                    
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
                    
                    prepare()
                    playWhenReady = true
                }
            }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch {
                    settingsRepository.savePlaybackPosition(url, exoPlayer.currentPosition)
                    
                    val tracks = exoPlayer.currentTracks
                    for (group in tracks.groups) {
                        if (group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    val idToSave = format.id ?: format.language ?: ""
                                    if (idToSave.isNotEmpty()) {
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
        onDispose {
            if (!com.ghost.io.SharedPlayer.isFloatingMode) {
                coroutineScope.launch {
                    settingsRepository.savePlaybackPosition(url, exoPlayer.currentPosition)
                    
                    val tracks = exoPlayer.currentTracks
                    for (group in tracks.groups) {
                        if (group.isSelected) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSelected(i)) {
                                    val format = group.getTrackFormat(i)
                                    val idToSave = format.id ?: format.language ?: ""
                                    if (idToSave.isNotEmpty()) {
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
    val dialogColors = rememberDialogColors(dialogThemePreference, appColorPreference, themePreference)
    
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
                    keepScreenOn = true

                    subtitleView?.apply {
                        // Apply embedded styles setting
                        setApplyEmbeddedFontSizes(subtitleEmbeddedStyles)
                        setApplyEmbeddedStyles(subtitleEmbeddedStyles)
                        
                        // Always set text size - user preference
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.toFloat())

                        // Apply custom style
                        if (!subtitleEmbeddedStyles) {
                            // Full custom style when embedded styles disabled
                            val typeface = when (subtitleFont) {
                                SubtitleFont.DEFAULT -> Typeface.DEFAULT
                                SubtitleFont.MONOSPACE -> Typeface.MONOSPACE
                                SubtitleFont.SANS_SERIF -> Typeface.SANS_SERIF
                                SubtitleFont.SERIF -> Typeface.SERIF
                            }

                            val style = CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                if (subtitleBackground) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                if (subtitleBold) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
                                android.graphics.Color.BLACK,
                                typeface
                            )
                            setStyle(style)
                        } else {
                            // For styled subtitles - minimal style that doesn't override colors
                            val fallbackStyle = CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                if (subtitleBackground) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                Typeface.DEFAULT
                            )
                            setStyle(fallbackStyle)
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
                        visibility = View.VISIBLE
                        
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
                        basicControls.addView(qualityButton, insertIndex + 5)
                    }
                    
                    // Gesture handling
                    var scale = 1f
                    var transX = 0f
                    var transY = 0f
                    
                    var isBrightnessScroll = false
                    var isVolumeScroll = false
                    var accumulatedVolume = 0f
                    
                    val scaleDetector = if (gestureZoomEnabled) {
                        android.view.ScaleGestureDetector(ctx, object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                                scale *= detector.scaleFactor
                                scale = scale.coerceIn(1f, 5f)
                                
                                val surface = videoSurfaceView as? View
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
                                
                                zoomPercent = (scale * 100).toInt()
                                zoomTrigger++
                                return true
                            }
                        })
                    } else null
                    
                    val gestureDetector = android.view.GestureDetector(ctx, object : android.view.GestureDetector.SimpleOnGestureListener() {
                        override fun onLongPress(e: android.view.MotionEvent) {
                            if (isLocked) return
                            exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(2.0f)
                            seekMessage = "2x Speed"
                            isForwardSeek = true
                            seekTrigger++
                        }

                        override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                            if (isLocked) return false
                            if (!gestureDoubleTapEnabled) return false
                            val surface = videoSurfaceView as? View ?: return false
                            
                            if (e.x > surface.width / 2f) {
                                exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                                seekMessage = "+10s"
                                isForwardSeek = true
                                seekTrigger++
                            } else {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                seekMessage = "-10s"
                                isForwardSeek = false
                                seekTrigger++
                            }
                            return true
                        }

                        override fun onDown(e: android.view.MotionEvent): Boolean {
                            if (isLocked) return false
                            isBrightnessScroll = false
                            isVolumeScroll = false
                            accumulatedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            return false
                        }

                        override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                            if (isLocked) return false
                            
                            val surface = videoSurfaceView as? View ?: return false
                            
                            val pointerCount = e2.pointerCount
                            
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
                            } else if (scale == 1f && pointerCount == 1) {
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
                                        if (distanceX > 0) {
                                            seekMessage = "+${(seekAmount / 1000).toInt()}s"
                                            isForwardSeek = true
                                        } else {
                                            seekMessage = "-${(kotlin.math.abs(seekAmount) / 1000).toInt()}s"
                                            isForwardSeek = false
                                        }
                                        seekTrigger++
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
                                        
                                        brightnessPercent = (newBrightness * 100).toInt()
                                        brightnessTrigger++
                                    }
                                    return true
                                }
                                
                                if (isVolumeScroll && gestureVolumeEnabled) {
                                    val sensMultiplier = gestureVolumeSensitivity * 1.5f
                                    accumulatedVolume += (distanceY / surface.height) * maxVolume * sensMultiplier
                                    
                                    if (volumeBoostEnabled) {
                                        // Volume boost mode: 0-200%
                                        val boostedVolume = accumulatedVolume.coerceIn(0f, maxVolume.toFloat() * 2f)
                                        val systemVolume = boostedVolume.coerceIn(0f, maxVolume.toFloat()).toInt()
                                        val boostMultiplier = if (boostedVolume > maxVolume) {
                                            boostedVolume / maxVolume
                                        } else {
                                            1f
                                        }
                                        
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolume, 0)
                                        exoPlayer.volume = boostMultiplier.coerceIn(1f, 2f)
                                        
                                        volumePercent = ((boostedVolume / maxVolume) * 100).toInt().coerceAtMost(200)
                                    } else {
                                        // Normal mode: 0-100%
                                        val newVol = accumulatedVolume.coerceIn(0f, maxVolume.toFloat()).toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        exoPlayer.volume = 1f
                                        
                                        volumePercent = ((newVol.toFloat() / maxVolume) * 100).toInt()
                                    }
                                    volumeTrigger++
                                    return true
                                }
                            }
                            return false
                        }
                    })
                    
                    setOnTouchListener { _, event ->
                        if (isLocked) {
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                showUnlockButton = true
                            }
                            return@setOnTouchListener true
                        }
                        
                        if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) {
                            if (exoPlayer.playbackParameters.speed == 2.0f) {
                                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f)
                            }
                        }
                        
                        scaleDetector?.onTouchEvent(event)
                        gestureDetector.onTouchEvent(event)
                        false
                    }
                }
            },
            update = { playerView ->
                if (isLocked) {
                    playerView.useController = false
                } else {
                    playerView.useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
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
    
    // Aspect Ratio Dialog
    if (showAspectRatioDialog) {
        AspectRatioDialog(
            onDismiss = { showAspectRatioDialog = false },
            onAspectRatioSelected = { resizeMode, customRatio ->
                playerViewRef?.let { playerView ->
                    if (customRatio != null) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        playerView.setAspectRatioListener { _, _, _ -> customRatio }
                    } else {
                        playerView.setAspectRatioListener(null)
                        playerView.resizeMode = resizeMode
                    }
                }
                showAspectRatioDialog = false
            },
            dialogColors = dialogColors
        )
    }
}

@Composable
fun rememberDialogColors(
    dialogTheme: DialogThemePreference,
    appColor: AppColorPreference,
    themePreference: ThemePreference
): DialogColors {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    return remember(dialogTheme, appColor, themePreference, isSystemDark) {
        when (dialogTheme) {
            DialogThemePreference.FOLLOW_SYSTEM -> {
                if (isSystemDark) DialogColors.Dark else DialogColors.Light
            }
            DialogThemePreference.DARK -> DialogColors.Dark
            DialogThemePreference.LIGHT -> DialogColors.Light
            DialogThemePreference.CUSTOM -> {
                val colorScheme = getColorScheme(appColor, true)
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
    var selectedOverride by remember { mutableStateOf<TrackSelectionOverride?>(null) }
    
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
    
    // Calculate item count for consistent height
    val itemCount = trackGroups.size + if (trackType == C.TRACK_TYPE_TEXT) 1 else 0 // +1 for "None" option
    val listHeight = when {
        itemCount <= 2 -> 140.dp
        itemCount <= 4 -> 220.dp
        itemCount <= 6 -> 300.dp
        else -> 380.dp
    }
    
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
                .widthIn(min = 340.dp, max = 460.dp)
                .fillMaxWidth(0.90f)
                .heightIn(min = 220.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = dialogColors.backgroundColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            ) {
                // Title with icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogColors.textColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
                
                HorizontalDivider(
                    color = dialogColors.textColor.copy(alpha = 0.12f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .height(listHeight)
                        .padding(vertical = 8.dp)
                ) {
                    // Add "None" option for subtitles
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
                                val subtitle = buildList {
                                    if (channels.isNotEmpty()) add(channels)
                                    if (bitrate.isNotEmpty()) add(bitrate)
                                    if (sampleRate.isNotEmpty()) add(sampleRate)
                                    if (codec.isNotEmpty()) add(codec)
                                }.joinToString(" • ")
                                
                                TrackOption(
                                    label = mainLabel,
                                    subtitle = subtitle.ifEmpty { null },
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
                                val subtitle = buildList {
                                    addAll(flags)
                                    if (mimeType.isNotEmpty()) add(mimeType)
                                }.joinToString(" • ")
                                
                                TrackOption(
                                    label = mainLabel,
                                    subtitle = subtitle.ifEmpty { null },
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) dialogColors.selectedColor.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bgColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (subtitle != null) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = dialogColors.selectedColor,
                unselectedColor = dialogColors.textColor.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) dialogColors.selectedColor else dialogColors.textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) dialogColors.selectedColor.copy(alpha = 0.8f) else dialogColors.textColor.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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

data class AspectRatioOption(
    val label: String,
    val description: String,
    val resizeMode: Int,
    val customRatio: Float? = null
)

@Composable
fun AspectRatioDialog(
    onDismiss: () -> Unit,
    onAspectRatioSelected: (Int, Float?) -> Unit,
    dialogColors: DialogColors
) {
    val options = listOf(
        AspectRatioOption("Original (Fit)", "Keep original aspect ratio, fit within screen", AspectRatioFrameLayout.RESIZE_MODE_FIT),
        AspectRatioOption("Stretch (Fill)", "Stretch to fill entire screen", AspectRatioFrameLayout.RESIZE_MODE_FILL),
        AspectRatioOption("Crop (Zoom)", "Zoom to fill screen, may crop edges", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
        AspectRatioOption("16:9", "Standard widescreen ratio", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, 16f / 9f),
        AspectRatioOption("18:9", "Modern smartphone ratio", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, 18f / 9f),
        AspectRatioOption("19:9", "Tall smartphone ratio", AspectRatioFrameLayout.RESIZE_MODE_FILL, 19f / 9f),
        AspectRatioOption("20:9", "Extra tall ratio", AspectRatioFrameLayout.RESIZE_MODE_FILL, 20f / 9f),
        AspectRatioOption("21:9", "Ultrawide cinema ratio", AspectRatioFrameLayout.RESIZE_MODE_FILL, 21f / 9f)
    )
    
    var selectedOption by remember { mutableStateOf(0) }
    
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
                .widthIn(min = 340.dp, max = 420.dp)
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = dialogColors.backgroundColor,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
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
                
                LazyColumn(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(options.size) { index ->
                        val option = options[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedOption = index
                                    onAspectRatioSelected(option.resizeMode, option.customRatio)
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == index,
                                onClick = {
                                    selectedOption = index
                                    onAspectRatioSelected(option.resizeMode, option.customRatio)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = dialogColors.selectedColor,
                                    unselectedColor = dialogColors.textColor.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selectedOption == index) dialogColors.selectedColor else dialogColors.textColor,
                                    fontWeight = if (selectedOption == index) FontWeight.Medium else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dialogColors.textColor.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
