package com.fireplay.com.service

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
import com.fireplay.com.MainActivity
import com.fireplay.com.ui.screens.LocalAudio
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
    
    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_PLAY = "com.fireplay.com.action.PLAY"
        const val ACTION_PAUSE = "com.fireplay.com.action.PAUSE"
        const val ACTION_STOP = "com.fireplay.com.action.STOP"
        
        private var instance: MusicService? = null
        
        fun getInstance(): MusicService? = instance
        
        fun getCurrentAudioFlow(): StateFlow<LocalAudio?> = instance?._currentAudio ?: MutableStateFlow(null)
        fun getIsPlayingFlow(): StateFlow<Boolean> = instance?._isPlaying ?: MutableStateFlow(false)
        
        fun playAudio(context: Context, audio: LocalAudio) {
            instance?.let { service ->
                service._currentAudio.value = audio
                service.player?.let { player ->
                    player.setMediaItem(MediaItem.fromUri(audio.uri))
                    player.prepare()
                    player.playWhenReady = true
                }
            } ?: run {
                val intent = Intent(context, MusicService::class.java).apply {
                    action = ACTION_PLAY
                    putExtra("audio_id", audio.id)
                    putExtra("audio_name", audio.name)
                    putExtra("audio_uri", audio.uri)
                    putExtra("audio_artist", audio.artist)
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
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }
        }
        
        fun stop(context: Context) {
            instance?.player?.stop()
            instance?._currentAudio?.value = null
            val intent = Intent(context, MusicService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        initializePlayer()
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    updateNotification()
                }
            })
        }
        
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: androidx.media3.session.MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    return com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
                }
            })
            .build()
    }
    
    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val id = intent.getLongExtra("audio_id", 0)
                val name = intent.getStringExtra("audio_name") ?: ""
                val uri = intent.getStringExtra("audio_uri") ?: ""
                val artist = intent.getStringExtra("audio_artist")
                
                val audio = LocalAudio(id, name, uri, artist)
                _currentAudio.value = audio
                player?.setMediaItem(MediaItem.fromUri(uri))
                player?.prepare()
                player?.playWhenReady = true
            }
            ACTION_PAUSE -> player?.pause()
            ACTION_STOP -> {
                player?.stop()
                _currentAudio.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun updateNotification() {
        val audio = _currentAudio.value ?: return
        
        val notification = createNotification(
            title = audio.name,
            artist = audio.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
            isPlaying = player?.isPlaying == true
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
    
    private fun createNotification(title: String, artist: String, isPlaying: Boolean): Notification {
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
        
        val stopIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken)
            .setShowActionsInCompactView(0)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setStyle(mediaStyle)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        instance = null
        serviceScope.cancel()
    }
}
