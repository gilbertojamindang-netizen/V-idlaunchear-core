/*
 * VøidLaunchear announcement and update notice.
 * Retains upstream GPL-3.0 licensing.
 */
package com.movtery.zalithlauncher.ui.screens.main

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.movtery.zalithlauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ANNOUNCEMENT_FEED_URL =
    "https://raw.githubusercontent.com/gilbertojamindang-netizen/V-idlaunchear-core/main/updates/announcement-feed.json"
private const val ANNOUNCEMENT_PREFS = "void_announcement_state"
private const val LAST_SHOWN_ID = "last_shown_announcement"

private data class LauncherAnnouncement(
    val id: String,
    val title: String,
    val message: String,
    val category: String,
    val releasePage: String,
    val latestVersion: String,
    val maintenance: Boolean = false
)

@Composable
internal fun AnnouncementUpdateDialog() {
    val context = LocalContext.current
    var announcement by remember { mutableStateOf<LauncherAnnouncement?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { fetchAnnouncement() }
        if (result != null) {
            val prefs = context.getSharedPreferences(ANNOUNCEMENT_PREFS, 0)
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
                context.getSharedPreferences(ANNOUNCEMENT_PREFS, 0)
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
                        context.getSharedPreferences(ANNOUNCEMENT_PREFS, 0)
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
                        context.getSharedPreferences(ANNOUNCEMENT_PREFS, 0)
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

private fun fetchAnnouncement(): LauncherAnnouncement? {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL(ANNOUNCEMENT_FEED_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VoidLaunchear-Android/${BuildConfig.VERSION_NAME}")
        }
        if (connection.responseCode !in 200..299) return null
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(body)
        val app = root.optJSONObject("app") ?: return null
        if (app.optString("name") != "VøidLaunchear") return null
        val releasePage = app.optString(
            "releasePage",
            "https://github.com/gilbertojamindang-netizen/V-idlaunchear-core/releases"
        )
        val latestVersion = app.optString("latestVersion")
        val maintenance = root.optJSONObject("maintenance")
        if (maintenance?.optBoolean("active", false) == true) {
            return LauncherAnnouncement(
                id = "maintenance-${maintenance.optString("title").hashCode()}",
                title = maintenance.optString("title", "Scheduled Maintenance"),
                message = maintenance.optString("message"),
                category = "MAINTENANCE",
                releasePage = releasePage,
                latestVersion = latestVersion,
                maintenance = true
            )
        }
        val items = root.optJSONArray("announcements") ?: return null
        (0 until items.length())
            .mapNotNull { items.optJSONObject(it) }
            .filter { it.optBoolean("active", false) }
            .maxByOrNull { it.optString("publishedAt") }
            ?.let {
                LauncherAnnouncement(
                    id = it.optString("id").takeIf(String::isNotBlank) ?: "announcement-${it.optString("publishedAt")}",
                    title = it.optString("title", "VøidLaunchear Update"),
                    message = it.optString("message"),
                    category = it.optString("category", "UPDATE"),
                    releasePage = releasePage,
                    latestVersion = latestVersion
                )
            }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

private fun isNewerVersion(latest: String, installed: String): Boolean {
    fun parts(value: String) = Regex("\\d+").findAll(value).mapNotNull { it.value.toIntOrNull() }.toList()
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
