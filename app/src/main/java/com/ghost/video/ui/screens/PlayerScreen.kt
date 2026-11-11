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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.sp
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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegSubtitleExtractorsFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.withFfmpegSubtitles
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleSpeed
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
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
import com.ghost.video.ui.components.AudioAdjustSheet
import com.ghost.video.ui.components.PlayerChoiceRow
import com.ghost.video.ui.components.PlayerMuteButton
import com.ghost.video.ui.components.PlayerTrackSheet
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // Picture-in-Picture: the activity reports it through SharedPlayer, so the
    // Compose overlays (lock button, gestures, dialogs) can stay out of the PiP window.
    val inPip by com.ghost.video.SharedPlayer.isInPictureInPicture
    // Saved so the lock survives a configuration change / process restore.
    var isLocked by rememberSaveable { mutableStateOf(false) }
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
    var lastVolumeBeforeMute by remember { mutableFloatStateOf(1f) }
    // What the mute control draws. Written from a listener on the player and from
    // this screen's own toggle, never from the control itself, so any other cause
    // of a volume change (a gesture, a track switch, a restore) shows up too.
    var isMuted by remember { mutableStateOf(false) }
    
    // Collect settings
    val decoderPriority by settingsRepository.decoderPriority.collectAsState(initial = DecoderPriority.PREFER_DEVICE)
    val subtitleFont by settingsRepository.subtitleFont.collectAsState(initial = SubtitleFont.DEFAULT)
    val subtitleBold by settingsRepository.subtitleBold.collectAsState(initial = true)
    val subtitleSize by settingsRepository.subtitleSize.collectAsState(initial = 20)
    val subtitleBackground by settingsRepository.subtitleBackground.collectAsState(initial = false)
    val subtitleEmbeddedStyles by settingsRepository.subtitleEmbeddedStyles.collectAsState(initial = true)
    // Subtitle sync: delay shifts the cues in time, speed corrects slow drift.
    // Both preferences also ship as sliders in Subtitle settings.
    val subtitleDelaySetting by settingsRepository.subtitleDelay.collectAsState(initial = 0)
    val subtitleSpeedSetting by settingsRepository.subtitleSpeed.collectAsState(initial = 1f)
    val dialogThemePreference by settingsRepository.dialogThemePreference.collectAsState(initial = DialogThemePreference.FOLLOW_SYSTEM)
    val appColor by settingsRepository.appColor.collectAsState(initial = com.ghost.video.data.AppColor.PURPLE)
    val themePreference by settingsRepository.themePreference.collectAsState(initial = ThemePreference.SYSTEM)
    val loadingIndicatorStyle by settingsRepository.loadingIndicatorStyle.collectAsState(
        initial = com.ghost.video.data.LoadingIndicatorStyle.ROUNDED_POLYGON
    )
    val pipMode by settingsRepository.pipMode.collectAsState(initial = true)
    val autoPipMode by settingsRepository.autoPipMode.collectAsState(initial = false)
    val backgroundPlay by settingsRepository.backgroundPlay.collectAsState(initial = false)
    val playerOrientation by settingsRepository.playerOrientation.collectAsState(initial = OrientationPreference.AUTO)
    val systemCaptionStyle by settingsRepository.systemCaptionStyle.collectAsState(initial = false)
    // NextPlayer audio preferences. These feed the player builder below, so they
    // are part of the player's remember key (changing one rebuilds the player
    // exactly like a decoder-priority change does).
    val preferredAudioLanguage by settingsRepository.preferredAudioLanguage.collectAsState(initial = "")
    val requireAudioFocus by settingsRepository.requireAudioFocus.collectAsState(initial = true)
    val pauseOnHeadsetDisconnect by settingsRepository.pauseOnHeadsetDisconnect.collectAsState(initial = true)
    val showSystemVolumePanel by settingsRepository.showSystemVolumePanel.collectAsState(initial = true)

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
            val brightnessToSave = activity?.window?.attributes?.screenBrightness ?: -1f
            // Same scope problem as the playback state above: the composition scope
            // is already cancelled here, so the value has to be written from a scope
            // that outlives this screen.
            CoroutineScope(Dispatchers.IO).launch {
                if (settingsRepository.rememberBrightness.first() && brightnessToSave in 0f..1f) {
                    settingsRepository.saveBrightness(brightnessToSave)
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
    
    // Audio focus is handled by ExoPlayer itself (its default audio attributes
    // request focus and pause/resume on focus loss). The old code grabbed focus
    // manually with no OnAudioFocusChangeListener, which meant a call or a
    // navigation prompt never paused playback - and the manual request competed
    // with the player's own focus request.
    
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

    // One OkHttp client for every network read on this screen: media requests and
    // subtitle sidecars. Built lazily and kept for the lifetime of the player.
    val okHttpClient = remember {
        okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            // Streams can pause for a long time; a read timeout would kill playback.
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Playback diagnostics. A decode/container/URI failure used to leave a black
    // screen with no sound and no explanation; now it is shown and copyable.
    var playbackError by remember { mutableStateOf<androidx.media3.common.PlaybackException?>(null) }
    var playbackFailure by remember { mutableStateOf<Throwable?>(null) }
    
    val exoPlayer = remember(decoderPriority, requireAudioFocus, pauseOnHeadsetDisconnect, preferredAudioLanguage) {
        val extensionMode = when (decoderPriority) {
            DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        }
        // Preferred audio language (NextPlayer: trackSelector parameters, ISO 639-2).
        // An empty value means "no preference", which is what upstream sends too.
        if (preferredAudioLanguage.isNotEmpty()) {
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setPreferredAudioLanguage(preferredAudioLanguage)
                .build()
        }

        // Data source used by the media source AND by external subtitle sidecars, so
        // a subtitle loaded from the same place as the video keeps the same URI,
        // header and caching support.
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            // OkHttp instead of the platform HTTP stack: HTTP/2, gzip and the
            // redirect/TLS handling that many streaming hosts require. Reads of
            // local files and content:// URIs are unaffected - DefaultDataSource
            // only hands http(s) URIs to this factory.
            OkHttpDataSource.Factory(okHttpClient)
        )
        // FfmpegSubtitleExtractorsFactory keeps standard Media3 parsing for SRT,
        // WebVTT and TTML while routing ASS/SSA (libass), PGS, VobSub and DVB through
        // the bundled FFmpeg decoders, and it extracts fonts attached to Matroska.
        val extractorsFactory = FfmpegSubtitleExtractorsFactory(DefaultExtractorsFactory())
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
            .withFfmpegSubtitles(dataSourceFactory)

        // NextPlayer's player stack: NextRenderersFactory adds FFmpeg software
        // decoders (H.264, HEVC, VP8, VP9, AV1 via dav1d, plus AAC/AC3/E-AC3/DTS/
        // TrueHD/FLAC/ALAC/Opus/Vorbis audio) behind the device's own MediaCodec
        // decoders, so files the phone cannot decode in hardware still play.
        // Decoder fallback is enabled as well, so a faulty MediaCodec decoder
        // hands the track to the next candidate instead of failing the video.
        fun buildPlayer(mode: Int): ExoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(
                NextRenderersFactory(context)
                    .setExtensionRendererMode(mode)
                    .setEnableDecoderFallback(true)
            )
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ requireAudioFocus,
            )
            // "Pause on disconnect" upstream maps straight onto Media3's
            // becoming-noisy handling (headphones unplugged pauses playback).
            .setHandleAudioBecomingNoisy(pauseOnHeadsetDisconnect)
            .build()

        // If the extension (software) renderers cannot be created on this device,
        // fall back to the platform decoders instead of leaving the screen without
        // any player at all.
        val player = try {
            buildPlayer(extensionMode)
        } catch (t: Throwable) {
            if (extensionMode == DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF) throw t
            buildPlayer(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        player.apply {
            // Subtitle timing. nextlib's NextTextRenderer exposes these on the
            // player itself (Media3 1.11 API), so no view plumbing is needed.
            runCatching {
                subtitleDelayMilliseconds = subtitleDelaySetting.toLong()
                subtitleSpeed = subtitleSpeedSetting
            }
            // Any failure has to be VISIBLE: the screen is black otherwise and the
            // user has no idea whether the file, the codec or the container failed.
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playbackError = error
                }
            })
            setMediaItem(MediaItem.fromUri(url))
        }
    }

    // Playback is started here, NOT inside the player builder, and it is started
    // BEFORE the saved settings are read. The old code awaited six DataStore reads
    // and only then called prepare(); any delay, cancellation or exception in that
    // coroutine left the player IDLE forever - a black screen with no audio, no
    // spinner and no error, which is exactly the reported symptom.
    LaunchedEffect(exoPlayer) {
        withContext(Dispatchers.Main.immediate) {
            runCatching {
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }.onFailure { playbackFailure = it }

            // Saved resume position / track selection / speed / autoplay. All of
            // this is cosmetic: it must never be able to stop playback.
            runCatching {
                val resumePlayback = settingsRepository.resumePlayback.first()
                val savedPosition = settingsRepository.getPlaybackPosition(url).first()
                if (resumePlayback && savedPosition > 0) {
                    exoPlayer.seekTo(savedPosition)
                }

                val defaultSpeed = settingsRepository.defaultPlaybackSpeed.first()
                exoPlayer.playbackParameters =
                    PlaybackParameters(defaultSpeed.coerceIn(0.25f, 3f))
                exoPlayer.volume = 1f
                exoPlayer.playWhenReady = settingsRepository.autoplay.first()
            }

            runCatching {
                if (!settingsRepository.rememberSelections.first()) return@runCatching
                val savedAudioId = settingsRepository.getAudioTrack(url).first()
                val savedTextId = settingsRepository.getTextTrack(url).first()
                if (savedAudioId.isEmpty() && savedTextId.isEmpty()) return@runCatching

                var applied = false
                val listener = object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        if (applied) return
                        var paramsBuilder = exoPlayer.trackSelectionParameters.buildUpon()
                        var changed = false

                        if (savedAudioId.isNotEmpty()) {
                            for (group in tracks.groups) {
                                if (group.type != C.TRACK_TYPE_AUDIO) continue
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    if (format.id == savedAudioId || format.language == savedAudioId) {
                                        paramsBuilder.setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                                        )
                                        changed = true
                                        break
                                    }
                                }
                            }
                        }

                        if (savedTextId.isNotEmpty()) {
                            for (group in tracks.groups) {
                                if (group.type != C.TRACK_TYPE_TEXT) continue
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    if (format.id == savedTextId || format.language == savedTextId) {
                                        paramsBuilder.setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                                        )
                                        changed = true
                                        break
                                    }
                                }
                            }
                        }

                        if (changed) exoPlayer.trackSelectionParameters = paramsBuilder.build()
                        applied = true
                        exoPlayer.removeListener(this)
                    }
                }
                exoPlayer.addListener(listener)
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

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                // Lifecycle events arrive on the main thread, so the player is read
                // HERE and only the DataStore write is delegated. The old version
                // read currentPosition/currentTracks inside a coroutine, which
                // Media3 rejects with IllegalStateException whenever that coroutine
                // resumes on another thread (caught by PlayerScreenSmokeTest).
                val position = exoPlayer.currentPosition
                val tracks = exoPlayer.currentTracks
                val audioId = selectedTrackId(tracks, C.TRACK_TYPE_AUDIO)
                val textId = selectedTrackId(tracks, C.TRACK_TYPE_TEXT)

                CoroutineScope(Dispatchers.IO).launch {
                    if (settingsRepository.resumePlayback.first()) {
                        settingsRepository.savePlaybackPosition(url, position)
                    }
                    if (!settingsRepository.rememberSelections.first()) return@launch
                    if (audioId.isNotEmpty()) settingsRepository.saveAudioTrack(url, audioId)
                    if (textId.isNotEmpty()) settingsRepository.saveTextTrack(url, textId)
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
            // Read everything we still need from the player BEFORE releasing it:
            // querying a released player throws, and the old code did exactly that
            // from a coroutine that ran after release().
            val lastPosition = exoPlayer.currentPosition
            val lastTracks = exoPlayer.currentTracks
            exoPlayer.release()

            // rememberCoroutineScope() is cancelled as this composable leaves, so a
            // launch on it never ran: the resume position and the chosen tracks were
            // silently never saved. A short-lived scope of its own completes the write.
            CoroutineScope(Dispatchers.IO).launch {
                if (settingsRepository.resumePlayback.first()) {
                    settingsRepository.savePlaybackPosition(url, lastPosition)
                }
                val rememberSelections = settingsRepository.rememberSelections.first()
                if (!rememberSelections) return@launch
                for (group in lastTracks.groups) {
                    if (!group.isSelected) continue
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
    
    // Get dialog colors based on preference
    // The player is the only source of truth for whether the output is silent, so
    // the glyph is derived from it: a listener for every event that can change the
    // volume, and one place that reads it. Declared before the view that installs
    // it, because that view's factory runs when it is first composed.
    val updateMuteState = {
        val muted = runCatching { exoPlayer.volume == 0f }.getOrDefault(false)
        if (isMuted != muted) isMuted = muted
    }
    val muteStateListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onVolumeChanged(volume: Float) {
                updateMuteState()
            }
        }
    }
    val toggleMute = {
        if (exoPlayer.volume > 0f) {
            // Remember the level that was actually active: a boost above 1f is
            // stored in the same value and must come back with the sound.
            lastVolumeBeforeMute = exoPlayer.volume
            exoPlayer.volume = 0f
        } else {
            exoPlayer.volume = lastVolumeBeforeMute.coerceIn(0f, 1f)
        }
        updateMuteState()
    }
    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(muteStateListener)
        updateMuteState()
        onDispose { exoPlayer.removeListener(muteStateListener) }
    }

    val dialogColors = rememberDialogColors(dialogThemePreference, appColor, themePreference)
    
    // Subtitle styling used to be applied only once, when the PlayerView was
    // first created, so preference changes ("System caption style", font, bold,
    // size, background, embedded styles) never reached the running player.
    // Re-apply on every change instead.
    LaunchedEffect(
        systemCaptionStyle,
        subtitleFont,
        subtitleBold,
        subtitleBackground,
        subtitleSize,
        subtitleEmbeddedStyles,
        playerViewRef,
    ) {
        val view = playerViewRef?.subtitleView ?: return@LaunchedEffect
        applySubtitleStyle(
            subtitleView = view,
            context = context,
            useSystemCaptionStyle = systemCaptionStyle,
            font = subtitleFont,
            bold = subtitleBold,
            background = subtitleBackground,
            textSize = subtitleSize,
            embeddedStyles = subtitleEmbeddedStyles,
        )
        view.requestLayout()
        view.invalidate()
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
                    keepScreenOn = !backgroundPlay
                    // The controls are our own loading/error surface too: show them
                    // at least once instead of leaving a bare black rectangle.
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    showController()

                    subtitleView?.let { view ->
                        applySubtitleStyle(
                            subtitleView = view,
                            context = ctx,
                            useSystemCaptionStyle = systemCaptionStyle,
                            font = subtitleFont,
                            bold = subtitleBold,
                            background = subtitleBackground,
                            textSize = subtitleSize,
                            embeddedStyles = subtitleEmbeddedStyles,
                        )
                    }
                    systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or 
                                         View.SYSTEM_UI_FLAG_FULLSCREEN or 
                                         View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    
                    setShowNextButton(false)
                    setShowPreviousButton(false)

                    // The mute control, stamped into the control row like the rest
                    // of this screen's buttons — Media3's own custom-layout API is
                    // not callable from a View factory, so the icon is drawn here
                    // with a ComposeView and given its own recomposer: that is what
                    // lets the glyph follow the player's volume rather than a
                    // remembered flag.
                    val muteHost = androidx.compose.ui.platform.ComposeView(ctx).apply {
                        setViewCompositionStrategy(
                            androidx.compose.ui.platform.ViewCompositionStrategy
                                .DisposeOnViewTreeLifecycleDestroyed
                        )
                        setContent {
                            val muted = isMuted
                            PlayerMuteButton(
                                muted = muted,
                                onToggle = toggleMute,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    }

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
                    
                    // Mute / Unmute. A Media3 custom layout past the settings
                    // button rather than an ImageView stuck into exo_basic_controls:
                    // the glyph is a composable, so it changes when the state
                    // changes — the volume is read by a listener on the player, not
                    // remembered by whoever set the icon last.
                    // The mute control is a custom layout past setup below: it has
                    // to be a composable for its glyph to follow the state, and
                    // Media3 lays custom layouts out for it.
                    
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
                        basicControls.addView(muteHost, insertIndex + 6)
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
                        showSystemVolumePanel = showSystemVolumePanel,
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
                // No state writes here: this runs during layout, and the old
                // `isMuted = exoPlayer.volume == 0f` assignment both fought the
                // mute button and re-triggered composition. The button owns the
                // icon; the controller is hidden while locked or in Picture-in-Picture.
                playerView.useController = !isLocked && !inPip
            },
            modifier = Modifier.fillMaxSize()
        )

        // Playback failure panel (replaces a silent black screen).
        val failure: Throwable? = playbackError ?: playbackFailure
        if (failure != null && !inPip) {
            PlaybackErrorPanel(
                details = buildString {
                    appendLine(url)
                    if (failure is androidx.media3.common.PlaybackException) {
                        appendLine("error: ${failure.errorCodeName} (${failure.errorCode})")
                    } else {
                        appendLine("error: ${failure::class.java.simpleName}")
                    }
                    failure.message?.let { appendLine("message: $it") }
                    failure.cause?.let { appendLine("cause: ${it::class.java.simpleName}: ${it.message}") }
                    playbackError?.let {
                        appendLine("state: ${playerStateName(exoPlayer.playbackState)}")
                        appendLine("position: ${exoPlayer.currentPosition} ms")
                        appendLine("duration: ${exoPlayer.duration} ms")
                    }
                },
                onRetry = {
                    playbackError = null
                    playbackFailure = null
                    runCatching { exoPlayer.prepare() }
                    exoPlayer.playWhenReady = true
                },
                onCopy = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Ghost Play error", text)
                    )
                    Toast.makeText(context, "Error details copied", Toast.LENGTH_SHORT).show()
                },
                onOpenExternal = {
                    // Last resort: hand the file/stream to whatever else the user has.
                    // A *typed* ACTION_VIEW is only handled by apps that declare that
                    // MIME type, which most browsers (the usual external opener for a
                    // stream link) do not - it then throws ActivityNotFoundException.
                    // Try the bare URI first and fall back to a typed intent.
                    runCatching {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setData(android.net.Uri.parse(url))
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }.recoverCatching {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(android.net.Uri.parse(url), "video/*")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }.onFailure {
                        Toast.makeText(context, "No other app can open this link", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Keep the official Material 3 RoundedPolygon loading indicator centered
        // at the player controls while Media3 is buffering.
        AnimatedVisibility(
            visible = isBuffering && !isLocked && !inPip,
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
        if (isLocked && showUnlockButton && !inPip) {
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
        if (!isLocked && !inPip) {
            IndicatorOverlay(Icons.Default.ZoomIn, "$zoomPercent%", showZoom)
            IndicatorOverlay(Icons.Default.VolumeUp, "$volumePercent%", showVolume)
            IndicatorOverlay(Icons.Default.BrightnessMedium, "$brightnessPercent%", showBrightness)
        }
        
        // Seek Overlay
        AnimatedVisibility(
            visible = showSeek && !isLocked && !inPip,
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
    if (showQualityDialog && !inPip) {
        TrackSelectionSheet(
            title = "Video quality",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_VIDEO,
            onDismiss = { showQualityDialog = false },
        )
    }
    
    // Audio Dialog
    if (showAudioDialog && !inPip) {
        TrackSelectionSheet(
            title = "Audio track",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_AUDIO,
            onDismiss = { showAudioDialog = false },
        )
    }
    
    
    // Subtitle Dialog
    if (showSubtitleDialog && !inPip) {
        TrackSelectionSheet(
            title = "Subtitles",
            exoPlayer = exoPlayer,
            trackType = C.TRACK_TYPE_TEXT,
            onDismiss = { showSubtitleDialog = false },
        )
    }
    
    // Audio Sync / Pitch Dialog
    if (showAudioAdjustDialog && !inPip) {
        AudioAdjustSheet(
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
        )
    }
    
    // Aspect Ratio Dialog
    if (showAspectRatioDialog && !inPip) {
        AspectRatioSheet(
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
        )
    }
}

/**
 * Id (or language) of the currently selected track of [trackType], or "" when the
 * player has no selected track of that type. Pure function of the track list so it
 * can be called from the main thread without touching the player twice.
 */
/**
 * True when audio currently goes to a wired / USB / Bluetooth output.
 *
 * Ported from NextPlayer's `VolumeState.AudioManager.isHeadsetOn` (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/VolumeState.kt
 */
private fun android.media.AudioManager.isHeadsetOn(): Boolean =
    getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).any { device ->
        device.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                device.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET)
    }

internal fun selectedTrackId(tracks: Tracks, trackType: Int): String {
    for (group in tracks.groups) {
        if (group.type != trackType || !group.isSelected) continue
        for (i in 0 until group.length) {
            if (!group.isTrackSelected(i)) continue
            val format = group.getTrackFormat(i)
            return format.id ?: format.language ?: ""
        }
    }
    return ""
}

/** Human readable name for a Media3 playback state, used in the error panel. */
internal fun playerStateName(state: Int): String = when (state) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "STATE_$state"
}

/**
 * Shown in place of the player when playback fails. Without it a decode or
 * container failure only produced a black screen, so the user had no way to tell
 * a broken file from a broken app.
 */
@Composable
private fun PlaybackErrorPanel(
    details: String,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenExternal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF151515),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "This video could not be played",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = details.trim(),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetry) { Text("Retry") }
                    OutlinedButton(onClick = { onCopy(details) }) { Text("Copy") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenExternal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in another app")
                }
            }
        }
    }
}

