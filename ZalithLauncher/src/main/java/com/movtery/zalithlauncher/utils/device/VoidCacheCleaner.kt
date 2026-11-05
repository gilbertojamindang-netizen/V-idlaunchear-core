/*
 * VøidLaunchear safe cache cleaner.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 *
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.utils.device

import com.movtery.zalithlauncher.path.PathManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * Removes only disposable launcher presentation caches.
 *
 * Download staging folders, modpacks, worlds, saves, accounts, Java runtimes,
 * and Minecraft libraries are deliberately excluded.
 */
object VoidCacheCleaner {
    data class CleanupResult(
        val removedBytes: Long,
        val clearedDirectories: Int
    )

    private fun targets(): List<File> = listOf(
        PathManager.DIR_CACHE_APP_ICON,
        PathManager.DIR_CACHE_HOME_PAGE,
        PathManager.DIR_IMAGE_CACHE
    )

    fun previewBytes(): Long = targets().sumOf { directory ->
        if (directory.isDirectory) {
            runCatching { FileUtils.sizeOfDirectory(directory) }.getOrDefault(0L)
        } else {
            0L
        }
    }

    suspend fun cleanKnownCaches(): CleanupResult = withContext(Dispatchers.IO) {
        var removedBytes = 0L
        var cleared = 0
        val failures = mutableListOf<String>()

        targets().forEach { directory ->
            if (directory.exists() && directory.isDirectory) {
                val size = runCatching { FileUtils.sizeOfDirectory(directory) }.getOrDefault(0L)
                try {
                    FileUtils.cleanDirectory(directory)
                    removedBytes += size
                    cleared++
                } catch (error: IOException) {
                    failures.add("${directory.name}: ${error.message ?: "unable to clear"}")
                } catch (error: SecurityException) {
                    failures.add("${directory.name}: permission denied")
                } catch (error: Exception) {
                    failures.add("${directory.name}: ${error.message ?: "unable to clear"}")
                }
            }
        }

        if (failures.isNotEmpty()) {
            throw IOException(
                "Some cache folders could not be cleared: ${failures.joinToString()}. " +
                    "$removedBytes bytes were cleared from $cleared folders."
            )
        }

        CleanupResult(
            removedBytes = removedBytes,
            clearedDirectories = cleared
        )
    }
}
