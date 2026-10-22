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

Shared actions center their icon-and-label group within the available width; icon-only actions also reserve their full inner height. Shared `ActionButton` has three roles: capsule-shaped primary and secondary actions, plus leading-aligned navigation rows. Primary actions use soft white; secondary actions use a translucent fill and fine border. Focus brightens the fill and enlarges actions by 4%, while navigation stays still to keep the sidebar aligned. Presses contract briefly; disabled controls are explicitly dimmed. Selected navigation rows retain a neutral fill and small indicator when focus leaves, with selected accessibility semantics.

Explore uses the primary capsule with a small trailing chevron instead of the filled information icon. Save, Use location, and Play trailer reuse the primary role. Dialog actions, search, settings controls, and recovery actions share the secondary role. The same palette and 22 dp control corners carry through app-menu rows. Text fields use capsules and a clear white focus ring; media artwork retains its outer focus ring. Header dismiss actions reuse the same button with a 44 dp circular icon treatment and a translated accessible label.

Dialogs share a 36 dp outer shape, one static translucent gradient, and a fine rim. The 480 dp app-action menu floats at the right with a short horizontal entrance; its descriptions wrap and its actions scroll at larger text sizes. Settings uses a 580 dp right-side panel, 24 dp padding, a scrollable category rail, and a stable update footer. The selected category appears in the header; repeated content headings are omitted. Other dialogs keep their established centered layouts and 32 dp padding. Movie details use the shared geometry and a softer continuous artwork scrim. Badges use compact dark capsules without bright outlines, including provider availability. Secondary copy uses a consistent lighter gray. The local hide icon depicts an eye with a slash. Dialog headers share explicit line heights and consistent spacing.

Home's foreground controls fade out while dialogs are open, leaving the existing artwork in place. This prevents text and app icons from showing through translucent panels. Their layout, focus targets, row state, and cached artwork stay mounted; closing returns the controls with the shared short fade. The same treatment applies to movie and apps-only Home. It adds no screenshot capture, live blur, duplicate backdrop, or network request.

Home also consumes remote input while covered by a dialog, preventing presses during window creation from navigating the hidden shelves. Category selection updates directly from focus events. The native-settings check waits for the menu to appear and locates the System category from its current selection, rather than assuming a fixed opening delay or reset category.

The reference direction is Apple's floating app shelf and artwork-first Home. Reelora's dock uses a translucent graphite fill, a static vertical sheen, and a graduated rim. App tiles are 128 × 76 dp with 18 dp corners and wider spacing; the shelf reserves label space using the system text scale. The hero uses larger 40/44 sp typography, neutral metadata, and lighter scrims away from the text. This approximates depth using ordinary Compose drawing, without live blur or refraction. Artwork stays visible while text and focus remain readable. See Apple's [focus guidance](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/) and [materials guidance](https://developer.apple.com/design/human-interface-guidelines/materials).

Dock tiles use one interruptible spring for an 8% focus enlargement and a separate damped 2 dp lift. Presses contract to 97%; outline focus keeps the tile stationary. These values are read inside graphics layers, preserving lazy-row measurements. App focus rims and action colors blend over 180 ms. Movie cards retain native TV focus animation. Shared entrances take 240 ms, browse transitions 360 ms, and dialogs combine their existing directional entrance with a subtle 97.5% to 100% scale.

Hero artwork waits for a successful Coil load before dissolving over 700 ms. The previous successful painter remains underneath during loading and failure, and is released when the replacement transition completes. Only a transition draws two images; steady artwork uses one. No additional image request, cache, perpetual animation, downloaded icon set, or dependency was added. The football/weather colors keep their informational meaning; app artwork and movie imagery retain their original colors.

## Research and decisions

The broader menu refinement follows primary Apple and Android guidance. These are adaptations to an Android TV launcher, rather than implementations of Apple's platform-specific materials or system controls.

