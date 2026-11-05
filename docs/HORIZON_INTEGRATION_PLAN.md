# Vøidlaunchear — Horizon Vulkan Compatibility Assessment

Date: 2026-11-05

## User-requested scope

The only Horizon feature requested for this project is its **Vulkan compatibility rendering mode**. Do not port Horizon's game engine, UI, input system, or unrelated features as part of this request.

## Decision

Keep Zalith Launcher 2 as the Minecraft Java 1.21.11 foundation. Horizon Android's Vulkan compatibility mode is an engine-level rendering path, not a generic Vulkan switch. It cannot be copied into Zalith by adding a label or a preference.

The Zalith source already contains a Vulkan-related alternative named **Kopper Zink**. This is the closest existing path to evaluate, but it is **not the same implementation** as Horizon's compatibility renderer:

- Horizon documents a Vulkan compatibility path that uses direct terrain draws to work around driver rendering issues.
- Zalith's Kopper Zink renderer is Mesa Zink-based. Its source selects the Zink/Kopper renderer libraries and sets `needsZinkDriverOverride = true`.
- Zalith registers Kopper Zink in its built-in renderer list and its renderer settings screen selects from that registry.
- Zalith's renderer source also marks Kopper Zink with a maximum game-version metadata value of `26.3-snapshot-3` (displayed as `26.2`). This is not proof of tested Minecraft 1.21.11 compatibility; it must be verified on-device.

## Integration plan

1. Keep the existing Zalith renderer registry and settings screen. Do not add a fake "Horizon Vulkan Compatibility" toggle.
2. Preserve Kopper Zink as the Vulkan-backed/Mesa-Zink option exposed through Zalith's existing renderer selection, subject to its packaged native libraries and device support.
3. Keep the normal compatible renderer available as a fallback. If Kopper Zink is missing, fails to initialize, or renders incorrectly, the user must be able to switch back.
4. Do not force Kopper Zink as the default renderer. Vulkan support varies by Android device, GPU, and driver.
5. Before calling this feature complete, build the APK and test Minecraft Java 1.21.11 on a real Android device: renderer discovery, launch, world load, terrain rendering, UI/textures, stability, and fallback.
6. Only consider a true Horizon-style direct-draw compatibility mode if a future engine/backend implementation exposes the necessary rendering hooks. That would be a separate engine-level project, not a settings-only change.

## Important distinction

**Kopper Zink is a Vulkan-backed OpenGL compatibility renderer, not Horizon's exact Vulkan compatibility mode.** It may provide a useful alternative on supported devices, but it does not guarantee better performance, fewer graphical bugs, or compatibility on every phone.

## Verified Zalith source locations

Upstream repository inspected: https://github.com/ZalithLauncher/ZalithLauncher2

- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/renderer/Renderers.kt` — built-in renderer registration includes Kopper Zink.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/renderer/renderers/KopperZinkRenderer.kt` — renderer ID, Mesa/Zink flags, environment and native library names.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/game/renderer/RendererInterface.kt` — renderer contract, including the Zink driver override flag.
- `ZalithLauncher/src/main/java/com/movtery/zalithlauncher/ui/screens/content/settings/RendererSettingsScreen.kt` — renderer selection UI backed by the registry.
- `ZalithLauncher/libs/kopper-zink-release.aar` — packaged Kopper Zink Android library exists in the upstream source tree.

## Horizon reference

Horizon Android documents three rendering choices: Vulkan/GPU batching, Vulkan/compatibility, and experimental OpenGL ES. Its compatibility mode is a separate engine rendering path. Horizon Android is a native Rust/wgpu client targeting Minecraft 26.3, not a drop-in renderer library for Zalith's Java 1.21.11 launch path.

- https://github.com/Vasysik/horizon-android/blob/android/docs/android.md
- https://github.com/Vasysik/horizon-android/blob/android/docs/android-features.md

## Status

Source inspection confirms that Zalith already has a Kopper Zink renderer entry and a renderer-selection UI. This update records the closest available Vulkan-backed option and the required validation plan. It does **not** claim that Minecraft 1.21.11 has been tested with Kopper Zink, that Horizon's exact compatibility path has been ported, or that an APK has been built.
