/*
 * VøidLaunchear
 * Licensed under the GNU General Public License, version 3 or later.
 */

package com.movtery.zalithlauncher.utils.network

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WithRetryTest {
    @Test
    fun retriesTransientIoFailureAndReturnsSuccess() = runBlocking {
        val attempts = AtomicInteger(0)

        val result = withRetry(
            logTag = "unit-test",
            maxRetries = 3,
            initialDelay = 0,
            maxDelay = 0
        ) {
            if (attempts.incrementAndGet() < 3) {
                throw IOException("temporary network failure")
            }
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(3, attempts.get())
    }

    @Test
    fun doesNotRetryNonNetworkFailure() = runBlocking {
        val attempts = AtomicInteger(0)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                withRetry(
                    logTag = "unit-test",
                    maxRetries = 3,
                    initialDelay = 0,
                    maxDelay = 0
                ) {
                    attempts.incrementAndGet()
                    throw IllegalArgumentException("invalid request")
                }
            }
        }

        assertEquals(1, attempts.get())
    }

    @Test
    fun retryDelayDoublesAndStopsAtMaximumWithoutOverflow() {
        assertEquals(2L, nextRetryDelay(1L, 10L))
        assertEquals(10L, nextRetryDelay(8L, 10L))
        assertEquals(Long.MAX_VALUE, nextRetryDelay(Long.MAX_VALUE / 2 + 1L, Long.MAX_VALUE))
        assertEquals(0L, nextRetryDelay(0L, 0L))
    }

    @Test
    fun cancellationIsNeverRetried() = runBlocking {
        val attempts = AtomicInteger(0)

        assertThrows(CancellationException::class.java) {
            runBlocking {
                withRetry(
                    logTag = "unit-test",
                    maxRetries = 3,
                    initialDelay = 0,
                    maxDelay = 0
                ) {
                    attempts.incrementAndGet()
                    throw CancellationException("cancelled")
                }
            }
        }

        assertEquals(1, attempts.get())
    }
}
