# Reelora TV 0.20.0 — local audit

## Findings and changes

| Finding | Change |
| --- | --- |
| The first usable launcher screen waited for six movie API requests. | Render the shelf and hero structure immediately; fill movie content asynchronously. |
| Apps-only mode still fetched the movie catalog. | Fetch it only for discovery or an explicitly opened search. |
| Full-screen artwork zoomed continuously, even at rest. | Still backdrops; short transitions only when the featured title changes. |
| Offscreen movie rows were eagerly composed. | Lazy vertical and horizontal lists, stable keys, and explicit scroll-before-focus navigation. |
| Weather, football, clock, and hero work continued behind another app. | Foreground gates; preserve weather/football refresh deadlines across app switches. |
| Every return to Home scanned packages and decoded icons. | Scan once, then refresh after package broadcasts. |
| Details and credits accumulated for the lifetime of the launcher. | LRU limits of 48 details and 24 credit lists; image memory capped at min(24 MiB, heap / 12), image disk cache at 96 MiB. |
| Cancelled searches could become fallback results and leave sockets running. | Shared OkHttp calls cancelled with their coroutine; cancellation propagates; transient failures are not cached as successful details. |
| Movie and TV detail state used only a numeric ID. | Use media type plus ID for identity and reset. |
| Cast navigation referenced potentially uncomposed cards. | Bring the destination row into view before requesting focus. |
| A trailer WebView could keep playing after leaving the activity. | Pause media and timers on pause, and detach/destroy the WebView on destruction. |
| Bright artwork reduced text contrast; the hero action used a fixed absolute position. | Stronger gradients, two-line titles, an action below the description, a quieter shelf, and a two-line clock/weather display. |

## Added controls

- Search on the app shelf in both home modes, including the remote Search key.
- An update control in the Settings footer, alongside language and the installed version.
- No Reelora wordmark in the hero.

## Update behavior

Checks happen only when requested. GitHub's release title supplies the semantic version because this repository uses the mutable `latest` tag. The updater accepts only the expected repository APK URL and a SHA-256 asset digest. Android DownloadManager handles the transfer and retry behavior; the app has no update service or polling loop.

Before installation, Reelora verifies the downloaded size, checksum, package name, increasing Android version code, and compatibility with the installed signing certificate. Downloads are tracked across activity/process recreation. Android handles permission to install unknown apps and installation confirmation; canceling the installer does not reopen it automatically. A completed download waits until Reelora is foreground before presenting installation.

The current CI signing key must remain stable. Locally signed debug builds and production-signed installations are separate update paths. A replacement of the mutable GitHub asset between the check and download will fail checksum verification and require a retry.

## Validation

- `./gradlew testDebugUnitTest lintDebug assembleRelease`: passed, 19 unit tests, no lint errors. Existing SDK/dependency/deprecation/style warnings remain.
- Android TV API 31, 1080p emulator: shelf search and live GitHub update check passed; directional navigation through movie rows, return to the shelf, and opening details passed without an AndroidRuntime crash.
- At testing time GitHub advertised 0.19.4, older than local 0.20.0. The normal check correctly reported up to date rather than downloading a downgrade.
- A temporary debug build reporting an earlier semantic version exercised the real GitHub download: Checking → Downloading → checksum/package validation → rejection of the older Android version code. The pending download and APK were cleaned up. The final build restores version 0.20.0.
- Visual inspection covered both home modes, settings, search, and details.

Sampled idle rendering from `dumpsys gfxinfo` over 10-second windows:

| Screen | Before | After |
| --- | ---: | ---: |
| Movie home | 602 frames | 15 frames |
| Apps-only | Not sampled separately | 1 frame |

These are individual emulator observations with live network data and changing artwork, not controlled device benchmarks or universal speedup claims. They support removal of continuous idle animation. No claim is made that startup is a particular percentage faster or that all TVs will be lag-free.

## Physical-TV follow-up

Run the release APK on the target TV and use `scripts/profile-tv.sh SERIAL`. Check cold launch with slow/offline networking, rapid D-pad movement, install/uninstall broadcasts, long-press reordering, launcher return after playback, and resident memory after prolonged use. Complete a same-signature upgrade with a genuinely newer release on Android TV/Fire TV, including permission denial, installer cancellation, interrupted downloads, and restart during download.

The existing baseline profile is broad; representative physical-TV startup and navigation traces would be needed before claiming profile-specific gains.

## References

