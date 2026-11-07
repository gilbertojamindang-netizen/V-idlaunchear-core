/*
 * VøidLaunchear announcement and update notice.
 * Retains upstream GPL-3.0 licensing.
 */
package com.movtery.zalithlauncher.ui.screens.main

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    val latestVersion: String,
    val downloadUrl: String
)

@Composable
internal fun AnnouncementUpdateDialog() {
    val context = LocalContext.current
    var announcement by remember { mutableStateOf<LauncherAnnouncement?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var downloadedApk by remember { mutableStateOf<Uri?>(null) }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { fetchAnnouncement(context) }
        if (result != null) {
            val prefs = context.getSharedPreferences(ANNOUNCEMENT_PREFS, Context.MODE_PRIVATE)
            if (prefs.getString(LAST_SHOWN_ID, null) != result.id) {
                announcement = result
            }
        }
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadedApk = manager.getUriForDownloadedFile(id)
                            downloadStatus = if (downloadedApk != null) "Download complete. Tap Install Update." else null
                            if (downloadedApk == null) downloadError = "Downloaded APK could not be opened."
                            downloadId = null
                            return@LaunchedEffect
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            downloadError = "Download failed (code " + reason + "). Please try again."
                            downloadStatus = null
                            downloadId = null
                            return@LaunchedEffect
                        }
                        else -> {
                            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            downloadStatus = if (total > 0L) "Downloading… " + (done * 100 / total) + "%" else "Downloading…"
                        }
                    }
                } else {
                    downloadError = "Download task not found. Please try again."
                    downloadId = null
                    return@LaunchedEffect
                }
            }
            delay(500)
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
                    downloadStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    downloadError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
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
                        val apkUri = downloadedApk
                        if (apkUri != null) {
                            runCatching {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(apkUri, "application/vnd.android.package-archive")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }.onFailure { downloadError = "Unable to open Android installer: " + (it.message ?: "unknown error") }
                        } else if (isUpdateAvailable && downloadId == null && item.downloadUrl.startsWith("https://")) {
                            runCatching {
                                val request = DownloadManager.Request(Uri.parse(item.downloadUrl))
                                    .setTitle("VøidLaunchear " + item.latestVersion)
                                    .setDescription("Downloading launcher update")
                                    .setMimeType("application/vnd.android.package-archive")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setAllowedOverMetered(true)
                                    .setAllowedOverRoaming(false)
                                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "VoidLaunchear-" + item.latestVersion + "-update.apk")
                                downloadId = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                                downloadError = null
                                downloadStatus = "Preparing download…"
                            }.onFailure { downloadError = "Could not start download: " + (it.message ?: "unknown error") }
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(item.releasePage))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure { downloadError = "Unable to open release notes: " + (it.message ?: "unknown error") }
                        }
                    },
                    enabled = downloadId == null
                ) {
                    Text(when {
                        downloadedApk != null -> "Install Update"
                        downloadId != null -> "Downloading…"
                        isUpdateAvailable -> "Download Update"
                        else -> "View Release Notes"
                    })
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

    val feed = if (fresh != null) {
        parseAnnouncement(fresh)?.also {
            prefs.edit().putString(CACHED_FEED, fresh).apply()
        }
    } else {
        prefs.getString(CACHED_FEED, null)?.let(::parseAnnouncement)
    }

    // The published GitHub release is authoritative. Never show an old UPDATE
    // announcement again after the installed app has reached that release.
    val latestRelease = fetchLatestRelease()
    if (latestRelease != null) {
        val latestVersion = latestRelease.first
        val downloadUrl = latestRelease.second
        if (isNewerVersion(latestVersion, BuildConfig.VERSION_NAME)) {
            return LauncherAnnouncement(
                id = "release-$latestVersion",
                title = "VøidLaunchear $latestVersion is available",
                message = "A newer VøidLaunchear release is available. Download it here, then tap Install Update.",
                category = "UPDATE",
                releasePage = RELEASES_URL,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl
            )
        }

        // Keep maintenance and informational notices, but suppress release notices
        // whose version is already installed (for example 1.0.12 on 1.0.12).
        if (feed != null && feed.category != "UPDATE") {
            return feed.copy(
                releasePage = RELEASES_URL,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl
            )
        }
        return null
    }
    // Offline fallback: a cached UPDATE must still be checked against the installed
    // version, so it cannot repeatedly prompt for the same or an older release.
    if (feed?.category == "UPDATE" &&
        !isNewerVersion(feed.latestVersion, BuildConfig.VERSION_NAME)
    ) return null
    return feed
}

private fun fetchLatestRelease(): Pair<String, String>? {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL("https://api.github.com/repos/gilbertojamindang-netizen/V-idlaunchear-core/releases/latest")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "VoidLaunchear-Android/${BuildConfig.VERSION_NAME}")
        }
        if (connection.responseCode !in 200..299) return null
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (body.toByteArray(Charsets.UTF_8).size > MAX_FEED_BYTES) return null
        val release = JSONObject(body)
        if (release.optBoolean("draft", true) || release.optBoolean("prerelease", true)) return null
        val version = release.optString("tag_name").removePrefix("v").take(32)
        if (version.isBlank()) return null
        val assets = release.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length())
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull {
                val name = it.optString("name")
                name.endsWith(".apk", ignoreCase = true) &&
                    (name.contains("arm64", ignoreCase = true) || name.contains("universal", ignoreCase = true))
            } ?: return null
        val uri = Uri.parse(apk.optString("browser_download_url"))
        if (uri.scheme != "https" || uri.host != "github.com") return null
        version to uri.toString()
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
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
        val candidateDownloadUri = Uri.parse(app.optString("downloadUrl"))
        val downloadUrl = if (candidateDownloadUri.scheme == "https" && candidateDownloadUri.host == "github.com") candidateDownloadUri.toString().take(2048) else ""
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
                    latestVersion = latestVersion,
                    downloadUrl = downloadUrl
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
                    latestVersion = latestVersion,
                    downloadUrl = downloadUrl
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
