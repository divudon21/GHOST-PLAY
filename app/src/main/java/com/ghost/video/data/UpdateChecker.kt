package com.ghost.video.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Info about the latest release found on GitHub. */
data class ReleaseInfo(
    val tag: String,
    val name: String,
    val notes: String,
    val htmlUrl: String,
    val publishedAt: String
)

/**
 * Checks the GHOST-PLAY GitHub repository for new releases.
 *
 * Uses the public GitHub REST API (no auth needed for public repos). Network work
 * runs on [Dispatchers.IO]. Any error returns null so callers can fail quietly.
 */
object UpdateChecker {

    private const val OWNER = "divudon21"
    private const val REPO = "GHOST-PLAY"
    private const val API = "https://api.github.com/repos/$OWNER/$REPO/releases"

    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "GhostPlay-App")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null

            // First non-draft release (the API returns newest first).
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optBoolean("draft", false)) continue
                val tag = obj.optString("tag_name").ifEmpty { obj.optString("name") }
                if (tag.isEmpty()) continue
                return@withContext ReleaseInfo(
                    tag = tag,
                    name = obj.optString("name").ifEmpty { tag },
                    notes = obj.optString("body"),
                    htmlUrl = obj.optString("html_url"),
                    publishedAt = obj.optString("published_at")
                )
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
