# AGENTS.md

Guidance for AI coding agents working in this repository. It is a self-contained
orienter for the codebase: what the app is, how it is built, the conventions it
holds (many of them hard-won and load-bearing), and the traps that bite.

The project also keeps detailed private notes outside the repo (the source of
truth for decisions and history). This file stays in the repo and is safe to
read; it deliberately does not reproduce private history.

## What this is

A free, open-source, native Android app for reading and memorizing Al-Asma
ul-Husna, the ninety-nine names of Allah, with Arabic, transliteration and
meaning. Content is based on the lecture of Sheikh Ibn Uthaymeen (Rahimahullah),
presented in *The Ninety Nine Names of Allah: A Memorisation Tool with
Transliteration and Meanings*, curated at muntasimulhaque.bearblog.dev/99-names.

Single-module Kotlin app. Jetpack Compose + Material 3 with a small bespoke
design system, Navigation Compose, DataStore (progress + settings), WorkManager
(daily schedule), Glance (home-screen widget), kotlinx.serialization (bundled
content). No DI framework, no database, no analytics, no ads, no network.

## This file is guidance, not the last word

AGENTS.md is an iteration of what has worked and what has not — not the
ultimate authority. Follow it while working, but never let it make you discard
a good idea. If a plan or idea would improve the app yet contradicts something
written here (including anything in "Hard invariants" or "Decisions already
settled"), do not reject it silently: bring it to the user, explain the
conflict and why breaking the rule is worth it, and let the user decide. If the
user approves, implement it and update this file in the same change so the rule
reflects the new decision.

## Hard invariants (never violate)

- **The app must never use the Internet.** No INTERNET permission (the only
  self-declared permission is `POST_NOTIFICATIONS`). Fonts are bundled TTFs,
  never downloaded. The few URLs in the app open an external browser on tap.
  The merged manifest's other permissions (WAKE_LOCK, ACCESS_NETWORK_STATE,
  RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE) all come from androidx.work and
  cannot move data without INTERNET, which is absent. `INTERNET` must stay 0 in
  the merged-manifest report.
- **`applicationId io.github.muntasimulhaque.ninetynine` is final.** Never
  propose changing it. A package name is burned to the Play account the moment
  any bundle is uploaded.
- **Three names, three strings in `strings.xml`, never merged:** `launcher_name`
  ("Ninety Nine"), `app_title` ("The Ninety Nine Names of Allah", the home
  running head), `store_title` (the Play listing title, carried on the shared
  image, kept in step with the Play Console BY HAND). `store_title` is exactly
  30 characters, Play's limit.
- **Content is the app's own, faithful to its source.** `names.json` holds 99
  entries. It is NFC-normalized and the Arabic must stay drawable by the bundled
  KFGQPC HAFS typeface. Do not re-extract it from the blog source markdown, and
  do not re-add a title clause to meanings it was deliberately stripped from.
- **No AI attribution in the repo, ever.** No Co-Authored-By trailers, no
  "generated with" footers, no tool named in contributors, commit messages, or
  code. Grep commit messages for `claude|co-authored|generated with|ai-attribution`
  (case-insensitive) before every push. After a push, `git ls-remote origin`
  should show only `refs/heads/main`.
- **Tool calls must be native, never XML/DSML/card-formatted text.** Prohibit outputting tool calls in XML/DSML/card format text (`<invoke>`/`<parameter>` are strictly prohibited). All tool calls must use native tool call.

## Versioning

- `versionName`/`versionCode` live in `app/build.gradle.kts`, shown in Settings
  via `BuildConfig.VERSION_NAME` so they can never disagree.
- The project versioning rule is +0.1 on `versionName` (single decimal segment)
  and +1 on `versionCode` per release. Since the first Play release the repo
  versioning restarted at store-friendly numbers: **0.1 / 1**, **0.2 / 2**,
  **0.3 / 3**, **0.4 / 4**, **0.5 / 5**, **0.6 / 6**, **0.7 / 7**, then **0.8 / 8** (current). Check `app/build.gradle.kts` for
  the live values and bump by the same rule for the next release.
