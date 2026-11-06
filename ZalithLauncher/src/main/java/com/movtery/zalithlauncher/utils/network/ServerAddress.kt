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

package com.movtery.zalithlauncher.utils.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.net.IDN
import java.util.Objects

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/e0805fc/HMCLCore/src/main/java/org/jackhuang/hmcl/util/ServerAddress.java)
 */
@Parcelize
class ServerAddress(
    val host: String,
    val port: Int = DEFAULT_PORT
) : Parcelable {
    /**
     * 尝试获取为国际化域名（IDN）
     */
    fun getASCIIHost(default: String = host): String {
        return try {
            IDN.toASCII(host)
        } catch (_: IllegalArgumentException) {
            default
        }
    }

    companion object {
        const val DEFAULT_PORT = 25565
        private val PORT_RANGE = 1..65535

        fun parse(address: String): ServerAddress {
            val normalized = address.trim()
            require(normalized.isNotEmpty()) { "Address cannot be empty" }
            require(normalized.none(Char::isWhitespace)) { "Server address cannot contain whitespace" }

            return when {
                // IPv6 literals must be bracketed when a port is supplied.
                normalized.startsWith('[') -> parseIPv6(normalized)
                ':' in normalized -> parseWithPort(normalized)
                else -> ServerAddress(normalized.also(::requireValidHost), DEFAULT_PORT)
            }
        }

        private fun requireValidHost(host: String) {
            require(host.isNotBlank()) { "Server host cannot be empty" }
        }
        
        private fun parseIPv6(address: String): ServerAddress {
            val closeBracketIndex = address.indexOf(']').takeIf { it != -1 }
                ?: throw illegalAddress(address)
            
            val host = address.substring(1, closeBracketIndex)
            requireValidHost(host)

            return when (val remaining = address.substring(closeBracketIndex + 1)) {
                "" -> ServerAddress(host, DEFAULT_PORT)
                else -> {
                    require(remaining.startsWith(':')) { "Expected colon after IPv6 address" }
                    val portPart = remaining.substring(1)
                    parsePort(host, portPart)
                }
            }
        }
        
        private fun parseWithPort(address: String): ServerAddress {
            val colonPos = address.indexOf(':')
            val hostPart = address.take(colonPos)
            val portPart = address.substring(colonPos + 1)
            requireValidHost(hostPart)
            return parsePort(hostPart, portPart)
        }
        
        private fun parsePort(host: String, portPart: String): ServerAddress {
            requireValidHost(host)
            val port = portPart.toIntOrNull()
                ?.takeIf { it in PORT_RANGE }
                ?: throw illegalAddress("$host:$portPart")
            
            return ServerAddress(host, port)
        }
        
        private fun illegalAddress(address: String): IllegalArgumentException = 
            IllegalArgumentException("Invalid server address: $address")
    }
    
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is ServerAddress -> false
        else -> port == other.port && host == other.host
    }
    
    override fun hashCode(): Int = Objects.hash(host, port)
    
    override fun toString(): String = "ServerAddress[host='$host', port=$port]"
}