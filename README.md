# 🎬 GHOST PLAY - Video & Audio Player

A powerful video and audio player for Android with advanced features.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

## ✨ Features

### 🎥 Video Player
- **Multiple Formats Support** - MP4, MKV, AVI, WebM, and more
- **Network Streaming** - Play videos from URL
- **HLS, DASH, RTSP** - Streaming protocol support
- **Subtitle Support** - Multiple subtitle tracks with selection
- **Audio Track Selection** - Switch between audio tracks
- **Quality Selection** - Choose video quality
- **Picture-in-Picture (PiP)** - Continue watching while using other apps
- **Aspect Ratio** - Multiple aspect ratio options (16:9, 19:9, 20:9, 21:9)
- **Screen Lock** - Lock screen to prevent accidental touches
- **Gestures** - Volume, brightness, zoom, and seek gestures
- **Double Tap** - Double tap to seek ±10 seconds
- **Long Press** - Long press for 2x speed
- **Playback Position** - Remembers where you left off

### 🎵 Audio Player
- **Background Playback** - Continue listening while using other apps
- **MediaStyle Notification** - Control playback from notification
- **MediaSession** - Bluetooth and Android Auto support
- **Foreground Service** - Reliable background playback

### 🎨 Customization
- **10 Color Themes** - Purple, Blue, Green, Orange, Red, Pink, Teal, Yellow, Cyan, Indigo
- **4 Theme Modes** - System, Light, Dark, AMOLED Dark

## 📱 Screenshots

| Home Screen | Video Player | Audio Screen |
|-------------|--------------|--------------|
| ![Home](screenshots/home.png) | ![Player](screenshots/player.png) | ![Audio](screenshots/audio.png) |

## 🚀 Build APK

### Option 1: GitHub Actions (Recommended)
1. Fork this repository
2. Go to **Actions** tab
3. Click on **Build APK** workflow
4. Click **Run workflow**
5. Download APK from **Artifacts**

### Option 2: Local Build
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/GHOST-PLAY.git
cd GHOST-PLAY

# Build Debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Requirements

- Android 5.0 (API 24) or higher
- Android Studio Hedgehog or later (for development)
- JDK 17

## 🛠️ Tech Stack

- **Kotlin** - Programming Language
- **Jetpack Compose** - Modern UI Toolkit
- **Material Design 3** - UI Components
- **Media3 ExoPlayer** - Video & Audio Playback
- **Coil** - Image Loading
- **DataStore** - Preferences Storage
- **Navigation Compose** - Navigation
- **Coroutines & Flow** - Async Operations

## 📦 Dependencies

```kotlin
// Media3
implementation("androidx.media3:media3-exoplayer:1.5.0")
implementation("androidx.media3:media3-ui:1.5.0")
implementation("androidx.media3:media3-session:1.5.0")

// Coil
implementation("io.coil-kt.coil3:coil-compose:3.3.0")
implementation("io.coil-kt.coil3:coil-video:3.3.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.2.0")
```

## 🔧 Customization

### Change App Name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

### Change Package Name
1. Update `namespace` and `applicationId` in `app/build.gradle.kts`
2. Refactor package in all Kotlin files
3. Update AndroidManifest.xml

### Change App Icon
Replace icons in `app/src/main/res/mipmap-*/` directories

## 📄 License

```
MIT License

Copyright (c) 2024 GHOST PLAY

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Contact

- GitHub: [@divudon21](https://github.com/divudon21)

---

⭐ If you like this project, give it a star!
