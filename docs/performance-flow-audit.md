# Launcher performance and interaction audit

Scope: startup through apps/catalog readiness, foreground/background work, artwork memory, remote navigation, dock/dialog motion, settings layout, and updater interaction. The latest pass below includes physical Fire TV frame statistics; earlier passes distinguish source/emulator checks from device measurements. These are bounded checks, not a guarantee of smoothness on every TV.

## Findings and changes

| Priority | Finding | Change |
| --- | --- | --- |
| High | Home used the first category for the hero and then dropped it from the grid. With only one category, Down had no destination. | Render every category beneath the hero and account for the hero's list position in navigation. |
| High | Programmatic focus and row focus callbacks could both scroll the vertical list, starting competing animations. | Route explicit vertical navigation through one cancellable job; focus callbacks only remember the selected column. |
| High | During validation, animating the full vertical trip delayed focus transfer: the next Right press could still operate the dock. | Explicit Up/Down navigation settles the scroll before requesting focus, without a long vertical tween. Dock bookkeeping ignores transient focus while navigation runs. |
| Medium | Discovery always waited 750 ms after apps were ready, including return-to-Home/reconnection. | Start after the existing frame yield. Package discovery still completes first; network requests remain asynchronous. This removes a fixed delay, not 750 ms of measured total startup time. |
| Medium | Settings content did not scroll, risking unreachable controls at larger text sizes. | Scroll the content pane, reset its offset on section changes, and preserve the header/footer. |
| Medium | Changing settings categories required an extra confirmation press. | Directional focus previews the section, with clear title-case labels and selected styling. |
| Low | Dock composition always began transparent, even when apps were already loaded. | Start loaded docks opaque; use a 140 ms fade and 6 dp upward slide for initial asynchronous loading. The entire shelf uses one graphics layer; no per-item stagger or per-frame layout work. |
| Low | Movie search appeared under app management despite searching films and series only. | Move it to Home settings and label it “Search movies & TV”. |
| Low | Busy update action remained visually enabled while clicks did nothing. | Disable the action during work; retain the existing status text. Let the button size to its label so enlarged text is not truncated. |

## Existing behavior retained

- Package enumeration runs on Dispatchers.IO and only repeats for package revisions; returning from another app does not enumerate again.
- Catalog data is cached for six hours, with date rollover and cancellable foreground loading. Genre labels reuse list response metadata.
- One foreground network callback drives recovery. Weather/football stop fetching while backgrounded; no new polling or service was introduced.
- Artwork memory is bounded by heap size and low-RAM status, with a bounded disk cache and memory-pressure cleanup.
- Dialogs use a short alpha/translation transition and a remote press gate. No splash delay, animated blur, perpetual shimmer, or extra animation dependency was added.
- Hero rotation pauses when hidden or covered by a dialog; clock updates once per minute.

## Limits and further measurement

Source review cannot prove smoothness on every TV. Actual cold-start time, warm-return latency, frame-time percentiles, and memory under Netflix playback pressure require controlled measurements on the target hardware. The existing `scripts/profile-startup.py` measures activity dispatch/reuse, not visible-frame completion. Use it together with frame statistics before making performance claims.

A cold hero image can still arrive after the text entrance animation. Keeping old artwork or introducing another crossfade should be judged on low-RAM hardware before adding overlapping full-screen image layers. Network latency and image availability remain external constraints.

## Original audit verification

- 34 unit tests, debug lint, debug build, and minified release build passed.
- Local Android TV emulator: repeated far-right dock/hero and movie/hero return paths passed, retaining the selected app and movie. The regression script now explicitly checks for media-card metadata so a dock action cannot be mistaken for a movie.
- Settings: directional focus switches the content without a confirmation press. Layout inspected at 720p with 1.0× and 1.3× text.
- No physical device update, Git commit, or push performed during this audit. Screenshots and raw local logs are in `work/flow-audit/`.

Motion follow-up: dialog entrances use the same 140 ms / 6 dp treatment. App insertion and hidden-app rows use 100 ms in, 140 ms placement, and 80 ms out. These transitions do not add app-launch or navigation waits. A local cold-start recording was inspected (`work/motion-audit/intro.mp4`); recording/emulator timing is not a target-TV frame-rate benchmark.

