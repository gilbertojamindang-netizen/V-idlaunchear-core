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

package com.movtery.zalithlauncher.utils.device

import android.content.pm.PackageManager

private const val VULKAN_API_VERSION_1_1_0 = 0x00401000

/** Return Android's declared highest Vulkan API version, or null when absent. */
fun getVulkanApiVersion(packageManager: PackageManager): Int? {
    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)) {
        return null
    }
    return packageManager.systemAvailableFeatures
        .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
        ?.version
        ?.takeIf { it > 0 }
}

/** Format the Vulkan version fields using the Vulkan 10/10/12-bit encoding. */
internal fun formatVulkanApiVersion(encodedVersion: Int): String? {
    if (encodedVersion <= 0) return null
    val major = (encodedVersion ushr 22) and 0x3FF
    val minor = (encodedVersion ushr 12) and 0x3FF
    val patch = encodedVersion and 0xFFF
    if (major == 0) return null
    return "$major.$minor.$patch"
}

/** Whether Android declares hardware-level Vulkan API 1.1.0 or newer. */
fun checkVulkan11Support(packageManager: PackageManager): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
        packageManager.hasSystemFeature(
            PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
            VULKAN_API_VERSION_1_1_0
        )

/** Whether Android declares any hardware-level Vulkan API version. */
fun checkVulkanSupport(packageManager: PackageManager): Boolean =
    getVulkanApiVersion(packageManager) != null