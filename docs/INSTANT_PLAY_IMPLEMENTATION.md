# VøidLaunchear — Instant Play implementation plan

Status: planned; not yet implemented or verified in an APK.

## User-facing goal

Make the path from tapping **Play** to entering Minecraft as direct as the Minecraft runtime permits. This is a launch-flow feature, not an in-game floating overlay.

## Required launch paths

1. **Existing single-player world** — one tap launches the selected Minecraft installation and opens the selected save automatically.
2. **New single-player world** — after the user finishes world creation settings and confirms Create, launch Minecraft and enter the newly created world without returning to the launcher or requiring a second manual world selection.
3. **Saved multiplayer server** — one tap launches the selected installation and connects to the selected server.
4. **Startup/loading optimization** — avoid redundant launcher-side work, reuse valid cached metadata/assets, and show clear progress while required runtime initialization continues.

## Correctness and safety requirements

- Never claim zero-second loading: Minecraft must initialize the JVM, renderer, assets, world data, and network connection.
- Do not skip required downloads, integrity checks, account authentication, or world initialization merely to make the progress screen disappear.
- Preserve the existing normal launch path as a fallback if a quick-play target is missing, incompatible, or fails to open.
- Validate save names and server addresses; never build shell commands by concatenating untrusted user input.
- A new-world flow must preserve all selected world options (name, seed, game mode, difficulty, cheats, datapacks, and version compatibility).
- Server quick join must report connection/authentication errors and allow a normal launch fallback.
- Quick Play must not silently change the selected Minecraft version or account.

## Existing code to integrate with

- `viewmodel/LaunchGameViewModel.kt` already exposes `quickPlaySave(...)` and `quickPlayServer(...)`.
- `game/launch/GameLaunchFlow.kt` owns account validation, mod scanning, integrity/download work, and the final `runGame(...)` call.
- `game/version/saves/SaveUtils.kt` parses save metadata and should be reused for save validation.

## Implementation sequence

1. Trace the existing QuickPlay model and the game process launch arguments end-to-end.
2. Connect selected-save and selected-server quick play to the final game launch invocation.
3. Add a dedicated new-world handoff that carries the complete world-creation settings into Minecraft's supported world-creation flow, then opens the created world.
4. Add launch preparation/caching only where correctness is preserved; keep required initialization and integrity repair.
5. Add unit tests for target validation, missing saves, invalid server addresses, fallback behavior, and one-launch-at-a-time protection.
6. Verify on-device for an existing world, a newly created world, and a multiplayer server before marking this feature complete.

## Acceptance criteria

- Existing world: one Play action reaches the selected world without an extra world-list selection.
- New world: one Create confirmation reaches the newly created world without returning to the launcher.
- Server: one Join action reaches the selected server, with useful error feedback on failure.
- Cold and warm launches both work; warm launch may be faster but neither skips required game initialization.
- Normal launch remains available and all existing account/version protections remain intact.

## Source audit findings (2026-11-06)

- `LaunchGameViewModel.kt` exposes `quickPlaySave(version, saveName)` and `quickPlayServer(version, address)`, but this only sets a `LaunchGameOperation.TryLaunch` request.
- `GameLaunchFlow.launch(...)` currently accepts the version and account, and its final task calls `runGame(context, version, account)`. The `quickPlay` target is not passed into this flow or the game process configuration in the inspected path.
- `GameLauncher.LaunchConfig` currently carries only the version and account. Therefore, the existing quick-play methods are not proof that automatic world/server entry is implemented.
- `SavesManagerScreen.kt` has an `onQuickPlay` callback for save entries, but it depends on the missing downstream handoff described above.
- The splash loading screen already contains the VØID/LAUNCHER collision animation in the current source; this area needs a visual/branding review rather than assuming it is absent.
- The manifest already enforces landscape orientation for launcher and game activities.

These findings are source inspection only. No Gradle build, APK build, or device test was run. The next implementation step is to trace the launch-operation consumer and VMActivity/GameService intent contract before changing parcelable launch configuration, so existing game startup and process boundaries are not broken.