## Home row navigation (0.24.10)

Home owns vertical D-pad movement: hero → dock → movie categories. A mounted focus target holds focus while a lazy destination is being laid out; rapid vertical input updates the logical destination and cancels only the obsolete transfer. Horizontal and confirm input cannot act on an old card during that transfer. Row endpoints consume vertical input instead of falling back to spatial focus search.

Movie navigation follows the focused card’s horizontal screen position and selects the nearest fully visible card in the destination row. Each row retains its independent horizontal scroll; a far-right catalog index is never copied to another row. Partially clipped cards are avoided when a fully visible target exists. Returning to the dock preserves the app identity and visible horizontal position.

Regression coverage in `scripts/check-tv-navigation.py` includes far-right dock/hero round trips, all movie categories, shorter rows, bottom/right boundaries and bursts of opposite directions. The script requires loaded movie categories and English labels on the selected test device.

## Discovery shelves and anchored navigation (0.24.12)

The blended Discover shelf and Popular movies previously opened with the same film. Home now deduplicates by media type and ID across shelves in display order. Each remaining title stays in its original category and source order; empty shelves disappear. Films and series with the same numeric ID remain separate. This uses fetched metadata without additional requests, random shuffling, or a second cache. A category can be shorter than its fetched 40-title shortlist.

Home previously put the full-screen hero and categories in one vertical list. Even minimum-distance scrolling still moved the entire page when entering discovery. Home now keeps the backdrop and status in a fixed stage. One cancellable 260 ms reveal lifts the dock, fades the hero copy, and introduces a fixed category area below it. Subsequent presses slide only the outgoing and incoming category rows through that clipped area. The dock, backdrop, clock, and attribution remain still. Back reverses the opening motion and restores the remembered dock target. A small category counter communicates position without another control.

Incoming rows mount and receive focus near the start of the transition; Right and OK need not wait for the visual slide. Outgoing rows cannot receive focus. Rapid vertical input updates the logical destination, cancels obsolete focus handoffs, and retargets the existing animations. Each horizontal LazyRow keeps its own state and chooses its destination card by screen position. Hidden hero copy is excluded from accessibility, and the hero action cannot receive focus while browsing.

Motion reads animation values in graphics layers rather than recomposing or measuring the screen every frame. Discovery content mounts on demand; at rest only the current horizontal shelf is composed. Transitions briefly retain outgoing content. Hero rotation pauses while browsing so the backdrop stays stable. There is no vertical full-page list, animated blur, per-card entrance stagger, or extra network request. The row viewport grows with heading text size.

Category changes use plain slides, avoiding the intermediate buffers needed for row crossfades. The static backdrop and its readability scrims share one retained GPU layer only at viewport sizes up to 1920×1080 (about 8 MiB for its texture); larger viewports use normal drawing to avoid a large extra allocation. The single artwork fade uses `ModulateAlpha`, without applying it to overlapping card content. See Android's [compositing strategy guidance](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#compositing-strategy).

Home cards use slightly larger artwork, two-line titles, explicit text line heights, and more readable metadata over a deeper scrim. Existing platform TV focus scaling and outlines remain, with no blur, parallax, overlapping backdrop layers, or new dependencies. The visual direction follows Apple's guidance on [predictable focus](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/) and [keeping focused content visible](https://developer.apple.com/design/human-interface-guidelines/scroll-views), adapted to Android TV's existing components.

Unit coverage checks cross-shelf identity, category membership, empty-shelf removal, deterministic ordering, and horizontal focus selection. The navigation regression checks that entering discovery preserves the clock anchor, category changes replace a single row at the same screen position, and the dock stays fixed while browsing. It also covers remembered horizontal viewports, boundaries, and rapid reversals.

Verification uses unit tests, debug lint, a minified release build, emulator remote-navigation regression, motion recordings, and layout inspection at normal and 1.3× system text (restoring the original size afterward). Fire TV AFTKRT's UIAutomator returns a null root, so target-device checks use direct remote input and screenshots. These checks do not establish a physical-TV frame-rate guarantee.

