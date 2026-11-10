package com.ghost.video.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Update checking, ported 1:1 from DownList (divudon21/DownList,
 * `lib/services/update_service.dart` + `lib/screens/settings/update_settings_screen.dart`).
 *
 * Differences are limited to this app's identity: package name, repository paths,
 * universal asset name and the metadata marker used in the release notes.
 */

const val UPDATES_PACKAGE_NAME = "com.ghost.video"
const val UPDATES_REPO_OWNER = "divudon21"
const val UPDATES_REPO_NAME = "GHOST-PLAY"
const val UPDATES_ASSET_NAME = "app-universal-release.apk"
const val UPDATES_REPO_URL = "https://github.com/$UPDATES_REPO_OWNER/$UPDATES_REPO_NAME"

private const val RELEASES_URL =
    "https://api.github.com/repos/$UPDATES_REPO_OWNER/$UPDATES_REPO_NAME/releases?per_page=100"

/** `<!-- ghost-play-update:{"packageName":...} -->` inside the release notes. */
private val METADATA_PATTERN =
    Regex("""<!--\s*ghost-play-update:(\{[^\r\n]*\})\s*-->""")

private val FINGERPRINT_PATTERN = Regex("^[a-f0-9]{64}$")

/** Thrown with the exact user-facing messages DownList shows. */
class UpdateFormatException(message: String) : Exception(message)

data class InstalledBuild(
    val versionName: String,
    val versionCode: Int,
    val packageName: String,
    val signerSha256: String,
) {
    companion object {
        fun from(
            versionName: String?,
            versionCode: Long,
            packageName: String?,
            signerSha256: String?,
        ): InstalledBuild {
            val code = versionCode.toInt()
            val packageId = packageName.orEmpty()
            val signer = signerSha256.orEmpty().lowercase()
            if (code <= 0 || packageId != UPDATES_PACKAGE_NAME || !FINGERPRINT_PATTERN.matches(signer)) {
                throw UpdateFormatException(
                    "Installed app information is unavailable or invalid.",
                )
            }
            return InstalledBuild(
                versionName = versionName.orEmpty(),
                versionCode = code,
                packageName = packageId,
                signerSha256 = signer,
            )
        }
    }
}

data class UpdateRelease(
    val versionName: String,
    val versionCode: Int,
    val prerelease: Boolean,
    val releaseUrl: String,
    val downloadUrl: String,
    val signerSha256: String,
) {
    val label: String
        get() = "$versionName (build $versionCode)" + if (prerelease) " · Beta" else ""

    fun isCompatibleWith(installed: InstalledBuild): Boolean =
        signerSha256 == installed.signerSha256
}

/**
 * Only releases with validated package, build number, certificate and APK
 * metadata are considered. Returns the newest candidate that is actually newer
 * than the installed build, or null when the app is up to date.
 */
fun selectUpdateRelease(
    releases: JSONArray,
    installed: InstalledBuild,
    includeBeta: Boolean,
): UpdateRelease? {
    var newest: UpdateRelease? = null
    var validCount = 0
    for (i in 0 until releases.length()) {
        val item = releases.optJSONObject(i) ?: continue
        if (item.optBoolean("draft", false)) continue
        if (!includeBeta && item.optBoolean("prerelease", false)) continue
        val body = item.optString("body", "")
        val match = METADATA_PATTERN.find(body) ?: continue
        try {
            val data = JSONObject(match.groupValues[1])
            val code = data.optInt("versionCode", 0)
            val name = data.optString("versionName", "").trim()
            val signer = data.optString("signerSha256", "").lowercase()
            val releaseUrl = item.optString("html_url", "")
            val releaseUri = runCatching { Uri.parse(releaseUrl) }.getOrNull()
            val assets = item.optJSONArray("assets")
            if (data.optString("packageName") != installed.packageName ||
                code <= 0 ||
                name.isEmpty() ||
                name.length > 80 ||
                !FINGERPRINT_PATTERN.matches(signer) ||
                releaseUri?.scheme != "https" ||
                releaseUri.host != "github.com" ||
                !(releaseUri.path?.startsWith("/$UPDATES_REPO_OWNER/$UPDATES_REPO_NAME/releases/tag/") == true) ||
                assets == null
            ) {
                continue
            }
            var apk: JSONObject? = null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name") == UPDATES_ASSET_NAME &&
                    asset.optString("state") == "uploaded"
                ) {
                    apk = asset
                    break
                }
            }
            if (apk == null) continue
            val downloadUrl = apk.optString("browser_download_url", "")
            val downloadUri = runCatching { Uri.parse(downloadUrl) }.getOrNull()
            if (downloadUri?.scheme != "https" ||
                downloadUri.host != "github.com" ||
                !(downloadUri.path?.startsWith(
                    "/$UPDATES_REPO_OWNER/$UPDATES_REPO_NAME/releases/download/",
                ) == true)
            ) {
                continue
            }
            validCount++
            val release = UpdateRelease(
                versionName = name,
                versionCode = code,
                prerelease = item.optBoolean("prerelease", false),
                releaseUrl = releaseUrl,
                downloadUrl = downloadUrl,
                signerSha256 = signer,
            )
            val current = newest
            if (current == null || release.versionCode > current.versionCode) newest = release
        } catch (_: Exception) {
            // Skip releases that don't carry valid metadata for this app.
        }
    }
    if (validCount == 0) {
        throw UpdateFormatException("No compatible release information is available yet.")
    }
    val candidate = newest
    return if (candidate != null && candidate.versionCode > installed.versionCode) candidate else null
}

