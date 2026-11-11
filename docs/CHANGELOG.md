# Ghost Play — technical changelog

Release notes on GitHub are generated from the commits between two tags
(`.github/scripts/generate_release_notes.py`), so this file holds the curated
summary of what each build actually changes. The app shows the matching
section in **Settings → Updates** and in the "update available" dialog.

## 3.16

### Updater
- Release notes are now generated from the curated technical changelog
  (`docs/CHANGELOG.md`, section of the released version) instead of a raw commit
  list, so the GitHub release describes real changes rather than README and
  asset commits. The commit-grouped generator remains as the fallback for
  versions without a curated entry, and now ignores documentation-only commits.

## 3.15

### Playback engine
- No player or decoder changes in this release; the Media3 1.11.1 stack, FFmpeg
  software decoders and native subtitle rendering from 3.13 are unchanged.

### Interface
- New music mini player (`MiniAudioPlayer`): Material 3 Expressive spring
  motion, album art with playing overlay, spring press-scale transport controls,
  animated progress line, drag-follow gesture — swipe up or tap to open the full
  player, swipe down to dismiss.
- Album art is decoded once per track through a shared `MusicArtworkLoader`
  memoised in `ThumbnailCache`.

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
