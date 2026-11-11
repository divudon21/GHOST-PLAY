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

## NextPlayer (settings UI, ported)

- Source: https://github.com/anilbeesetti/nextplayer
- Copyright (C) Anil Beesetti
- License: GPL-3.0
- Ghost Play's Settings screen is a port of NextPlayer's settings UI:
  - `ui/screens/SettingsScreen.kt` — from NextPlayer's `SettingsScreen`
    (top app bar + segmented preference list on surfaceContainer);
  - `ui/components/NextSegmentedListItem.kt` — from NextPlayer's
    `ListItemComponent.kt` (`NextSegmentedListItem`, `ListSectionTitle`);
  - `ui/components/PreferenceItem.kt` — from NextPlayer's `PreferenceItem.kt`
    and `ClickablePreferenceItem.kt`;
  - `ui/components/NextTopAppBar.kt` — from NextPlayer's `TopAppBar.kt`;
  - `ui/components/NextIcons.kt` — from NextPlayer's `NextIcons.kt`.
  Package names were changed to `com.ghost.video.*`, Ghost's own categories
  and navigation callbacks are used instead of NextPlayer's, and the
  Android-TV focus modifiers (no-op on touch devices) were omitted. The
  Material3 version was aligned to NextPlayer's (1.5.0-alpha15) so the
  Expressive `SegmentedListItem` APIs compile unchanged.

## FloatingTabBar v1.0.1 (compose-floating-tab-bar)

- Source: https://github.com/elyesmansour/compose-floating-tab-bar (as vendored by Convx)
- Copyright: Elyes Mansour — Apache License 2.0

## Backdrop / LiquidBottomTabs

- Adapted from Kyant0/AndroidLiquidGlass (Apache-2.0), as vendored by Convx.

## Lato

- Source: https://github.com/google/fonts/tree/main/ofl/lato
- Copyright (C) 2010-2014 Łukasz Dziedzic
- License: SIL Open Font License 1.1 (`assets/licenses/lato-OFL.txt`)
- Vendored files: `app/src/main/res/font/lato_regular.ttf`,
  `app/src/main/res/font/lato_bold.ttf` (used by the Appearance > App text
  option, whose font list follows DownList's appearance settings).
