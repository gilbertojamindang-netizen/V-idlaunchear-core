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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.Plugin
import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.ConfigurableRenderer
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.renderers.KopperZinkRenderer
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.path.URL_CLOUD_DRIVE_DRIVER_PLUGINS
import com.movtery.zalithlauncher.path.URL_CLOUD_RENDERER_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_DRIVER_PLUGINS
import com.movtery.zalithlauncher.path.URL_GITHUB_RENDERER_PLUGINS
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.ResolutionRule
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedColumn
import com.movtery.zalithlauncher.ui.components.IntInputField
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.IntSliderSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.customResolutionRange
import com.movtery.zalithlauncher.utils.device.VulkanCapabilities
import com.movtery.zalithlauncher.utils.device.VulkanChecker
import com.movtery.zalithlauncher.utils.device.profileSupport
import com.movtery.zalithlauncher.utils.device.checkVulkanSupport
import com.movtery.zalithlauncher.utils.device.formatVulkanApiVersion
import com.movtery.zalithlauncher.utils.device.getVulkanApiVersion
import com.movtery.zalithlauncher.utils.ensureCustomResolutionInitialized
import com.movtery.zalithlauncher.utils.getRealScreenSize
import com.movtery.zalithlauncher.utils.isAdrenoGPU
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.sendDLPlugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RendererSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    eventViewModel: EventViewModel,
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Renderer, settingsScreenKey, false)
    ) { isVisible ->
        AnimatedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 12.dp),
            isVisible = isVisible
        ) {
            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    val currentRendererId = AllSettings.renderer.state
                    val currentRenderer = remember(currentRendererId) {
                        Renderers.getRenderers()
                            .find { it.getUniqueIdentifier() == currentRendererId }
                    }
                    val configurableRenderer = remember(currentRenderer) {
                        (currentRenderer as? ConfigurableRenderer)
                            ?.takeIf { it.configSpec.entries.isNotEmpty() }
                    }
                    var showConfigDialog by remember { mutableStateOf(false) }

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.renderer,
                        items = Renderers.getRenderers(),
                        title = stringResource(R.string.settings_renderer_global_renderer_title),
                        summary = stringResource(R.string.settings_renderer_global_renderer_summary),
                        getItemText = { it.getRendererName() },
                        getItemId = { it.getUniqueIdentifier() },
                        getItemSummary = {
                            RendererSummaryLayout(it)
                        },
                        trailingIcon = {
                            if (configurableRenderer != null) {
                                IconButton(
                                    onClick = { showConfigDialog = true }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_settings_filled),
                                        contentDescription = stringResource(R.string.settings_renderer_config_title)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    eventViewModel.sendDLPlugin(
                                        githubLink = URL_GITHUB_RENDERER_PLUGINS,
                                        cloudDrives = listOf(
                                            EventViewModel.Event.DownloadPlugins.CloudDrive(
                                                language = "zh",
                                                link = URL_CLOUD_RENDERER_PLUGINS
                                            )
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download_2_filled),
                                    contentDescription = stringResource(R.string.generic_download)
                                )
                            }
                        }
                    )

                    if (showConfigDialog && configurableRenderer != null) {
                        RendererConfigDialog(
                            renderer = configurableRenderer,
                            onDismissRequest = { showConfigDialog = false }
                        )
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.sfpewPluginLayer,
                        enabled = currentRenderer is Plugin,
                        title = stringResource(R.string.settings_renderer_sfpew_plugin_layer_title),
                        summary = stringResource(R.string.settings_renderer_sfpew_plugin_layer_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.sfpewRelaxedContext,
                        enabled = currentRenderer?.let { renderer ->
                            renderer.supportsSfpewByDefault() ||
                            // 如果是插件，需要看是否给插件启用了这个玩意
                            (renderer is Plugin && AllSettings.sfpewPluginLayer.state)
                        } == true,
                        title = stringResource(R.string.renderer_config_sfpew_relaxed_context_title),
                        summary = stringResource(R.string.renderer_config_sfpew_relaxed_context_desc)
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.vulkanDriver,
                        items = DriverPluginManager.getDriverList(),
                        title = stringResource(R.string.settings_renderer_global_vulkan_driver_title),
                        getItemText = { it.name },
                        getItemId = { it.id },
                        getItemSummary = {
                            DriverSummaryLayout(it)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    eventViewModel.sendDLPlugin(
                                        githubLink = URL_GITHUB_DRIVER_PLUGINS,
                                        cloudDrives = listOf(
                                            EventViewModel.Event.DownloadPlugins.CloudDrive(
                                                language = "zh",
                                                link = URL_CLOUD_DRIVE_DRIVER_PLUGINS
                                            )
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download_2_filled),
                                    contentDescription = stringResource(R.string.generic_download)
                                )
                            }
                        }
                    )

                    ListSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.graphicsApi,
                        items = GraphicsApi.entries,
                        title = stringResource(R.string.settings_game_graphics_api_title),
                        summary = stringResource(R.string.settings_game_graphics_api_summary),
                        getItemText = {
                            when (it) {
                                GraphicsApi.DEFAULT -> stringResource(R.string.settings_game_graphics_api_default)
                                GraphicsApi.DEFAULT_OPENGL -> stringResource(R.string.settings_game_graphics_api_default_opengl)
                                else -> it.displayName
                            }
                        }
                    )
                }
            }

            AnimatedItem { ctx ->
                VulkanSetupPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                )
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    val context = LocalContext.current

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 分辨率规则
                        val resolutionRule = AllSettings.resolutionRule.state
                        ListSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Top,
                            unit = AllSettings.resolutionRule,
                            items = ResolutionRule.entries,
                            title = stringResource(R.string.settings_renderer_resolution_rule_title),
                            summary = stringResource(R.string.settings_renderer_resolution_rule_summary),
                            getItemText = { stringResource(it.nameRes) },
                            onValueChange = { rule ->
                                // 自定义分辨率尚未初始化时，以屏幕真实宽高填充
                                if (rule == ResolutionRule.CUSTOM) {
                                    ensureCustomResolutionInitialized(context)
                                }
                            }
                        )

                        // 百分比分辨率
                        AnimatedVisibility(
                            visible = resolutionRule == ResolutionRule.PERCENTAGE,
                            enter = fadeIn(animationSpec = getAnimateTween()) +
                                    expandVertically(animationSpec = getAnimateTween()),
                            exit = fadeOut(animationSpec = getAnimateTween()) +
                                    shrinkVertically(animationSpec = getAnimateTween())
                        ) {
                            IntSliderSettingsCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                position = CardPosition.Middle,
                                unit = AllSettings.resolutionRatio,
                                title = stringResource(R.string.settings_renderer_resolution_scale_title),
                                summary = stringResource(R.string.settings_renderer_resolution_scale_summary),
                                valueRange = AllSettings.resolutionRatio.floatRange,
                                suffix = "%",
                                fineTuningControl = true
                            )
                        }

                        // 自定义分辨率
                        AnimatedVisibility(
                            visible = resolutionRule == ResolutionRule.CUSTOM,
                            enter = fadeIn(animationSpec = getAnimateTween()) +
                                    expandVertically(animationSpec = getAnimateTween()),
                            exit = fadeOut(animationSpec = getAnimateTween()) +
                                    shrinkVertically(animationSpec = getAnimateTween())
                        ) {
                            CustomResolutionSettingsCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                position = CardPosition.Middle
                            )
                        }
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.gameFullScreen,
                        title = stringResource(R.string.settings_renderer_full_screen_title),
                        summary = stringResource(R.string.settings_renderer_full_screen_summary)
                    )
                }
            }

            AnimatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Top,
                        unit = AllSettings.sustainedPerformance,
                        title = stringResource(R.string.settings_renderer_sustained_performance_title),
                        summary = stringResource(R.string.settings_renderer_sustained_performance_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.requestMaxRefreshRate,
                        title = stringResource(R.string.settings_renderer_max_refresh_rate_title),
                        summary = stringResource(R.string.settings_renderer_max_refresh_rate_summary)
                    )

                    if (checkVulkanSupport(LocalContext.current.packageManager)) {
                        val vulkanApiLabel = getVulkanApiVersion(LocalContext.current.packageManager)
                            ?.let(::formatVulkanApiVersion)
                            ?: "version not reported"
                        Text(
                            text = "Detected Vulkan API: $vulkanApiLabel. Android's feature declaration does not guarantee every renderer or driver plugin will work.",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        var adrenoGPUAlert by remember { mutableStateOf(false) }

                        SwitchSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            position = CardPosition.Middle,
                            unit = AllSettings.zinkPreferSystemDriver,
                            title = stringResource(R.string.settings_renderer_vulkan_driver_system_title),
                            summary = stringResource(R.string.settings_renderer_vulkan_driver_system_summary),
                            onCheckedChange = { checked ->
                                if (checked && isAdrenoGPU()) adrenoGPUAlert = true
                            }
                        )

                        if (adrenoGPUAlert) {
                            SimpleAlertDialog(
                                title = stringResource(R.string.generic_warning),
                                text = stringResource(R.string.settings_renderer_zink_driver_adreno),
                                onConfirm = {
                                    AllSettings.zinkPreferSystemDriver.save(true)
                                    adrenoGPUAlert = false
                                },
                                onDismiss = {
                                    AllSettings.zinkPreferSystemDriver.save(false)
                                    adrenoGPUAlert = false
                                }
                            )
                        }
                    }

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Middle,
                        unit = AllSettings.vsyncInZink,
                        title = stringResource(R.string.settings_renderer_vsync_in_zink_title),
                        summary = stringResource(R.string.settings_renderer_vsync_in_zink_summary)
                    )

                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Bottom,
                        unit = AllSettings.dumpShaders,
                        title = stringResource(R.string.settings_renderer_shader_dump_title),
                        summary = stringResource(R.string.settings_renderer_shader_dump_summary)
                    )
                }
            }
        }
    }
}


