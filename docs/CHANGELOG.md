# Ghost Play — technical changelog

Release notes on GitHub are generated from the commits between two tags
(`.github/scripts/generate_release_notes.py`), so this file holds the curated
summary of what each build actually changes. The app shows the matching
section in **Settings → Updates** and in the "update available" dialog.

## 3.28 (beta)

### Player — sheets instead of dialogs
- **Audio sync and pitch is a Material 3 modal bottom sheet.** It can be dragged
  down and dismissed with a swipe (and stops at half height on the way), so the
  picture stays visible while the two values are judged by ear. Each control is
  now a value, a −/+ stepper and a slider: the slider for a rough move, the
  steppers for the one tick that matters, because a swipe cannot land on exactly
  "0 ms". "Reset to default" appears only when a control is off its default.
- **Every track picker is a sheet too** — video quality, audio track, subtitles
  and aspect ratio were four copies of a fixed 460x400 dialog with a separate
  hand-rolled dark/light palette. They now share one `PlayerTrackSheet` whose
  height follows its rows, on the app's own colour scheme.
- **Selection rows are Material 3.** `RadioButton` in a selectable `Surface` with
  the tonal container and a check/empty-check glyph, instead of a custom row with
  hard-coded accent colours (`#BB86FC`, `#6200EE`).

### Player — controls
- **The mute control is a Material 3 icon that reacts.** It was an `ImageView`
  stamped into `exo_basic_controls` with a hand-drawn vector swapped on click, so
  its icon only ever knew what it had last been told and could not follow a
  volume change from anywhere else. It is now a Media3 custom layout holding a
  composable, driven by a `Player.Listener` that reads the player's volume: the
  glyph turns over between Material 3's `VolumeOff` and `VolumeUp` on every change,
  including this screen's own toggle, a gesture and a track switch.

## 3.27 (beta)

### Fixes
- **"No videos found" no longer flashes on launch.** An empty list and a list that
  has not been read yet look identical, and the home screen drew the empty state
  in the gap between being opened and MediaStore answering — every launch, for
  about a second, before the grid replaced it. The gap is real, because the query
  belongs off the main thread; what was wrong is that nothing was drawn in it.
  The screen now holds a loading state from its first frame and shows the app's
  loading indicator until the first read returns, then draws the grid or the
  empty state as the result actually is. A cached list still comes up instantly
  when returning to the tab, and a manual refresh keeps the rows on screen
  instead of clearing them.
- The music tab had the same one-frame gap between its first composition and the
  effect that asks for the first page; its page flag now starts set when there is
  nothing cached, so its empty state cannot win that frame either.

## 3.26 (beta)

### About
- **Settings → About**, ported from NextPlayer's about page: the rounded card
  whose radial gradient drifts across it, this app's own icon and name with the
  version under them, and the two buttons at the foot. The second opens Ghost
  Play's source on GitHub.
- **Settings → About → Libraries**, also NextPlayer's: one segmented row per
  third-party project with its version, its authors and a badge per licence.
  NextPlayer generates that list with `mikepenz.aboutlibraries` at build time;
  this app declares the same entries by hand instead of adding a plugin and a
  code-generated report for a screen that changes when a dependency does.
- The About row sits last in Settings, after Updates — where NextPlayer puts it.

### Fixes
- **The end of a list is covered again while music plays.** The mini player
  rides on the tab bar, so with a track playing the bottom of every list had
  one more pill over it than the 3.25 clearance allowed for: exactly the bug
  that release fixed, back by 64dp. `tabBarClearance()` now adds the pill's
  height while it is up, read from a composition local that MainActivity
  provides — it is the only scope that knows, because playback outlives any one
  screen.

### Interface
- **Home's header is the app's name alone.** The mark beside it was the previous
  app's icon; it is gone rather than swapped, since the launcher icon now says
  what the app is.

## 3.25 (beta)

### Interface
- **New app icon.** The launcher icon, its round variant and the adaptive-icon
  layers were replaced with the Swiity icon set: five densities, foreground and
  background kept as separate layers so every launcher shape masks it correctly.
  The adaptive-icon XML is unchanged — the pack ships the same layer names.