On the AFTKRT, a repeated 13-key warm navigation sequence with picture-in-picture playback briefly paused measured a median of 69 ms before the rendering changes and 18 ms afterward; p95 changed from 85 ms to 61 ms. Playback was resumed and verified in the media session. A further sample with video playing measured median 19 ms, p90 31 ms, and p95 48 ms. The earlier playing-video sample measured median 77 ms and p95 133 ms. These are short `dumpsys gfxinfo` observations under the device's current load, not a controlled benchmark or a guarantee of 60 fps; some slow frames remain. The measured summary is retained here; temporary raw captures were removed during the requested release cleanup.

## Rendering audit and polish (0.24.14, October 15, 2026)

This pass reviews native TV focus, animated layers, artwork decoding/caching, lazy-row composition, modal rendering, foreground work, and release compilation. The implementation retains the glass design and existing motion durations. The target is the smallest amount of work per frame, rather than adding animations or shortening them to hide slow drawing.

### Ranked findings and fixes

| Priority | Finding | Result |
| --- | --- | --- |
| High | Movie details repeatedly drew its full-panel artwork and scrim during focus/scroll animation. Main-thread work was roughly 1.4 ms median while standard process frame time was 57–61 ms. | Reuse one bounded retained backdrop for movie Home, apps-only Home, and details. Changing controls stay outside it. |
| High | Details painted the shared glass gradient and an opaque black rectangle underneath its own full-panel artwork. Those hidden fills still cost GPU work every frame. | Omit the shared surface for this artwork-backed dialog and include its black fallback inside the retained artwork layer. Other dialogs retain their glass surface. |
| Medium | Movie-card focus scaling replayed the image, gradient scrims, badges, and labels every frame. | Retain their static interior; native TV focus scale, press feedback, border, and semantics remain outside. Lazy rows still mount only visible/prefetched items and transient outgoing content. |
| Medium | Spatial focus could escape from the leftmost app to the hero when Left was repeatedly pressed. | Consume Left at the dock's first app (or Search with no apps), and Right at its last action. Up/Down keep their explicit Home navigation. |
| Medium | A profiling script that checks only median misses slow tails and different window lifetimes. | Add `scripts/profile-interactions.py`: fixed warm sequences, process p50/p90/p95/p99, jank counts, raw frame files, and screenshots of the measured surface. It never changes preferences or pauses external media. |

### Memory and appearance

A retained full-screen layer is enabled only when its measured pixel area is at most 1920×1080, limiting each RGBA texture to about 8 MiB. True 4K component viewports draw normally. Details may coexist with Home's retained backdrop while open; its texture is disposed with the dialog. Movie-card buffers are small (about 0.44 MiB for a 432×264 card on the AFTKRT), scale with visible lazy content, and use the same area limit. Render caches may keep reusable GPU allocations after a dialog closes; this is a memory-for-rendering tradeoff, not a reduction in memory use.

Artwork resolution, text, gradients, colors, geometry, and animation timing remain the same. There is no live blur, bitmap screenshot layer, full-screen crossfade pair, image prefetch sweep, new dependency, system-animation override, or forced display mode. The original heap/low-RAM artwork budget, disk cache, and memory-pressure cleanup remain in effect.

### Existing good practices verified

- Animation progress is read in graphics/draw lambdas; no per-frame animated layout state was introduced. Shelves keep a fixed viewport and independent horizontal positions.
- Movie/cast/app lazy items have stable identity. Package discovery and icon loading run off the main thread and are reused until packages change.
- JSON/network work stays off the UI thread. Catalog requests have bounded concurrency and a six-hour cache; search debounces for 350 ms and cancels obsolete calls.
- Catalog, widgets, search, and clock work stop or pause with the foreground lifecycle. Hero rotation pauses under dialogs and while browsing.
- Release builds shrink/optimize code and resources. AndroidX/Compose baseline profiles are packaged already; no additional runtime library is needed for them.
- Standard TV Material controls still own focus/press animations and accessibility. Translucent panels keep readable text, a clear focus state, and short transitions rather than animated blur.