/**
 * Current system screen brightness as 0..1, used as the starting point for the
 * first brightness gesture (the window override is -1 until we set one).
 */
private fun systemBrightness(): Float = try {
    val raw = android.provider.Settings.System.getInt(
        com.ghost.video.SharedPlayer.appContext?.contentResolver,
        android.provider.Settings.System.SCREEN_BRIGHTNESS
    )
    (raw / 255f).coerceIn(0f, 1f)
} catch (_: Exception) {
    0.5f
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
    /** "System volume panel" preference (NextPlayer `showSystemVolumePanel`). */
    private val showSystemVolumePanel: Boolean,
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
                
                if (e.x > playerView.width / 2f) {
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
                            if (currentBrightness < 0f) {
                                // Window brightness is BRIGHTNESS_OVERRIDE_NONE while the
                                // system controls it, so the first swipe used to snap the
                                // screen to a hardcoded 50%. Start from the real system
                                // brightness instead and only take over from there.
                                currentBrightness = systemBrightness()
                            }
                            
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
                        // "System volume panel": show the OS volume UI when a
                        // headset is connected, matching NextPlayer's VolumeState.
                        val showPanel = showSystemVolumePanel && audioManager.isHeadsetOn()
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            newVol,
                            if (showPanel) AudioManager.FLAG_SHOW_UI else 0,
                        )
                        
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
    appColor: com.ghost.video.data.AppColor,
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

@Composable
fun TrackSelectionSheet(
    title: String,
    exoPlayer: ExoPlayer,
    trackType: Int,
    onDismiss: () -> Unit,
) {
    val tracks = exoPlayer.currentTracks

    // One entry per track of the requested type, in the order the container lists
    // them.
    val trackGroups = remember(tracks) {
        buildList {
            for (group in tracks.groups) {
                if (group.type == trackType) {
                    for (i in 0 until group.length) add(group to i)
                }
            }
        }
    }

    fun selectOverride(group: Tracks.Group, index: Int) {
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(index))
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        onDismiss()
    }

    PlayerTrackSheet(title = title, onDismiss = onDismiss) {
        if (trackType == C.TRACK_TYPE_TEXT) {
            val subtitlesOff = trackGroups.isEmpty() ||
                tracks.groups.none { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
            PlayerChoiceRow(
                label = "None",
                subtitle = "Disable subtitles",
                selected = subtitlesOff,
                onClick = {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .build()
                    onDismiss()
                },
            )
        }

        trackGroups.forEach { (group, trackIndex) ->
            val format = group.getTrackFormat(trackIndex)
            val selected = group.isTrackSelected(trackIndex)
            when (trackType) {
                C.TRACK_TYPE_VIDEO -> {
                    val bitrate = if (format.bitrate > 0) " • ${format.bitrate / 1000} kbps" else ""
                    val fps = if (format.frameRate > 0) " • ${format.frameRate.toInt()} fps" else ""
                    val codec = format.sampleMimeType?.substringAfter("/")?.uppercase() ?: ""
                    val label = if (format.width > 0 && format.height > 0) {
                        "${format.width}x${format.height}$bitrate$fps"
                    } else {
                        format.label ?: "Video"
                    }
                    PlayerChoiceRow(
                        label = label,
                        subtitle = codec.ifEmpty { null }?.let { "Codec: $it" },
                        selected = selected,
                        onClick = { selectOverride(group, trackIndex) },
                    )
                }
                C.TRACK_TYPE_AUDIO -> {
                    val lang = format.language?.uppercase() ?: "Unknown"
                    val trackLabel = format.label ?: ""
                    val channels = when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        in 3..5 -> "${format.channelCount}ch"
                        else -> ""
                    }
                    val subInfo = buildList {
                        if (channels.isNotEmpty()) add(channels)
                        if (format.bitrate > 0) add("${format.bitrate / 1000} kbps")
                        if (format.sampleRate > 0) add("${format.sampleRate / 1000} kHz")
                        format.sampleMimeType?.substringAfter("/")?.let { add(it.uppercase()) }
                    }.joinToString(" • ")
                    PlayerChoiceRow(
                        label = buildString {
                            append(lang)
                            if (trackLabel.isNotEmpty()) append(" • $trackLabel")
                        },
                        subtitle = subInfo.ifEmpty { null },
                        selected = selected,
                        onClick = { selectOverride(group, trackIndex) },
                    )
                }
                C.TRACK_TYPE_TEXT -> {
                    val lang = format.language?.uppercase() ?: "Unknown"
                    val trackLabel = format.label ?: ""
                    val flags = buildList {
                        if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) add("Forced")
                        if ((format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0) add("SDH")
                        if ((format.roleFlags and C.ROLE_FLAG_CAPTION) != 0) add("Caption")
                        if ((format.roleFlags and C.ROLE_FLAG_DUB) != 0) add("Dub")
                        if ((format.roleFlags and C.ROLE_FLAG_COMMENTARY) != 0) add("Commentary")
                        if ((format.roleFlags and C.ROLE_FLAG_EASY_TO_READ) != 0) add("Easy Read")
                        format.sampleMimeType?.substringAfter("/")?.let { add(it.uppercase()) }
                    }.joinToString(" • ")
                    PlayerChoiceRow(
                        label = buildString {
                            append(lang)
                            if (trackLabel.isNotEmpty() && trackLabel != lang) append(" • $trackLabel")
                        },
                        subtitle = flags.ifEmpty { null },
                        selected = selected,
                        onClick = { selectOverride(group, trackIndex) },
                    )
                }
                else -> PlayerChoiceRow(
                    label = format.label ?: "Track $trackIndex",
                    subtitle = null,
                    selected = selected,
                    onClick = { selectOverride(group, trackIndex) },
                )
            }
        }

        if (trackGroups.isEmpty() && trackType != C.TRACK_TYPE_TEXT) {
            Text(
                text = "No tracks available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
fun IndicatorOverlay(icon: ImageVector, text: String, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(initialOffsetY = { -50 }, animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)) +
            slideOutVertically(targetOffsetY = { -50 }, animationSpec = tween(300)),
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
fun AspectRatioSheet(
    currentAspectRatio: Int,
    onAspectRatioSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val aspectOptions = listOf(
        AspectRatioOption("Original (Fit)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
        AspectRatioOption("Stretch (Fill)", AspectRatioFrameLayout.RESIZE_MODE_FILL),
        AspectRatioOption("Crop (Zoom)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
        AspectRatioOption("16:9", 8),
        AspectRatioOption("18:9", 9),
        AspectRatioOption("19:9", 5),
        AspectRatioOption("20:9", 6),
        AspectRatioOption("21:9", 7),
    )
    PlayerTrackSheet(title = "Aspect ratio", onDismiss = onDismiss) {
        aspectOptions.forEach { option ->
            PlayerChoiceRow(
                label = option.label,
                subtitle = null,
                selected = currentAspectRatio == option.value,
                onClick = {
                    onAspectRatioSelected(option.value)
                    onDismiss()
                },
            )
        }
    }
}

data class AspectRatioOption(
    val label: String,
    val value: Int,
)

private fun applySubtitleStyle(
    subtitleView: SubtitleView,
    context: Context,
    useSystemCaptionStyle: Boolean,
    font: SubtitleFont,
    bold: Boolean,
    background: Boolean,
    textSize: Int,
    embeddedStyles: Boolean,
) {
    // Embedded styling only ever applies to the app-managed look; the system
    // caption style must not be overridden by the subtitle file.
    subtitleView.setApplyEmbeddedFontSizes(!useSystemCaptionStyle && embeddedStyles)
    subtitleView.setApplyEmbeddedStyles(!useSystemCaptionStyle && embeddedStyles)

    if (useSystemCaptionStyle) {
        // Reads the style + text size configured in the system captioning
        // preferences (Settings > Accessibility > Caption preferences).
        subtitleView.setUserDefaultStyle()
        subtitleView.setUserDefaultTextSize()
        return
    }

    val typeface = when (font) {
        SubtitleFont.DEFAULT -> Typeface.DEFAULT
        SubtitleFont.LORA -> ResourcesCompat.getFont(context, R.font.lora_regular)
        SubtitleFont.JETBRAINS_MONO ->
            ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
        SubtitleFont.NUNITO -> ResourcesCompat.getFont(context, R.font.nunito_regular)
    }

    val style = CaptionStyleCompat(
        android.graphics.Color.WHITE,
        if (background) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
        if (bold) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
        android.graphics.Color.BLACK,
        typeface,
    )
    subtitleView.setStyle(style)

    // Fixed text size only when embedded styles are off; otherwise the subtitle
    // file's own sizes win.
    if (!embeddedStyles) {
        subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())
    }
}