### Fixes
- **Settings could not be scrolled to the end.** The last rows sat behind the
  floating tab bar: the bar is drawn over the pages rather than laid out below
  them (that is what keeps the mini player free of a band above and below it),
  and nothing reserved the strip it covers, so "Updates" — the last row —
  stopped half under it and, on a page whose content only just overflowed, could
  not be brought up at all. Every scrolling tab page now ends with
  `tabBarClearance()`: the bar's own 80dp plus the measured navigation-bar inset,
  so the last row scrolls clear of the bar on gesture navigation, on three-button
  navigation and on a device with no inset. Applied to Settings and its nine
  sub-pages, to the home grid and list, to search results and to the music list.

### Release
- Published as a pre-release, and tagged `v3.25-beta`. Release notes are now
  diffed against the last **stable** tag rather than the previous beta, so a beta
  lists everything since the stable line instead of repeating the last beta's
  work; a pre-release also titles itself "beta".

## 3.24 (beta)

### Interface
- Every expanding preference is now the same row. **Subtitle font**, **Decoder**
  and **Player screen orientation** join **Preferred audio language**: one
  `ExpandingPreferenceItem`, with a chevron that turns over while its list is
  open, and the options opening directly beneath the row that owns them rather
  than at the foot of the page. Extracted from the audio-language row, which is
  where the shape was first got right, so there is one implementation instead of
  four.
- This build is published as a **pre-release**: tagged `v3.24-beta`, marked
  pre-release on GitHub, and therefore labelled `Beta` in the updater and offered
  only to people who have turned beta builds on.

## 3.23

### Fixed
- **The blank moment when the full player opened or closed is gone**, and the
  cause was structural: the player was a navigation route, so the page it was
  opened from had to leave before it could arrive and there was always a frame
  with neither of the two on screen — 100ms of it, then a whole transition
  afterwards. It is now a modal sheet drawn over the app by `MainApp`, which is
  how BitChord holds its own player: the page stays composed and painted
  underneath for the entire arrival, so there is no frame that can be empty.
  The sheet rises from the bottom edge on BitChord's spring (`SLIDE_STIFFNESS`
  195, critically damped) and takes itself off on the same spring when it is
  dragged or dismissed, so nothing animates after the content has gone.
- **Settings scrolls again.** `Modifier.tabSwipe` claimed the pointer at the
  touch slop for any drag whose horizontal travel merely matched its vertical
  travel, which a slow vertical scroll does — so the page under it stopped
  moving. The swipe now waits until the drag is 1.5x further sideways than up or
  down before it takes the gesture, and a gesture decided vertical is never
  stolen later; a drag that something under the finger already consumed is left
  alone. The list's scroll state is also hoisted out of the modifier chain, so a
  recomposition cannot replace it mid-scroll.

## 3.22

### Interface
- **Preferred audio language** opens its picker directly beneath its own row,
  between it and the switch below, instead of appearing under the last switch in
  the list. The row also carries a chevron that turns over while the list is
  open. The picker still expands in place — no dialog — and the switches keep
  their own grouped card below it.
- The full player's transport now draws **BitChord's own glyphs**, copied from
  `sharedUi/src/commonMain/composeResources/drawable/ic_player_play.xml`,
  `ic_player_pause.xml`, `ic_player_next.xml` and `ic_player_previous.xml`
  (rounded-triangle play, 40-unit-radius pause bars, the double-chevron skip
  pair), at BitChord's sizes and its 0.85 vertical squash on the skip glyphs. The
  play/pause pair crossfades over 180ms as BitChord's does, instead of swapping
  shape in one frame.

### Fixed
- Opening the full player from the mini player no longer blanks the screen for
  a moment. The route's exit faded out in 200ms while its enter took 300ms, so
  for the difference the page was gone and the player had not arrived; the two
  are now the same length and overlap, the player rising a sixth of its height
  behind its own fade. Coming back is the mirror of the same thing.
- The player's playhead, timestamps and sleeve are correct on the opening frame
  rather than a poll later: the position is read when the screen is created, not
  only by the 500ms poll, and the sleeve is seeded from the artwork cache the
  mini player has already filled.

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
  player opens, while a downward drag of the same distance puts the bar away.
  `PULL_BIAS` 1.5 keeps an arcing thumb from being read as a sideways swipe.
- The bar and the tab bar are now drawn over the pages instead of inside a
  Scaffold bottom slot, and the bar is placed against the tab bar's own measured
  top — measured through the bar, since the wrapper around it does not contain
  the bar's own height or its navigation-bar inset. Nothing reserves a strip: no
  container colour above, below or beside either pill, and no gap between the
  bottom of the page and the pill floating over it.

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
