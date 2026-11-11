# Ghost Play — technical changelog

Release notes on GitHub are generated from the commits between two tags
(`.github/scripts/generate_release_notes.py`), so this file holds the curated
summary of what each build actually changes. The app shows the matching
section in **Settings → Updates** and in the "update available" dialog.

## 3.21

### Interface
- The full audio player is rebuilt from BitChord's own player code rather than
  from a sketch of it. Ported file by file from `kushagrasinghx/BitChord`:
  - `ui/player/ArtworkMeshBackdrop.kt` → `ui/player/ArtworkMesh.kt`: the sleeve's
    own colours, averaged into a 6×6 mesh, flipped and rotated below the seam,
    resampled to 32×32, blurred with three box passes, and crossfaded over 900 ms
    when the track changes. The seam is the sleeve's bottom edge, held from there
    up, so the backdrop continues out of the artwork instead of starting behind
    it.
  - `ui/player/ThinSlider.kt` → `ui/player/ThinSlider.kt`: the hairline capsule
    with no thumb that thickens under the finger, plus the travelling sheen that
    claims the bar while a seek is being resolved.
  - `ui/player/PlayerControls.kt` → `ui/player/PlayerControls.kt`:
    `PlayerScrubber` (elapsed at the start, remaining at the end, both on the
    scrubber's own lift), `TransportRow` (SpaceAround, skip glyphs squashed to
    0.85, faded rather than hidden at the ends of a queue, spinner in the play
    target while loading) and the fading credits line.
  - `ui/graphics/FadeCurve.kt` + `FadingEdges.kt` → the same files: smootherstep
    edge fades, so a long title runs out instead of ending in an ellipsis.
  - `NowPlayingScreen.kt`'s measurements: 30dp gutter, a 560dp maximum width
    that centres itself on a tablet, the sleeve bounded by whichever axis runs
    out first, the artwork-and-credits block centred above a control deck pinned
    to the foot, and the 32dp pull-down handle strip.
- Motion comes from `app/.../player/PlayerSheetMotion.kt`: a critically damped
  spring at stiffness 195 with a 0.5px settle threshold, so the player closes on
  the same curve Apple Music's sheet does.

### Mini player
- Rebuilt on `ui/components/MiniPlayer.kt`'s figures: a 56dp pill (8dp row
  padding around a 40dp cover at 8dp corner), 12dp horizontal padding because a
  pill's ends curve away under the cover, a 40dp glyph slot with the glyph drawn
  inside it so the bar's height never depends on the glyph, a hairline cover
  border, and edge-fading title and artist lines.
- `miniPlayerGestures`: one gesture detector for both drags — sideways past 72dp
  changes track, an upward pull raises the bar at half the finger's rate and
  slows with distance (`pullLift`), then springs home at 0.65 damping as the
  player opens. `PULL_BIAS` 1.5 keeps an arcing thumb from being read as a
  sideways swipe.
- The bar and the tab bar are now drawn over the pages instead of inside a
  Scaffold bottom slot, and the bar is placed against the tab bar's own measured
  top. Nothing reserves a strip: no container colour above, below or beside
  either pill.

## 3.20

### Interface
- Mini player: the *Swipe up for full player* hint row is gone, and the host
  `Scaffold` now draws a transparent container so the strip above and below the
  bar shows the page instead of a container-coloured band. The gesture itself is
  unchanged (tap or swipe up opens the player, swipe down dismisses).

## 3.19

### Interface
- Audio settings now holds NextPlayer's complete audio preference set, copied
  from `AudioPreferencesScreen` + `AudioPreferencesViewModel`: **Preferred audio
  language**, **Require audio focus**, **Pause on disconnect**, **System volume
  panel** and **Volume boost**, with upstream titles, descriptions, icons,
  ordering and defaults. The language list comes from `AudioLocales`
  (`LocalesHelper` port: display name → ISO 639-2 code, "None" first).

### Playback engine
- Preferences are wired to the same Media3 APIs NextPlayer uses:
  `DefaultTrackSelector.setPreferredAudioLanguage`, `AudioAttributes` +
  `handleAudioFocus`, `setHandleAudioBecomingNoisy`, and the volume gesture now
  passes `FLAG_SHOW_UI` only when the system-volume-panel preference is on and a
  wired/USB/Bluetooth headset is connected (`VolumeState.isHeadsetOn` port).
- `MusicService` applies the same three preferences live to the music player, so
  audio settings are not video-only.

## 3.18

### Interface
- The link button is back in the Home top bar (`FilledTonalIconButton` with the
  app's `ic_url` drawable, stock Material 3 ripple and press state). Tapping it
  expands the network stream field with the standard
  `fadeIn + expandVertically` / `fadeOut + shrinkVertically` pair; tapping again
  collapses it and clears focus.
- `NetworkStreamCard` no longer draws its *Network stream* title and
  *Choose a video file from network and play.* description; the card is a single
  instance above the content instead of one copy per layout.

## 3.17

### Interface
- Home URL box replaced with NextPlayer's official **Network stream** card
  (`NetworkStreamCard` + `NextOutlinedTextField`, ported verbatim from
  `feature/network/.../NetworkScreen.kt`): bold title, description, shared
  outlined text field and a trailing right-aligned `Button`. The bespoke
  expressive field, chip row, morphing top-bar toggle and their helper imports
  are removed, so the only feedback is Material 3's own ripple / focused outline.
- The card renders as the first item of every Home layout (list, grid, compact,
  cinema) and above the permission / empty states, so a stream link can be
  played before local storage access is granted.
- Home top bar keeps only the Search action.

### Release notes
- The static `### APKs` list is gone from the release body; the three assets are
  attached to the release itself, so the notes now contain only the version
  header, the updater metadata and the technical changelog.

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
