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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.movtery.cardgrid.model.CardRect
import com.movtery.cardgrid.state.CardGridState
import com.movtery.cardgrid.state.CardSeed
import com.movtery.cardgrid.ui.CardGrid
import com.movtery.cardgrid.ui.CardGridAutoScroll
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardManager
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardSettingsHost
import com.movtery.zalithlauncher.ui.screens.content.home.version.VersionCardManager
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.device.VoidCoreCommandRouter
import com.movtery.zalithlauncher.utils.device.VoidPerformanceApplier
import com.movtery.zalithlauncher.utils.device.VoidPerformanceAdvisor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 主页网格的卡片操作 */
private sealed interface HomeCardOperation {
    data object None : HomeCardOperation
    /** 打开服务器卡片的设置对话框 */
    data class ServerCardSettings(val cardId: String) : HomeCardOperation
}

/**
 * 主页网格：系统卡片列 + 卡片网格库容器，
 * 负责布局的播种、持久化、卡片记录同步与服务器卡片的可见性上报。
 */
@Composable
fun HomeGrid(
    state: CardGridState,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val appContext = LocalContext.current
    val performancePreferences = remember(appContext) {
        appContext.getSharedPreferences("void_performance", android.content.Context.MODE_PRIVATE)
    }
    var selectedPerformanceProfile by remember(appContext) {
        mutableStateOf(
            runCatching {
                VoidPerformanceAdvisor.Profile.valueOf(
                    performancePreferences.getString("profile", null)
                        ?: VoidPerformanceAdvisor.recommend(appContext).profile.name
                )
            }.getOrDefault(VoidPerformanceAdvisor.recommend(appContext).profile)
        )
    }
    val safeCustomMemoryMb = remember(appContext) {
        VoidPerformanceAdvisor.recommend(
            appContext,
            VoidPerformanceAdvisor.Profile.CUSTOM
        ).maxMemoryMb
    }
    var customMemoryMb by remember(appContext) {
        mutableStateOf(
            performancePreferences.getInt("custom_memory", safeCustomMemoryMb)
                .coerceIn(512, safeCustomMemoryMb)
        )
    }
    var customRenderDistance by remember(appContext) {
        mutableStateOf(performancePreferences.getInt("custom_render", 8).coerceIn(2, 24))
    }
    var customSimulationDistance by remember(appContext) {
        mutableStateOf(performancePreferences.getInt("custom_simulation", 5).coerceIn(2, 16))
    }
    var customEntityDistance by remember(appContext) {
        mutableStateOf(performancePreferences.getInt("custom_entities", 100).coerceIn(25, 150))
    }
    val performanceAdvice = remember(
        appContext,
        selectedPerformanceProfile,
        customMemoryMb,
        customRenderDistance,
        customSimulationDistance,
        customEntityDistance
    ) {
        val base = VoidPerformanceAdvisor.recommend(appContext, selectedPerformanceProfile)
        if (selectedPerformanceProfile == VoidPerformanceAdvisor.Profile.CUSTOM) {
            base.copy(
                maxMemoryMb = customMemoryMb.coerceIn(512, base.maxMemoryMb),
                renderDistance = customRenderDistance.coerceIn(2, 24),
                simulationDistance = customSimulationDistance.coerceIn(2, 16),
                entityDistancePercent = customEntityDistance.coerceIn(25, 150),
                explanation = "Custom profile uses your saved values while keeping the memory recommendation within the device-safe limit."
            )
        } else {
            base
        }
    }
    //长按调整工具栏触发的卡片操作
    var operation by remember { mutableStateOf<HomeCardOperation>(HomeCardOperation.None) }
    var showVoidCore by remember { mutableStateOf(false) }
    var voidCoreInput by remember { mutableStateOf("") }
    var showPerformanceApply by remember { mutableStateOf(false) }
    var isApplyingPerformance by remember { mutableStateOf(false) }
    var performanceApplyMessage by remember { mutableStateOf<String?>(null) }
    val performanceScope = rememberCoroutineScope()
    val voidCorePreferences = remember(appContext) {
        appContext.getSharedPreferences("void_core", android.content.Context.MODE_PRIVATE)
    }
    val voidCoreHistory = remember(appContext) {
        mutableStateListOf<String>().apply {
            addAll(
                voidCorePreferences.getString("history", null)
                    .orEmpty()
                    .split("\n--VOID-CORE-ENTRY--\n")
                    .filter { it.isNotBlank() }
                    .takeLast(20)
            )
        }
    }

    // 播种持久化的用户卡片布局
    LaunchedEffect(Unit) {
        val snapshot = HomeGridStore.load()
        state.seed(
            types = HomeCards.userCardTypes,
            seeds = snapshot?.cards?.map { entry ->
                CardSeed(
                    id = entry.id,
                    typeId = entry.type,
                    layout = CardRect(
                        id = entry.id,
                        x = entry.x,
                        y = entry.y,
                        width = entry.width,
                        height = entry.height
                    )
                )
            } ?: emptyList(),
            storedColumns = snapshot?.columns ?: 0
        )
    }

    // 布局结算后持久化
    LaunchedEffect(Unit) {
        state.onLayoutCommitted = {
            HomeGridStore.save(
                cards = state.cards,
                columns = state.geometry.columns
            )
        }
    }

    // 卡片移除回调：同步各卡片管理器的记录
    LaunchedEffect(Unit) {
        state.onCardRemoved = { cardId ->
            VersionCardManager.removeCard(cardId)
            ServerCardManager.removeCard(cardId)
        }
    }

    // 与版本卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        VersionCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.versionCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 与服务器卡片记录保持同步：记录存在而网格缺卡时补齐
    LaunchedEffect(Unit) {
        ServerCardManager.cards.collect { states ->
            states.forEach { cardState ->
                if (state.cards.none { it.id == cardState.record.cardId }) {
                    state.addCard(HomeCards.serverCardType(), cardState.record.cardId)
                }
            }
        }
    }

    // 服务器卡片可见性：可见 = 屏幕可见 ∧ App 前台 ∧ 卡片矩形与滚动视口相交，
    // 新进入可见集合的卡片会触发一次静默刷新（15s 冷却由管理器约束）
    val isVisibleState = rememberUpdatedState(isVisible)
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            resumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            val top = state.viewportTopPx
            val bottom = top + state.viewportHeightPx
            if (!isVisibleState.value || !resumed || bottom <= 0f) {
                emptySet()
            } else {
                state.cards.mapNotNullTo(mutableSetOf()) { card ->
                    if (card.type.typeId != HomeCards.SERVER_CARD_TYPE_ID) return@mapNotNullTo null
                    val rect = state.rootRectOf(card)
                    if (rect.bottom >= top && rect.top <= bottom) card.id else null
                }
            }
        }.distinctUntilChanged().collect { ids ->
            ServerCardManager.onVisibleCardsChanged(ids)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                state.onViewportPositioned(
                    topPx = coordinates.positionInRoot().y,
                    heightPx = coordinates.size.height.toFloat()
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // VØID Control Deck — a purpose-built home surface.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF35145A), Color(0xFF1A0D2B), Color(0xFF090711))
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 19.dp, vertical = 19.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(8.dp).background(Color(0xFFB879FF), CircleShape))
                        Text(
                            text = "VØID CONTROL DECK",
                            color = Color(0xFFE1C6FF),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "JAVA EDITION",
                            color = Color(0xFFBCA7D2),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "YOUR NEXT\nWORLD STARTS HERE.",
                        color = Color(0xFFFFFAFF),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        lineHeight = 31.sp
                    )
                    Text(
                        text = "Build your setup. Choose a version. Enter your world.",
                        color = Color(0xFFD1C0E0),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf("VERSION CONTROL", "MOD SUPPORT", "VØID ENGINE").forEach { label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 5.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color(0xFFE8D8F7),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Hardware-aware recommendations; this panel does not alter game settings.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFF21152F), Color(0xFF100D18))),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VØID PERFORMANCE ENGINE",
                                color = Color(0xFFDAB9FF),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = "DEVICE PROFILE • SMART RECOMMENDATIONS",
                                color = Color(0xFF9D8AAC),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.55.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF49256A), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = performanceAdvice.profile.name.replace('_', ' '),
                                color = Color(0xFFF3E7FF),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        VoidPerformanceAdvisor.Profile.values().forEach { profile ->
                            val selected = profile == selectedPerformanceProfile
                            Text(
                                text = profile.name.replace('_', ' '),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) Color(0xFF65358D) else Color.White.copy(alpha = 0.055f),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable {
                                        selectedPerformanceProfile = profile
                                        performancePreferences.edit()
                                            .putString("profile", profile.name)
                                            .apply()
                                    }
                                    .padding(horizontal = 5.dp, vertical = 9.dp),
                                color = if (selected) Color.White else Color(0xFFBCA7C8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    if (selectedPerformanceProfile == VoidPerformanceAdvisor.Profile.CUSTOM) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "CUSTOM ENGINE TUNING",
                                color = Color(0xFFDAB9FF),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("JAVA MEMORY • ${performanceAdvice.maxMemoryMb} MB", color = Color(0xFFBFA9D2), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = customMemoryMb.toFloat().coerceIn(512f, safeCustomMemoryMb.toFloat()),
                                onValueChange = {
                                    customMemoryMb = it.toInt().coerceIn(512, safeCustomMemoryMb)
                                    performancePreferences.edit().putInt("custom_memory", customMemoryMb).apply()
                                },
                                valueRange = 512f..safeCustomMemoryMb.coerceAtLeast(513).toFloat(),
                                steps = if (safeCustomMemoryMb > 512) 6 else 0,
                                enabled = safeCustomMemoryMb > 512
                            )
                            Text("RENDER DISTANCE • $customRenderDistance CHUNKS", color = Color(0xFFBFA9D2), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = customRenderDistance.toFloat(),
                                onValueChange = {
                                    customRenderDistance = it.toInt().coerceIn(2, 24)
                                    performancePreferences.edit().putInt("custom_render", customRenderDistance).apply()
                                },
                                valueRange = 2f..24f,
                                steps = 10
                            )
                            Text("SIMULATION DISTANCE • $customSimulationDistance CHUNKS", color = Color(0xFFBFA9D2), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = customSimulationDistance.toFloat(),
                                onValueChange = {
                                    customSimulationDistance = it.toInt().coerceIn(2, 16)
                                    performancePreferences.edit().putInt("custom_simulation", customSimulationDistance).apply()
                                },
                                valueRange = 2f..16f,
                                steps = 6
                            )
                            Text("ENTITY DISTANCE • $customEntityDistance%", color = Color(0xFFBFA9D2), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = customEntityDistance.toFloat(),
                                onValueChange = {
                                    customEntityDistance = it.toInt().coerceIn(25, 150)
                                    performancePreferences.edit().putInt("custom_entities", customEntityDistance).apply()
                                },
                                valueRange = 25f..150f,
                                steps = 4
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "RAM" to "${performanceAdvice.maxMemoryMb} MB",
                            "RENDER" to "${performanceAdvice.renderDistance} CHUNKS",
                            "SIMULATION" to "${performanceAdvice.simulationDistance} CHUNKS"
                        ).forEach { (label, value) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 9.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color(0xFFAA96BC),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp
                                )
                                Text(
                                    text = value,
                                    color = Color(0xFFF4EAFE),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Text(
                        text = performanceAdvice.explanation,
                        color = Color(0xFFB6A7C3),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showPerformanceApply = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApplyingPerformance
                    ) {
                        Text(if (isApplyingPerformance) "APPLYING VØID PROFILE…" else "APPLY PROFILE TO CURRENT VERSION")
                    }
                    performanceApplyMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color(0xFFD8C5E9),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "Applying first creates a local backup. Nothing changes until you confirm.",
                        color = Color(0xFF8F809C),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF29143B), Color(0xFF120D1B))), RoundedCornerShape(18.dp))
                    .clickable { showVoidCore = true }.padding(horizontal = 15.dp, vertical = 13.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("VØID CORE", color = Color(0xFFE5CCFF), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        Text("LOCAL COMMAND ASSISTANT • OFFLINE", color = Color(0xFFB7A2C8), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("OPEN CONSOLE  ›", color = Color(0xFFD7B3FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            val systemCards = HomeCards.systemCards()
            if (!systemCards.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    systemCards.forEach { systemCard ->
                        key(systemCard.id) {
                            Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                                systemCard.content()
                            }
                        }
                    }
                }
            }

            CardGrid(
                state = state,
                containerColor = cardColor(),
                contentColor = onCardColor(),
                cardBackground = { base ->
                    base.backgroundGlass(
                        blur = AllSettings.backgroundBlur.state,
                        color = cardColor(),
                        enabled = true
                    )
                },
            )
        }
        CardGridAutoScroll(
            state = state,
            scrollState = scrollState
        )

        //调整态底部操作栏
        AnimatedVisibility(
            visible = state.isAdjusting,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 9.dp)
        ) {
            AdjustingBar(
                state = state,
                onShowSettings = {
                    operation = HomeCardOperation.ServerCardSettings(it)
                }
            )
        }
    }

    HomeCardOperation(
        operation = operation,
        onChange = { operation = it }
    )
    if (showPerformanceApply) {
        AlertDialog(
            onDismissRequest = { showPerformanceApply = false },
            title = { Text("APPLY VØID PERFORMANCE PROFILE?") },
            text = {
                Text(
                    "This will update Java memory allocation and Minecraft render, simulation, and entity-distance values for the selected installation. Existing options.txt and version.config files are copied to .void-backups first. Continue?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPerformanceApply = false
                        isApplyingPerformance = true
                        performanceApplyMessage = null
                        performanceScope.launch {
                            val outcome = runCatching {
                                VoidPerformanceApplier.apply(performanceAdvice)
                            }
                            performanceApplyMessage = outcome.fold(
                                onSuccess = { result ->
                                    "Applied to ${result.minecraftVersion}: ${result.memoryMb} MB RAM • Render ${result.renderDistance} • Simulation ${result.simulationDistance} • Entity ${result.entityDistancePercent}%. Backup folder: .void-backups."
                                },
                                onFailure = { error ->
                                    "Profile was not applied: ${error.message ?: "unknown error"}"
                                }
                            )
                            isApplyingPerformance = false
                        }
                    }
                ) { Text("APPLY") }
            },
            dismissButton = {
                TextButton(onClick = { showPerformanceApply = false }) { Text("CANCEL") }
            }
        )
    }
    if (showVoidCore) {
        AlertDialog(
            onDismissRequest = { showVoidCore = false },
            title = { Text("VØID CORE") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Offline command console. Try /help, /device, /optimize, /low-end, /check gpu, /check java, /check mods, or /fps.", style = MaterialTheme.typography.bodySmall)
                    voidCoreHistory.takeLast(4).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(value = voidCoreInput, onValueChange = { voidCoreInput = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Enter a command") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val command = voidCoreInput.trim()
                    if (command.isNotEmpty()) {
                        val response = VoidCoreCommandRouter.route(command, appContext)
                        voidCoreHistory.add("> " + command + "\n" + response.title + ": " + response.message + if (response.requiresConfirmation) "\nConfirmation required before this action." else "")
                        while (voidCoreHistory.size > 20) voidCoreHistory.removeAt(0)
                        voidCorePreferences.edit()
                            .putString("history", voidCoreHistory.joinToString("\n--VOID-CORE-ENTRY--\n"))
                            .apply()
                        voidCoreInput = ""
                    }
                }, enabled = voidCoreInput.isNotBlank()) { Text("RUN") }
            },
            dismissButton = { TextButton(onClick = { showVoidCore = false }) { Text("CLOSE") } }
        )
    }
}

