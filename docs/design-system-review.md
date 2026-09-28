# Shared TV design review

## Critique

- Bright accent borders appeared on buttons, cards, badges, fields, and menus at once, weakening focus hierarchy.
- App options and settings used different surface treatments; this made related interactions feel inconsistent.
- Secondary copy was often very dim. Enlarged text exposed the importance of reserved space and scrollable content.
- A trash icon represented hiding apps, even though hiding is reversible.
- Wrapped movie detail titles lacked an explicit line height and could overlap; the title now uses 44 sp line height for 38 sp text.
- Movie details combined a busy backdrop with a separate bottom overlay, producing a hard horizontal transition behind text.

## Shared design

`DesignTokens.kt` owns the graphite surfaces, restrained ice-blue accent, warm release-date accent, high-contrast soft-white focus state, spacing, and control/dialog radii. Existing TV controls retain their focus, press, disabled, and accessibility behavior.

Buttons and app-menu actions share solid surfaces and restrained borders. Focused buttons switch to dark text on soft white; media artwork retains an outer focus ring. Selected settings categories remain distinct after focus leaves them. Button focus enlargement is reduced to 2%.

Badges use compact dark fills without bright outlines. Secondary copy uses a consistent lighter gray. The local hide icon depicts an eye with a slash. Movie details use one continuous dark gradient for readable text over artwork.

No blur, perpetual animation, downloaded icon set, or dependency was added. The existing short entrance transitions are retained. The football/weather colors keep their informational meaning; app artwork and movie imagery retain their original colors.

## Verification

Local Android TV emulator visual review covers settings, app options, search, and movie details, including 1.3× text. Screenshots are in `work/design-refresh/`. Unit tests, lint, and debug/release builds are run for the shared-component changes. Emulator review does not establish frame-rate guarantees on physical TVs.

## Ownership and reuse

| File | Responsibility |
| --- | --- |
| `DesignTokens.kt` | Palette, focus colors, shared geometry, spacing, and entrance/placement timing. |
| `TvControls.kt` | Shared buttons, action rows, dialog headers, and compact badges; typography and interactive states live here. |
| `AppIcons.kt` | Small local semantic icons absent from the existing icon set. |
| `UiStrings.kt` | Shared language context, translations, and formatting fallbacks. |
| `MainActivity.kt` | Screen composition and existing app interaction/state wiring. |

New screens should use these controls and tokens rather than copying their styling. Platform TV focus handling and Coil image loading remain the existing implementations; no parallel UI framework, state layer, or image cache is introduced. Screen-specific layout and artwork treatments remain local to each screen.
