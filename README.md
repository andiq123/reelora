<p align="center">
  <img src="app/src/main/res/drawable-nodpi/reelora_mark.png" width="112" alt="Reelora TV">
</p>

<h1 align="center">Reelora TV</h1>

<p align="center">
  A calm, cinematic home launcher built for Android TV remotes.
</p>

<p align="center">
  <a href="https://github.com/andiq123/reelora/releases/download/latest/ReeloraTV-latest.apk"><strong>Download the latest APK</strong></a>
</p>

![Reelora TV home screen](docs/screenshots/home.png)

## Your TV, simplified

Reelora blends an Apple TV-inspired app shelf with optional movie discovery. It opens directly into a focused, remote-first home screen where apps, time, weather, live match context, and entertainment recommendations feel like one coherent experience.

## Highlights

- **A real launcher** — discovers installed TV apps, launches them instantly, and can become the default Android home screen
- **Remote-first organization** — hold an app to move, rename, inspect, or hide it; restore hidden apps whenever needed
- **Two home experiences** — choose cinematic movie discovery or a distraction-free apps-only wallpaper mode
- **Smart football glance** — optional World Cup, Champions League, and Premier League fixtures, results, live scores, and timely hints
- **Useful at a glance** — persistent local weather, 12/24-hour clock, location search, Celsius or Fahrenheit
- **Cinematic discovery** — current releases, weekly trends, recommendations, rich artwork, cast, ratings, trailers, and availability
- **Search made for TV** — suggestions, native on-screen keyboard, and voice input
- **Ambient theater mode** — starts trailers after a configurable idle period and avoids recently shown titles
- **English and Romanian** — switch the complete launcher interface from Settings while competition names remain familiar
- **Fast by design** — stable focus, restrained GPU animations, lazy rows, cached data, and compact layouts for modest TV hardware

## Designed for the couch

The app shelf stays visually anchored while focus moves. Selected tiles lift outside their artwork instead of zooming the icon, dialogs retain the current wallpaper or movie backdrop, and every important action is reachable with directional keys, OK, Back, or Menu.

Settings keep customization in one place: home mode, wallpapers, app labels, focus style, football, weather, language, theater timing, hidden apps, and Android launcher controls.

## Install

Download **ReeloraTV-latest.apk** above, allow installation from unknown sources, and open it on an Android 11+ TV or Fire TV device. A fresh signed APK is published automatically whenever `main` changes.

Reelora is a personal launcher and discovery app, not a streaming service. Provider availability is informational and trailers play through YouTube inside the app.

## Data notice

This product uses the TMDB API but is not endorsed or certified by TMDB. Availability is supplied through TMDB's JustWatch integration. Weather comes from Open-Meteo, wallpapers from Picsum Photos, and football fixtures from TheSportsDB.

## What’s new in 0.20.0

The app shelf opens independently of movie requests. Discovery rows are lazy in both directions; backdrops stay still; movie rotation pauses behind dialogs, offscreen, and while another app is open. Apps-only mode skips movie loading. Weather and football refreshes pause in the background and retain their last result.

Artwork uses a shared cache capped at the smaller of 24 MiB or one-twelfth of the app heap, plus 96 MiB on disk. Detail and cast-credit caches have bounded entry counts. Search cancellation closes its network call, and returning from another app reuses installed app icons unless a package has changed.

The visual refresh improves contrast, allows two-line movie titles, places actions below their description, and simplifies the shelf and settings surfaces. Focus outlines remain clear for TV remotes.

See [the audit and validation notes](docs/performance-audit.md) for findings, measured observations, and physical-TV checks. Build a local APK with `./gradlew assembleRelease`; run checks with `./gradlew testDebugUnitTest lintDebug`.

Movie search is available directly on the app shelf in both home modes. Settings includes **Check for updates**: it checks the GitHub release on demand, downloads a newer APK through Android, verifies its checksum, package, and signing certificate, then opens Android’s installer. Allow installation from Reelora when Android asks. Updates require the same signing key as the installed app; local debug-signed builds cannot upgrade a production-signed installation.

## Discovery and navigation refinements in 0.21.0

