package com.ghost.io.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.ghost.io.MainActivity
import com.ghost.io.ui.screens.LocalAudio
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentAudio = MutableStateFlow<LocalAudio?>(null)
    val currentAudio: StateFlow<LocalAudio?> = _currentAudio.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Playlist queue
    private var playlist: List<LocalAudio> = emptyList()

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.ghost.io.action.PLAY"
        const val ACTION_PAUSE = "com.ghost.io.action.PAUSE"
        const val ACTION_STOP = "com.ghost.io.action.STOP"
        const val ACTION_NEXT = "com.ghost.io.action.NEXT"
        const val ACTION_PREVIOUS = "com.ghost.io.action.PREVIOUS"

        private var instance: MusicService? = null

        fun getInstance(): MusicService? = instance

        fun getCurrentAudioFlow(): StateFlow<LocalAudio?> = instance?._currentAudio ?: MutableStateFlow(null)
        fun getIsPlayingFlow(): StateFlow<Boolean> = instance?._isPlaying ?: MutableStateFlow(false)

        fun playAudio(context: Context, audio: LocalAudio, fullPlaylist: List<LocalAudio> = emptyList()) {
            instance?.let { service ->
                service.playAudioInternal(audio, fullPlaylist)
            } ?: run {
                val intent = Intent(context, MusicService::class.java).apply {
                    action = ACTION_PLAY
                    putExtra("audio_id", audio.id)
                    putExtra("audio_name", audio.name)
                    putExtra("audio_uri", audio.uri)
                    putExtra("audio_artist", audio.artist)
                    putParcelableArrayListExtra("playlist", ArrayList(fullPlaylist.ifEmpty { listOf(audio) }))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun togglePlayPause() {
            instance?.player?.let { player ->
                player.playWhenReady = !player.isPlaying
            }
        }

        fun skipToNext() {
            instance?.player?.seekToNext()
        }

        fun skipToPrevious() {
            instance?.player?.seekToPrevious()
        }

        fun stop(context: Context) {
            instance?.let { service ->
                service.player?.stop()
                service._currentAudio.value = null
                service._isPlaying.value = false
                service.playlist = emptyList()
                service.stopForeground(STOP_FOREGROUND_REMOVE)
                service.stopSelf()
            }
        }
    }

    private fun playAudioInternal(audio: LocalAudio, fullPlaylist: List<LocalAudio>) {
        val songs = fullPlaylist.ifEmpty { listOf(audio) }
        playlist = songs
        val startIndex = songs.indexOfFirst { it.uri == audio.uri }.coerceAtLeast(0)

        val mediaItems = songs.map { MediaItem.fromUri(it.uri) }
        player?.let { p ->
            p.setMediaItems(mediaItems, startIndex, 0L)
            p.prepare()
            p.playWhenReady = true
        }
        _currentAudio.value = audio
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    updateNotification()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Auto-advance handled by ExoPlayer when playlist exists
                        // If last song ended and no repeat, stop
                        if (currentMediaItemIndex >= mediaItemCount - 1 && repeatMode == Player.REPEAT_MODE_OFF) {
                            _isPlaying.value = false
                            updateNotification()
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Update current audio when song changes (next/prev/auto)
                    mediaItem?.let { item ->
                        val uri = item.localConfiguration?.uri?.toString()
                        val audio = playlist.find { it.uri == uri }
                        _currentAudio.value = audio
                        updateNotification()
                    }
                }
            })
        }

        mediaSession = MediaSession.Builder(this, player!!).build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY -> {
                val id = intent.getLongExtra("audio_id", 0)
                val name = intent.getStringExtra("audio_name") ?: "Unknown"
                val uri = intent.getStringExtra("audio_uri") ?: ""
                val artist = intent.getStringExtra("audio_artist")
                val parcelList = intent.getParcelableArrayListExtra<LocalAudio>("playlist") ?: arrayListOf()
                val fullPlaylist = parcelList.ifEmpty { listOf(LocalAudio(id, name, uri, artist)) }

                playAudioInternal(LocalAudio(id, name, uri, artist), fullPlaylist)

                val notification = createNotification(
                    title = name,
                    artist = artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                    isPlaying = true,
                    hasNext = fullPlaylist.size > 1
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_PAUSE -> {
                player?.playWhenReady = false
                updateNotification()
            }
            ACTION_NEXT -> {
                player?.seekToNext()
            }
            ACTION_PREVIOUS -> {
                player?.seekToPrevious()
            }
            ACTION_STOP -> {
                player?.stop()
                _currentAudio.value = null
                _isPlaying.value = false
                playlist = emptyList()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val audio = _currentAudio.value ?: return
        val hasNext = playlist.size > 1

        val notification = createNotification(
            title = audio.name,
            artist = audio.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
            isPlaying = player?.isPlaying == true,
            hasNext = hasNext
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(title: String, artist: String, isPlaying: Boolean, hasNext: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 3, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 4, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show actions in compact view: prev, play/pause, next
        val compactActions = if (hasNext) {
            intArrayOf(0, 1, 2)
        } else {
            intArrayOf(1)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken)
            .setShowActionsInCompactView(*compactActions)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setStyle(mediaStyle)

        // Previous button (only if playlist has multiple songs)
        if (hasNext) {
            builder.addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            )
        }

        // Play/Pause button
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        )

        // Next button (only if playlist has multiple songs)
        if (hasNext) {
            builder.addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            )
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        instance = null
        playlist = emptyList()
        serviceScope.cancel()
    }
}
