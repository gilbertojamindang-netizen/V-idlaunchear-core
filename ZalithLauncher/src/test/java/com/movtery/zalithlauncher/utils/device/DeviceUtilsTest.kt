/*
 * VØID device capability helper tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later.
 */
package com.movtery.zalithlauncher.utils.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceUtilsTest {
    @Test
    fun decodesVulkanApiVersionFields() {
        assertEquals("1.1.0", formatVulkanApiVersion(0x00401000))
        assertEquals("1.2.3", formatVulkanApiVersion(0x00402003))
    }

    @Test
    fun rejectsMissingOrInvalidVulkanVersion() {
        assertNull(formatVulkanApiVersion(0))
        assertNull(formatVulkanApiVersion(-1))
        assertNull(formatVulkanApiVersion(0x00001000))
    }
}
