# AGENTS.md

Guidance for AI coding agents working in this repository: what the app is,
the conventions it holds (many hard-won), and the traps that bite. Detailed
private notes live outside the repo; this file is safe to read and
deliberately does not reproduce them.

## What this is

A free, open-source, native Android app for reading and memorizing Al-Asma
ul-Husna, the ninety-nine names of Allah, with Arabic, transliteration and
meaning. Content is based on the lecture of Sheikh Ibn Uthaymeen
(Rahimahullah), presented in *The Ninety Nine Names of Allah: A Memorisation
Tool with Transliteration and Meanings*, curated at
muntasimulhaque.bearblog.dev/99-names.

Single-module Kotlin app. Jetpack Compose + Material 3 with a small bespoke
design system, Navigation Compose, DataStore (progress + settings),
WorkManager (daily schedule), Glance (home-screen widget),
kotlinx.serialization (bundled content). No DI framework, no database, no
analytics, no ads, no network.

This file is guidance, not the last word. If a good idea contradicts it, do
not reject it silently: bring it to the user, make the case, and if approved,
implement it and update this file in the same change.

## Pull before working

The owner works from more than one machine (LENOVO and Dev Pro; see Build,
test, verify), so this checkout is only one of several. At the start of any
session, `git fetch` and pull whatever is new on `main` from GitHub, and do
the work on that pulled head, never on a stale local one. This is the first
action of every session, before any file is read or command run.

## Hard invariants (never violate)

- **The app must never use the Internet.** No INTERNET permission (the only
  self-declared permission is `POST_NOTIFICATIONS`). Fonts are bundled TTFs.
  The few URLs in the app open an external browser on tap. `INTERNET` must
  stay 0 in the merged-manifest report.
- **`applicationId io.github.muntasimulhaque.ninetynine` is final.**
- **Three names, three strings in `strings.xml`, never merged:**
  `launcher_name`, `app_title`, `store_title` (exactly 30 chars, Play's
  limit, kept in step with the Play Console BY HAND). The strings.xml comment
  explains each.
- **Content is the app's own, faithful to its source.** `names.json` holds 99
  entries. NFC-normalized; Arabic must stay drawable by the bundled HAFS
  typeface. Do not re-extract from the blog markdown or re-add title clauses
  to meanings.
- **No AI attribution in the repo, ever.** No Co-Authored-By trailers, no
  "generated with" footers, nothing named in contributors, commits, or code.
  Grep commit messages for `claude|co-authored|generated with|ai-attribution`
  before every push. After a push, `git ls-remote origin` should show only
  `refs/heads/main`.
- **Tool calls must be native**, never XML/DSML/card-formatted text
  (`<invoke>`/`<parameter>` are strictly prohibited).

## Versioning

`versionName`/`versionCode` live in `app/build.gradle.kts`; Settings shows
`BuildConfig.VERSION_NAME`, so they can never disagree. Rule: **+0.1 on
versionName, +1 on versionCode per release.** Sequence since the store
restart: 0.1 … 0.9, then 1.0 / 10, then 1.1 / 11, then **1.2 / 12 (current)**.

The release keystore path/credentials live in a `keystore.properties` outside
the repo (Google Play Signing Key folder). When absent (CI, fresh clone) the
release build degrades to unsigned rather than failing.

## Release hand-off (every push to main is a Play release candidate)

1. **Bump the version**: `app/build.gradle.kts`, the sequence above, and the
   version field in `docs/play-listing.md`.
2. **Write the "What's new" notes** (≤500 chars) into `docs/play-listing.md`
   — the copy/paste source for the Console.
3. **Verify locally**: full CI suite (`:app:testDebugUnitTest :app:lintDebug
   :app:assembleDebug :app:assembleRelease`).
4. **Commit and push**, verify CI green via the Actions API (full 40-char
   SHA — see CI).
