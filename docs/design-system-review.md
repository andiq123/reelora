# Shared TV design review

## Critique

- Bright accent borders appeared on buttons, cards, badges, fields, and menus at once, weakening focus hierarchy.
- App options and settings used different surface treatments; this made related interactions feel inconsistent.
- Secondary copy was often very dim. Enlarged text exposed the importance of reserved space and scrollable content.
- A trash icon represented hiding apps, even though hiding is reversible.
- Wrapped movie detail titles lacked an explicit line height and could overlap; the title now uses 44 sp line height for 38 sp text.
- Movie details combined a busy backdrop with a separate bottom overlay, producing a hard horizontal transition behind text.

## Shared design

`DesignTokens.kt` owns the translucent graphite surfaces, restrained ice-blue accent, warm release-date accent, high-contrast soft-white focus state, spacing, and control/dialog radii. Existing TV controls retain their focus, press, disabled, and accessibility behavior.

Shared `ActionButton` has three roles: capsule-shaped primary and secondary actions, plus leading-aligned navigation rows. Primary actions use soft white; secondary actions use a translucent fill and fine border. Focus brightens the fill and enlarges actions by 4%, while navigation stays still to keep the sidebar aligned. Presses contract briefly; disabled controls are explicitly dimmed. Selected navigation rows retain a neutral fill and small indicator when focus leaves, with selected accessibility semantics.

Explore uses the primary capsule with a small trailing chevron instead of the filled information icon. Save, Use location, and Play trailer reuse the primary role. Dialog actions, search, settings controls, and recovery actions share the secondary role. The same palette and 22 dp control corners carry through app-menu rows. Text fields use capsules and a clear white focus ring; media artwork retains its outer focus ring. Header dismiss actions reuse the same button with a 44 dp circular icon treatment and a translated accessible label.

Dialogs share a 36 dp outer shape, 32 dp padding, one static translucent gradient, and a fine rim. The app-action menu floats at the right with a short horizontal entrance; its actions scroll at larger text sizes. Other dialogs keep their established centered layouts. Settings uses a quiet grouped category rail and update footer instead of hard dividers and repeated category headings. Movie details use the shared geometry and a softer continuous artwork scrim. Badges use compact dark capsules without bright outlines, including provider availability. Secondary copy uses a consistent lighter gray. The local hide icon depicts an eye with a slash. Dialog headers share explicit line heights and consistent spacing.

Home's foreground controls fade out while dialogs are open, leaving the existing artwork in place. This prevents text and app icons from showing through translucent panels. Their layout, focus targets, row state, and cached artwork stay mounted; closing returns the controls with the shared short fade. The same treatment applies to movie and apps-only Home. It adds no screenshot capture, live blur, duplicate backdrop, or network request.

The reference direction is Apple's floating app shelf and artwork-first Home. Reelora's dock uses one translucent charcoal fill, a static low-contrast vertical sheen, and a fine rim; the hero scrims are lighter away from the text. This approximates depth using ordinary Compose drawing, without live blur or refraction. Artwork stays visible while text and focus remain readable. See Apple's [focus guidance](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/) and [materials guidance](https://developer.apple.com/design/human-interface-guidelines/materials).

No blur, perpetual animation, downloaded icon set, or dependency was added. The existing short entrance transitions are retained. The football/weather colors keep their informational meaning; app artwork and movie imagery retain their original colors.

## Verification

Android TV emulator visual review covers settings, app options, rename, hidden apps, search, location selection, and movie details, including 1.3× text and apps-only Home. Checks include remote focus, icon-only accessible labels, cancel/back behavior, and visibility of the last app-menu action. Temporary text size and Home-mode changes are restored. Temporary screenshots were removed after review during the requested release cleanup. Unit tests, debug lint, and a minified release build verify the shared-component changes. Version 0.24.13 (69) was installed with app data preserved on the Amazon AFTKRT Fire TV Stick (Android 11/API 30). Its settings panel was visually checked on-device, Home return was verified, and the AndroidRuntime log contained no crash. These checks do not establish frame-rate guarantees.

## Ownership and reuse

| File | Responsibility |
| --- | --- |
| `DesignTokens.kt` | Palette, focus colors, shared geometry, spacing, and entrance/placement timing. |
| `TvControls.kt` | Shared buttons, action rows, dialog headers, and compact badges; typography and interactive states live here. |
| `ArtworkBackdrop.kt` | Bounded retained artwork/scrim layer shared by Home and movie details; dynamic controls stay outside. |
| `AppIcons.kt` | Small local semantic icons absent from the existing icon set. |
| `UiStrings.kt` | Shared language context, translations, and formatting fallbacks. |
| `MainActivity.kt` | Screen composition and existing app interaction/state wiring. |

New screens should use these controls and tokens rather than copying their styling. Platform TV focus handling and Coil image loading remain the existing implementations; no parallel UI framework, state layer, or image cache is introduced. Screen-specific layout and artwork treatments remain local to each screen.
