/*
 * VøidLaunchear announcement and update notice.
 * Retains upstream GPL-3.0 licensing.
 */
package com.movtery.zalithlauncher.ui.screens.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val ANNOUNCEMENT_FEED_URL =
    "https://raw.githubusercontent.com/gilbertojamindang-netizen/V-idlaunchear-core/main/updates/announcement-feed.json"
private const val ANNOUNCEMENT_PREFS = "void_announcement_state"
private const val LAST_SHOWN_ID = "last_shown_announcement"
private const val CACHED_FEED = "cached_announcement_feed"
private const val MAX_FEED_BYTES = 64 * 1024
private const val RELEASES_URL =
    "https://github.com/gilbertojamindang-netizen/V-idlaunchear-core/releases"

private data class LauncherAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val category: String,
    val releasePage: String,
    val latestVersion: String
)

@Composable
internal fun AnnouncementUpdateDialog() {
    val context = LocalContext.current
    var announcement by remember { mutableStateOf<LauncherAnnouncement?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { fetchAnnouncement(context) }
        if (result != null) {
            val prefs = context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
            if (prefs.getString(LAST_SHOWN_ID, null) != result.id) {
                announcement = result
            }
        }
    }

    val item = announcement
    if (item != null && !dismissed) {
        val isUpdateAvailable = isNewerVersion(item.latestVersion, BuildConfig.VERSION_NAME)
        AlertDialog(
            onDismissRequest = {
                context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
                    .edit().putString(LAST_SHOWN_ID, item.id).apply()
                dismissed = true
            },
            title = {
                Column {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column {
                    Text(item.message, style = MaterialTheme.typography.bodyMedium)
                    if (item.latestVersion.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Installed: ${BuildConfig.VERSION_NAME}  •  Latest: ${item.latestVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(item.releasePage))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
                            .edit().putString(LAST_SHOWN_ID, item.id).apply()
                        dismissed = true
                    },
                    enabled = item.releasePage.startsWith("https://")
                ) {
                    Text(if (isUpdateAvailable) "Download Update" else "View Release Notes")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
                            .edit().putString(LAST_SHOWN_ID, item.id).apply()
                        dismissed = true
                    }
                ) {
                    Text("Later")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

/**
 * Uses the newest feed when online and the last validated feed when offline.
 * The cache is bounded so a malformed or unexpectedly large response is ignored.
 */
private fun fetchAnnouncement(context: Context): LauncherAnnouncement? {
    val prefs = context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
    var connection: HttpURLConnection? = null
    val fresh = try {
        connection = (URL(ANNOUNCEMENT_FEED_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VoidLaunchear-Android/${BuildConfig.VERSION_NAME}")
        }
        if (connection.responseCode !in 200..299) null
        else {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            connection.inputStream.use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_FEED_BYTES) {
                        throw IllegalStateException("Announcement feed exceeds size limit")
                    }
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name())
        }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }

    if (fresh != null) {
        val parsed = parseAnnouncement(fresh)
        if (parsed != null) {
            prefs.edit().putString(CACHED_FEED, fresh).apply()
            return parsed
        }
    }

    return prefs.getString(CACHED_FEED, null)?.let(::parseAnnouncement)
}

private fun parseAnnouncement(body: String): LauncherAnnouncement? {
    return try {
    if (body.toByteArray(Charsets.UTF_8).size > MAX_FEED_BYTES) return null
    val root = JSONObject(body)
    val app = root.optJSONObject("app") ?: return null
    if (app.optString("name") != "VøidLaunchear") return null

    val releaseUri = Uri.parse(app.optString("releasePage", RELEASES_URL))
    val releasePage = if (
        releaseUri.scheme == "https" && releaseUri.host == "github.com"
    ) releaseUri.toString() else RELEASES_URL
    val latestVersion = app.optString("latestVersion").take(32)
    val maintenance = root.optJSONObject("maintenance")

    if (maintenance?.optBoolean("active", false) == true) {
        val title = maintenance.optString("title").ifBlank { "Scheduled Maintenance" }
        val message = maintenance.optString("message").take(1200)
        if (message.isNotBlank()) {
            return LauncherAnnouncement(
                id = "maintenance-${title.hashCode()}-${message.hashCode()}",
                title = title.take(120),
                message = message,
                category = "MAINTENANCE",
                releasePage = releasePage,
                latestVersion = latestVersion
            )
        }
    }

    val items = root.optJSONArray("announcements") ?: return null
    (0 until items.length())
        .mapNotNull { items.optJSONObject(it) }
        .filter { it.optBoolean("active", false) }
        .maxByOrNull { it.optString("publishedAt") }
        ?.let { item ->
            val message = item.optString("message").take(1200)
            if (message.isBlank()) return@let null
            LauncherAnnouncement(
                id = item.optString("id")
                    .takeIf(String::isNotBlank)
                    ?.take(100)
                    ?: "announcement-${item.optString("publishedAt").hashCode()}",
                title = item.optString("title").ifBlank { "VøidLaunchear Update" }.take(120),
                message = message,
                category = item.optString("category").ifBlank { "UPDATE" }.take(32),
                releasePage = releasePage,
                latestVersion = latestVersion
            )
        }
    } catch (_: Exception) {
        null
    }
}

private fun isNewerVersion(latest: String, installed: String): Boolean {
    fun parts(value: String) =
        Regex("\\d+").findAll(value).mapNotNull { it.value.toIntOrNull() }.toList()

    val a = parts(latest)
    val b = parts(installed)
    if (a.isEmpty() || b.isEmpty()) return false
    for (index in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(index) { 0 }
        val right = b.getOrElse(index) { 0 }
        if (left != right) return left > right
    }
    return false
}
