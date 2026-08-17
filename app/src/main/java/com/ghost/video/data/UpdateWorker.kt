package com.ghost.video.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ghost.video.BuildConfig
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically checks GitHub for a newer release and, if the user enabled update
 * notifications, posts a notification. Only notifies once per new release tag
 * (tracked via [SettingsRepository.lastSeenRelease]).
 */
class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = SettingsRepository(applicationContext)

        // Respect the user's toggle.
        val enabled = repo.updateNotifications.first()
        if (!enabled) return Result.success()

        val release = UpdateChecker.fetchLatestRelease() ?: return Result.retry()
        // Compare against the real installed version instead of a hardcoded one,
        // so users who are already up to date never get a false "update" nag.
        val current = BuildConfig.VERSION_NAME
        if (!isNewer(release.tag, current)) return Result.success()

        // Don't nag about a release the user already saw.
        val lastSeen = repo.lastSeenRelease.first()
        if (lastSeen == release.tag) return Result.success()

        postNotification(release)
        repo.setLastSeenRelease(release.tag)
        return Result.success()
    }

    private fun postNotification(release: ReleaseInfo) {
        // On Android 13+ we need POST_NOTIFICATIONS granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when a new Ghost Play version is available" }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getActivity(applicationContext, 0, intent, flags)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update available: ${release.name.ifEmpty { release.tag }}")
            .setContentText("A new version of Ghost Play is available. Tap to view.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                release.notes.ifEmpty { "A new version of Ghost Play is available. Tap to view the release." }
            ))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify — ignore.
        }
    }

    companion object {
        private const val CHANNEL_ID = "ghost_play_updates"
        private const val NOTIF_ID = 4201
        private const val WORK_NAME = "ghost_play_update_check"
        private const val WORK_NAME_ONCE = "ghost_play_update_check_once"

        /**
         * Schedule the update checks. Safe to call repeatedly (on app start and
         * when the user enables the toggle).
         *
         * Two pieces of work are enqueued:
         *  1. An immediate one-time check, so a newly published release is noticed
         *     right away instead of waiting up to a full day for the first periodic
         *     run (the old behaviour made notifications appear to "never arrive").
         *  2. A daily periodic check, so it keeps working even if the app is not
         *     opened for a long time.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic
            )

            val once = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                once
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONCE)
        }
    }
}

/** Version comparison shared with the update screen. */
internal fun isNewer(tag: String, current: String): Boolean {
    fun parts(s: String) = s.trim().removePrefix("v").removePrefix("V")
        .split(".", "-", "_")
        .mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
    val a = parts(tag)
    val b = parts(current)
    val n = maxOf(a.size, b.size)
    for (i in 0 until n) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
