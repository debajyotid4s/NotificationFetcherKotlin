# 📲 NotificationFetcherKotlin

A silent, persistent Android background app built in Kotlin that captures and stores notification metadata from all apps on the device. Built as a **personal data collection tool** to gather real-world notification data for use in a future research.

---

## 📌/ Purpose

This app runs silently in the background of an Android device and collects metadata from every notification posted by any installed app. The collected data is stored locally in an encrypted database and can be exported in multiple formats (JSON, CSV, SQLite) for downstream analysis and project use.

> ⚠️ This app is intended for **personal use only** on your own device. It requires explicit user permission (Notification Access) to function and does not transmit any data over the network.

---

## ✨ Features

- 🔕 **Silent background operation** — runs without any visible interruption
- 🔒 **Survives phone lock & idle** — powered by a Foreground Service + NotificationListenerService
- 🔁 **Auto-restarts after reboot** — via a Boot Receiver
- 🧹 **Deduplication logic** — prevents duplicate entries within a 5-second window
- 📤 **Multiple export formats** — JSON, CSV, and raw `.db` file
- 📡 **No internet required** — fully offline, zero data transmission

---

## 🗂️ Data Collected

Each notification entry captures the following metadata:

| Field | Description |
|-------|-------------|
| `postedAt` | Unix timestamp (ms) when the notification was posted |
| `packageName` | App package name (e.g. `com.whatsapp`) |
| `appName` | Human-readable app name (e.g. `WhatsApp`) |
| `title` | Notification title |
| `text` | Short notification body text |
| `bigText` | Expanded notification text (if available) |
| `subText` | Sub-text below the main content (if available) |
| `category` | Notification category (e.g. `msg`, `email`, `alarm`) |
| `channelId` | Notification channel ID |
| `notificationKey` | Unique system key for this notification |
| `isOngoing` | Whether it is a persistent/ongoing notification |
| `isGroupSummary` | Whether it is a group summary (bundle header) |
| `importanceHint` | Importance level from the system RankingMap (0–5) |

---

## 🏗️ Architecture

```
com.example.notificationfetcher
│
├── MainActivity.kt                  # UI — status dashboard + export controls
├── NotificationCollectorService.kt  # Core — captures all notifications silently
├── BootReceiver.kt                  # Ensures awareness is restored after reboot
├── NotificationChannelHelper.kt     # Creates the foreground service notification channel
├── AppDatabase.kt                   # Room database singleton (SQLCipher in release)
├── NotificationDao.kt               # Database queries (insert, fetch, export, clear)
├── NotificationEntity.kt            # Data model / Room table schema
├── ExportManager.kt                 # Handles JSON, CSV, and .db export to Downloads
```

---

## 🔐 Security

| Measure | Details |
|---------|---------|
| **Backup disabled** | `allowBackup="false"` — prevents ADB data extraction |
| **Screenshot blocked** | `FLAG_SECURE` — hides app from Recents and blocks screenshots |

---

## 📤 Exporting Data

Tap the export buttons inside the app to save data to:

```
📱 Internal Storage → Downloads → NotificationExport →
    ├── notifications_YYYY-MM-DD_HH-mm-ss.json
    ├── notifications_YYYY-MM-DD_HH-mm-ss.csv
    └── notifications_YYYY-MM-DD_HH-mm-ss.db
```

| Format | Best Used With |
|--------|----------------|
| `.json` |
| `.csv` |
| `.db` |

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 36 |
| UI | XML Layouts + Material3 |
| Background | `NotificationListenerService` + Foreground Service |
| Database | Room + SQLCipher (AES-256) |
| Async | Kotlin Coroutines |
| Export | Gson (JSON) + custom CSV writer |
| Security | Android Keystore + EncryptedSharedPreferences |
| Build | Gradle Kotlin DSL + KSP |

---

## ⚙️ Setup & Build

### Prerequisites
- Android Studio Hedgehog or newer
- Android device running API 24+
- USB debugging enabled

### Clone & Build

```bash
git clone https://github.com/debajyotid4s/NotificationFetcherKotlin.git
cd NotificationFetcherKotlin
```

**Debug build** (plain SQLite — DB Inspector works in Android Studio):
```bash
./gradlew installDebug
```

**Release build** (SQLCipher encrypted — for real data collection):
```bash
./gradlew installRelease
```

### Grant Permission
1. Open the app on your device
2. Tap **"Enable Collection"**
3. Find **"Notification Fetcher"** in the list
4. Toggle it **ON**
5. Return to the app — status will show **RUNNING** ✅

---
## APP Inspection > DB Access via Android Studio is turned OFF in last version.

---

## 📁 Project Structure

```
NotificationFetcherKotlin/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/notificationfetcher/
│   │   │   ├── MainActivity.kt
│   │   │   ├── NotificationCollectorService.kt
│   │   │   ├── BootReceiver.kt
│   │   │   ├── NotificationChannelHelper.kt
│   │   │   ├── AppDatabase.kt
│   │   │   ├── NotificationDao.kt
│   │   │   ├── NotificationEntity.kt
│   │   │   ├── ExportManager.kt
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/strings.xml
│   │   │   ├── values/colors.xml
│   │   │   ├── values/themes.xml
│   │   │   └── xml/
│   │   │       ├── file_paths.xml
│   │   │       ├── network_security_config.xml
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## ⚠️ Permissions Required

| Permission | Why |
|------------|-----|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Core permission to read notifications |
| `FOREGROUND_SERVICE` | Keep service alive during idle/Doze mode |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required on Android 14+ |
| `RECEIVE_BOOT_COMPLETED` | Restore awareness after device reboot |

---

## 🚧 Known Limitations

- **Battery optimization** — Some OEM devices (Xiaomi, Huawei, OnePlus) aggressively kill background services. Manual exemption from battery optimization may be required.
- **Notification content** — Some apps use encrypted or redacted notification content. In those cases, `text` and `title` may be null or generic.
- **Android 14+ restrictions** — Stricter foreground service rules apply. The `specialUse` type is declared as required.

---

## 👤 Author

**debajyotid4s**
- GitHub: [@debajyotid4s](https://github.com/debajyotid4s)

---

*This app is for personal research purposes only. No data is shared, uploaded, or transmitted externally.*