- The release keystore path/credentials live in a `keystore.properties` file
  outside the repo (Google Play Signing Key folder). When absent (CI, fresh
  clone) the release build degrades to unsigned rather than failing.

## Release hand-off (every push to main is a Play release candidate)

The app is published, so every change is built to ship. A release prep is not
done until the agent has done ALL of the following itself, in order — never
hand any step back to the user:

1. **Bump the version** by the rule above: `app/build.gradle.kts`, the list
   above, and the version field in `docs/play-listing.md`.
2. **Write the "What's new" notes** (≤500 chars) into `docs/play-listing.md`
   — it is the copy/paste source for the Console.
3. **Verify locally**: the full CI suite (`:app:testDebugUnitTest
   :app:lintDebug :app:assembleDebug :app:assembleRelease`).
4. **Commit and push**, then verify the CI run is green via the Actions API
   (full 40-char SHA — see CI below).
5. **Build the signed AAB and verify the signature.**
   `./gradlew :app:bundleRelease` signs automatically when
   `keystore.properties` exists on the machine (paths probed in
   `build.gradle.kts`: D: on the LENOVO box, E: on Dev Pro). Confirm with
   `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`
   ("jar verified"). Only if no keystore is available, fall back to the CI
   artifact and say so explicitly.
6. **Hand over the AAB and the release notes.** Copy the bundle somewhere
   obvious, named with the version (e.g. `ninetynine-0.7-vc7.aab` on the
   Desktop), and paste the notes in the reply. Once the user confirms the
   bundle was submitted to Play, delete the hand-off copy — the Console's
   App Bundle Explorer retains the uploaded artifact.
7. **Screenshots: decide explicitly, every time.** If the release changes no
   visible UI, say "no new screenshots needed" and why. If it does and
   capture is cumbersome, say so — the user takes them by hand.

## Build, test, verify

```bash
# Unit tests (canonical suite) + debug APK
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Everything CI runs
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- The Gradle wrapper is committed (9.5.0). `local.properties` is gitignored and
  is lost on folder renames; recreate it with `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` or the first build fails "SDK location not found".
- Toolchain: JDK 17 (`JAVA_HOME`), AGP 9.3.0, Kotlin 2.4.10, Gradle 9.5.0,
  Compose BOM 2026.06.01, compileSdk/targetSdk 37, minSdk 24. AGP auto-installs
  the SDK platform it wants (android-37.0 — note android-37.1 is a separate
  platform and does not satisfy it).
- AGP 9 has built-in Kotlin; the `kotlin-android` plugin is deliberately absent.
  Compiler options live in a top-level `kotlin { compilerOptions { jvmTarget } }`
  block.

## CI

- **build.yml** (on push to main and PRs): unit tests, `lintDebug` (fails on new
  issues), `assembleDebug`, `assembleRelease` (minified, unsigned — R8 exercised
  on every change), and uploads the debug APK (7-day retention). Actions are
  SHA-pinned. Nothing writes to the repo (`permissions: contents: read`).
- **screenshots.yml** (manual dispatch): renders the five listing scenes (home,
  home-dark, name, quiz, memorize) via the instrumentation screenshot test on
  phone + 7-inch + 10-inch emulators and uploads the PNGs. The `for f in ...`
  loop must list every scene in ScreenshotTest, or that scene's PNG is never
  pulled off the device.
- Verify a green run via the GitHub Actions API using a token from
  `git credential fill` (no `gh` CLI on the dev machines). `?head_sha=` matches
  only the FULL 40-char SHA (`git rev-parse HEAD`) — short SHAs return an empty
  list that reads as "still running".

## Docs & Play listing assets

`docs/` holds the Play store assets and listing copy: `play-listing.md` (the
copy/paste listing doc), `play-icon-512.png`, `play-feature-1024x500.png`,
`privacy-policy.html` (hosted on GitHub Pages), and the 8 phone screenshots
under `docs/screenshots/` that the README shows. Regression: the 7-inch and
10-inch tablet screenshot sets (`docs/screenshots/tablet-7/`, `tablet-10/`)
were committed as Play assets but referenced by nothing in the repo and were
removed (commit `ac293db`). Do not re-add them. Tablet screenshots are captured
by running ScreenshotTest from Android Studio on tablet AVDs and uploaded to
Play by hand, not from the repo, and the screenshots.yml run-as pull is
unreliable (see Known quirks).

## Code layout

```
app/src/main/java/io/github/muntasimulhaque/ninetynine/
  MainActivity.kt        Host activity; deep-link handling (widget/notification),
                         consumeNameNumber, cold-start guard, daily re-anchor.
  NamesApp.kt            Application class; keeps (does not re-anchor) the work.
  data/                  Name, NamesRepository (asset load), Prefs (DataStore).
  util/                  DailyName (epoch-day rotation), DeckBuilder, QuizBuilder,
                         SearchFilter (pure, unit-tested).
  daily/                 DailyNameWidget (Glance), DailyScheduler (WorkManager),
                         TimeChangeReceiver.
  ui/                    NamesViewModel (shared state), and per-screen packages:
                         home, detail, memorize (Flashcards/Quiz/Learned/Memorize),
                         bookmarks, share, settings, about.
  ui/theme/              Color, Type, Theme, Motion, Haptics, Shapes, SquircleShape,
                         and components/ (ArabicText, MixedText, FitText, PageParts,
                         LearnedButton, NameListItem, Hairline, ...).