5. **Build the signed AAB and verify the signature.** `./gradlew
   :app:bundleRelease` signs when `keystore.properties` exists (probed in
   `build.gradle.kts`: D: on LENOVO, E: on Dev Pro). Confirm with
   `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`
   ("jar verified"; the PKIX warning on the self-signed upload key is
   normal). Only if no keystore is available, fall back to the CI artifact
   and say so explicitly.
6. **Hand over the AAB and release notes.** Copy the bundle into the
   repo's `releases/` folder named with the version (e.g.
   `releases/ninetynine-1.0-vc10.aab`). Hand-off copies live in the repo —
   never on the Desktop or anywhere outside it — so they travel with the
   checkout to every machine. Paste the notes VERBATIM as a standalone
   copy-paste block — never just point at `play-listing.md`. Once the user
   confirms submission to Play, delete the copy from `releases/` (the App
   Bundle Explorer retains the artifact).
7. **Screenshots: decide explicitly, every time.** Visible UI changed →
   refresh the COMPLETE Play-ready sets (phone, 7-inch, 10-inch) in
   `docs/screenshots/` (`phone/`, `tablet7/`, `tablet10/`) from the
   screenshots.yml run — that folder IS the hand-off destination; never
   copy sets to the Desktop or anywhere else in the repo. If nothing
   visible changed, say "no new screenshots needed" and why. Captures
   come from the screenshots.yml run (see Store screenshots from CI
   below), never a hand-rolled local session.

### Store screenshots come from CI, not from a hand-rolled local session

The Play sets are whatever `screenshots.yml` captured, never per-machine
re-derivations. The workflow mirrors the proven one from the count-and-play
repo (API 35, KVM enabled, snapshot-less boots, cached AVDs) and goes green
in five to seven minutes (proven 2026-08-24, run 32762916428: all three
legs, 15 PNGs). It triggers on pushes touching UI files, or via
workflow_dispatch.

- When visible UI changes: wait for the run, download the three
  `store-screenshots-*` artifacts (phone/tablet7/tablet10, eight scenes
  each — 24 PNGs a full set),
  and hand over/refresh from exactly those PNGs.
  `gh run download <run-id> -R muntasimulhaque/ninetynine` works on both
  machines (gh is installed and authenticated on each).
- The repo keeps a copy of the latest full set in `docs/screenshots/`
  (`phone/`, `tablet7/`, `tablet10/`; the README thumbnails show the
  phone set). Refresh it from the run's artifacts and commit —
  `gh run download <run-id> -R muntasimulhaque/ninetynine -n
  store-screenshots-phone -D docs/screenshots/phone` (likewise
  tablet7/tablet10). Proven end-to-end by workflow_dispatch
  (2026-08-25, run 32815084999, green in ~4 minutes).
- Never re-capture a listing set by hand. The local adb recipes in Known
  quirks remain for interactive checks and one-off scenes; if a local capture
  fails twice, stop debugging the emulator and let CI do it.
- Port proven code by DIFFING against the source, never by re-typing from
  memory. When this workflow was ported, a re-typed line invented a
  nonexistent method (`getArguments` lives on InstrumentationRegistry, not on
  Instrumentation) and the upload step was dropped entirely; both reached CI
  because the local check read a pipeline's exit code instead of gradle's.
  Verify builds by the real exit code, never by grepping piped output.

## Build, test, verify

```bash
# Unit tests (canonical suite) + debug APK
./gradlew :app:testDebugUnitTest :app:assembleDebug

# Everything CI runs
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- Wrapper committed (9.5.0). `local.properties` is gitignored and lost on
  folder renames — recreate with
  `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` or the first
  build fails.
- Toolchain: JDK 17, AGP 9.3.0, Kotlin 2.4.10, Gradle 9.5.0, Compose BOM
  2026.06.01, compileSdk/targetSdk 37, minSdk 24. AGP auto-installs the
  android-37.0 platform it wants (android-37.1 does NOT satisfy it).
- AGP 9 has built-in Kotlin; the `kotlin-android` plugin is deliberately
  absent. Compiler options live in a top-level
  `kotlin { compilerOptions { jvmTarget } }` block.
- **Machines:** LENOVO (has Android Studio's JBR at
  `C:\Program Files\Android\Android Studio\jbr` + a Pixel 4 AVD running the
  android-37.1 image): build with
  `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" ./gradlew …`.
  Dev Pro: JDK 17 at `C:\Users\Dev Pro\.jdks\jdk-17.0.19+10`, SDK under its
  own AppData, no emulator.

