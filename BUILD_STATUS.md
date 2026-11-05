# Vøidlaunchear Build and Integration Status

This file records what has been verified in the `void-branding-foundation` branch.

## Verified in the repository

- The repository contains the Android Gradle project and the `:ZalithLauncher` application module, plus its supporting modules.
- The launcher application ID is `com.voidlaunchear.launcher`.
- The displayed application name is `Vøidlaunchear`; the short name is `Vøid`.
- The Android launcher, main activity, game activity, and file manager activities are configured for landscape orientation.
- The launcher icon foreground is a purple Ø-style ring/slash on a dark background.
- The first-run welcome screen identifies the build as an unofficial modified version and uses the configured launcher name.
- The project includes a Gradle wrapper and GitHub Actions workflows for debug builds.
- The project is based on Zalith Launcher 2 and retains its upstream launcher/game functionality and GPL-3.0 license notices.

## Renderer scope

The requested Horizon-inspired feature is limited to renderer compatibility. The current plan is to preserve Zalith's renderer registry and existing renderer choices, including Kopper Zink where available. Kopper Zink is not the same implementation as Horizon's Vulkan compatibility/direct-terrain-draw mode. Do not label it as an exact Horizon port or force it as the default renderer.

Actual renderer support depends on the Android device, GPU, driver, and Minecraft version. It must be verified on a real device before being described as working.

## Not yet verified

- The full VØID home-screen redesign; the main screen still uses the upstream layout.
- The newly added VØID/LAUNCHEAR intro animation has not yet been compiled or device-tested.
- Installation and launch on a physical Android device.
- Microsoft account sign-in and a complete Minecraft 1.21.11 launch.
- Offline singleplayer after the required game files and runtime have been installed.
- World loading, terrain rendering, controls, multiplayer, mods, and renderer fallback on target devices.
- An exact Horizon-style Vulkan compatibility rendering path.

## Build

The repository's `.github/workflows/push_ci.yml` invokes the debug build workflow on pushes. A successful Debug ARM64 build was verified for commit `f8c4636903d129ecbbdebe4159bc856ca268abd2` (workflow run #3, artifact `Vøidlaunchear-debug-arm64`). Later source changes must trigger a fresh build before they are considered verified. The build workflow supports manual `workflow_dispatch`: open Actions → Build APK → Run workflow → choose `Debug`.

Do not distribute an APK as tested or release-ready until the build and device checks above have passed.