/** Reads the installed package identity (version + signing certificate SHA-256). */
fun readInstalledBuild(context: Context): InstalledBuild {
    val packageManager = context.packageManager
    @Suppress("DEPRECATION")
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }
    val info = packageManager.getPackageInfo(context.packageName, flags)
    @Suppress("DEPRECATION")
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.signingInfo?.apkContentsSigners
    } else {
        info.signatures
    }
    val signature = signatures?.firstOrNull()
        ?: throw UpdateFormatException("Installed app information is unavailable or invalid.")
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(signature.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    @Suppress("DEPRECATION")
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        info.versionCode.toLong()
    }
    return InstalledBuild.from(
        versionName = info.versionName,
        versionCode = code,
        packageName = context.packageName,
        signerSha256 = hash,
    )
}

/** State exposed to the Updates screen (mirrors DownList's UpdateService fields). */
data class UpdatesState(
    val autoCheck: Boolean = true,
    val notifications: Boolean = true,
    val includeBeta: Boolean = false,
    val popup: Boolean = true,
    val ready: Boolean = false,
    val checking: Boolean = false,
    val checked: Boolean = false,
    val error: String? = null,
    val installed: InstalledBuild? = null,
    val available: UpdateRelease? = null,
)

object UpdateOption {
    const val AUTO_CHECK = "auto_check"
    const val NOTIFICATIONS = "notifications"
    const val INCLUDE_BETA = "include_beta"
    const val POPUP = "popup"
}

class UpdatesService(private val context: Context) {

    private val repository = SettingsRepository(context)
    private val _state = MutableStateFlow(UpdatesState())
    val state: StateFlow<UpdatesState> = _state.asStateFlow()

    suspend fun initialize() {
        val preferences = repository.updatesPreferences.first()
        _state.value = _state.value.copy(
            autoCheck = preferences.autoCheck,
            notifications = preferences.notifications,
            includeBeta = preferences.includeBeta,
            popup = preferences.popup,
            ready = true,
        )
    }

    suspend fun setOption(key: String, value: Boolean) {
        when (key) {
            UpdateOption.AUTO_CHECK -> repository.setUpdatesAutoCheck(value)
            UpdateOption.NOTIFICATIONS -> repository.setUpdatesNotifications(value)
            UpdateOption.INCLUDE_BETA -> repository.setUpdatesIncludeBeta(value)
            UpdateOption.POPUP -> repository.setUpdatesPopup(value)
            else -> throw IllegalArgumentException(key)
        }
        _state.value = when (key) {
            UpdateOption.AUTO_CHECK -> _state.value.copy(autoCheck = value)
            UpdateOption.NOTIFICATIONS -> _state.value.copy(notifications = value)
            UpdateOption.POPUP -> _state.value.copy(popup = value)
            else -> _state.value.copy(
                includeBeta = value,
                checked = false,
                error = null,
                available = null,
            )
        }
        if (key == UpdateOption.NOTIFICATIONS) {
            if (!value) UpdateNotifications.clear(context)
        }
    }

    /** Fetches the release list and resolves the newest compatible update. */
    suspend fun check(): UpdateRelease? {
        if (_state.value.checking) return _state.value.available
        _state.value = _state.value.copy(checking = true, error = null)
        return try {
            val build = withContext(Dispatchers.IO) { readInstalledBuild(context) }
            val releases = withContext(Dispatchers.IO) { fetchReleases() }
            val release = selectUpdateRelease(releases, build, _state.value.includeBeta)
            _state.value = _state.value.copy(
                installed = build,
                available = release,
                checked = true,
                checking = false,
                error = null,
            )
            release
        } catch (e: Exception) {
            val message = (e.message ?: e.toString())
                .removePrefix("Exception: ")
                .removePrefix("UpdateFormatException: ")
                .removePrefix("FormatException: ")
            _state.value = _state.value.copy(
                checked = false,
                checking = false,
                available = null,
                error = message,
            )
            null
        }
    }

    suspend fun notifyIfNeeded(release: UpdateRelease) {
        val current = _state.value
        if (!current.notifications) return
        val installed = current.installed ?: return
        if (repository.updatesNotifiedBuild.first() == release.versionCode) return
        val shown = UpdateNotifications.show(
            context = context,
            version = release.label,
            url = release.releaseUrl,
            compatible = release.isCompatibleWith(installed),
        )
        if (shown) repository.setUpdatesNotifiedBuild(release.versionCode)
    }

    fun openRelease(release: UpdateRelease) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.releaseUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun fetchReleases(): JSONArray {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "GhostPlay-App")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw UpdateFormatException(
                    "Update service returned HTTP ${connection.responseCode}.",
                )
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return runCatching { JSONArray(body) }.getOrElse {
                throw UpdateFormatException("Invalid response from the update service.")
            }
        } finally {
            connection?.disconnect()
        }
    }
}

/** Update notification, mirroring DownList's `showUpdateNotification` channel. */
object UpdateNotifications {

    private const val CHANNEL_ID = "ghost_play_updates"
    private const val CHANNEL_NAME = "App updates"
    private const val NOTIFICATION_ID = 9351

    fun show(context: Context, version: String, url: String, compatible: Boolean): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" ||
            uri.host != "github.com" ||
            !(uri.path?.startsWith("/$UPDATES_REPO_OWNER/$UPDATES_REPO_NAME/releases/") == true)
        ) {
            return false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) {
            return false
        }
        val launch = Intent(Intent.ACTION_VIEW, uri)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = version.take(80)
        val text = if (compatible) {
            "Ghost Play $label is available."
        } else {
            "Ghost Play $label is available; reinstall may be required."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Ghost Play update available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
}
