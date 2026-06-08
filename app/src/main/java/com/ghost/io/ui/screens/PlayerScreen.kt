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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghost.io.data.SettingsRepository
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
    
    // Android 11+ compatibility flag
    val isAndroid11OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    
    // Immersive Mode - Optimized for Android 11+ and older devices
    DisposableEffect(Unit) {
        activity?.window?.let { window ->
            // Enable drawing under the cutout/notch area
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
            
            // Critical for edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            
            // Force landscape mode for better viewing
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
                
                // Restore portrait mode when leaving player
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
    
    // Initialize audio manager
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    // Request audio focus
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
    
    val trackSelector = remember {
        DefaultTrackSelector(context)
    }
    
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true) // Enable fallback for older devices

        val extractorsFactory = DefaultExtractorsFactory()

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context, extractorsFactory))
            .build().apply {
                setMediaItem(MediaItem.fromUri(url))
                
                // Restore playback position and track selections
                coroutineScope.launch {
                    val savedPosition = settingsRepository.getPlaybackPosition(url).first()
                    val savedAudioId = settingsRepository.getAudioTrack(url).first()
                    val savedTextId = settingsRepository.getTextTrack(url).first()
                    
                    if (savedPosition > 0) {
                        seekTo(savedPosition)
                    }
                    
                    // Listen for when tracks are ready so we can restore the exact selection by ID
                    addListener(object : Player.Listener {
                        override fun onTracksChanged(tracks: Tracks) {
                            var paramsBuilder = trackSelectionParameters.buildUpon()
                            var changed = false
                            
                            // Restore Audio Track
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
                            
                            // Restore Subtitle Track
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
                            removeListener(this) // Only need to do this once on startup
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
                    
                    // Save currently selected tracks
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
                    
                    // Save currently selected tracks
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
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer

                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Prevent clipping when zooming/panning the video surface
                    clipChildren = false
                    (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_content_frame) as? android.view.ViewGroup)?.clipChildren = false
                    
                    // Force the PlayerView to ignore system window insets so it draws into the cutout
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setOnApplyWindowInsetsListener { _, _ -> android.view.WindowInsets.CONSUMED }
                    } else {
                        setOnApplyWindowInsetsListener { _, insets -> insets }
                    }
                    
                    // Important for filling the notch area
                    setPadding(0, 0, 0, 0)
                    
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    
                    // Lock state for disabling touch
                    var isLocked = false
                    
                    // Native integration into the bottom control bar
                    val basicControls = findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_basic_controls)
                    
                    // Quality Button
                    val qualityButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_hq)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE // ALWAYS VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setOnClickListener {
                            val builder = TrackSelectionDialogBuilder(
                                ctx,
                                "Select Video Quality",
                                exoPlayer,
                                C.TRACK_TYPE_VIDEO
                            )
                            builder.build().show()
                        }
                    }
                    
                    // Audio Track Button
                    val audioButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_aud)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE // ALWAYS VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setOnClickListener {
                            val builder = TrackSelectionDialogBuilder(
                                ctx,
                                "Select Audio Track",
                                exoPlayer,
                                C.TRACK_TYPE_AUDIO
                            )
                            builder.setTrackNameProvider { format ->
                                val language = format.language ?: "Unknown"
                                val label = format.label
                                val bitrate = if (format.bitrate > 0) "${format.bitrate / 1000} kbps" else ""
                                val channels = if (format.channelCount > 0) "${format.channelCount} ch" else ""
                                
                                val info = mutableListOf<String>()
                                info.add(language.uppercase())
                                
                                if (label != null && label.isNotEmpty()) {
                                    info.add("($label)")
                                }
                                if (channels.isNotEmpty()) {
                                    info.add(channels)
                                }
                                if (bitrate.isNotEmpty()) {
                                    info.add(bitrate)
                                }
                                
                                info.joinToString(" ")
                            }
                            builder.build().show()
                        }
                    }
                    val ccButton = android.widget.ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_cc)
                        val paddingPx = (12 * ctx.resources.displayMetrics.density).toInt()
                        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        visibility = View.VISIBLE // ALWAYS VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setOnClickListener {
                            val builder = TrackSelectionDialogBuilder(
                                ctx,
                                "Select Subtitles",
                                exoPlayer,
                                C.TRACK_TYPE_TEXT
                            )
                            builder.setTrackNameProvider { format ->
                                val language = format.language ?: "Unknown"
                                val label = format.label
                                val roleFlags = format.roleFlags
                                
                                val info = mutableListOf<String>()
                                info.add(language.uppercase())
                                
                                if (label != null && label.isNotEmpty()) {
                                    info.add(label)
                                }
                                
                                if ((format.selectionFlags and androidx.media3.common.C.SELECTION_FLAG_FORCED) != 0) {
                                    info.add("[Forced]")
                                }
                                if ((roleFlags and androidx.media3.common.C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0) {
                                    info.add("[SDH]")
                                }
                                
                                info.joinToString(" ")
                            }
                            builder.build().show()
                        }
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
                        
                        setOnClickListener { v ->
                            val wrapper = ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault)
                            val popup = PopupMenu(wrapper, v)
                            popup.menu.add(0, AspectRatioFrameLayout.RESIZE_MODE_FIT, 0, "Original (Fit)")
                            popup.menu.add(0, AspectRatioFrameLayout.RESIZE_MODE_FILL, 1, "Stretch (Fill)")
                            popup.menu.add(0, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, 2, "Crop (Zoom)")
                            popup.menu.add(0, AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH, 3, "16:9")
                            popup.menu.add(0, AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT, 4, "18:9")
                            popup.menu.add(0, 5, 5, "19:9")
                            popup.menu.add(0, 6, 6, "20:9")
                            popup.menu.add(0, 7, 7, "21:9")
                            
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL,
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
                                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> {
                                        setAspectRatioListener(null)
                                        resizeMode = item.itemId
                                    }
                                    5 -> { // 19:9
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { targetAspectRatio, naturalAspectRatio, aspectRatioMismatch ->
                                            19f / 9f
                                        }
                                    }
                                    6 -> { // 20:9
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { targetAspectRatio, naturalAspectRatio, aspectRatioMismatch ->
                                            20f / 9f
                                        }
                                    }
                                    7 -> { // 21:9
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { targetAspectRatio, naturalAspectRatio, aspectRatioMismatch ->
                                            21f / 9f
                                        }
                                    }
                                }
                                true
                            }
                            popup.show()
                        }
                    }
                    
                    // PiP / Background Play Button
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
                        
                        setOnClickListener {
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
                            isLocked = !isLocked
                            if (isLocked) {
                                setImageResource(R.drawable.ic_unlock)
                                // Hide all controls except lock button
                                basicControls?.visibility = View.GONE
                                pipButton.visibility = View.GONE
                                aspectButton.visibility = View.GONE
                                ccButton.visibility = View.GONE
                                audioButton.visibility = View.GONE
                                qualityButton.visibility = View.GONE
                                (this@apply.parent as? View)?.visibility = View.VISIBLE
                            } else {
                                setImageResource(R.drawable.ic_lock)
                                // Show all controls
                                basicControls?.visibility = View.VISIBLE
                                pipButton.visibility = View.VISIBLE
                                aspectButton.visibility = View.VISIBLE
                                ccButton.visibility = View.VISIBLE
                                audioButton.visibility = View.VISIBLE
                                qualityButton.visibility = View.VISIBLE
                            }
                        }
                    }
                    
                    if (basicControls != null) {
                        // Find the settings gear icon to insert right before it
                        val settingsButton = basicControls.findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                        val settingsIndex = basicControls.indexOfChild(settingsButton)
                        val insertIndex = if (settingsIndex >= 0) settingsIndex else basicControls.childCount
                        
                        // Add Lock, PiP, Aspect, CC, Audio, then HQ
                        basicControls.addView(lockButton, insertIndex)
                        basicControls.addView(pipButton, insertIndex + 1)
                        basicControls.addView(aspectButton, insertIndex + 2)
                        basicControls.addView(ccButton, insertIndex + 3)
                        basicControls.addView(audioButton, insertIndex + 4)
                        basicControls.addView(qualityButton, insertIndex + 5)
                    }
                    
                    // Removed the onTracksChanged listener that was hiding the buttons
                    // exoPlayer.addListener(listener)
                    
                    // Gesture handling for Zoom, Brightness, and Volume
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
                            exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(2.0f)
                            seekMessage = "2x Speed"
                            isForwardSeek = true
                            seekTrigger++
                        }

                        override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                            if (!gestureDoubleTapEnabled) return false
                            val surface = videoSurfaceView as? View ?: return false
                            
                            if (e.x > surface.width / 2f) {
                                // Double tap right: forward 10 seconds
                                exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                                seekMessage = "+10s"
                                isForwardSeek = true
                                seekTrigger++
                            } else {
                                // Double tap left: rewind 10 seconds
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                seekMessage = "-10s"
                                isForwardSeek = false
                                seekTrigger++
                            }
                            return true
                        }

                        override fun onDown(e: android.view.MotionEvent): Boolean {
                            isBrightnessScroll = false
                            isVolumeScroll = false
                            accumulatedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            return false
                        }

                        override fun onScroll(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                            val surface = videoSurfaceView as? View ?: return false
                            
                            val pointerCount = e2.pointerCount
                            
                            // Pan logic when zoomed in AND using 2 fingers
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
                                // Brightness & Volume & Seek logic when not zoomed AND using 1 finger
                                if (e1 == null) return false
                                
                                if (!isBrightnessScroll && !isVolumeScroll) {
                                    if (abs(distanceY) > abs(distanceX) + 10) {
                                        // Vertical scroll: brightness or volume
                                        if (e1.x < surface.width / 2f && gestureBrightnessEnabled) {
                                            isBrightnessScroll = true
                                        } else if (e1.x >= surface.width / 2f && gestureVolumeEnabled) {
                                            isVolumeScroll = true
                                        } else {
                                            return false
                                        }
                                    } else if (abs(distanceX) > abs(distanceY) + 10 && gestureSeekEnabled) {
                                        // Horizontal scroll: seek
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
                                        if (currentBrightness < 0f) currentBrightness = 0.5f // Default
                                        
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
                                    val newVol = accumulatedVolume.coerceIn(0f, maxVolume.toFloat()).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                    
                                    volumePercent = ((newVol.toFloat() / maxVolume) * 100).toInt()
                                    volumeTrigger++
                                    return true
                                }
                            }
                            return false
                        }
                    })
                    
                    setOnTouchListener { _, event ->
                        // If locked, only allow touch on lock button area (right side)
                        if (isLocked) {
                            // When locked, don't process any gestures
                            return@setOnTouchListener false
                        }
                        
                        if (event.action == android.view.MotionEvent.ACTION_UP || event.action == android.view.MotionEvent.ACTION_CANCEL) {
                            if (exoPlayer.playbackParameters.speed == 2.0f) {
                                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f)
                            }
                        }
                        
                        scaleDetector?.onTouchEvent(event)
                        gestureDetector.onTouchEvent(event)
                        false // Let PlayerView handle single taps for controls
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay UI
        IndicatorOverlay(Icons.Default.ZoomIn, "$zoomPercent%", showZoom)
        IndicatorOverlay(Icons.Default.VolumeUp, "$volumePercent%", showVolume)
        IndicatorOverlay(Icons.Default.BrightnessMedium, "$brightnessPercent%", showBrightness)
        
        // Seek Overlay
        AnimatedVisibility(
            visible = showSeek,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(if (isForwardSeek) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(Color.White.copy(alpha = 0.15f))
                    // Add padding to avoid the edge/system gesture areas
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
                    // Add extra top padding to clear any potential status bar or cutout bounds
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