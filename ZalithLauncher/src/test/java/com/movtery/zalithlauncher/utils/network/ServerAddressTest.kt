/*
 * VøidLaunchear
 * Licensed under the GNU General Public License, version 3 or later.
 */

package com.movtery.zalithlauncher.utils.network

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ServerAddressTest {
    @Test
    fun parsesHostnameWithDefaultPort() {
        assertEquals(ServerAddress("play.example.net", 25565), ServerAddress.parse("play.example.net"))
    }

    @Test
    fun trimsOuterWhitespace() {
        assertEquals(ServerAddress("play.example.net", 25565), ServerAddress.parse("  play.example.net  "))
    }

    @Test
    fun parsesHostnameAndExplicitPort() {
        assertEquals(ServerAddress("play.example.net", 25566), ServerAddress.parse("play.example.net:25566"))
    }

    @Test
    fun parsesBracketedIpv6WithPort() {
        assertEquals(ServerAddress("2001:db8::1", 25570), ServerAddress.parse("[2001:db8::1]:25570"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyHostWithPort() {
        ServerAddress.parse(":25565")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyBracketedIpv6Host() {
        ServerAddress.parse("[]:25565")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWhitespaceInsideAddress() {
        ServerAddress.parse("play example.net")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPortZero() {
        ServerAddress.parse("play.example.net:0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPortAboveRange() {
        ServerAddress.parse("play.example.net:65536")
    }
}