app/src/main/assets/     names.json (99 entries), intro.txt, fonts/ (bundled TTFs +
                         their licenses).
```

- **State:** activity- and screen-scoped ViewModels. Rotation survives via
  ViewModel; process death resets sessions (accepted). DataStore emits a frame
  or two late, so any `stateIn(…, emptyList())`-style flow must be gated on a
  `*Loaded` flag before building UI, or you flash "0 learned"/"nothing kept"/
  a spinner over real data.
- **Content reads must be crash-proof.** `NamesRepository.load` wraps the asset
  read in `runCatching` and distinguishes "still loading" from "could not be
  loaded" on Home. `Prefs` uses `retryWhen` (a bare `catch` emits and COMPLETES
  the flow, killing every derived flow for the process lifetime). Writes are
  hardened the same way: `Prefs.write` swallows any exception (cancellation
  rethrown), because a failed save escaping a `viewModelScope.launch` kills
  the process over a toggle. `intro.txt` parse normalizes `\r\n` → `\n`.

## Design system (read before touching any size/color/spacing)

- **Arabic is set in KFGQPC Uthmanic Script HAFS** (the Madinah Mushaf face),
  Latin in Spectral. Both bundled. HAFS is a single weight (W400) with a large
  body; **never let it synthesize a weight** (fake-bold) — pin weight to Normal
  in `ArabicText`/`MixedText`. KFGQPC's license forbids modification and
  derivative artwork, so the font must stay byte-identical.
- **`ArabicSize`** (in `ArabicText.kt`) names each Arabic step by the Latin slot
  it pairs with. Use it; Arabic must not inherit the Latin size (HAFS body is
  ~0.35em vs Spectral x-height 0.45em, so matched sizes look smaller).
- **`FitText`** (PageParts) shrinks text to fit, stepping `fontSize` AND
  `letterSpacing` down together (tracking is part of a type size). Guard
  `TextUnit.Unspecified`. It is the house pattern for anything that must never
  break a Divine Name or truncate a title — transliteration on all five surfaces,
  the running head, the share wordmark, bottom-bar labels.
- **Centering** baked into a style via `.copy(textAlign = TextAlign.Center)`; a
  bare `FitText` does not center itself.
- **Never set a sentence in `labelMedium`/`labelSmall`** (they carry 1.8sp/1.2sp
  tracking — for 1–3 word overlines; prose must be `bodySmall`+).
- **Reading rule:** the short meaning (`title`) only shows where the full
  `meaning` does NOT (name page, share card, flashcard back show the meaning
  only; list rows, hero card, widget, notification, quiz keep the title).
- **Shared components live in `PageParts.kt`** (BackButton, FitText, ScreenLabel,
  SectionLabel, NavRow, PageRule, SettingsAction, paperTopBarColors, scaledGap,
  readingMeasure, named insets). Reuse them; do not re-derive per screen.
- **Contrast:** comments in `Color.kt` are mostly right but re-verify before
  building on a claim. Changes must keep WCAG 2.1 AA (4.5:1 text, 3:1 UI
  components). `outline`/`outlineVariant` carry real meaning in places — do not
  assume they are decorative.
- **Motion/haptics:** `Motion.kt` (QUICK/GENTLE/CALM) and `Haptics.kt`. The
  `@Composable` variants (`Motion.tween()`, `Motion.soft()`) collapse to `snap()`
  when the system reduce-motion/animator-duration-scale is 0; use the
  non-composable `spec()` variants for coroutines/gesture callbacks.

## Content invariants (guarded by NamesAssetTest)

`assets/names.json` must hold: 99 sequential entries; no blank or duplicate
fields; NFC-normalized Arabic; every Arabic character drawable by the bundled
HAFS TTF (only U+0622 آ is allowed, which `forArabicFont()` decomposes at render
time); and every `meaning` begins with its `title` clause (the app depends on
this — the title renders only inside the meaning on Detail/Share/Flashcard-back,
so dropping the clause silently loses the epithet). The transliteration follows
the source's own convention (regularised for #28, #32, #44, #48, #80, #87, #94,
#95).

## Testing

- **60+ unit tests** (JUnit4, `app/src/test`): daily-name rotation, quiz
  generation + guards against subsuming distractors, search, deck building
  (incl. the 10-card session cap), ViewModels, and NamesAssetTest over the real
  asset. Count grows as guards are added — sum the result XMLs in
  `app/build/test-results/testDebugUnitTest/` rather than trusting any
  hardcoded number.
- **Instrumentation** (`app/src/androidTest`): ScreenshotTest renders five
  scenes (home, home-dark, name, quiz, memorize), saving PNGs to the app's
  `files/screenshots/` dir; used by the screenshots workflow and by the local
  Android Studio capture flow.
- Pure logic (DeckBuilder, QuizBuilder, SearchFilter, DailyName) lives in
  `util/` precisely so it is unit-testable without Android.

## Editing pitfalls that bite

- **Files are CRLF** (`.gitattributes text=auto`). The patch tool fails to match
  an old_string ending in a trailing newline on CRLF files — include the
  FOLLOWING line in the match instead.
- **Repo-wide greps: use `git grep`, not a filesystem search tool** when the
  working directory path contains spaces (this repo's does). `git grep` also
  only searches tracked files, so build output can never pollute a sweep.
- **After removing a Text/composable block, re-grep for now-unused imports** —
  the project holds a zero-warning build standard.
- **Modifier-order matters in Compose:** `.fillMaxWidth().heightIn(max = X)` must
  have `heightIn` BEFORE `fillMaxHeight` or the cap is ignored. And never pair
  `heightIn` on a Column child with `fillMaxHeight` on its own children — it
  expands to full screen and blanks the app.
- **Arabic widths from hmtx are nominal (isolated advances)** — no shaping in
  stdlib. Shaped Arabic runs ~0.6–0.7× narrower. Never assert Arabic overflow
  from nominal widths alone; say "upper bound, needs device check". Latin
  advances are exact.
- **KFGQPC HAFS has no U+0622 آ** (and no en/em dash — MixedText keeps those in
  Spectral). Any new Arabic must avoid آ or rely on `forArabicFont()`.
- Widget/notification deep links: the activity is `singleTop` with an exported
  intent; consume the `nameNumber` extra and guard cold-start replay with
  `savedInstanceState == null`.
- The daily scheduler's `Application.onCreate` must KEEP (not re-anchor) the
  schedule; `MainActivity.onCreate` is where re-anchoring happens. Re-anchoring
  on every process start cancels the pending job and pushes it a day out.
- **ViewModel flags must be declared BEFORE their eager flow.** `stateIn(Eagerly)`
  starts collecting immediately; if its `.onEach { _flag.value = true }`
  references a flag declared on a LATER line, a cold start that reads DataStore
  during construction hits a null flag and crashes with an init-order NPE
  (`NamesViewModel.kt:87`). This shipped as an intermittent cold-start crash on
  widget/notification deep links. The `_??Loaded` flags must precede their flows
  (as `_namesLoaded`/`_bookmarkedLoaded` do).
- **`DailyName.numberFor` uses `Math.floorDiv`/`floorMod`, never `/` and `%`.**
  Plain division truncates toward zero, wrong for pre-epoch instants, and the
  remainder of a negative day maps to the wrong name. The pre-epoch test in
  `DailyNameTest` guards it, so do not "simplify" it back.
- **Widget/notification workers fold failures to `Result.retry()`, not
  `Result.success()`.** The render stays wrapped in runCatching, since a throw
  must never kill the worker, but a swallowed throw used to mean the daily
  widget refresh was silently skipped until the next day. Retry lets
  WorkManager's backoff handle the transient cold-start Glance race; a
  persistent failure merely burns a few retries.
- **The widget's `cornerRadius` breaks its `clickable` on API < 31.** Glance's
  `cornerRadius` is a no-op before Android 12 (it logs "Cannot set the rounded
  corner before Api 31"), and that no-op path swallows the following
  `clickable` — the widget renders but never answers a tap. Apply it
  conditionally: `if (SDK_INT >= S) m.cornerRadius(...) else m`, BEFORE
  `clickable`. On API < 31 corners stay square.
- **The widget's `appwidget-provider` MUST set `android:initialLayout`.** Without
  it, `initialLayout=#0` and the launcher throws `Resources$NotFoundException:
  Resource ID #0x0` on the initial bind — "Problem loading widget" on stricter
  launchers (Vivo Funtouch on Android 8.1), silently recovered on others. Point
  it at `@layout/widget_preview`.