Coming Soon selects up to 40 films with artwork and positive popularity from three popularity-ranked pages across the next year, then sorts this shortlist earliest first. It does not require ratings for unreleased films or fill gaps with obscure nearest-date results. Discover movies mixes popular, trending, and cinema releases, with one upcoming highlight after each four released movies. Artwork remains lazy-loaded; metadata uses the existing six-hour cache. Each category retains its own titles, deduplicated within the row. Cards show the first recognized genre from existing list metadata without extra requests. Catalog data refreshes at midnight or after six hours while Home is active. Unavailable APIs no longer produce invented movie titles, cast members, genres, or runtimes. Unrated titles show “Not rated”.

Settings uses a category sidebar and a stable update footer. Hidden apps adapts to the TV viewport and restores focus after returning apps to Home. Search has Clear and Retry actions, distinguishes unavailable search from no results, and retains results when returning from movie details. Idle theater mode pauses while a dialog is open.

Dialogs use a single short transition, with platform window animations disabled. Movie details keeps its backdrop consistent and reserves metadata space to avoid layout jumps as data arrives.

## Interaction polish in 0.22.0

Holding OK opens a compact app-action list. Release the button, then press again to choose: continued holds cannot activate the first action. The same protection applies across dialogs and Move mode. App-menu actions finish their short exit transition before changing screens or the dock. Rename cancellation returns to the app menu.

Hide and restore use brief item transitions with stable app keys. Hiding focuses the neighboring app; closing Hidden apps after a restore focuses the restored app in its original dock position. Move mode keeps directional navigation on the dock until OK or Back finishes it.

## Startup and return-to-Home in 0.23.0

Opening Reelora from the app list now routes into its Home task before constructing the UI. Returning from another app reuses that same launcher instance, including dock focus and cached artwork. Previously, app-list and Home entry could leave two launcher activities alive.

Discovery and weather wait until the app shelf is ready, then allow a short settling period before refresh work. Existing content remains visible while refreshing; returning to a resident launcher does not rescan installed apps unless packages changed. Android startup reporting now marks shelf readiness separately from the first window.

Use `python3 scripts/profile-startup.py SERIAL OTHER_APP_COMPONENT` with Android's `adb` on PATH to reproduce the entry/return checks. Sample emulator timings and their limitations are recorded in [the audit](docs/performance-audit.md).

## Low-resource defaults in 0.24.0

Automatic trailers are now opt-in; saved settings remain respected. Android-designated low-RAM devices use at most 8 MiB for the artwork memory cache; other devices retain the 24 MiB cap, and both are further limited to one-twelfth of the app heap. Optional detail/actor caches are released when the UI is hidden, and Android background-memory pressure can clear the artwork cache without creating a new loader.

Back from discovery returns to the dock, and Back at Home keeps the launcher open. Apps-only focus no longer resets on every hide/restore. Search and detail requests pause with the activity; completed search results remain available on return.

## Theater codec selection in 0.24.1

Removed the unconditional AV1-blocking script from the embedded YouTube player. WebView now reports its native codec support, leaving adaptive quality selection to YouTube. This removes an app-imposed restriction; it does not force the highest resolution or guarantee an increase in quality. YouTube no longer supports programmatically setting embedded-player quality through its public API. No quality polling, extra player, or dependency was added.

## Remote navigation fix in 0.24.2

Hero Down now returns to the last focused dock item, including Search, Hidden, and Settings. It brings that item into view before requesting focus, so scrolling to the right end of a lazy row cannot leave the hero pointing to a disposed first app. App identity is retained across reordering, with a safe fallback after removal. Moving up from movie rows preserves the dock position; moving down again restores the previous first-row movie position.

Run `python3 scripts/check-tv-navigation.py SERIAL` with `adb` on PATH and movie Home enabled to check repeated far-right dock/movie round trips.

## Native device settings in 0.24.3

Settings → System → **Device settings** opens the TV's own network, display, sound, and system controls. Native settings, Default home, and App info open outside Reelora's Home task, so a Home-task redirect cannot clear their screens. Back preserves Reelora's settings position. Unsupported vendor pages fall back to the main device settings, with a message if neither is available.

Run `python3 scripts/check-device-settings.py SERIAL` with `adb` on PATH to check native settings task isolation, navigation, and return on a TV using the English interface.

