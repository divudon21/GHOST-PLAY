# Ghost Play — technical changelog

Release notes on GitHub are generated from the commits between two tags
(`.github/scripts/generate_release_notes.py`), so this file holds the curated
summary of what each build actually changes. The app shows the matching
section in **Settings → Updates** and in the "update available" dialog.

## 3.15

### Playback engine
- AVFoundation-style container fallback: an unplayable container no longer ends
  with a black screen — the failure surface reports the extractor, codec and URI
  that failed, and playback can be retried or handed to another app.

### Interface
- New music mini player: Material 3 Expressive surface (dynamic container
  colour, spring press morph, animated album art), seekable wavy progress line,
  swipe-up / tap to open the full player, swipe-down to dismiss.

### Updater
- Release notes are generated per tag from the commit range and grouped into
  technical sections (playback, network, interface, updater, tooling).
- The in-app updater now parses those notes: the Updates screen and the update
  dialog show the changelog of the offered build instead of only its version.

## 3.14

### Streaming & network
- Added `media3-datasource-rtmp` (rtmp://), `media3-exoplayer-smoothstreaming`
  (.ism/ISMV) and `media3-exoplayer-midi` (.mid) on the Media3 1.11.1 line.
- All http(s) playback reads go through `OkHttpDataSource` (HTTP/2, gzip,
  redirect/TLS handling); local file, content:// and asset reads are unchanged.

### Interface
- Home URL box rebuilt on Material 3 Expressive motion: morphing top-bar
  toggle (corner radius, colour, press scale, haptic), focus-reactive field with
  animated elevation and outline, spring clear/paste/play surfaces and a
  supported-protocol chip row.

## 3.13

### Playback engine
- Media3 1.11.1 (`exoplayer`, `ui`, `hls`, `dash`, `rtsp`, `session`) with
  `nextlib-media3ext` FFmpeg decoders: AV1 (dav1d), H.264, HEVC, VP8, VP9 and
  AAC/AC3/E-AC3/DTS/TrueHD/FLAC/ALAC/Opus/Vorbis, plus decoder fallback.
- Native subtitle rendering: libass for ASS/SSA, FFmpeg for PGS, VobSub and DVB;
  Matroska attached fonts are extracted.

### Updater
- `MediaSession.sessionCompatToken` (removed in Media3 1.11) replaced with
  `MediaStyleNotificationHelper` for the media notification.