@Composable
private fun HomeCardOperation(
    operation: HomeCardOperation,
    onChange: (HomeCardOperation) -> Unit
) {
    when (operation) {
        is HomeCardOperation.None -> {}
        is HomeCardOperation.ServerCardSettings -> {
            ServerCardSettingsHost(
                cardId = operation.cardId,
                onDismiss = { onChange(HomeCardOperation.None) }
            )
        }
    }
}

/**
 * 调整态底部操作栏
 */
@Composable
private fun AdjustingBar(
    state: CardGridState,
    onShowSettings: (String) -> Unit
) {
    val adjustingId = state.adjustingCardId
    val isServerCard = remember(state.cards, adjustingId) {
        state.cards.firstOrNull { it.id == adjustingId }?.type?.typeId == HomeCards.SERVER_CARD_TYPE_ID
    }

    Surface(
        modifier = Modifier
            .padding(all = 3.dp)
            .barGuard(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(all = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isServerCard && adjustingId != null) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    onClick = { adjustingId?.let(onShowSettings) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = stringResource(R.string.generic_setting)
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            IconButton(
                modifier = Modifier.size(34.dp),
                enabled = adjustingId != null,
                onClick = { adjustingId?.let(state::removeCard) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_filled),
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = stringResource(R.string.generic_delete)
                )
            }

            Spacer(Modifier.width(2.dp))

            IconButton(
                modifier = Modifier.size(34.dp),
                onClick = { state.exitAdjusting() }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.generic_done)
                )
            }
        }
    }
}

/**
 * 消费落在自身范围内的全部指针事件
 */
private fun Modifier.barGuard(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).consume()
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) return@awaitEachGesture
        }
    }
}