/**
 * VØID Vulkan setup: performs a real native capability scan and exposes safe API fallbacks.
 * A positive scan is not a guarantee that every Minecraft version, renderer, or mod will work.
 */
@Composable
private fun VulkanSetupPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val systemFeatureDetected = remember(context) {
        checkVulkanSupport(context.packageManager)
    }
    val declaredApiVersion = remember(context, systemFeatureDetected) {
        if (systemFeatureDetected) {
            getVulkanApiVersion(context.packageManager)?.let(::formatVulkanApiVersion)
        } else null
    }

    // Scan using the driver selected in global Renderer Settings, including installed driver plugins.
    val selectedDriverId = AllSettings.vulkanDriver.state
    val selectedDriver = remember(selectedDriverId) {
        DriverPluginManager.getDriver(selectedDriverId)
    }

    var isScanning by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }
    var scannedDriverId by remember { mutableStateOf<String?>(null) }
    var capabilities by remember { mutableStateOf<VulkanCapabilities?>(null) }
    var showNativeVulkanWarning by remember { mutableStateOf(false) }
    var showExperimentalWarning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // A scan result belongs only to the exact driver that was selected when it ran.
    val hasCurrentScan = hasScanned && scannedDriverId == selectedDriverId
    val currentCapabilities = capabilities.takeIf { hasCurrentScan }
    val supportedProfiles = currentCapabilities?.profileSupport()?.filter { it.supported }.orEmpty()
    val unsupportedProfiles = currentCapabilities?.profileSupport()?.filterNot { it.supported }.orEmpty()

    SettingsCardColumn(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "VØID VULKAN SETUP",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Scan the selected driver, review supported Minecraft Vulkan profiles, then choose a graphics path.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Selected Vulkan driver: ${selectedDriver.name}" +
                    if (selectedDriver.isLauncher) " (built-in)" else " (plugin)",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (systemFeatureDetected) {
                    "Android Vulkan feature: detected"
                } else {
                    "Android Vulkan feature: not reported by the system"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (systemFeatureDetected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
            declaredApiVersion?.let { api ->
                Text(
                    text = "System-reported Vulkan API: $api",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                onClick = {
                    isScanning = true
                    hasScanned = false
                    capabilities = null
                    scannedDriverId = selectedDriverId
                    val driverIdForScan = selectedDriverId
                    scope.launch {
                        val driver = selectedDriver
                        val tempDir = if (driver.isLauncher) {
                            null
                        } else {
                            File(PathManager.DIR_CACHE, "vulkan_setup_temp")
                        }
                        val result = try {
                            withContext(Dispatchers.IO) {
                                VulkanChecker.checkCapabilities(
                                    driverPath = null,
                                    nativeDir = driver.path.takeUnless { driver.isLauncher },
                                    cacheDir = tempDir?.absolutePath
                                )
                            }
                        } catch (cancelled: CancellationException) {
                            // Never turn coroutine cancellation into a failed compatibility scan.
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                        if (AllSettings.vulkanDriver.state == driverIdForScan) {
                            capabilities = result
                            hasScanned = true
                        } else {
                            capabilities = null
                            hasScanned = false
                        }
                        isScanning = false
                    }
                }
            ) {
                Text(if (isScanning) "SCANNING SELECTED DRIVER..." else "RUN COMPATIBILITY SCAN")
            }

            if (hasCurrentScan) {
                val result = currentCapabilities
                if (result == null) {
                    Text(
                        text = "The selected driver scan could not complete. Try the built-in driver or keep OpenGL as the fallback.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Detected Vulkan API: ${result.versionString} " +
                            if (result.isVersionSupported) "(1.2 baseline passed)" else "(below 1.2 baseline)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (supportedProfiles.isNotEmpty()) {
                            "Native Vulkan profiles passing required checks: " +
                                supportedProfiles.joinToString { it.versionRangeText }
                        } else {
                            "No native Minecraft Vulkan profile passed the required checks."
                        },
                        color = if (supportedProfiles.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (unsupportedProfiles.isNotEmpty()) {
                        Text(
                            text = "Profiles not passing: " +
                                unsupportedProfiles.joinToString { it.versionRangeText },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Kopper Zink is bundled as a built-in renderer. Do not disable its
            // selection button just because the runtime renderer registry has not
            // finished populating when this settings panel is composed.

            Text(
                text = "VØID RENDERING MODES",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )

            // OpenGL experimental mode is intentionally available even when the device has not
            // been validated. The warning explains the risk and gives the user a safe fallback.
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                onClick = { showExperimentalWarning = true }
            ) {
                Text("OPENGL ES / EXPERIMENTAL")
            }
            Text(
                text = "Uses the launcher's standard OpenGL ES path with an experimental warning profile; it does not install a separate renderer. Use at your own risk: some devices or drivers may show glitches, fail to render, or crash. You can return here and switch to a supported Vulkan mode.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = supportedProfiles.isNotEmpty() && !isScanning,
                onClick = { showNativeVulkanWarning = true }
            ) {
                Text("VULKAN / GPU BATCHING")
            }
            Text(
                text = "Uses Minecraft's native Vulkan path only when the selected driver passes the supported profile scan (currently 26.2+ profiles).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                onClick = {
                    AllSettings.renderer.save(KopperZinkRenderer.getUniqueIdentifier())
                    AllSettings.graphicsApi.save(GraphicsApi.DEFAULT_OPENGL)
                }
            ) {
                Text("VULKAN / COMPATIBILITY")
            }
            Text(
                text = "Uses Kopper Zink (OpenGL translated over Vulkan), not native Vulkan. This option is available when the Zink renderer is installed; a successful native Vulkan profile scan is not required. It may still fail on unsupported GPU drivers.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            if (showExperimentalWarning) {
                SimpleAlertDialog(
                    title = "Enable OpenGL ES / Experimental?",
                    text = "This mode is experimental and is used at your own risk. Rendering glitches, poor performance, or launch failures are possible on some devices. You can switch back to a supported Vulkan mode or default OpenGL if needed.",
                    onConfirm = {
                        AllSettings.renderer.save("")
                        AllSettings.graphicsApi.save(GraphicsApi.DEFAULT_OPENGL)
                        showExperimentalWarning = false
                    },
                    onDismiss = {
                        showExperimentalWarning = false
                    }
                )
            }

            if (showNativeVulkanWarning) {
                SimpleAlertDialog(
                    title = "Apply Native Vulkan globally?",
                    text = "This changes the global graphics API for versions that follow global settings. Native Minecraft Vulkan is only available in versions with the native Vulkan backend (26.2+ in the current compatibility profile). For mixed or older versions such as 1.21.11, keep OpenGL or Kopper Zink globally and configure Native Vulkan only on a compatible version.",
                    onConfirm = {
                        AllSettings.renderer.save("")
                        AllSettings.graphicsApi.save(GraphicsApi.VULKAN)
                        showNativeVulkanWarning = false
                    },
                    onDismiss = {
                        showNativeVulkanWarning = false
                    }
                )
            }

            Text(
                text = "A passing scan checks the selected driver's Vulkan capabilities, but cannot guarantee every device, game version, shader, or modpack will work. Vulkan GPU Batching is native Vulkan; Vulkan Compatibility uses Kopper Zink to translate OpenGL over Vulkan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun RendererSummaryLayout(renderer: RendererInterface) {
    FlowRow(
        modifier = Modifier.alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        with(renderer) {
            getRendererSummary()?.let { summary ->
                Text(text = summary, style = MaterialTheme.typography.labelSmall)
            }

            val minVer = getDisplayMinMCVersion()
            val maxVer = getDisplayMaxMCVersion()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (minVer != null || maxVer != null) {
                    Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                    minVer?.let {
                        Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                    }

                    maxVer?.let {
                        Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DriverSummaryLayout(driver: Driver) {
    with(driver) {
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CustomResolutionSettingsCard(
    modifier: Modifier = Modifier,
    position: CardPosition = CardPosition.Middle
) {
    val context = LocalContext.current
    val screenSize = remember(context) { getRealScreenSize(context) }

    SettingsCard(
        modifier = modifier,
        position = position
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_renderer_resolution_scale_title),
                summary = stringResource(R.string.settings_renderer_resolution_custom_summary)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntInputField(
                    modifier = Modifier.weight(1f),
                    value = AllSettings.customResolutionWidth.state,
                    permitted = customResolutionRange(screenSize.width),
                    label = stringResource(R.string.settings_renderer_resolution_custom_width),
                    onValueChange = { AllSettings.customResolutionWidth.save(it) }
                )
                IntInputField(
                    modifier = Modifier.weight(1f),
                    value = AllSettings.customResolutionHeight.state,
                    permitted = customResolutionRange(screenSize.height),
                    label = stringResource(R.string.settings_renderer_resolution_custom_height),
                    onValueChange = { AllSettings.customResolutionHeight.save(it) }
                )
            }
        }
    }
}