- **A widget's tap PendingIntent dies on app update on Android 8.0–8.1.** The
  launcher keeps the pre-update RemoteViews, and the system invalidates the
  PendingIntent inside them (created by the old APK) on package replace — the
  widget renders but never opens the app until the next app open re-renders it
  (MainActivity.onResume → updateAll). Three layers of defense now ensure the
  widget is re-rendered immediately after an update:
  1. `PackageReplacedReceiver` enqueues a `WidgetUpdateWorker` via WorkManager
     (persisted, survives process death, retries on failure).
  2. `NamesApp.onCreate` calls `updateAll` on every process start as a fallback
     for OEMs that block `MY_PACKAGE_REPLACED` (e.g., Vivo Funtouch OS).
  3. `DailyNameWidget.render` always calls `provideContent`, even when the name
     lookup fails, so the widget never keeps stale RemoteViews with an
     invalidated PendingIntent — an empty-but-tappable emerald plate is rendered
     instead, guaranteeing a fresh PendingIntent in every widget update.
  Not reproducible on Android 12+ (S23).

## Decisions already settled — do not reopen

These were made deliberately, often after measurement or a user decision, and
were sometimes implemented and then reversed. Re-proposing them wastes a cycle.
They are not beyond appeal: if a genuinely better idea contradicts one, take it
to the user as described above — approval reopens the decision and this list is
revised.

