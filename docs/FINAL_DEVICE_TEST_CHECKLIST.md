# VøidLaunchear final device test checklist

This checklist is for the next APK test pass. It deliberately separates implemented source paths from behavior that still requires a real Android device and Minecraft account.

## Already present — do not reimplement
- Custom launcher background image/video selection and persistence.
- Account management and Microsoft authentication flow.
- Existing-world Quick Play argument construction.
- Server Quick Play argument construction with address parsing and legacy argument fallback.
- Minecraft version manifest validation and cache fallback.
- Modrinth and CurseForge project/version search and pagination.
- Vulkan capability detection and safe renderer fallback.
- Low-end performance advisor and profile application.
- Companion stage with idle/walk/sit, small autonomous roaming, drag handling, and a PetLib ragdoll trigger.

## Device checks still required
1. Background: import a valid image, import a valid video, restart app, switch between them, and verify a failed/invalid import does not erase the previous background.
2. Login: complete Microsoft device-code login, restart app, confirm account persists, then test expired-token refresh. Test offline launch separately; do not treat offline mode as a way to bypass ownership/account requirements.
3. Versions: load the official version catalog, select representative 1.18.2, 1.20.1, 1.21.x and current supported release, install, then launch each compatible profile.
4. Existing-world Quick Play: launch a known save by its folder name; test a missing save and confirm the launcher reports/handles the failure instead of silently launching normal gameplay.
5. Server Quick Play: test hostname, hostname with port, IPv4 with port, IPv6 if supported, and malformed/blank input. Confirm malformed input does not silently become an ordinary launch.
6. New World: no launcher-owned Create New World screen exists in the current Kotlin/Java source tree. Use Minecraft's native Create New World screen for now. Automatic launcher creation-to-entry remains an open feature gap; do not add undocumented command-line flags or fabricate world metadata.
7. Mods: search and install one Modrinth mod and one CurseForge mod; test no results, pagination, cancellation, rate limiting, and retry after network restoration. JSON GET calls now retry transient I/O/DNS, Ktor request timeouts, HTTP 408, 429 and 5xx with bounded backoff; POST is not automatically retried. Modrinth and CurseForge pagination also have upper safety bounds. Retry unit tests were added but not executed.
8. Renderer: test OpenGL on a known-compatible device. Run the Vulkan scan for the currently selected driver and change drivers before scanning again; stale results must not enable Vulkan for a different driver. Native Minecraft Vulkan is only for versions with the native backend (26.2+ in the current profile); for 1.21.11 use OpenGL or Kopper Zink (OpenGL over Vulkan), subject to driver compatibility.
9. Low-end profile: apply the low-memory profile, launch, check memory allocation and render/simulation distances, then restore balanced settings.
10. Companion: drag, release after lifting, confirm the model stays at the release point while PetLib plays the fall animation, then returns to the home position. Observe walk/sit/idle and roaming. Current ragdoll is an animation trigger, not a full rigid-body physics simulation.
11. Controls: verify touch layout editing, on-screen controls, keyboard, mouse, rotation, and landscape orientation in an actual game session.
12. Branding: verify app label and navigation title both read VøidLaunchear, launcher icon, splash/loading collision animation, credits, APK filename Vøidlaunchear, and version label on a clean install and upgrade install.

## Build gate
Do not dispatch an APK build until the feature work is reviewed and the project owner explicitly approves the build. Source changes and source tests are not proof of a successful Android build or device test.

## Latest source commits (2026-11-06)
- `63847cc5933244185f20a3202dc28529cb7cf203` — stricter server address parsing.
- `0a97173e50bb313a3a1a6b3283bf7538df7532ce` — server address parser tests.
- `34e17ad92c9bfbec1991adb774a8678b3f2cdd9c` — bounded retry for transient JSON GET failures and HTTP 429/5xx.
- `7efdba33ba8f326e1a1d0a0b240761bf491b1247` — retry behavior tests.
- `d5aa9ef3d2364225a29322d51478163a352ea937` — retry-count semantics and input validation.
- `ff0cb045da3ebe09c71ceadc77b3621fdf22d6ff` — mod download retry documentation.
- `54fa11ca7722327a942d3bef5874da44c722b6ca` — New World source-tree audit.
- `5c7c1bba9626d73e18bfb6d7c11e0a468951ec82` and `9168b80e3d5a05644065b97c95a4f9b24e3d219a` — bounded JSON GET retries including Ktor request timeouts.
- `bd3f713981626b2c4100ca3ad7f27ed67ff182b2` and `104f6a5708dd2633cd25eee562342aca58c26807` — Modrinth and CurseForge pagination safety caps.
- `69e59ecc3cf58b4d2e316628594fb2ceee2e6882`, `f6f4fe88db025bb045ceee0a66c73228258f8f20`, and `af33132fe485fbdc4fb5589b02f9f00956ad9d2b` — retry backoff arithmetic and retry-count tests.
- `350102e8e88c8e603969cc08ba1d5343ea362e81` through `a8e412bf47c4eac1f8ad29730c70dbef5a77c706` — world backup metadata, archive-entry, and restore-target hardening.
- `dd0bf9d2a43e204e10843e6e0bb486f3002aa776` — VøidLaunchear navigation-title branding.
- `dc8f9aad0a467b0b633c23c68c371715088d5820` and `e6cab2bfc22ae0962cfb516e1c586ddd4a88e7dd` — Vøidlaunchear APK artifact filename and labels.
- `535a0d31b56afc4a08eecdc1b5c262a254a2484c` — discard stale Vulkan compatibility results after driver changes.
- `02065066f6934d79fde1c6e550b0e4096680b2bc` — retain companion drop position during the ragdoll animation.
- `280c637a958af350d98705d4acad6b5ce9bdc2e1` — improve screen-reader heading and live status semantics in Vulkan setup.
- `8ace4e4f0da849cbe06b30fafcb087479fbc8db7` — propagate coroutine cancellation instead of misreporting it as a failed Vulkan scan.
- `0f2a0842954861a1d71ae02f1858217da2338eeb` — preserve the companion fall animation when an airborne drag gesture is cancelled.
- `1eb22e0b0d9b50d5de7a1a55ceb7959e870cbbfc` and `a5531412ebd4e3b4c6227a8341acb04c87144508` — recover world-save operation UI state after failures and keep Compose state updates on the main thread.
