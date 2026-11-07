/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.versioninfo

import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.game.addons.mirror.mapBMCLMirrorUrls
import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest
import com.movtery.zalithlauncher.game.versioninfo.models.mapVersion
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.URL_MINECRAFT_VERSION_REPOS
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.fetchStringFromUrls
import com.movtery.zalithlauncher.utils.network.withRetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

private const val TAG = "MinecraftVersions"

object MinecraftVersions {
    private var manifest: VersionManifest? = null

    private val _allVersions = MutableStateFlow<List<MinecraftVersion>>(emptyList())
    val allVersions = _allVersions.asStateFlow()

    /**
     * 刷新Minecraft版本的版本号列表
     * @param force 强制下载更新版本列表
     */
    @Throws(IllegalStateException::class)
    suspend fun refreshVersions(force: Boolean = false) {
        if (!force && _allVersions.value.isNotEmpty()) return

        val vm = if (force) {
            getVersionManifest(force = true)
        } else {
            manifest ?: getVersionManifest(force = false)
        }
        val versions = vm.versions.mapVersion()
        _allVersions.update { versions }
    }

    /**
     * 获取Minecraft版本信息列表
     * @param force 强制下载更新版本列表
     */
    @Throws(IllegalStateException::class)
    suspend fun getVersionManifest(force: Boolean = false): VersionManifest {
        manifest?.takeIf { !force }?.let { return it }

        return withContext(Dispatchers.IO) {
            val localManifestFile = PathManager.FILE_MINECRAFT_VERSIONS
            val isOutdated = !localManifestFile.exists() || !localManifestFile.isFile ||
                    //一天更新一次版本信息列表
                    localManifestFile.lastModified() + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()

            val newManifest = if (force || isOutdated) {
                try {
                    downloadVersionManifest()
                } catch (e: CancellationException) {
                    throw e
                } catch (networkError: Exception) {
                    // Keep the last known-good catalog available when Mojang or the
                    // network is temporarily unreachable. This prevents an otherwise
                    // usable launcher from showing an empty version list.
                    if (!localManifestFile.isFile) throw networkError
                    try {
                        Logger.warning(TAG, "Version manifest refresh failed; using cached manifest", networkError)
                        parseManifest(localManifestFile.readText())
                    } catch (cacheError: Exception) {
                        cacheError.addSuppressed(networkError)
                        Logger.warning(TAG, "Cached version manifest is invalid", cacheError)
                        throw cacheError
                    }
                }
            } else {
                try {
                    parseManifest(localManifestFile.readText())
                } catch (e: Exception) {
                    Logger.warning(TAG, "Failed to parse version manifest, will redownload", e)
                    //读取失败则删除当前的版本信息文件
                    FileUtils.deleteQuietly(localManifestFile)
                    downloadVersionManifest()
                }
            }

            val newManifest0 = newManifest ?: throw IllegalStateException("Version manifest is null after all attempts")
            mergeUnlistVersions(newManifest0) ?: newManifest0
        }.also { newManifest ->
            manifest = newManifest
        }
    }

    /**
     * Parse and minimally validate a manifest before it is cached or exposed.
     * A syntactically valid but empty/truncated catalog must not replace the
     * last-known-good manifest on disk.
     */
    private fun parseManifest(rawJson: String): VersionManifest {
        val parsed = GSON.fromJson(rawJson, VersionManifest::class.java)
            ?: throw IllegalStateException("Minecraft version manifest is empty")
        require(parsed.latest.release.isNotBlank()) { "Manifest latest release is missing" }
        require(parsed.latest.snapshot.isNotBlank()) { "Manifest latest snapshot is missing" }
        require(parsed.versions.isNotEmpty()) { "Minecraft version catalog is empty" }
        require(parsed.versions.all { it.id.isNotBlank() && it.url.isNotBlank() }) {
            "Minecraft version catalog contains an invalid entry"
        }
        return parsed
    }

    /**
     * 从官方版本仓库获取版本信息
     */
    private suspend fun downloadVersionManifest(): VersionManifest {
        return withContext(Dispatchers.IO) {
            withRetry("MinecraftVersions", maxRetries = 2) {
                val rawJson = fetchStringFromUrls(URL_MINECRAFT_VERSION_REPOS.mapBMCLMirrorUrls())
                val versionManifest = parseManifest(rawJson)
                val manifestFile = PathManager.FILE_MINECRAFT_VERSIONS
                val parent = manifestFile.toPath().toAbsolutePath().parent
                Files.createDirectories(parent)
                val temporaryFile = Files.createTempFile(parent, "minecraft-versions-", ".json.tmp")
                try {
                    temporaryFile.toFile().writeText(rawJson, Charsets.UTF_8)
                    try {
                        Files.move(
                            temporaryFile,
                            manifestFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        )
                    } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                        Files.move(
                            temporaryFile,
                            manifestFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                } finally {
                    Files.deleteIfExists(temporaryFile)
                }
                versionManifest
            }
        }
    }

    /**
     * 尝试从本地合并官方隐藏的版本
     */
    private suspend fun mergeUnlistVersions(
        currentManifest: VersionManifest
    ): VersionManifest? {
        return withContext(Dispatchers.IO) {
            MinecraftVersions::class.java.getResourceAsStream("/assets/game/unlist_versions.json")?.use { input ->
                input.readString()
            }?.let { unlistVersionJson ->
                GSON.fromJson<List<VersionManifest.Version>>(
                    unlistVersionJson,
                    object : TypeToken<List<VersionManifest.Version>>() {}.type
                )
            }?.let { unlistVersions ->
                // Merge by version ID so bundled hidden entries cannot create
                // duplicate picker rows when Mojang later publishes the same ID.
                // Prefer the live manifest whenever an ID exists in both sources.
                val versionsById = LinkedHashMap<String, VersionManifest.Version>()
                currentManifest.versions.forEach { version ->
                    versionsById[version.id] = version
                }
                unlistVersions.forEach { version ->
                    versionsById.putIfAbsent(version.id, version)
                }

                val versions = versionsById.values.sortedWith { version, other ->
                    other.releaseTime.compareTo(version.releaseTime)
                }

                currentManifest.copy(
                    versions = versions
                )
            }
        }
    }
}