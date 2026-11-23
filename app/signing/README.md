# Persistent Debug Signing Key

## Why `persistent-debug.keystore.base64` Exists

When this project is exported to GitHub and imported into another Google AI Studio account, AI Studio preserves `applicationId` (`com.aistudio.kotha.dictate`) but may regenerate the root `debug.keystore.base64` for the new Google account. Installing a debug APK signed with a different certificate over an existing installation fails with an `INSTALL_FAILED_UPDATE_INCOMPATIBLE` signature conflict.

Pinning the Base64-encoded keystore in `app/signing/persistent-debug.keystore.base64` and syncing it to `${rootDir}/debug.keystore` and `${rootDir}/debug.keystore.base64` before builds ensures the debug signing certificate stays identical across accounts and rebuilds.

## Restoring an Older Account's Key

If you need to upgrade over an existing on-device install signed by an older account without uninstalling first:

1. Copy the Base64 text from that older account's `debug.keystore.base64`.
2. Paste it into `app/signing/persistent-debug.keystore.base64` (replacing the existing contents).
3. Rebuild the app (`assembleDebug` or run `:app:syncPersistentKeystore`).
