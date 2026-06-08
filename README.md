# GHOST PLAY - Media Player

A beautiful, powerful video and audio player for Android built with Jetpack Compose and Media3.

## Optimized Version (v1.1.0-Optimized)

**This release is specifically optimized for broader device compatibility**, especially Android 11 devices where the previous version had stability issues.

### Changes Made:
- Removed Jellyfin FFmpeg decoder (caused crashes on some Android 11 devices)
- Added `requestLegacyExternalStorage="true"` for better storage access on older Android versions
- Updated version code and name
- Improved decoder fallback to default ExoPlayer decoders
- Maintained full feature set (URL playback, background audio, gestures, themes, PiP, etc.)
- Works perfectly on **Realme P4 Pro** and most other devices

### Features
- Play from URL or local files
- Advanced video player with gestures, subtitles, aspect ratio, HQ mode
- Background audio playback with notification controls
- Multiple beautiful themes (Purple, Blue, Green, AMOLED, etc.)
- Highly customizable settings (gestures, thumbnails, player behavior)
- Picture-in-Picture support
- Dark & Light themes with dynamic colors

**Tested on Realme P4 Pro. Should now work much better on Android 11 devices.**

Download the optimized APK from Releases.