## CI

- **build.yml** (push to main, PRs): unit tests, `lintDebug` (fails on new
  issues), `assembleDebug`, `assembleRelease` (minified, unsigned — R8 on
  every change), uploads the debug APK (7-day retention). Actions SHA-pinned;
  `permissions: contents: read`.
- **screenshots.yml** (pushes touching UI files, plus manual dispatch):
  captures the eight ScreenshotTest scenes on phone/7"/10" emulators at
  API 35 and uploads `store-screenshots-phone/-tablet7/-tablet10` artifacts.
  See Store screenshots from CI under Release hand-off.
- Verify green runs with `gh` — installed and authenticated on both machines
  (e.g. `gh run view <run-id> -R muntasimulhaque/ninetynine`). The raw
  Actions API with a token from `git credential fill` remains the fallback;
  there, `?head_sha=` matches only the FULL 40-char SHA; short SHAs return
  an empty list that reads as "running".

## Docs & Play listing assets

`docs/` holds `play-listing.md` (copy/paste listing doc), `play-icon-512.png`,
`play-feature-1024x500.png`, `privacy-policy.html` (hosted on GitHub Pages),
and the phone screenshots under `docs/screenshots/phone/` shown by the
README (the CI-captured sets live beside it in `docs/screenshots/tablet7/`
and `docs/screenshots/tablet10/`).
The tablet sets were removed once (commit `ac293db`) and re-added later as
CI-captured sets (owner decision, 2026-08-25); tablet captures are still
uploaded to Play by hand.

## Code layout

```
app/src/main/java/io/github/muntasimulhaque/ninetynine/
  MainActivity.kt        Host activity; deep links, consumeNameNumber,
                         cold-start guard, daily re-anchor.
  NamesApp.kt            Application; keeps (never re-anchors) the work.
  data/                  Name, NamesRepository (asset load), Prefs (DataStore).
  util/                  DailyName, DeckBuilder, QuizBuilder, SearchFilter
                         (pure, unit-tested).
  daily/                 DailyNameWidget (Glance), DailyScheduler
                         (WorkManager), TimeChangeReceiver,
                         PackageReplacedReceiver.
  ui/                    NamesViewModel (shared state) + per-screen packages:
                         home, detail, memorize (Flashcards/Quiz/Learned/
                         Memorize), bookmarks, share, settings, about.
  ui/theme/              Color, Type, Theme, Motion, Haptics, Shapes,
                         SquircleShape, components/.
app/src/main/assets/     names.json (99 entries), intro.txt, fonts/ (+licenses).
```

## State & crash-proofing rules

- **DataStore emits a frame or two late.** Any `stateIn(…, emptyList())`-style
  flow must be gated on a `*Loaded` flag before building UI, or you flash
  "0 learned"/"nothing kept"/a spinner over real data.
- **ViewModel flags must be declared BEFORE their eager flow.**
  `stateIn(Eagerly)` starts collecting immediately; if its
  `.onEach { _flag.value = true }` references a flag declared later, a cold
  start that reads DataStore during construction hits a null flag and crashes
  with an init-order NPE (this shipped once, `NamesViewModel.kt:87`).
- **Content reads are crash-proof.** `NamesRepository.load` wraps the asset
  read in `runCatching` (empty list → screens say so). `Prefs` uses
  `retryWhen` (a bare `catch` emits then COMPLETES the flow, killing every
  derived flow for the process); `Prefs.write` swallows exceptions
  (cancellation rethrown) because a failed save escaping a
  `viewModelScope.launch` kills the process over a toggle.
- `intro.txt` parse normalizes `\r\n` → `\n`.