The changes follow [Compose performance guidance](https://developer.android.com/develop/ui/compose/performance/bestpractices), [Coil's cache configuration](https://github.com/coil-kt/coil/blob/main/docs/image_loaders.md), [Android DownloadManager](https://developer.android.com/reference/android/app/DownloadManager), and [GitHub release asset digests](https://docs.github.com/en/rest/releases/assets).

All work is local. No GitHub push or release was performed.

## 0.21.0 follow-up

The second audit found a concrete classification problem: the sampled TMDB upcoming feed returned 19 already-released titles out of 20. A discover query constrained by future primary release dates returned zero past-date titles. Coming soon now uses that query, excludes missing/invalid dates and non-movies locally, and treats releases today as released. The cinema feed also excludes future primary dates. Cache refreshes at midnight/six hours prevent indefinitely stale date groupings.

Fabricated catalog and details fallbacks were removed. API failures retain real cached sections when available and otherwise leave honest empty/unavailable content. Search results reject unsupported media types and adult entries and deduplicate movie/TV identities. Titles without usable ratings display “Not rated” instead of a misleading zero-star score.

Settings was reorganized into Home, App shelf, Weather & time, and System categories. Update status fades in a fixed footer area and the update button stays focusable during a check. Hidden apps uses viewport-relative dimensions, a single empty state, and deliberate focus restoration after removing a row. Search keeps its state while details is open, distinguishes network failures from no matches, supports retry, and returns to the originating Settings category. Idle trailers no longer interrupt dialogs.

The movie dialog now uses one draw-phase entrance/exit animation, disables competing native window animations, avoids swapping to alternate artwork on details arrival, and reserves genre/provider/cast space to reduce layout shifts. Its background image has a short Coil crossfade for a first-time decode; cached images avoid that transition.

Verification: all 21 unit tests passed, including date-boundary, duplicate, rating, and update-asset regressions; lint and the minified release build passed. Android TV emulator checks covered six repeated movie-dialog open/close cycles, hiding/restoring an app and empty-state focus, search-to-details-to-results-to-Settings navigation, and a live GitHub update check. Search and weather fields explicitly route remote Down presses out of the text editor; returning from movie details focuses results without reopening the keyboard. No AndroidRuntime errors were logged during the final checks. Physical-device frame timing still needs to be measured; this is not a claim of zero lag on every TV.

## 0.22.0 interaction follow-up

Replaced the app menu's 350 ms activation delay and Move mode's 300 ms confirmation delay with a shared press/release gate. Dialogs accept a confirmation release only after receiving that key's fresh initial press; inherited repeats and orphan releases are consumed. A regression test covers inherited repeats, entrance-time presses, valid holds, duplicate releases, and mismatched keys.

App options is now a compact vertical action list. Actions wait for the existing short dialog exit animation before mutating the dock or opening another screen. Rename uses entrance-ready focus, disables empty saves, and returns to its parent menu on cancellation. Weather input also waits for dialog readiness instead of a fixed delay.

Dock items retain their keyed focus requesters as indices change, with short appearance/removal/placement animations. Hide chooses a surviving neighbor; restore preserves the app's saved order and focuses it when the overlay closes. Only offscreen targets trigger a scroll. Reorder scroll work cancels a superseded request, and Up/Down cannot escape Move mode. No dependencies or background workers were added.

Emulator checks verified a 3.5-second OK hold with repeated down events leaves App options open after release, Rename cancellation returns to App options, hide focuses the next app, restore returns focus to that app, and left/right reordering finishes with a fresh OK press.

Final validation: 22 unit tests passed with zero failures/errors, lint passed, and the minified 0.22.0 APK built successfully. The final APK also passed the long-hold and Move-direction navigation checks on the TV emulator. Physical-TV timing remains unmeasured. All changes remain local; nothing was pushed or published.

## 0.23.0 startup follow-up

A controlled app-list → YouTube → Reelora Home-intent check created two MainActivity records before this change. The app-list entry now uses a no-display Activity that immediately routes into the Home task and finishes, while MainActivity uses singleTask. It constructs no Compose content or loaders. All three final runs retained exactly one MainActivity record with the same identity across the return. This respects Android's separate Home task handling instead of relying on launchMode alone to merge activity types. See [Android task and launch-mode documentation](https://developer.android.com/guide/components/activities/tasks-and-back-stack).

Installed-app loading takes priority over initial discovery/weather work. Optional refreshes yield a frame and wait 750 ms after the shelf is ready or the screen resumes, cancelling when backgrounded. Football refresh also yields on resume. Fresh in-memory catalog data can populate a newly created UI immediately. ReportDrawnWhen now marks when the app shelf is populated; artwork/network readiness is intentionally separate. No keep-alive service, wake lock, periodic background work, or additional cache was added.

Three samples on the same Android TV API 31 emulator:

| Observation | 0.22.0 | 0.23.0 |
| --- | ---: | ---: |
| Median cold `am start -W` TotalTime | 178 ms | 177 ms |
| Median return TotalTime | 52 ms | 52 ms |
| Return launch classification | WARM | HOT |
| MainActivity records after return | 2 | 1 |

These small samples show activity reuse, not a statistically established timing improvement. TotalTime is Android's launch measurement, not a guarantee of fully loaded content or physical-TV performance. Netflix is not installed on the emulator, so YouTube was used. The Google TV emulator continued routing its physical Home key to its stock launcher despite a preference command; tests therefore target Reelora's Home intent explicitly, and the original Home preference was restored. Raw samples are in [startup-samples.json](startup-samples.json). Real Netflix playback, memory pressure, and Home-button return latency must be measured on the target TV.

Final validation: 22 unit tests, lint, and minified release compilation passed. UI checks confirmed that returning from a launched YouTube tile retains its focus and activity identity. Killing the background Reelora process changed its PID; returning through the Home intent rebuilt a usable app shelf successfully. All work remains local and unpublished.

## 0.24.0 low-resource follow-up

The artwork cache now uses Android's low-RAM classification: min(heap/12, 8 MiB) for low-RAM devices, min(heap/12, 24 MiB) otherwise. One regression test covers both device classes and heap-limited cases. Detail and actor metadata caches clear when the UI becomes hidden. At background memory-pressure levels, the existing image loader's memory cache clears too; the callback never initializes an image loader or removes the retained Home UI. This follows [Android's memory guidance](https://developer.android.com/topic/performance/memory/manage-app-memory) while retaining the current screen for quick returns.

Automatic trailers default off unless the preference was saved as enabled. WebView remains limited to actual trailer playback. Search, movie-detail, and actor-credit effects cancel when the launcher is backgrounded; completed search/detail state is retained. Back navigation keeps the Home activity available. Apps-only initial focus runs once instead of resetting every time the app list changes.

Validation: 23 unit tests, lint, and minified release build passed. TV emulator checks cover repeated Back, apps-only hide/restore focus, background memory-trim handling, and returning to a usable dock. Low-RAM cache limits were verified by unit test; this emulator is not a physical low-RAM TV. No keep-alive service, wake lock, boot receiver, or new dependency was added.

After backgrounding apps-only Home behind YouTube and delivering a BACKGROUND trim request, a 10-second emulator sample recorded 0 newly rendered frames and 2 process CPU clock ticks. This is a single observation, not a zero-resource guarantee. The Home intent subsequently returned to a usable app shelf without a crash. All changes remain local and unpublished.

## 0.24.1 theater codec correction

The player previously replaced `MediaSource.isTypeSupported` to reject every AV1 request, regardless of the device. Removed that page-start injection so the WebView's actual codec capabilities are available to YouTube. Playback quality remains adaptive and controlled by YouTube; the public `setPlaybackQuality`, `getAvailableQualityLevels`, and suggested-quality arguments are unsupported, as documented in [YouTube's API revision history](https://developers.google.com/youtube/iframe_api_revision_history). No deprecated quality parameter or polling workaround was added.

Actual resolution, hardware decoding, and CPU use still require playback testing on the target TV and WebView version; this change is not evidence that every stream now plays at its maximum resolution.

Validation: all 23 unit tests, lint, and minified release compilation passed. No maximum-resolution or physical-device codec-performance claim was verified in this pass. The local 0.24.1 APK is debug-signed. Nothing was pushed or published.

## 0.24.2 navigation correction

The hero's Down focus link targeted the first app, which a LazyRow disposes when scrolled far enough right. Replaced it with a cancellable scroll-then-focus action and an entry focus requester attached to the remembered dock item. The entry uses app identity (or the Search/Hidden/Settings action key), not a stale numeric position; missing apps fall back to the first available entry. Movie-to-dock navigation preserves the dock position and dock-to-movie navigation retains the previous first-row movie index. Removed the unused per-app focus requesters.

A unit regression covers reordered/removed apps and all dock actions, including an empty app list. `scripts/check-tv-navigation.py` drives a real TV remote sequence across the rightmost action, last installed app, and last movie, with repeated hero round trips. All 24 unit tests, lint, and release compilation passed.

On-device result: all repeated round trips passed on Andi TV (onn. 4K Plus Streaming), including Settings, the last installed app (OttPlayer), and the last first-row movie (Supergirl). The local release was installed in place as version 0.24.2, build 58.