| Area | Decision and source |
| --- | --- |
| TV composition | Keep artwork full-screen and put settings on the right, within the existing screen insets. Preserve space between focusable items. [Apple tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos/), [Android TV layouts](https://developer.android.com/training/tv/playback/compose/layouts). |
| Focus | Use native TV button/card feedback in menus. Right enters the selected category's first setting; Left returns to that category. Category selection remains visible after focus moves. Settings and app menus use Compose focus restoration with explicit initial targets. [Apple focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/), [Android focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system), [Compose focus restoration](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-restoration). |
| Menu organization | Keep four stable categories and one shared label/value row. Reserve chevrons for navigation; direct value changes have no disclosure indicator. Use the existing native TV switch without a second focus target; the whole row exposes the translated On/Off state to accessibility. Only show trailer timing while ambient trailers are enabled. [Apple menus](https://developer.apple.com/design/human-interface-guidelines/menus/). |
| Materials | Apply the translucent treatment to controls and panels, retaining ordinary artwork for content. Use a more opaque panel gradient to keep text readable over detailed imagery without requiring live blur. [Apple materials](https://developer.apple.com/design/human-interface-guidelines/materials/). |
| Motion | Keep entrances short, directional, and finite, with reversible transitions. Menus accept focus as soon as they compose, while the entrance continues; the held-OK gate still requires a fresh press. Text-entry dialogs retain delayed focus so the native keyboard opens after the entrance. Reuse the existing Compose animations and Android duration scale rather than adding a separate animation engine. [Apple motion](https://developer.apple.com/design/human-interface-guidelines/motion/). |
| Readability | Allow menu descriptions to wrap, scroll both settings columns independently, and let detail metadata grow with text. Raise badge and cast text to 12 sp; scale the cast viewport with text size. Search Clear remains labeled for accessibility while using a compact icon button. [Android TV accessibility](https://developer.android.com/training/tv/accessibility). |
| System integration | Device settings and Default home continue opening the real Android screens. Launcher options retain their existing saved values and callbacks. |

## Smart-feature and alignment audit (October 22)

| Finding | Change |
| --- | --- |
| A 60 dp settings background contained a shorter inner row, leaving its content above the visual center. Full-width action content also remained at the leading edge. | Apply minimum height to the inner settings row, center ordinary action groups, and explicitly size compact icon content. Keep navigation leading-aligned. |
| Categories had no common icon grid; disclosure arrows, values, and toggles occupied different trailing positions. Focus scaling expanded settings outside their grid. | Reuse existing icons in the category rail, reserve a centered 40 dp accessory slot, align values at the end, and keep focused settings stationary. All four categories fit at normal text size and scroll at larger sizes. |
| A custom Canvas reimplemented switch drawing and animation. | Use the existing AndroidX TV switch with no independent input target. Remove duplicate visual On/Off labels while retaining switch state and translated accessibility announcements on the row. |
| Editing a selected location to fewer than two characters retained the old save selection. Old suggestions remained usable during another query. Failures appeared as no results. | Clear selection and suggestions immediately on editing; cancel obsolete requests; distinguish failure, offline, loading, and empty results. Location suggestions scroll. |
| Apps-only movie search also triggered the full multi-category catalog load. Search could show cards from the preceding query. | Load the catalog only for movie Home. Search uses its existing endpoint, displays only results for the current query, and retries incomplete queries after reconnection. Completed results remain cached in the dialog. |
| An automatic trailer lookup could finish after remote input resumed. A lookup with no trailer did not try again until another state change. | Race the lookup against remote input, cancel it on interaction, and restart the idle period. Leaving Home, going offline, opening an overlay, or disabling theater cancels the idle operation. |
| The daily wallpaper date was captured only when apps-only Home composed. | Update its date at midnight while foregrounded and on return to Home; no background service or polling loop is added. |

The audit traced settings callbacks and saved preferences, installed-app organization, catalog/search requests, weather/location lookup, football refresh and flags, idle trailers, clock/wallpaper updates, and the update installer. Existing foreground network gates, bounded caches, native app/system launches, and APK checksum/package/signing validation remain in place. No dependency, personalization engine, or extra background service is introduced.

## Verification

The smart-feature follow-up passes 44 unit tests (including lookup cancellation, renewed idle timing, and unavailable-trailer retry), debug/release lint, and debug/minified-release builds. Final emulator captures cover the corrected grid in all four categories at 1.3× text, plus enlarged Romanian labels; category names remain readable without truncation. Remote checks cover category entry/return, switch semantics, toggle restoration, and the last app-menu action. Enlarged movie details also open with the shared action controls and return with Back. A real location lookup verifies that selecting then clearing a city removes Use location without saving. A Wi-Fi reconnect check verifies that movie search hides previous-query cards while offline and automatically loads the edited query after connectivity returns. Original text size, language, toggle settings, and Wi-Fi state are restored. Midnight rollover is source-reviewed; actual overnight behavior, production update installation, full TalkBack navigation, and physical-TV frame timing were not measured for this pass.


The October 22 refinement passes all 41 unit tests, debug and release lint, and debug and minified release builds. The Android TV API 31 emulator passes `scripts/check-tv-navigation.py`, including far-right hero/dock/movie round trips, independent row positions, anchored chrome, boundaries, and rapid direction reversals. Visual checks cover movie Home, apps-only Home, discovery, and settings at normal and 1.3× system text size; the original Home mode and text scale were restored. No AndroidRuntime crash was observed. Physical-TV frame timing was not measured for this refinement.

The broader menu review also checks all four settings categories at 1.0× and 1.3× text, category/content focus entry and return, minimal menu scrolling, switch semantics and saved-value restoration, Romanian labels, and operation with system animations disabled. App actions, search, and movie details were reviewed at 1.3× text; the last app action remains reachable. A held OK opens app actions without activating Move. Trailer timing appears when enabled and the original state is restored afterward. With Home loaded, `scripts/check-device-settings.py` verifies two native settings round trips, task isolation, and return focus. Temporary text, animation, language, and toggle settings are restored. No physical-TV frame-rate or full TalkBack audit is claimed.

Android TV emulator visual review covers settings, app options, rename, hidden apps, search, location selection, and movie details, including 1.3× text and apps-only Home. Checks include remote focus, icon-only accessible labels, cancel/back behavior, and visibility of the last app-menu action. Temporary text size and Home-mode changes are restored. Temporary screenshots were removed after review during the requested release cleanup. Unit tests, debug lint, and a minified release build verify the shared-component changes. Version 0.24.13 (69) was installed with app data preserved on the Amazon AFTKRT Fire TV Stick (Android 11/API 30). Its settings panel was visually checked on-device, Home return was verified, and the AndroidRuntime log contained no crash. These checks do not establish frame-rate guarantees.

## Release 0.24.16 (72)

The release combines the artwork, dock, menu, accessibility, alignment, and smart-feature refinements above. All 44 unit tests and both lint variants pass. The distribution check verifies package/version, non-debuggable release flags, exclusion of preview activities, R8 mapping, ART profile, and APK signature. Installation on the Android TV emulator preserves preferences, and the installed APK matches the verified artifact byte for byte. The signing certificate matches the previously published release, allowing upgrades without uninstalling.

## Ownership and reuse

| File | Responsibility |
| --- | --- |
| `DesignTokens.kt` | Palette, focus colors, shared geometry, spacing, and entrance/placement timing. |
| `TvControls.kt` | Shared buttons, action rows, dialog headers, compact badges, and dock focus motion; typography and interactive states live here. |
| `ArtworkBackdrop.kt` | Bounded retained artwork/scrim layer shared by Home and movie details, plus load-aware hero artwork transitions; dynamic controls stay outside. |
| `AppIcons.kt` | Small local semantic icons absent from the existing icon set. |
| `UiStrings.kt` | Shared language context, translations, and formatting fallbacks. |
| `MainActivity.kt` | Screen composition and existing app interaction/state wiring. |

New screens should use these controls and tokens rather than copying their styling. Platform TV focus handling and Coil image loading remain the existing implementations; no parallel UI framework, state layer, or image cache is introduced. Screen-specific layout and artwork treatments remain local to each screen.