- **The three name strings are never merged** (`launcher_name` / `app_title` /
  `store_title`). The strings.xml comment explains each; two hold the same words
  and are still separate on purpose.
- **No first-run epigraph / "opening" page.** A fresh install landing on the
  About hadith was implemented and reverted one release later — the app must
  open on the names list. Do not propose it again.
- **No grid view.** Implemented and removed; the list is the only view.
- **No `applicationId` change.** Final.
- **No re-extracting `names.json` from the blog markdown**, and no re-adding
  the title clause to the `meaning` fields. The UI-layer approach (short meaning
  shows only where the full meaning doesn't) is settled.
- **The transliteration of the source is faithfully reproduced**, regularised in
  exactly eight places to the source's own convention (#28, #32, #44, #48, #80,
  #87, #94, #95). Do not "standardise" the rest.
- **`displayMedium` (30sp) is a standard M3 slot deliberately left in `Type.kt`
  with no render site.** Don't delete it as dead code without asking.
- **No INTERNET / no network / no analytics / no ads / no billing — ever.**

## Known quirks & accepted limitations

- **The home-screen widget renders Arabic in the system serif (Noto Naskh), not
  the app's KFGQPC HAFS.** Glance cannot bundle fonts. This is accepted; the
  widget's numerals/marks are also sanitized via `systemFontSafeArabic()`.
- **KFGQPC HAFS has no U+0622 آ and no en/em dash** (MixedText keeps dashes in
  Spectral). New Arabic content must avoid آ or rely on `forArabicFont()`.
- **Counters always render Western digits, via `%s` on purpose.** The
  `detail_counter`, `card_x_of_y`, `question_x_of_y` and `quiz_score_format`
  strings use `%1$s` with Int arguments: `%d` follows the device locale, so
  ar/ur devices rendered Arabic-Indic digits whose bidi order reversed the
  pair visually, and the counters never matched the folio numbers
  (`Int.toString()`, always Western). Guarded by CounterFormatTest — do not
  "fix" them back to `%d`.
- **WorkManager notification timing can drift a few minutes** (system batching).
  Accepted.
- **The screenshots.yml `adb exec-out run-as` pull has been failing.** Every
  scene comes back as a 62-byte error file even when the instrumentation tests
  pass (observed on a 5-scene run). Listing screenshots are therefore captured
  by running ScreenshotTest from Android Studio on phone, 7-inch and 10-inch
  AVDs and pulling the PNGs from
  `/data/data/io.github.muntasimulhaque.ninetynine/files/screenshots/` via the
  Device File Explorer or `adb exec-out run-as ... cat`.
- **Local debug APKs sign with the machine's debug keystore; CI artifacts sign
  with CI's.** Installing one over the other fails with
  INSTALL_FAILED_UPDATE_INCOMPATIBLE and uninstalling wipes DataStore progress.
  Device-test from the CI artifact, not a locally built APK.
- **The app deliberately has no SnackbarHost** (reset has no Undo; some failures
  surface as a Toast). A shared snackbar infra is a recurring candidate — don't
  assume one exists.

## Machines & build environments

- **Dev Pro machine (this one):** JDK 17 at
  `C:\Users\Dev Pro\.jdks\jdk-17.0.19+10` (JAVA_HOME), Gradle wrapper 9.5.0, SDK
  at `C:\Users\Dev Pro\AppData\Local\Android\Sdk`. Build:
  `JAVA_HOME="C:\Users\Dev Pro\.jdks\jdk-17.0.19+10" ./gradlew :app:testDebugUnitTest :app:assembleDebug`.
  No emulator — visual verification is the user's device test of the CI artifact.
- **LENOVO machine:** Android Studio's JBR as JAVA_HOME
  (`C:\Program Files\Android\Android Studio\jbr`) + a Pixel 4 emulator for
  visual verification. Emulator gotchas: the first screencap is black (~20s to
  draw — a "not black" test passes instantly and wrongly); the SystemUI ANR
  dialog on swiftshader clears via its Wait button, then force-stop + relaunch;
  poll for page content, never background colour (splash ground is the same
  paper colour as the page).
- `local.properties` is gitignored and lost on folder renames — recreate with
  `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` before the first
  build.