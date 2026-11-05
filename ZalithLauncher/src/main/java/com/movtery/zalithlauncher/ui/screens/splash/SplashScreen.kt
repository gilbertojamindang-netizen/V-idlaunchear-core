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

package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.legal.AvailableAgreement
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay

/**
 * @param startAllTask 开启全部的解压任务
 * @param onWelcomeFinished 完成欢迎页
 * @param availableAgreement 完整的可用协议数据
 * @param unpackItems 解压任务列表
 */
@Composable
fun SplashScreen(
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel
) {
    NavStateHost(rootKey = screenViewModel.splashScreen) {
        NavigationUI(
            modifier = Modifier.fillMaxSize(),
            startAllTask = startAllTask,
            onWelcomeFinished = onWelcomeFinished,
            availableAgreement = availableAgreement,
            unpackItems = unpackItems,
            screenViewModel = screenViewModel
        )
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel
) {
    val backStack = screenViewModel.splashScreen.backStack

    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        screenViewModel.splashScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = screenViewModel.splashScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {},
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<SplashOperation.Loading> {
                    LoadingScreen(screenViewModel = screenViewModel)
                }
                entry<SplashOperation.Welcome> {
                    WelcomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onFinished = onWelcomeFinished,
                        availableAgreement = availableAgreement,
                        screenViewModel = screenViewModel
                    )
                }
                entry<SplashOperation.Unpacking> {
                    UnpackScreen(
                        items = unpackItems,
                        screenViewModel = screenViewModel,
                        onAgreeClick = startAllTask
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun LoadingScreen(
    screenViewModel: SplashBackStackViewModel
) {
    BaseScreen(
        screenKey = SplashOperation.Loading,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)
        val logoProgress = remember { Animatable(0f) }
        LaunchedEffect(isVisible) {
            if (isVisible) {
                logoProgress.snapTo(0f)
                delay(180)
                logoProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = keyframes {
                        durationMillis = 1080
                        0f at 0
                        1.10f at 720
                        0.96f at 875
                        1f at 1080
                    }
                )
            }
        }

        val collisionGlow = (1f - kotlin.math.abs(1f - logoProgress.value) * 5f)
            .coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF07040D),
                            Color(0xFF160A25),
                            Color(0xFF080510)
                        )
                    )
                )
                .animated(ctx, from = AnimatedDirection.Bottom),
            contentAlignment = Alignment.Center
        ) {
            // Soft central glow; all elements are vector/text based and scale with display density.
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8C35D6).copy(alpha = 0.22f + collisionGlow * 0.38f),
                                Color(0xFF542080).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.offset(
                            x = (-76f * (1f - logoProgress.value)).dp
                        ),
                        text = "VØID",
                        color = Color(0xFFE0BDFF),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        modifier = Modifier.offset(
                            x = (76f * (1f - logoProgress.value)).dp
                        ),
                        text = "LAUNCHER",
                        color = Color(0xFFFFF8FF),
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .width(230.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFB66CFF).copy(alpha = 0.9f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Text(
                    text = "MINECRAFT JAVA  •  VØID CORE",
                    color = Color(0xFFBFA6D5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                LinearWavyProgressIndicator(
                    modifier = Modifier.width(164.dp),
                    color = Color(0xFFB66CFF),
                    trackColor = Color(0xFF38234A),
                    wavelength = 36.dp
                )

                Text(
                    text = "INITIALIZING LAUNCHER",
                    color = Color(0xFF8F7B9F),
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp
                )
            }
        }
    }
}