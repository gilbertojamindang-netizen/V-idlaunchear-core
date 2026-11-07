/*
 * VøidLaunchear
 * Copyright (C) 2026 Gilbert Jamindang
 *
 * This file is part of VøidLaunchear and is distributed under the
 * GNU General Public License, version 3 or later.
 */

package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VulkanCapabilitiesTest {
    private fun capabilities(major: Int, minor: Int) = VulkanCapabilities(
        apiVersionMajor = major,
        apiVersionMinor = minor,
        apiVersionPatch = 0,
        extensions = emptyList(),
        features = emptyMap()
    )

    @Test
    fun vulkan11IsZinkCandidateButNotNativeMinecraftBaseline() {
        val caps = capabilities(1, 1)

        assertTrue(caps.isZinkVulkanCandidate)
        assertFalse(caps.isVersionSupported)
    }

    @Test
    fun vulkan12MeetsBothVersionBaselines() {
        val caps = capabilities(1, 2)

        assertTrue(caps.isZinkVulkanCandidate)
        assertTrue(caps.isVersionSupported)
    }

    @Test
    fun vulkan10MeetsNeitherVersionBaseline() {
        val caps = capabilities(1, 0)

        assertFalse(caps.isZinkVulkanCandidate)
        assertFalse(caps.isVersionSupported)
    }

    @Test
    fun newerMajorVersionMeetsBothVersionBaselines() {
        val caps = capabilities(2, 0)

        assertTrue(caps.isZinkVulkanCandidate)
        assertTrue(caps.isVersionSupported)
    }
}
