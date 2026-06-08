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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.TrackSelectionDialogBuilder
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghost.io.data.DecoderPriority
import com.ghost.io.data.SettingsRepository
import com.ghost.io.data.SubtitleFont
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
    
    // Collect settings
    val decoderPriority by settingsRepository.decoderPriority.collectAsState(initial = DecoderPriority.PREFER_DEVICE)
    val subtitleFont by settingsRepository.subtitleFont.collectAsState(initial = SubtitleFont.DEFAULT)
    val subtitleBold by settingsRepository.subtitleBold.collectAsState(initial = true)
    val subtitleSize by settingsRepository.subtitleSize.collectAsState(initial = 20)
    val subtitleBackground by settingsRepository.subtitleBackground.collectAsState(initial = false)
    val subtitleEmbeddedStyles by settingsRepository.subtitleEmbeddedStyles.collectAsState(initial = true)

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
    
    // Handle lock state changes - immediately hide/show controls
    LaunchedEffect(isLocked) {
        playerViewRef?.let { playerView ->
            if (isLocked) {
                // Immediately hide all controls
                playerView.useController = false
                playerView.hideController()
            } else {
                // Re-enable controls
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
                        setApplyEmbeddedFontSizes(subtitleEmbeddedStyles)
                        setApplyEmbeddedStyles(subtitleEmbeddedStyles)
                        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize.toFloat())

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
                    }
                    systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    
                    // Store reference for lock control
                    playerViewRef = this
                    
                    val basicControls = findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_basic_controls)
                    
                    // Helper function for button click feedback
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
                        
                        setClickFeedback {
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
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback {
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
                        visibility = View.VISIBLE
                        
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        setClickFeedback {
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
                        
                        setClickFeedback {
                            val wrapper = ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault)
                            val popup = PopupMenu(wrapper, this@apply)
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
                                    5 -> {
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { _, _, _ -> 19f / 9f }
                                    }
                                    6 -> {
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { _, _, _ -> 20f / 9f }
                                    }
                                    7 -> {
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        setAspectRatioListener { _, _, _ -> 21f / 9f }
                                    }
                                }
                                true
                            }
                            popup.show()
                        }
                    }
                    
                    // PiP Button - Directly enters PiP mode
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
                    
                    // Lock Button - Immediately hides UI
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
                            // Flash cyan
                            setColorFilter(android.graphics.Color.CYAN)
                            postDelayed({
                                setColorFilter(android.graphics.Color.WHITE)
                            }, 200)
                            
                            // Immediately hide controls and lock
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
                // Update lock state
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
        
        // Overlay UI (only when not locked)
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
