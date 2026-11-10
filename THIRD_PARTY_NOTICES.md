# Third-party notices

Ghost Play vendors the navigation layer from **Convx** so the bottom navigation bar
(shape, glass selector, fold/collapse motion and scroll behaviour) is the original
implementation instead of a re-drawn approximation.

## Convx

- Source: https://github.com/cosmictaserdev-creator/Convx
- Copyright (C) 2026 Convx Project
- License: GPL-3.0 (see the header of each vendored file)
- Vendored files: `app/src/main/java/com/ghost/video/navigation/convx/**`
  (`floatingtabbar`, `shapes`, `backdrop` and the small `compat` bridges).
- Changes made while vendoring:
  - package rename to `com.ghost.video.navigation.convx.*`;
  - the Chinese/Convx-only symbols the bar reads (`AppTheme`, `PlayerMorph`,
    `glassUsesDarkAppearance`, `lastNavWasPop`, `elasticStretchLayerBlock`) are
    bridged onto Ghost's Material theme in `compat/ConvxCompat.kt`;
  - `FoldingTabBar` gained a `detachedTabEnabled` flag, because Ghost has no
    search/standalone destination and the circle Convx folds out for it would be
    an empty ring here;
  - `FloatingTabBar.kt`'s scroll connection, inline behaviours and tab scope were
    extracted verbatim into `FloatingTabBarScroll.kt`; the rest of that file (the
    older, non-folding bar) is not used by Ghost.

## NextPlayer (settings list UI design)

- Source: https://github.com/anilbeesetti/nextplayer
- Copyright (C) Anil Beesetti
- License: GPL-3.0
- The Settings list UI — top app bar with a tonal back button and title,
  followed by segmented card rows (leading icon, bold title, smaller
  description, no trailing chevron) — is adapted from NextPlayer's
  `SettingsScreen` / `PreferenceItem` design. Ghost Play's implementation
  (`ui/screens/SettingsScreen.kt`) is written against Ghost's own theme and
  navigation; no NextPlayer source files are vendored verbatim.

## FloatingTabBar v1.0.1 (compose-floating-tab-bar)

- Source: https://github.com/elyesmansour/compose-floating-tab-bar (as vendored by Convx)
- Copyright: Elyes Mansour — Apache License 2.0

## Backdrop / LiquidBottomTabs

- Adapted from Kyant0/AndroidLiquidGlass (Apache-2.0), as vendored by Convx.