These choices follow Android's [Compose performance guidance](https://developer.android.com/develop/ui/compose/performance/bestpractices) and [graphics/compositing guidance](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#compositing-strategy), while preserving the visual hierarchy of Apple's [materials](https://developer.apple.com/design/human-interface-guidelines/materials) and [focus guidance](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection).

### Physical measurements and limits

The before build is 0.24.13; the after build is the minified 0.24.14 release on Amazon AFTKRT, Android 11/API 30. App UI screenshots are 1920×1080; the connected display reports a 59.94 Hz mode. Warm detail samples use the same first Discover movie, Spider-Man: Brand New Day, and the same directional sequence through cast/similar content. Each flow is warmed before resetting statistics. Three samples cover details; two cover shelves and settings dialogs. Source images and metadata are allowed to finish first.

Results use the top-level **process** percentiles from `dumpsys gfxinfo`, including dialogs that have closed. Raw per-window frame records contain a bounded trailing buffer and omit destroyed windows; their wall times include scheduling, so they are reported separately. Fire OS GPU percentile values sometimes report an invalid 4950 ms bucket and are deliberately excluded. Background load is not controlled; Home's surrounding artwork can differ between process starts. This is a short practical comparison, not a randomized laboratory benchmark, cold-image test, or sustained 60 fps guarantee.

| Warm interaction | Before median | After median | Before p95 | After p95 |
| --- | --- | --- | --- | --- |
| Movie details (3 samples) | 57–61 ms | 7–8 ms | 73 ms | 18–22 ms |
| Shelves (2 samples) | 18 ms | 9–10 ms | 34–36 ms | 26–27 ms |
| Settings dialogs (2 samples) | 12–15 ms | 14–18 ms | 30–34 ms | 34–36 ms |

Details and shelves show a repeatable improvement in these samples. Settings dialogs do **not** show a gain: some percentiles are slightly higher. Their panel treatment is unchanged, and this short sample cannot isolate device load, warm compilation, or allocation effects. Further menu profiling should target opening/closing and slow-tail frames; the new build does not promise uniformly faster dialogs. Shelves also retain occasional 61–65 ms p99 frames despite the lower median. A 59.94 Hz display has roughly a 16.7 ms frame budget, so none of these measurements justify an unconditional 60 fps claim.

The older `scripts/profile-tv.sh` now delegates to the same profiler for shelves, avoiding a second implementation and a misleading median-only pass/fail threshold.

The compact [measurement record](measurements/fire-tv-0.24.14.json) preserves all process percentile/frame-count samples. Temporary raw captures and screenshots were removed during the requested release cleanup. Reproduce with `adb` on PATH:

```sh
python3 scripts/profile-interactions.py SERIAL work/profile-details --flow details --samples 3
python3 scripts/profile-interactions.py SERIAL work/profile-shelves --flow shelves --samples 2
python3 scripts/profile-interactions.py SERIAL work/profile-dialogs --flow dialogs --samples 2
```

The scripts require movie Home with loaded categories and at least one visible app. They leave Reelora at Home and preserve preferences.

### Further work ranked by evidence

1. Investigate any remaining slow mount/decode frames using a system trace under normal concurrent playback. Warm interactions do not establish cold-image behavior or eliminate contention from another app.
2. If cold-start or main-thread compilation is measured as a bottleneck, generate an app-specific baseline profile from real startup/focus journeys. The current GPU bottleneck does not justify adding a benchmark module merely to claim a speedup.
3. Profile real 4K app viewports and memory-pressure recovery separately. They intentionally avoid large retained full-screen textures; the Full HD results cannot be generalized to them.

### Validation and delivery

42 unit tests, debug lint, and the minified release build pass. The retained-buffer boundary test covers Full HD, 4K, empty/negative sizes, and integer-overflow inputs. Emulator navigation checks cover both dock endpoints, far-right round trips, independent row positions, anchored clock/dock geometry, category boundaries, and rapid reversals. Physical screenshots of the same movie confirm the artwork-backed dialog remains intact. Apps-only wallpaper, settings return, and restoration to movie Home were checked on the emulator. The final warm Home memory snapshot was about 158 MiB PSS; there is no equivalent before snapshot for a memory-reduction claim. The device AndroidRuntime log contained no crash. Version 0.24.14 (70) is installed on the AFTKRT with app data preserved.

## Release cleanup (0.24.15)

Removed unused ambient-dialog branches, old column-navigation/reordering helpers, obsolete weather-symbol formatting and its test, unused imports, and an unused preview dependency. The actual weather-icon and row-navigation tests remain. The launcher icon no longer has an unnecessary API-26 resource qualifier because the app supports API 30 and newer.

Release debugging is explicitly disabled. Existing R8 code optimization, integrated resource shrinking, and ART profiles remain enabled, following [Android's release optimization guidance](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization). `scripts/dist.sh` checks the actual APK's package/version, debug/test flags, preview-component exclusion, ART profile, R8 mapping, and signature before installation. After installing, it pulls the device APK and compares every byte with the verified release. It retains a compressed R8 mapping for crash diagnosis.

A clean build, 41 unit tests, debug lint, and release lint passed. Rejection checks confirm debug, test-only, and stale-version manifests are refused. The release contains no Compose tooling classes or widget-preview activity. The final APK is 2,922,764 bytes (about 2.79 MiB); the earlier runtime rendering gains are preserved rather than rebenchmarked for this dead-code cleanup.

Version 0.24.15 (71) was installed on Amazon AFTKRT with preferences preserved. Its certificate matches both the existing installation and the published GitHub APK. The certificate's subject is Android Debug; signing identity is distinct from build variant, and this APK is the optimized, non-debuggable **release** variant. The device package flags and byte comparison independently confirm the installed artifact. Settings and discovery were visually checked on-device, and AndroidRuntime recorded no crash.

Old local APKs, intermediate build outputs, project build caches, temporary screenshots/videos, downloaded comparison APKs, and ADB test dumps were removed after verification. Source, signing material, SDK/shared dependency caches, final release deliverables, and the compact historical measurement record remain. Re-running the distribution script regenerates project build outputs normally.

Release review follow-up: when refreshed categories shrink during a slide, the row transition is recreated with the new catalog and focus is clamped to an available row or the dock. This prevents outgoing content from indexing a replaced category list. GitHub publishing now runs both lint variants and the same APK verification used locally, retaining the compressed R8 mapping in the workflow artifact.

An emulator fixture changed the catalog from multiple rows to one during a slide, then to zero. Dock focus returned and the process remained alive; the temporary fixture was removed before the production build.

## Fire TV clipping and motion follow-up (0.24.17, October 22, 2026)

### Findings and changes

- Apps instructions were below the last focusable setting, clipped by the scroll viewport and unreachable with the remote. Move that copy and apps-only wallpaper attribution into the visible header. Allow two subtitle lines and fill the existing safe panel height, including enlarged Romanian text.
- The movie shelf painted a second gradient that stopped 24 dp above the viewport bottom. Remove it and use one continuous full-screen artwork gradient. Remove the stage's obsolete fixed height and improve attribution readability.
- The dock repainted its static glass fill, sheen, and rim during motion. Retain those through the existing bounded drawing cache while app cards, focus, and scrolling remain independent.
- The animated app rim color was read during composition. Read it only while drawing, cache the existing tile outline, and skip the transparent rim. Its geometry, spring motion, press response, and Move state remain intact.
- Enlarged cast labels exposed another fixed-height ceiling. Let the cast row grow above its reserved minimum and reserve consistent name/role text slots. Detail badges also use a minimum height.
- Clock hours/minutes and temperatures use a short slide/fade only on value changes. Symbols remain steady, loading placeholders fade, and weather icons transition independently of temperature. Native animation-scale handling, tabular digits, and one accessibility value per number avoid extra timers and duplicate announcements.

### Device and method

Amazon AFTKRT, Android 11/API 30. The physical display reports 4K at 59.94 Hz; its existing 1920×1080 app display override and density 320 were left unchanged. Rendering samples use font scale 1.0, warm release builds, the existing `scripts/profile-interactions.py` sequences, and no external media playback changes. The initial media sessions were inactive/paused. Background system load, artwork, and compilation warmth are not controlled, so these are practical short samples rather than a laboratory benchmark.

| Warm flow | Original median / p95 | Fixed median / p95 |
| --- | --- | --- |
| Mixed shelf navigation | 18 / 34 ms (2 samples) | 6–8 / 22 ms (2 final samples) |
| Settings open/navigation/close | 6 / 10 ms (2 samples) | 7 / 11–13 ms (2 samples) |
| Movie details | 7 / 19–22 ms (2 samples) | 7 / 11–22 ms (2 final samples) |

Removing the shelf overlay first measured 9–10 / 24 ms. Retaining the dock glass measured 6–7 / 22 ms across three samples. The final draw-only rim keeps mixed navigation within a similar range. Settings samples do not demonstrate a speed improvement; their p99 opening tails remain around 93–97 ms versus 73–77 ms originally. Mixed shelf p99 remains 57–61 ms, and `gfxinfo` still marks 28–44% of frames janky in that broader sequence. Smooth warm animation does not eliminate mount, input-handoff, or scheduling stalls.

The compositor check reads actual presentation timestamps from `dumpsys SurfaceFlinger --latency` for Reelora's current activity surface, keeping only timestamps newer than the sample start and excluding gaps of 100 ms or more as idle. Four directional movements per flow produced these bounded trailing samples:

| Final warm animation | Active intervals | Median | p95 | Within one refresh interval (allowing timestamp jitter) |
| --- | --- | --- | --- | --- |
| Dock focus | 88 | 16.689 ms | 16.738 ms | 88 / 88 |
| Discovery reveal/return | 94 | 16.683 ms | 16.741 ms | 94 / 94 |
| Category slide | 92 | 16.687 ms | 16.768 ms | 92 / 92 |

An earlier candidate's dock-focus sample had a 33.414 ms p95. The final sample matches the display's approximately 16.683 ms refresh period. These isolated warm movements differ from the mixed interaction benchmark above; neither proves sustained 60 FPS under every load. Driver GPU percentiles remain excluded because this vendor can report invalid timestamps.

The compact [measurement record](measurements/fire-tv-0.24.17.json) includes original, intermediate, and final render samples and compositor cadence. The dock's additional retained texture is approximately 1.4–1.9 MiB for the tested UI, depending on labels/text size; it reuses the existing Full HD pixel-area cap. There is no claim of reduced memory consumption.

### Layout and release checks

Native screenshots cover all four settings categories, the last Apps toggle, English/Romanian at normal and 1.3× text, first/last movie categories, app-menu actions, rename, search, movie details/cast/similar, and apps-only Home. Remote input reaches the last controls, and Back returns through dialogs to Home. Move mode is checked without reordering. Original font scale, language, Home mode, app-label/focus settings, app names/order/visibility, and display configuration are preserved. Fire TV's UIAutomator does not provide a usable hierarchy; screenshot review complements direct remote input instead.

All 44 unit tests, debug/release lint, debug assembly, and optimized release assembly pass. Distribution verification checks the manifest/version, release flags, signature, R8 mapping, and ART profile. The earlier clipping/rendering candidate was installed on Fire TV with app data preserved and its APK compared byte for byte. The device became unreachable before the number-animation addition; the user requested continuing without device verification. The physical measurements above therefore cover the clipping/rendering changes before that addition, rather than the final published artifact or number transitions.

The number transitions are checked with actual Home controls in an excluded debug fixture on the Android TV API 31 emulator. Recordings and screenshots cover midnight/noon, 12/24-hour formats, negative/zero temperatures, digit-count changes, loading placeholders, 1.0×/1.3× text, and system animations disabled. Emulator recordings are visual evidence, not Fire TV performance measurements. Original emulator settings are restored. No dependency, background service, new image request, hidden UI precomposition, or change to existing focus durations is introduced.
