# 🎙️ Kotha (কথা) — Next-Gen Continuous Voice Dictation for Android

<p align="center">
  <img src="assets/kotha-mark.svg" width="128" height="128" alt="Kotha Logo" />
</p>

<p align="center">
  <b>Speak naturally. Write everywhere. Zero keyboard friction.</b><br>
  A floating, continuous voice-dictation companion fine-tuned for <b>Bangla (বাংলা)</b> and 50+ global languages. Injects transcribed text in real-time into <i>any</i> Android app via an intelligent Accessibility Text Bridge.
</p>

<p align="center">
  <a href="../../releases/latest">
    <img src="https://img.shields.io/badge/Download-Kotha.apk-F2542D?style=for-the-badge&logo=android&logoColor=white" alt="Download Kotha APK" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%20%7C%20Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Primary%20Language-Bangla%20(বাংলা)-E3A93B?style=flat-square" alt="Bangla" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20On--Device%20Bridge-45A596?style=flat-square" alt="Privacy" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square" alt="License" />
</p>

---

## 🌟 Why Kotha?

Typing long messages, notes, and emails in Bengali or regional scripts on standard mobile keyboards is slow, tedious, and prone to spelling mistakes. Standard voice keyboards force you to switch keyboards or automatically cut off after just 5 seconds of silence.

**Kotha changes that completely.** 

With Kotha, a sleek floating **Orb** stays on your screen across all your favorite applications (WhatsApp, Messenger, Gmail, Samsung Notes, Slack, Telegram, Notion, Chrome, and more). Tap the orb and start speaking — your words stream directly into the active text field, punctuated and formatted in real time.

---

## ⚡ Key Highlights & Capabilities

### 1. 🔄 True Continuous Dictation (No 5-Second Cutoffs)
- **Non-stop listening engine**: Speak uninterrupted for paragraphs, articles, or minutes at a time without the engine timing out.
- **Smart Seamless Recovery**: Instant loop restart within milliseconds (configurable 150ms buffer) to keep voice sessions alive indefinitely.
- **Dual Operating Modes**:
  - **Continuous Mode**: Endless speech flow with auto-silence committing.
  - **Pause-and-Restart Mode**: Utterance-by-utterance discrete sentence capture.

### 2. 🇧🇩 Specialized Bangla Natural Language Processing
- **Deep Dialect & Locale Support**: Tailored support for both `bn-BD` (Bangladesh) and `bn-IN` (West Bengal, India), plus 50+ curated international and regional languages.
- **Spoken Punctuation Translation**: Spoken punctuation words automatically turn into native Bengali typographic marks:
  | You Say (Spoken) | Kotha Injects | Symbol Name |
  | :--- | :---: | :--- |
  | `"দাঁড়ি"` or `"দাড়ি"` | `।` | Bengali Danda |
  | `"কমা"` | `,` | Comma |
  | `"প্রশ্নবোধক চিহ্ন"` / `"প্রশ্ন চিহ্ন"` | `?` | Question Mark |
  | `"বিস্ময়বোধক চিহ্ন"` / `"বিস্ময় চিহ্ন"` | `!` | Exclamation Mark |
  | `"হাইফেন"` | `-` | Hyphen |
  | `"কলন"` | `:` | Colon |
  | `"সেমিকলন"` | `;` | Semicolon |
- **Digit Conversion Engine**: Instantly converts spoken numerals to Bengali numerals (`১, ২, ৩...`) or Western Arabic numbers (`1, 2, 3...`) on the fly.
- **Smart Auto-Spacing & Typography**: Fixes punctuation spacing automatically (no orphaned punctuation or double spaces).

### 3. 🎯 Hands-Free Voice Commands
Control text flow without touching the screen:
- **`"নতুন লাইন"` / `"new line"`** ➔ Inserts a clean line break.
- **`"নতুন প্যারা"` / `"new paragraph"`** ➔ Inserts a double line break for new paragraph.
- **`"শব্দ মুছুন"` / `"delete word"`** ➔ Instantly erases the last uttered word.
- **`"আনডু"` / `"পূর্বাবস্থায় ফেরান"` / `"undo"`** ➔ Reverts the last recognized text span.
- **`"শোনা বন্ধ"` / `"stop listening"`** ➔ Pauses speech engine safely.

### 4. 🌁 Universal Text Bridge (Works with ANY App)
- **Universal Compatibility**: Injects text directly into text inputs in third-party apps without replacing your default keyboard (Gboard, Ridmik, SwiftKey).
- **Dual Injection Engine**:
  - **Accessibility Set-Text**: Directly modifies the focused input field's text tree without touching the clipboard.
  - **Clipboard Paste Fallback**: For complex WebViews or hardened text inputs.
- **Live Streaming Partials**: See your speech typed in real-time as you utter words, with zero lag.
- **Smart Selection & Cursor Tracking**: Seamlessly replaces highlighted text and preserves cursor positioning.
- **Password Guard**: Automatically refuses to activate on password, PIN, or secure input fields.

### 5. 🫧 Floating HUD: The Orb & Live Caption Bubble
- **Draggable Floating Orb**: Interactive overlay with edge-snapping physics, customizable shape (Circle, Squircle, Rounded Square, Seal).
- **Audio Visualizer Rings**: Reactive canvas rings (Radial Ticks, Solid Arc, Dotted) showing speech volume and engine state in real time.
- **Live Subtitle Preview**: Translucent floating caption bubble above or below the orb so you can see what is being transcribed without glancing away.
- **Smart Visibility Triggers**:
  - *Always Visible*
  - *Only when an editable input is focused*
  - *Auto-appear only when the software keyboard opens*