## Design system (read before touching any size/color/spacing)

- **Arabic is set in KFGQPC Uthmanic Script HAFS**, Latin in Spectral; both
  bundled. HAFS is single-weight (W400) — **never let it synthesize a
  weight**; pin Normal in `ArabicText`/`MixedText`. KFGQPC's license forbids
  modification and derivative artwork; the font stays byte-identical.
- **`ArabicSize`** (in `ArabicText.kt`) names each Arabic size by the Latin
  slot it pairs with. Arabic must not inherit Latin sizes (HAFS body ~0.35em
  vs Spectral x-height 0.45em).
- **`FitText`** (PageParts) shrinks text to fit, stepping fontSize AND
  letterSpacing down together. Guard `TextUnit.Unspecified`. House pattern for
  anything that must never break a Divine Name or truncate a title.
- **Centering** baked into styles via `.copy(textAlign = Center)`; bare
  FitText doesn't center.
- **Never set a sentence in `labelMedium`/`labelSmall`** (they carry wide
  tracking — overlines only; prose is `bodySmall`+).
- **Reading rule:** the short meaning (`title`) shows only where the full
  `meaning` does NOT (Detail/Share/flashcard back: meaning only; list rows,
  hero card, widget, notification, quiz keep the title).
- **Shared components live in `PageParts.kt`** (BackButton, FitText,
  ScreenLabel, SectionLabel, NavRow, PageRule, SettingsAction, EmptyState,
  paperTopBarColors, scaledGap, readingMeasure, named insets). Reuse them.
- **Empty screens that offer an action use `EmptyState`** (title + optional
  line + optional TextButton); `PageMessage` stays for failure cases with no
  action. Empty states sit centred via `Modifier.fillParentMaxSize()` inside
  their `item {}` (a LazyItemScope member — no import). The bookmarks and
  learned empties offer "Browse the names" — an empty screen that can act
  should act.
- **Search shows its work:** literal query matches in a row's transliteration
  and title render gold + SemiBold. `util/Highlight` computes the ranges
  (≥2-char trimmed query, case-insensitive, non-overlapping); fuzzy-only
  matches stay uncoloured — never invent a span that corresponds to nothing.
  Only Home passes a `query` to `NameListItem`; other lists stay pristine.
- **The text-size slider previews live:** the specimen answers the bead
  mid-drag, set at the slider's CURRENT absolute value
  (`appTypography(sliderValue).headlineMedium`), not the theme's committed
  scale. Commit-on-release guards DataStore, never the preview.
- **Flashcard drags answer the hand:** the card wears an overline label —
  I KNOW IT / STILL LEARNING — that fades in toward the commit threshold, and
  a tick haptic fires exactly once as the drag crosses it. The label composes
  only while a drag is live (merged-node children reach TalkBack even at zero
  alpha), and its graded alpha reads in the draw phase so a moving finger
  redraws without recomposing the faces.
