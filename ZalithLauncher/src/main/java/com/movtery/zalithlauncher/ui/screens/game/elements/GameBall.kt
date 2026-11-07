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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.enums.FpsDisplayMode
import com.movtery.zalithlauncher.utils.device.VoidFpsMonitor
import com.movtery.zalithlauncher.ui.components.FloatingBall
import com.movtery.zalithlauncher.ui.screens.content.elements.MemoryPreview

private val VoidAccent = Color(0xFFB56CFF)
private val VoidMuted = Color(0xFFB9A9CC)

@Composable
fun DraggableGameBall(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    onSavePos: () -> Unit,
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
    alpha: Float = 1f,
    onClick: () -> Unit = {}
) {
    FloatingBall(
        modifier = Modifier.focusProperties { canFocus = false },
        position = position,
        onPositionChanged = onPositionChanged,
        onSavePos = onSavePos,
        onClick = onClick,
        alpha = alpha,
        color = Color(0xF0120D1D),
        contentColor = Color(0xFFF5EEFF),
        shape = RoundedCornerShape(14.dp)
    ) {
        GameBallContent(
            gameFps = gameFps,
            fpsDisplayMode = fpsDisplayMode,
            fpsHistory = fpsHistory,
            fpsMax = fpsMax,
            fpsMin = fpsMin,
            showMemory = showMemory,
            opened = opened,
        )
    }
}

@Composable
private fun GameBallContent(
    gameFps: () -> Int?,
    fpsDisplayMode: FpsDisplayMode,
    fpsHistory: () -> List<Int>,
    fpsMax: () -> Int,
    fpsMin: () -> Int,
    showMemory: Boolean,
    opened: Boolean,
) {
    val fps = gameFps()
    val history = fpsHistory()
    val fpsMaxValue = fpsMax()
    val fpsMinValue = fpsMin()
    val stutterDropDetected = remember(history) {
        VoidFpsMonitor.hasSevereRecentDrop(history)
    }
    val showFps = fps != null

    Column(
        modifier = Modifier
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoidAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(opened) { state ->
                    Icon(
                        modifier = Modifier.size(17.dp),
                        painter = painterResource(
                            if (state) R.drawable.ic_menu_open else R.drawable.ic_menu
                        ),
                        contentDescription = null,
                        tint = VoidAccent
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "VØID STATUS",
                    color = Color(0xFFF5EEFF),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF69E6A6))
                    )
                    Text(
                        text = "GAME RUNNING",
                        color = VoidMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (showFps) {
                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(VoidAccent.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    text = "${fps} FPS",
                    color = VoidAccent,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        CustomAnimatedVisibility(visible = showFps && stutterDropDetected) {
            Text(
                text = "FPS DROP DETECTED",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        CustomAnimatedVisibility(
            visible = showFps && fpsDisplayMode == FpsDisplayMode.CHART
        ) {
            FpsChart(
                modifier = Modifier,
                history = history,
                fpsMax = fpsMaxValue,
                fpsMin = fpsMinValue
            )
        }

        CustomAnimatedVisibility(visible = showMemory) {
            MemoryPreview(
                modifier = Modifier
                    .defaultMinSize(minWidth = 132.dp)
                    .fillMaxWidth(),
                mainColor = VoidAccent.copy(alpha = 0.8f),
                backgroundColor = Color(0xFF332443),
                textStyle = MaterialTheme.typography.labelSmall,
                usedText = { usedMemory, totalMemory ->
                    "RAM  ${usedMemory.toInt()}MB / ${totalMemory.toInt()}MB"
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.CustomAnimatedVisibility(
    visible: Boolean,
    content: @Composable (AnimatedVisibilityScope.() -> Unit)
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandIn(expandFrom = Alignment.CenterStart) + fadeIn(),
        exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
        content = content
    )
}
