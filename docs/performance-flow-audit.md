# Launcher performance and interaction audit

Scope: startup through apps/catalog readiness, foreground/background work, artwork memory, remote navigation, dock/dialog motion, settings layout, and updater interaction. This is a source review plus local emulator validation, not a claim that every bug is eliminated or a TV frame-time benchmark.

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

## Verification

- 34 unit tests, debug lint, debug build, and minified release build passed.
- Local Android TV emulator: repeated far-right dock/hero and movie/hero return paths passed, retaining the selected app and movie. The regression script now explicitly checks for media-card metadata so a dock action cannot be mistaken for a movie.
- Settings: directional focus switches the content without a confirmation press. Layout inspected at 720p with 1.0× and 1.3× text.
- No physical device update, Git commit, or push performed during this audit. Screenshots and raw local logs are in `work/flow-audit/`.

Motion follow-up: dialog entrances use the same 140 ms / 6 dp treatment. App insertion and hidden-app rows use 100 ms in, 140 ms placement, and 80 ms out. These transitions do not add app-launch or navigation waits. A local cold-start recording was inspected (`work/motion-audit/intro.mp4`); recording/emulator timing is not a target-TV frame-rate benchmark.

## Home row navigation (0.24.10)

Home owns vertical D-pad movement: hero → dock → movie categories. A mounted focus target holds focus while a lazy destination is being laid out; rapid vertical input updates the logical destination and cancels only the obsolete transfer. Horizontal and confirm input cannot act on an old card during that transfer. Row endpoints consume vertical input instead of falling back to spatial focus search.

Movie navigation follows the focused card’s horizontal screen position and selects the nearest fully visible card in the destination row. Each row retains its independent horizontal scroll; a far-right catalog index is never copied to another row. Partially clipped cards are avoided when a fully visible target exists. Returning to the dock preserves the app identity and visible horizontal position. Row scrolling is immediate before focus handoff, with existing focus animations retained; there is no overlapping animated vertical scroll job.

Regression coverage in `scripts/check-tv-navigation.py` includes far-right dock/hero round trips, all movie categories, shorter rows, bottom/right boundaries and bursts of opposite directions. The script requires loaded movie categories and English labels on the selected test device.