- **The quiz celebrates a new best:** `QuizViewModel.bestBefore` captures the
  standing best once, when a round finishes (guarded — a rotation re-runs the
  capturing effect after the round's own write has raised the stored best).
  The result page shows the gold NEW BEST overline only when an existing best
  fell; first rounds stay silent.
- **Pushed-screen TITLES sit left (`ScreenLabel` in `TopAppBar`); sequence
  COUNTERS sit centre** (`CenterAlignedTopAppBar`: detail "3 of 99",
  flashcards "3 of 12", quiz "3 of 10"). Don't mix.
- **Contrast:** keep WCAG 2.1 AA (4.5:1 text, 3:1 UI). `outline` vs
  `outlineVariant` carry real meaning in places; comments in `Color.kt` are
  mostly right but re-verify claims.
- **Motion/haptics:** `Motion.kt` (QUICK/GENTLE/CALM), `Haptics.kt`. The
  `@Composable` variants collapse to `snap()` when animator scale is 0; use
  non-composable `spec()` variants inside coroutines/gesture callbacks — AND
  inside `AnimatedContent.transitionSpec`, which is not composable either:
  hoist `LocalMotionScale.current` above and build specs from it. Content
  turns use the house push (fade GENTLE/Settle + rise it/12); nothing user-
  facing hard-cuts between states.
- **Counters roll, never teleport:** the Memorize count seeds its start from
  `rememberSaveable lastSeen` so returning after learning more animates old →
  new exactly once (first composition starts ON target — never from zero);
  the quiz score counts up once per result (`rememberSaveable played` guard).
- **Scroll thumbs:** reading pages use `ScrollbarThumb` (ScrollState); lazy
  lists use `LazyScrollbarThumb` (estimated from average row height × count —
  position cue only, shares THUMB_MAX_FRACTION/24dp floor). Both display-only;
  dragging would make them a fast-scroller (rejected decision).
- **Bottom bar:** the SELECTED tab's glyph fills; resting tabs wear outlined
  variants (`TopLevelRoute.iconResting`) — a third selection channel beside
  tint and label weight. Don't collapse back to one filled icon.
- **Splash is held until first frame:**
  `setKeepOnScreenCondition { !contentReady }`, released by a `SideEffect`
  after the first composition commits — without it a slow device flashes bare
  window background between splash and app.
- **The hero card rises in once:** the daily card enters like a pushed screen
  (fade + 24dp rise, CALM/Settle), guarded by rememberSaveable so returning
  to the tab or rotating never replays it — the same discipline as the detail
  and About entrance fades.
- **Every reading page carries the thumb:** About is a reading page too and
  runs to several screens; it wears the same quiet `ScrollbarThumb` the name
  pages do.
- **The share sheet offers the plate AND the words:** "Share text" sends the
  Arabic, the name and epithet on one line, the full meaning, and the store
  title — the card's hierarchy as plain text. The name page's meaning is the
  app's one selectable text (`SelectionContainer`, long-press to copy); the
  flashcard faces stay swipe surfaces on purpose.
- **Theme rows wear a swatch:** a 22dp circle of the theme's own paper with
  its ink as an 8dp bead — System split across both papers. The eye picks
  before the mind reads; the row still carries all the semantics.
- **Themed launcher icons already ship:** the adaptive icon's monochrome layer
  is the Kufic mark — do not re-add it.
- **The two axes need no manual, by design:** the pill is a verb ("Mark as
  learned"), the bookmark is the platform's universal keep glyph, and every
  empty state teaches its own axis at the moment it matters. An explainer
  line was tried and deleted on review — if the controls ever need one again,
  fix the controls, not the prose.
- **The notification's one line is set, not joined:** Arabic · transliteration,
  the same middle dot the feature graphic's tagline wears.
- **Widget corners follow the device:** render-time read of the framework
  dimen `system_app_widget_background_radius` (24dp on Pixel images), falling
  back to 20dp when an OEM omits it; still API 31+-only and still applied
  before `clickable` (see pitfalls below).

## Content invariants (guarded by NamesAssetTest)

`assets/names.json`: 99 sequential entries; no blank/duplicate fields;
NFC-normalized Arabic; every Arabic character drawable by the bundled HAFS TTF
(only U+0622 آ allowed — `forArabicFont()` decomposes it at render time);
every `meaning` begins with its `title` clause (Detail/Share/flashcard-back
render the title only inside the meaning — dropping the clause silently loses
it). Transliteration follows the source's convention, regularised in exactly
eight places (#28, #32, #44, #48, #80, #87, #94, #95).

## Testing

- **75 unit tests** (JUnit4, `app/src/test`): daily rotation, quiz generation
  + subsuming-distractor guards, search and the literal highlight ranges,
  deck building (incl. 10-card cap),
  ViewModels (incl. the tagged-selection contract that keeps a turning
  question's verdict and the best-before capture), NamesAssetTest over the
  real asset, CounterFormatTest.
  Count grows as guards are added — sum the XMLs in
  `app/build/test-results/testDebugUnitTest/`.
- Instrumentation (`ScreenshotTest`) renders eight scenes (home, home-dark,
  name, quiz, memorize, flashcards, share, settings) to the instrumentation
  run's additional test output
  directory (AGP copies them off-device for the workflow; local runs fall
  back to the app's files dir); pure render, no input injection, so it runs
  on API ≤ 35 images. Used by screenshots.yml and Android Studio captures.
- Pure logic lives in `util/` precisely so it is unit-testable.

## Editing pitfalls that bite

- **Files are CRLF** (`.gitattributes text=auto`). The patch tool fails to
  match old_text ending in a trailing newline — include the FOLLOWING line.
- **Repo-wide greps: use `git grep`** when the cwd path contains spaces, and
  it only searches tracked files so build output can't pollute a sweep.
- **After removing a Text/composable block, re-grep unused imports** — the
  project holds a zero-warning standard.
- **Modifier order matters:** `heightIn(max=X)` BEFORE `fillMaxHeight()`, or
  the cap is ignored; never pair `heightIn` on a Column child with
  `fillMaxHeight` on its children (expands to full screen, blanks the app).
- **Arabic widths from hmtx are nominal (isolated advances)** — no shaping in
  stdlib; shaped runs ~0.6–0.7× narrower. Never assert Arabic overflow from
  nominal widths alone ("upper bound, needs device check").
- **KFGQPC HAFS has no U+0622 آ and no en/em dash** (MixedText keeps dashes
  in Spectral). New Arabic avoids آ or relies on `forArabicFont()`.
- **Deep links:** activity is `singleTop` with an exported intent; consume the
  `nameNumber` extra and guard cold-start replay with
  `savedInstanceState == null`.
- **Launcher shortcuts** (long-press the icon → Flashcards, Quiz; API 25+,
  ignored below) ride a `startRoute` extra consumed exactly like
  `nameNumber`. `App()` pushes the memorize tab first, then the screen, so
  Back lands where a reader who walked there would be. Keep
  `res/xml/shortcuts.xml` in step with the ROUTE_* constants in
  MainActivity, and the two glyph drawables tinted by `shortcut_glyph`
  (values/ day emerald, values-night/ mint).
- **Scheduler anchoring:** `Application.onCreate` KEEPs the schedule
  (re-anchoring there cancels the work that woke the process);
  `MainActivity.onCreate` re-anchors, guarded by an is-running check.
- **`DailyName.numberFor` uses `Math.floorDiv`/`floorMod`, never `/` and
  `%`** — plain division breaks pre-epoch instants; a test guards it.
- **Widget/notification workers fold failures to `Result.retry()`**, not
  success — a swallowed throw used to skip the daily widget refresh until the
  next day.
- **Glance `cornerRadius` breaks `clickable` on API < 31** (its no-op path
  swallows the following clickable). Apply conditionally
  (`if (SDK_INT >= S)`), BEFORE `clickable`.
- **`appwidget-provider` MUST set `android:initialLayout`** (point it at
  `@layout/widget_preview`) or stricter launchers throw
  Resources$NotFoundException on bind.
- **A widget's tap PendingIntent dies on app update on Android 8.0–8.1.**
  Three layers of defense: `PackageReplacedReceiver` → WorkManager worker;
  `NamesApp.onCreate` calls updateAll every process start; `DailyNameWidget`
  always calls provideContent (fresh PendingIntent even on lookup failure).
  Not reproducible on Android 12+.
- **Pre-API 26 launcher icons need real bitmaps.** `mipmap-anydpi/` without
  `-v26` resolves below 26 where `<adaptive-icon>` can't inflate — devices
  got a default icon (fixed in 1.0). Keep the `mipmap-{mdpi…xxxhdpi}` PNGs
  while minSdk is 24.
- **`previewLayout` renders only on API 31+.** Pickers on 26–30 use
  `android:previewImage` (`drawable-nodpi/widget_preview_image.png`).
- **Generating image assets with Arabic locally:** Python/PIL has FreeType
  but NO raqm — raw text draws unshaped. Working recipe (1.0 widget preview):
  uharfbuzz shaping → `font.draw_glyph_with_pen` outlines → flatten curves →
  even-odd fill (XOR contours within a glyph, OR across glyphs); positions
  y-up, flip once at raster. Fonts in `res/font/`. Verify against the basmala.

## Decisions already settled — do not reopen

(Apppealable: bring a genuinely better idea to the user; approval reopens it.)

- The three name strings are never merged.
- No first-run epigraph/"opening" page (implemented, reverted — app opens on
  the list).
- No spaced-repetition queue, no separate review screen, no reverse
  flashcards, no sticky learned button, no first-run explainer line — all
  designed or built during the 1.2 review and rejected on the simplicity
  rule: the app stays a book that asks for almost no decisions, and its
  controls explain themselves. Re-propose only with a genuinely better idea.
- No grid view (implemented, removed).
- No `applicationId` change.
- No re-extracting `names.json`; no title clause re-added to meanings.
- Transliteration faithfully reproduced except those eight regularisations.
- `displayMedium` (30sp) deliberately sits unused in `Type.kt` — ask before
  deleting.
- No INTERNET / network / analytics / ads / billing — ever.
- Scrollbar thumb capped at 40% of track (`THUMB_MAX_FRACTION`), floor 24dp:
  exact position, clamped length cue. Don't "fix" back to raw proportions.

## Known quirks & accepted limitations

- The home-screen widget renders Arabic in the system serif (Noto Naskh), not
  HAFS — Glance can't bundle fonts; numerals/marks sanitized via
  `systemFontSafeArabic()`.
- Counters always render Western digits via `%1$s` on purpose (`%d` follows
  device locale; ar/ur bidi reversed the pairs). Guarded by CounterFormatTest.
- WorkManager notification timing drifts a few minutes (system batching).
- Local debug APKs sign with the machine keystore; CI artifacts with CI's —
  installing one over the other fails
  INSTALL_FAILED_UPDATE_INCOMPATIBLE and uninstalling wipes DataStore
  progress. Device-test from the CI artifact.
- The app deliberately has no SnackbarHost (reset has no Undo; some failures
  surface as Toast). Don't assume one exists.
- ScreenshotTest cannot run on local android-37.1 images (Espresso input
  injection dies on an InputManager reflection error). Listing screenshots
  come from the CI workflow (see Store screenshots from CI under Release
  hand-off); the local recipes below are fallback for interactive checks and
  one-off scenes: driving the real app over adb (`uiautomator dump` → match
  text or content-desc → `input tap` → `exec-out screencap`) with the debug
  APK installed, or ScreenshotTest on API ≤ 35 images (the test now saves to
  the AGP additional-test-output dir, not files/screenshots). Hard-won adb
  gotchas:
  - Returning from a name page restores search mode with the query intact —
    tap "Close search" first.
  - Match text EXACTLY, not by substring ("NAMES" also occurs inside
    "99 names still to learn").
  - uiautomator dumps go stale during animations/IME — retry until the node
    appears; dismiss the keyboard (BACK) before tapping rows; Gboard's toolbar
    panel swallows taps aimed through it.
  - The swiftshader SystemUI ANR appears seconds after launch — loop checks;
    if Wait doesn't stick, kill the emulator process and cold-boot
    (`-no-window -no-snapshot` is fine; first screencaps may still be black —
    poll for page content, never background colour). Better: prevent the
    dialog outright with `adb shell settings put global hide_error_dialogs
    1` — uiautomator dump runs through SystemUI's accessibility pipeline, so
    while the ANR dialog is up the dump itself fails and `Wait` can never be
    found by text; suppressing the dialog keeps dumps and captures clean.
  - The list starts 1 Allah, 2 Al-Ahad, 3 Al-A'laa … (source order):
    Ar-Rahmaan is NOT near the top. For detail/share scenes use search
    ("Aleem" → Al-Aleem, longest meaning, scrollbar thumb visible) or the row
    "Allah".