- **Aesthetic Ink Themes**: Custom accent hues including **Vermilion (সিঁদুর)**, **Ochre (গেরুয়া)**, **Verdigris (তামাটে)**, and **Indigo (নীল)**.

### 6. 🔇 Silent Audio Beep Suppression
- Standard Android speech recognition plays loud, annoying chime beeps every time it listens or restarts.
- Kotha features a built-in **Audio Beep Suppressor** that mutes the notification stream for the exact 80ms window of the chime, providing a serene, silent dictation experience.
- Built-in crash safety automatically restores volume levels if an unexpected event occurs.

### 7. 🛡️ Self-Healing Accessibility Keeper
- Aggressive OEM battery killers (MIUI/HyperOS, ColorOS, OneUI) frequently kill background accessibility services.
- Kotha includes an active **Accessibility Keeper** that monitors service health, provides 1-tap repair, and displays system diagnostic ledger entries.
- Quick Settings Tile integration: Toggle Kotha on or off directly from your Android notification drawer.

### 8. 🧪 Interactive Built-in Test Pad & Diagnostics
- Test and fine-tune dictation, punctuation, and custom regex dictionary replacements inside Kotha's native test laboratory before using it in your daily apps.
- Full JSON configuration export & import to back up, share, or transfer your setups between devices.

---

## 📱 Real-World Use Cases

- 💬 **Messaging on WhatsApp, Messenger & Telegram**: Dictate conversational Bangla messages with correct *দাঁড়ি* and commas at 150+ words per minute.
- 📝 **Writing Essays, Blogs & Long-Form Articles**: Dictate full pages of text in Google Docs, Samsung Notes, or Obsidian without arm or finger strain.
- 🚗 **Hands-Free Productivity on the Go**: Reply to emails or jot down urgent thoughts while commuting, cooking, or multitasking.
- 🩺 **Professional Dictation**: Fast note-taking for doctors, researchers, lawyers, and journalists conducting field interviews.
- ♿ **Accessibility Support**: Essential utility for users with repetitive strain injury (RSI), motor challenges, or dyslexia.

---

## 📦 How to Download & Install

### Step 1: Download `Kotha.apk`
Download the compiled APK package directly:
- **[Download Latest Kotha.apk](../../releases/latest)** (from GitHub Releases)
- Or grab `Kotha.apk` from the root directory of this repository.

### Step 2: Install on Your Device
1. Open the downloaded `Kotha.apk` on your Android device.
2. If Android displays **"Install unknown apps"**, tap **Settings** and turn on **"Allow from this source"**.
3. Tap **Install** and open **Kotha**.

### Step 3: Quick Setup (Takes 60 Seconds)
1. **Grant Microphone Permission**: For speech recognition.
2. **Enable Floating Window (Overlay)**: To show the floating Orb.
3. **Turn on Accessibility Service (`Kotha Text Bridge`)**: Allows Kotha to insert dictated text directly into your apps.
4. Tap the **Start Orb** button or use the **Quick Settings Tile** in your notification shade!

---

## 🎛️ The 9 Stations of Kotha

Inside the Kotha app, configuration is organized into 9 modular control stations:

| Station | Title | What You Can Configure |
| :---: | :--- | :--- |
| **01** | **Voice Engine** | Speech engines (Google, on-device), primary & secondary languages, offline preference, offensive word filtering. |
| **02** | **Mode & Timing** | Continuous vs Pause-and-Restart, silence detection thresholds, restart delay buffers, session length. |
| **03** | **Text Insertion** | Live partial streaming vs batch, Accessibility insertion vs clipboard paste, cursor following, live caption bubble. |
| **04** | **Floating Orb** | Dimensions, squircle/circle shapes, canvas rings, idle/active opacities, keyboard-docking, double-tap actions. |
| **05** | **Sound & Feel** | Beep suppression, soft audio cues, haptic feedback intensity, keep-screen-awake, audio ducking. |
| **06** | **Language Tools** | Auto-danda (*দাঁড়ি*), punctuation conversion, Bengali ↔ Western numerals, custom word replacement dictionary. |
| **07** | **System & Health** | Accessibility keeper watchdog, battery optimization bypass guide, MIUI/HyperOS permission ledger. |
| **08** | **Data & Profiles** | Theme switching (Dark, Paper, AMOLED), JSON profile export/import (includes all custom triggers & settings), event logs. |
| **09** | **Voice Commands** | Configure and manage 15 smart voice editing & cursor actions with unlimited custom trigger chips, per-command On/Off switches, and confirmation popups. |

---

## 🔒 Privacy & Security First

- **Zero Cloud Data Harvesting**: Kotha runs locally on your device. Your dictated text is inserted directly into your chosen app via Android's local Accessibility API.
- **Password Protection**: Kotha automatically detects secure password, PIN, and biometric fields and disables insertion to prevent sensitive data exposure.
- **No Account Required**: Ready to use out of the box with zero logins or subscriptions.

---

## 💻 Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Modern Jetpack Compose with Material Design 3
- **Audio & Speech**: Android `SpeechRecognizer` API with custom lifecycle loop & stream muter
- **Text Injection**: Android `AccessibilityService` (`AccessibilityNodeInfo` action setText / selection)
- **Window Management**: Android `WindowManager` overlay with dynamic touch dispatchers and drag physics
- **Architecture**: MVI / MVVM with Kotlin Coroutines and `StateFlow` reactive pipelines

---

## 🤝 Building From Source

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>

# 2. Grant execute permission
chmod +x gradlew

# 3. Compile the debug APK
./gradlew assembleDebug

# Output file generated at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

<p align="center">
  <b>Developed with ❤️ for the Bangla language community and typists worldwide.</b><br>
  <i>Kotha (কথা) — Voice your thoughts without boundaries.</i>
</p>
