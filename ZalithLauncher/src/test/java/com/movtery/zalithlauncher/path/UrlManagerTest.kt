/*
 * VøidLaunchear network identity regression tests.
 * Copyright (C) 2026 Gilbert Jamindang and contributors.
 * Licensed under GPL-3.0-or-later, consistent with the upstream project.
 */
package com.movtery.zalithlauncher.path

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlManagerTest {
    @Test
    fun userAgentContainsOnlyAsciiHeaderCharacters() {
        assertTrue(URL_USER_AGENT.all { it.code in 0x20..0x7e })
    }

    @Test
    fun userAgentUsesStableAsciiLauncherIdentifier() {
        assertTrue(URL_USER_AGENT.startsWith("VoidLaunchear/Android_"))
        assertFalse(URL_USER_AGENT.contains("ø", ignoreCase = true))
    }
}