## Calm startup and varied features in 0.24.4

The dock reserves its space during the first app scan, then fades in at the left edge with the first app focused. Search and other trailing actions no longer anchor the row before apps arrive. This applies to both Home modes, including an empty or fully hidden app list. Later app changes and returns from streaming apps preserve your position.

Featured titles draw from current cinema and trending rows. Startup avoids the last featured title when alternatives exist; catalog refreshes retain the current selection while it remains available. Rotation stays paused in the background and behind dialogs. Returning to the dock uses cancellable native smooth scrolling.

Run `python3 scripts/check-cold-start.py SERIAL` with `adb` on PATH and at least one visible app to check three fresh process starts without clearing settings. This does not simulate a full device reboot.

## Widget recovery in 0.24.5

Weather, football, and discovery now share a foreground-only Android default-network listener. Requests wait for validated internet; failed widget loads retry when connectivity returns without a remote press or waiting out their old retry deadline. Leaving Home cancels requests and unregisters the listener. Fresh data keeps its normal refresh deadline and existing content remains visible during refresh.

Weather reuses a bounded cache of resolved coordinates. Football parsing runs off the UI thread, with two-minute updates for live or today's matches and ten-minute updates otherwise (up to 80% fewer quiet-period requests). No polling service, wake lock, or new dependency was added.

The football widget uses a transparent layout, larger team names and scores, readable schedules, and soft blue/mint accents. Compact match rows keep team names close to the score, with calendar, live-dot, and finished-match icons. Updates use a short fade and retain prior results when a refresh fails.

Validated on Xiaomi MiTV-AFMU0 (Android 14): launched Reelora with Wi-Fi disabled, restored Wi-Fi on-device, and observed weather and football populate without remote input. The reconnect check preserves launcher preferences.

Weather uses larger temperatures and native vector icons for clear day/night, partly cloudy day/night, overcast, fog, rain, snow, and storms. A short fade runs only when weather content changes; offline/loading states and retained temperatures remain readable.

The aligned layout was visually checked on the Mi Box at 720p with normal and 1.3× system text size; the original system text size was restored afterward. Unit checks cover every supported weather code, clear/partly-cloudy nights, unknown codes, and missing/zero/double-digit football scores.

### Local widget design loop

The latest refinement removes stretched columns and excess spacing: compact match groups, warm neutral text, restrained status colors, and mint only for live scores. A subtle wallpaper scrim keeps dates readable without a widget panel.

Build `./gradlew assembleDebug`, install the debug APK on a local Android TV emulator, then run `python3 scripts/preview-widgets.py emulator-5554`. The debug-only activity renders the actual production widgets with repeatable fixtures; it and its sample wallpaper are excluded from release APKs. The dock in these previews is illustrative. See [the design review](docs/widget-design-review.md) for iterations and checked states.

Football team flags use Android’s bundled emoji artwork and country metadata fetched only for displayed teams. Lookups have a two-second timeout and a bounded in-memory cache; match content never waits for flags. Unknown countries remain unmarked.

Settings sections follow directional focus and their content scrolls for larger text. Home keeps every movie category below the hero and uses one cancellable vertical navigation operation. See [the performance and interaction audit](docs/performance-flow-audit.md) for findings and measurement limits.

The first app shelf reveal uses one 140 ms fade with a 6 dp upward slide. Already-loaded shelves appear immediately on return; app insertion/removal uses short bounded transitions. Dialog entrances share the same restrained motion, without staggered delays.

Shared colors, focus surfaces, spacing, and corner radii live in `DesignTokens.kt`. See [the design-system review](docs/design-system-review.md) for the visual rationale and verification scope.

All catalog categories sample three pages and keep up to 40 qualifying titles. Shared curation requires artwork and a synopsis; established releases need at least 20 votes and a 6/10 rating, while recent premieres can qualify before enough votes arrive. Top-rated films require 200 votes and 7/10. The series row focuses on rated narrative/documentary shows rather than news, talk, reality, or soaps. Discovery blends up to 32 released films with eight upcoming highlights, and the hero uses that same pool with backdrop artwork. Catalog requests are bounded to four concurrent fetches and retain the six-hour cache. These are transparent general-interest rules, not personalized recommendations.
