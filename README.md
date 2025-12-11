# 📤 GitHub Uploader

<p align="center">
  <img src="https://img.shields.io/badge/Android-21+-green.svg" alt="API">
  <img src="https://img.shields.io/badge/Kotlin-1.8.10-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License">
</p>

Android application for uploading files and folders to GitHub directly from your mobile device.

## ✨ Features

- 📁 **Folder Selection** — upload entire directories while preserving structure
- 📄 **File Selection** — upload single or multiple files at once
- 🔄 **Base64 Encoding** — automatic file conversion for GitHub API
- 🌐 **WebView Interface** — modern web interface inside the app
- 📱 **Android 5.0+ Support** — works on most devices

## 📋 Requirements

- Android 5.0 (API 21) or higher
- Android Studio Arctic Fox or newer
- JDK 11
- Gradle 8.0+

## 🚀 Installation

### Clone the Repository

```bash
git clone https://github.com/itachicoders/GitHubUploader.git
cd GitHubUploader
```

### Build the Project

```bash
./gradlew assembleDebug
```

APK file will be located in `app/build/outputs/apk/debug/`

### Install on Device

```bash
./gradlew installDebug
```

## 🏗️ Project Structure

```
GitHubUploader/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── index.html          # Web interface
│   │   ├── java/q/z/en/
│   │   │   └── MainActivity.kt     # Main activity
│   │   ├── res/
│   │   │   ├── drawable/           # Icons and graphics
│   │   │   ├── layout/             # XML layouts
│   │   │   └── values/             # Resources (strings, colors)
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 🔧 Architecture

### WebView Bridge

The app uses a JavaScript Bridge to connect native Android code with the web interface:

```kotlin
// Call from JavaScript
window.AndroidFileHelper.pickFolder()  // Select folder
window.AndroidFileHelper.pickFiles()   // Select files
```

### File Processing

```kotlin
// Receive files in JavaScript
window.receiveAndroidFiles = function(filesData) {
    // filesData - array of objects with file information
    // { name, path, size, content (base64), type }
}
```

## 📱 Permissions

The app requests the following permissions:

| Permission | Description |
|------------|-------------|
| `INTERNET` | Internet access to upload to GitHub |
| `READ_EXTERNAL_STORAGE` | Read files (Android < 13) |
| `READ_MEDIA_IMAGES` | Read images (Android 13+) |
| `READ_MEDIA_VIDEO` | Read videos (Android 13+) |
| `READ_MEDIA_AUDIO` | Read audio files (Android 13+) |

## 🎨 Customization

### Changing Theme

Edit `app/src/main/res/values/colors.xml`:

```xml
<color name="purple_500">#7C3AED</color>
<color name="purple_700">#5B21B6</color>
<color name="gray_900">#111827</color>
```

### Web Interface

Modify `app/src/main/assets/index.html` to customize the UI.

## 🔒 Security

- GitHub tokens are stored locally in WebView localStorage
- The app does not send data to third-party servers
- All requests go directly to GitHub API

## 📝 Usage

1. Open the app
2. Enter your GitHub Personal Access Token
3. Select a repository
4. Tap "Select Folder" or "Select Files"
5. Confirm the upload

## 🛠️ Development

### Run in Debug Mode

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep -E "(WebView|GitHubUploader)"
```

### View WebView Logs

```bash
adb logcat | grep "WebView"
```

## 📦 Dependencies

```gradle
dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.webkit:webkit:1.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 👤 Author

**itachicoders**

- GitHub: [@itachicoders](https://github.com/itachicoders)

---

<p align="center">
  ⭐ Star this repo if you find it useful!
</p>
