# MainBrain — Kotha (কথা)

## App Identity & Overview
- **App Name**: Kotha (কথা) — Floating orb continuous dictation Android app (Bangla & English).
- **Build & Deliverable**: Build with `assembleDebug` once after all changes are done, and place a compiled APK named `<app_name>.apk` in the root directory (no `app-debug.apk` in root). Do not run `gradle testDebugUnitTest`.

## Core Features & Bug Fixes
- **Floating Orb Button**: Must reliably appear and stay visible when the floating orb setting is ON and required permissions/Shizuku permissions are granted; attaches overlay on permission grant and keeps the disconnected/repair orb visible when Accessibility Bridge is not yet connected.
- **Shizuku One-Tap Setup**: Automatic one-tap button-pressing setup via Shizuku that seamlessly grants the complete set of necessary permissions (microphone, notifications, restricted settings, overlay, MIUI background popup op 10021, secure settings, accessibility service with safe non-destructive force-rebind, battery/doze whitelist, background execution, auto-revoke ignore, clipboard read).
- **UI Copy & Spelling**: Use correct "Shizuku" spelling across all app menus, headers, cards, and identifiers.

## Permissions & Shizuku Integration
- `ShizukuManager` executes complete one-tap setup and verifies core runtime/appops permissions (`RECORD_AUDIO`, `Settings.canDrawOverlays`, `TextBridgeStateHolder.isConnected`) before reporting success.
- Accessibility enable command strips/re-appends only Kotha's component safely without clobbering third-party accessibility services.
- Fallback runtime permission requests (`RECORD_AUDIO`, `POST_NOTIFICATIONS`) run automatically when starting the orb without Shizuku.

## Coding Structure
- `com.example.MainActivity`: Lifecycle resume checks and one-tap Shizuku auto-setup coordination.
- `com.example.util.ShizukuManager`: Shizuku binding, permission granting, self-healing, and shell command execution.
- `com.example.overlay.*`: Floating orb view (`OrbFloatingView`), window manager (`OrbWindowManager`), and ancillary overlays.
- `com.example.engine.*`: Dictation foreground service (`DictationService` with live `isRunningState` and safe microphone FGS start) and speech engine manager.
- `com.example.bridge.*`: Accessibility service (`TextBridgeService` with immediate/delayed IME & focus detection), `TextBridgeStateHolder`, and text insertion.
- `com.example.settings.*`: Settings model (`KothaSettings`) and persisted preferences repository (`SettingsRepository`).
- `com.example.ui.screens.*`: `HomeScreen`, `StationsScreens` (`Station10Shizuku`), `OnboardingScreen`, `TestPadScreen`.
