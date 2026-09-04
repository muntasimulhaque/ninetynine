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
action of every session, immediately after reading this file, before any other
file is read or command run. Do it without asking, without exception.

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
`BuildConfig.VERSION_NAME`, so they can never disagree. Rule: **+0.1 on versionName, +1 on versionCode per release** (currently **1.27 / 37**; there is no 1.18 — it was skipped, don't go looking for it).

The release keystore path/credentials live in a `keystore.properties` outside
the repo (Google Play Signing Key folder). When absent (CI, fresh clone) the
release build degrades to unsigned rather than failing.

## Release hand-off (every push to main is a Play release candidate)

1. **Bump the version**: `app/build.gradle.kts`, the sequence above, and the
   version field in `docs/play-listing.md`.
2. **Write the "What's new" notes** (≤500 chars) into `docs/play-listing.md`
   — the copy/paste source for the Console. No boilerplate beyond what the
   release actually touches: the closing "All 99 Names and your progress are
   unchanged" line appears ONLY when content or progress behaviour really
   was at risk and the note reassures about it — an ordinary UI change
   carries no such line (owner decision, after 1.21). Set each bullet as
   one unbroken line, no mid-sentence wraps: the hand-off paste is VERBATIM,
   and hard breaks from the source force the owner to rejoin every line in
   the Console's textbox by hand (owner decision, after 1.25).
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
   checkout to every machine. Paste the notes VERBATIM as plain flowing
   text — no code fence, no indentation, no leading spaces, each bullet one
   unbroken line — never just point at `play-listing.md`. What the owner
   copies must paste straight into the Console's textbox with no rework. Once the user
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

The Play sets are whatever `screenshots.yml` captured (phone/7"/10" emulators, API 35), never per-machine re-derivations. It triggers on pushes touching UI files, or via workflow_dispatch.

- When visible UI changes (or the canonical scene set itself changes, as
  in 1.23): wait for the run, download the three
  `store-screenshots-*` artifacts and refresh `docs/screenshots/` from
  exactly those PNGs:
  `gh run download <run-id> -R muntasimulhaque/ninetynine -n
  store-screenshots-phone -D docs/screenshots/phone` (likewise
  tablet7/tablet10). `gh` is installed and authenticated on both machines.
- Never re-capture a listing set by hand. The local adb recipes in Known
  quirks are for interactive checks and one-off scenes; if a local capture
  fails twice, stop debugging the emulator and let CI do it.
- Port proven code by DIFFING against the source, never by re-typing from
  memory. Verify builds by the real exit code, never by grepping piped output.

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
  captures the eight ScreenshotTest scenes on phone/7"/10" emulators (API 35)
  and uploads the three `store-screenshots-*` artifacts.
- Verify green runs with `gh` — installed and authenticated on both machines
  (e.g. `gh run view <run-id> -R muntasimulhaque/ninetynine`). The raw
  Actions API with a token from `git credential fill` remains the fallback;
  there, `?head_sha=` matches only the FULL 40-char SHA; short SHAs return
  an empty list that reads as "running".

## Docs & Play listing assets

`docs/` holds `play-listing.md` (copy/paste listing doc), `play-icon-512.png`,
`play-feature-1024x500.png`, `privacy-policy.html` (hosted on GitHub Pages),
and the CI-captured screenshot sets in `docs/screenshots/` (`phone/` — the
README thumbnails — plus `tablet7/` and `tablet10/`; tablet captures are
uploaded to Play by hand).

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
                         (WorkManager), DailyPlate (notification plate),
                         TimeChangeReceiver, PackageReplacedReceiver.
  ui/                    NamesViewModel (shared state) + per-screen packages:
                         home, detail, memorize (Flashcards/Quiz/Learned/
                         Memorize), bookmarks, share, settings, about.
  ui/theme/              Color, Type, Theme, Motion, Haptics, Shapes,
                         SquircleShape, components/.
app/src/main/assets/     names.json (99 entries), intro.txt, fonts/ (+licenses).
```

## State & crash-proofing rules

- **DataStore emits a frame or two late.** Gate `stateIn(…)` flows on a
  `*Loaded` flag before building UI, or you flash
  "0 learned"/"nothing kept"/a spinner over real data.
- **ViewModel flags must be declared BEFORE their eager flow.**
  `stateIn(Eagerly)` collects immediately; an `.onEach` touching a
  later-declared flag crashes cold start with an init-order NPE.
- **Content reads are crash-proof.** `NamesRepository.load` catches
  `Exception` (`CancellationException` rethrown; empty list → screens say
  so). `Prefs` reads through `retryWhen` (a bare `catch` COMPLETES the
  flow, killing every derived flow for the process) and sanitizes what it
  returns; `Prefs.write` swallows `Exception` (cancellation rethrown) and
  validates what it stores — a failed save must never kill the process
  over a toggle.
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
- **Two voices for small type (owner decision, 1.22):** tracked wide caps are
  the register of ANNOTATION — `SectionLabel`, `ScreenLabel`, the overlines
  ("NAME OF THE DAY", "I KNOW IT / STILL LEARNING", "NEW BEST"), the counters
  ("QUESTION 1 OF 10") and the share wordmark. Mixed case at `tabLabelStyle()`
  (9sp × device factor, 0.5sp tracking) is the register of CHROME THE READER
  TAPS — the four tab labels and the detail plate's Learned / Bookmark. Don't
  set a tappable label in caps, and don't lowercase an overline. Any
  `.uppercase()`/`.lowercase()` on user-visible text or matching logic must be
  locale-insensitive (`Locale.ROOT`) — the default-locale form renders
  "MEMORİZE" on Turkish devices.
- **Reading rule:** the short meaning (`title`) shows only where the full
  `meaning` does NOT (Detail/Share/flashcard back: meaning only; list rows,
  hero card, widget, notification, quiz keep the title).
- **Shared components live in `PageParts.kt`** (BackButton, FitText,
  ScreenLabel, SectionLabel, NavRow, PageRule, FloatingBar, EmptyState,
  paperTopBarColors, scaledGap, readingMeasure, named insets). Reuse them.
- **Empty screens that offer an action use `EmptyState`** (title + optional
  line + optional TextButton); `PageMessage` stays for failure cases with no
  action. Centred via `Modifier.fillParentMaxSize()` inside their `item {}`
  (a LazyItemScope member — no import). An empty screen that can act should
  act — the bookmarks and learned empties offer "Browse the names".
- **Search shows its work:** literal query matches in a row's transliteration
  and title render gold + SemiBold. `util/Highlight` computes the ranges
  (≥2-char trimmed query, case-insensitive, non-overlapping); fuzzy-only
  matches stay uncoloured — never invent a span that corresponds to nothing.
  Only Home passes a `query` to `NameListItem`; other lists stay pristine.
- **Search lives in the bar, one entry point, everywhere:** the home bar
  carries a magnifier at its end; tapping it swaps the running head for a
  BasicTextField (Crossfade, QUICK) and the keyboard rises. Typing filters
  live through the shared ViewModel query; openness is `rememberSaveable`
  AND re-derived from a live query (a filtered list must never appear
  without its field). **Back unwinds search one layer per press** — typed
  text → empty field → out of search — and only past all three does Back
  exit on a top-level tab; never eject a reader who can still see evidence
  of their search. The ✕ clears AND closes in one tap. The query persists
  until cleared (✕, Back, or the no-results empty's "Clear search") — and it
  survives process death too, riding the ViewModel's SavedStateHandle
  (openness is rememberSaveable; the query must be, or an open field would
  restore empty).
- **Settings is the fourth tab; About lives at its foot:** Settings joined
  the bar rightmost and quietest (owner decision, 1.18); top bars carry
  content only. About sits as the gold-chevron `NavRow` at the foot of the
  Settings page, above the version line; Settings wears the quiet running
  head and no back button (it is a tab, not a pushed screen), and Back from
  About still lands on Settings. Worst case (2.0 system font scale on a
  320dp phone) the longest label still fits above the FitText floor —
  nothing clipping.
- **Tab heads differ by register:** Home passes `sizeScale = 1f` to
  `TabTitle` — the book's title page, at the full `headlineSmall` where the
  measured width allows (FitText shrinks it back for the second bar icon or
  a large font scale) — while Bookmarks and Memorize keep the default 0.85
  quiet running head.
- **The list rows carry their folio numbers** (owner decision): not lookup
  scaffolding but the list's coordinate system — the way memorization
  speaks ("I've memorized up to 19"). A book's folio, not a badge:
  `onSurfaceVariant` `labelLarge`, right-aligned in a measured widest-number
  column (`folioWidth()`) so the units digits line up down the page.
  Dividers start where the names do (`nameRowTextInset()`), never under the
  numbers; `NameRowInset` stays the row's outer margin.
- **The flashcards carry no instruction lines:** "Tap the card…" and the
  swipe hint are gone — the whole front face is one plate holding one Name
  (nothing else to tap), and the drag answers the hand through the I KNOW IT
  / STILL LEARNING overline. The fixed-height box under the card remains so
  the undo control never resizes the deck.
- **The text-size slider previews live:** the specimen answers the bead
  mid-drag, set at the slider's CURRENT absolute value × the device factor
  — not the theme's committed scale — so the preview matches the page on
  the device it is standing on. Commit-on-release guards DataStore, never the
  preview.
- **Flashcard drags answer the hand:** the card wears an overline label —
  I KNOW IT / STILL LEARNING — that fades in toward the commit threshold, and
  a tick haptic fires exactly once as the drag crosses it. The label composes
  only while a drag is live (an invisible merged child still reaches
  TalkBack), and its graded alpha reads in the draw phase — a moving finger
  redraws without recomposing the faces.
- **The quiz celebrates a new best:** `QuizViewModel.bestBefore` captures the
  standing best once, when a round finishes (a rotation re-runs the capturing
  effect after the round's own write — capture-once, or the moment never
  fires). The gold NEW BEST overline shows only when an existing best fell;
  first rounds stay silent.
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
- **Counters roll, never teleport:** the Memorize count seeds from
  `rememberSaveable lastSeen` (first composition starts ON target — never
  from zero); the quiz score counts up once per result (`rememberSaveable
  played` guard).
- **Scroll thumbs:** reading pages use `ScrollbarThumb` (ScrollState) —
  About included, it runs to several screens — lazy lists use
  `LazyScrollbarThumb` (position cue only, THUMB_MAX_FRACTION/24dp floor).
  Both display-only; dragging would make them a fast-scroller (rejected
  decision).
- **Bottom bar:** the SELECTED tab's glyph fills; resting tabs wear outlined
  variants (`TopLevelRoute.iconResting`) — a third selection channel beside
  tint and label weight. Don't collapse back to one filled icon. A tap's press
  highlight clips to the bar's own capsule register
  (`RoundedCornerShape(50)` before `selectable`) — never a hard-cornered
  rectangle inside the pill plate.
- **Splash is held until first frame AND the theme is known:**
  `setKeepOnScreenCondition { !contentReady || !themeSettled }` — released by a
  `SideEffect` after the first composition commits AND once the stored
  theme/text-scale have been read from DataStore (bounded, 400 ms timeout —
  MainActivity). Without it a slow device flashes bare window background
  between splash and app, and without the theme gate the first committed
  frame is the flows' defaults: a DARK/BLACK reader on a light system saw the
  app flash light before its real theme landed.
- **Motion only where meaning changes:** pages turn, counters roll, drags
  answer the hand, and pushed screens arrive through the house push —
  everything else stands still. First frames get no entrance (the hero,
  detail and About fades were removed); the daily card still turns at
  midnight, because THAT is a change of meaning.
- **Landing at the top means ALL of the top:** re-tapping NAMES animates
  item 0 back into view AND reveals the tucked-away home bar — HomeScreen
  snaps `scrollBehavior.state.heightOffset` to 0 on ARRIVAL at item 0, an
  edge, not a state, so a continuous watch can't fight the tuck. The RE-TAP
  itself is one contract every tab answers (owner decision, 1.19): Names and
  Bookmarks hoist their `LazyListState` (scroll to item 0), Memorize and
  Settings hoist a `ScrollState` (scroll to offset 0) — their screens take a
  defaulted `scrollState` parameter so test call sites keep their own state.
- **The detail plate's keep-acts wear the quiet ink and a short label:**
  resting, the check-circle and bookmark render `onSurfaceVariant` — the
  same grey as the top bar's share icon, not the page's near-black — and
  each carries a short chrome label (Learned / Bookmark — mixed case, the
  chrome-you-tap voice of the two-voice register) at the tab bar's
  `tabLabelStyle()` register (9sp × device factor, FitText-fitted,
  `clearAndSetSemantics` so TalkBack keeps hearing the full action + state
  once). They are explicit stadium-clipped clickable Columns with a 48dp
  touch floor — NOT IconButtons (see pitfalls): an IconButton's circle clip
  cuts labels mid-glyph. Active, they fill gold as always. The full phrase
  "Mark as learned" cannot fit the plate's centre slot at a readable size;
  the short words can (owner decision, 1.19).
- **The share sheet offers the plate AND the words:** "Share text" sends the
  Arabic, the name and epithet on one line, the full meaning, and the store
  title — the card's hierarchy as plain text. The name page's meaning is the
  app's one selectable text (`SelectionContainer`, long-press to copy); the
  Name pairs its Arabic and transliteration into one selectable unit above
  the meaning. The flashcard faces stay swipe surfaces on purpose.
- **The share sheet must always settle:** its card scroller wears the
  `quenchUpward` nested-scroll connection (ShareSheet.kt), which eats upward
  drag/fling leftover between content and sheet — without it the
  near-full-height sheet oscillates against its own bounds (m3 1.4.0,
  `skipPartiallyExpanded`). It must chain BEFORE `.verticalScroll`: after
  it, the connection is a DESCENDANT the leftover never passes through.
  Do not remove it as redundant, and keep any future sheet content behind
  the same guard.
- **Theme rows wear a swatch:** a 22dp circle of the theme's own paper with
  its ink as an 8dp bead — System split across both papers. The eye picks
  before the mind reads; the row still carries all the semantics.
- **Themed launcher icons already ship:** the adaptive icon's monochrome layer
  is the Kufic mark — do not re-add it.
- **The two axes need no manual, by design:** the name page carries one
  floating capsule (`DetailNavPlate`, the same `FloatingBar` plate the tab
  bar wears) holding everything a reader does to a name — previous and next
  wearing the neighbour's transliteration (FitText-fitted at `titleSmall`,
  20dp chevrons — the longest transliteration shrinks a little instead of
  ellipsizing), and the two acts of keeping side by side: an unfilled
  check-circle that fills gold when learned (`LearnedAction`), and the
  bookmark. Share alone stays in the top bar (a send-away act reads at the
  page's edge; five slots would crowd a 320dp phone). The capsule is FIXED
  and an OVERLAY on the pager, not a Scaffold bottom bar — a reserved slot
  clips the meaning at the plate's top edge and the floating read dies
  (1.21): its measured height (onSizeChanged) becomes the clearance the
  page's tail scrolls above, inside the min-height column so a short page
  never scrolls and a long one gains exactly the extent it needs. The
  weighted end slots keep the keep-acts centred on first/last pages; labels
  change as the pager settles, the same moment the counter does. If the
  controls ever need an explainer line again, fix the controls, not the prose.
- **The notification's one line is set, not joined:** Arabic · transliteration,
  the same middle dot the feature graphic's tagline wears.
- **Widget corners follow the device:** render-time read of the framework
  dimen `system_app_widget_background_radius` (24dp on Pixel images), falling
  back to 20dp when an OEM omits it; still API 31+-only and still applied
  before `clickable` (see pitfalls below).
- **Arabic is tagged `ar` for readers:** `ArabicText` and `MixedText`'s Arabic
  runs carry an `ar` locale span (`ArabicLocale` in ArabicText.kt), so
  TalkBack picks an Arabic voice for the Name instead of attempting it with
  the default English one. Keep the span on any new Arabic surface.
- **Wide screens keep the book's column:** full-screen content sits inside a
  centred `pageMeasure()` cap (560dp × the reading scale; the name page at
  `readingMeasure()`), so a tablet gets page proportions, not rows stretched
  edge to edge, and the list thumbs hug the column. The pattern is
  `fillMaxSize().wrapContentWidth(CenterHorizontally).widthIn(max = …)` —
  wrapContentWidth BEFORE widthIn, the same order rule as the heightIn
  pitfall. Phones never reach the cap.
- **Chrome joins the column:** every top bar (all eight call-sites) and the
  bottom bar's divider + tabs wear `Modifier.barMeasure()` (PageParts) — the
  same fillMaxWidth · wrapContentWidth · widthIn(max = pageMeasure()) chain —
  so on wide screens the running head, the page and the footer share one set
  of margins. Same cap, same order rule; on phones (and 7" portrait,
  600dp < the cap) it never binds and nothing changes.
- **Wide devices set larger type — the device factor:** sp type is
  physically identical on every screen, which reads small at the distance a
  7"/10" tablet is held. `Names99Theme` folds a factor (1.0 phone / 1.125 at
  ≥600sw / 1.25 at ≥840sw — smallest-width, so rotation cannot change it)
  into the reading scale, so typography, Arabic, column caps and gaps all
  grow together — the same book in a larger format, proportions unchanged.
  The bottom bar's labels take the device factor but NEVER the reader's
  slider; the widget and the notification plate keep their own fixed sizing.
- **The reminder is on by default:** `dailyEnabled` defaults to true, so a
  fresh install gets the Name each morning without finding a switch. The
  reader's consent lives in the system dialog, not in prose: MainActivity
  asks for POST_NOTIFICATIONS once at first launch (API 33+ only, guarded by
  the `notifications_asked` pref, written before the dialog opens so a
  process death mid-dialog never nags), and ONLY when the reminder is
  actually wanted — a reader whose pref says off is never asked. A denial
  writes the pref off and cancels the work, so switch, scheduler and worker
  agree. Below API 33 there is nothing to ask and the reminder just works.
- **The daily notification expands to the plate:** `DailyPlate` renders the
  hero-card identity (HAFS Arabic via Canvas — `DailyNameWidget.arabicBitmap`,
  internal — plus Spectral Latin) into a 16:9 bitmap for BigPictureStyle,
  falling back to the plain BigTextStyle when a render fails. Collapsed, the
  notification is unchanged. With the plate up, the summary is the BARE tap
  hint (`notification_summary_hint`) — the short meaning lives in the plate,
  and repeating it made the expanded shade read as the old text notification
  duplicated beneath a card of itself; the BigText fallback keeps the full
  `{title}. Tap to read the full meaning.` line, where the meaning has
  nowhere else to be. `MainActivity.onResume` nudges the widget only
  when the local day has changed (`widgetNudgeDay`), not on every resume.
- **`SettleOnce`** (PageParts) is the shared one-time settle: scale from
  `fromScale` on the lively spring plus a QUICK fade, played once per arrival
  (saved-instance-state guarded; snap at animator scale 0). PerfectSeal uses
  it at 0.6; the all-learned ٩٩ at the default 0.85.

## Content invariants (guarded by NamesAssetTest)

`assets/names.json`: 99 sequential entries; no blank/duplicate fields;
NFC-normalized Arabic; every Arabic character drawable by the bundled HAFS TTF
(only U+0622 آ allowed — `forArabicFont()` decomposes it at render time);
every `meaning` begins with its `title` clause (Detail/Share/flashcard-back
render the title only inside the meaning — dropping the clause silently loses
it). Transliteration follows the source's convention, regularised in exactly
eight places (#28, #32, #44, #48, #80, #87, #94, #95). The honorific is
spelled "Rahimahullah" (regularised from the source's "Rahimuallah": the
intro, and #26's meaning).

## Testing

- **80 unit tests** (JUnit4, `app/src/test`): daily rotation, quiz generation
  + subsuming-distractor guards, search and the literal highlight ranges,
  deck building (incl. 10-card cap),
  ViewModels (incl. the tagged-selection contract that keeps a turning
  question's verdict and the best-before capture, plus corrupted-restore
  guards for the quiz and deck), NamesAssetTest over the
  real asset, CounterFormatTest.
  Count grows as guards are added — sum the XMLs in
  `app/build/test-results/testDebugUnitTest/`.
- Instrumentation (`ScreenshotTest`) renders the CANONICAL PLAY SCENE SET
  (owner decision, 1.23; trimmed to eight scenes in 1.27 — the Memorize page
  left the set, so a refresh is 8 × 3 sizes = 24 captures, and the phone set
  now fits Play's 8-per-form-factor cap exactly; no scene targets a
  particular name — any name will do):

    1. `home` — the Names page
    2. `flashcards-front` and `flashcards-back` — BOTH faces of the card
       (the back is reached by flipping the deck ViewModel directly, never
       by injecting a tap — the test stays a pure render)
    3. `quiz` — the Quiz page
    4. `bookmarks` — the Bookmarks page, POPULATED (the first three loaded
       names are bookmarked through the ViewModel and the capture waits for
       the rows; an empty shelf says nothing)
    5. `settings` — the Settings page, with the reminder seeded ON first
       (the app's real default; a reused CI device's DataStore once showed
       the advertised toggle off, and the capture must not lie)
    6. `name` — a name page (the first in the book; "any name")
    7. `share` — a name's share screen (the first loaded name; the plate
       renders outside the sheet, which the compose root cannot capture)

  rendered to the run's additional test output directory (AGP copies them
  off-device for the workflow; local runs fall back to the app's files dir);
  pure render, no input injection, so it runs on API ≤ 35 images. The rule is
  `createAndroidComposeRule<ComponentActivity>()` — the plain rule exposes no
  `.activity`, which the flashcard scenes need. A stale set in
  `docs/screenshots/` must be DELETED before re-downloading (`gh run download`
  overwrites but never removes).
- Pure logic lives in `util/` precisely so it is unit-testable.

## Editing pitfalls that bite

- **Files are CRLF** (`.gitattributes text=auto`). The patch tool fails to
  match old_text ending in a trailing newline — include the FOLLOWING line.
- **IconButton clips its content to a 48dp circle.** Anything taller than
  an icon — an icon+label column, a two-line stack — is measured fine but
  CUT mid-glyph, worst just off-centre where the inscribed chord narrows to
  ~44dp; FitText cannot save it, because the text fits the constraints and
  the clip eats it. Multi-element buttons are explicit Columns with
  `clip(RoundedCornerShape(50))` before `clickable` and
  `minimumInteractiveComponentSize()` for the touch floor (as the detail
  plate's keep-acts do).
- **Repo-wide greps: use `git grep`** when the cwd path contains spaces, and
  it only searches tracked files so build output can't pollute a sweep.
- **After removing a Text/composable block, re-grep unused imports** — the
  project holds a zero-warning standard.
- **Changing a screen's signature breaks `ScreenshotTest`** — the
  instrumentation source renders HomeScreen, DetailScreen, SettingsScreen et
  al. DIRECTLY, so an argument removed from a screen is a compile error CI
  only reaches at `:app:compileDebugAndroidTestKotlin` (inside
  screenshots.yml, all three legs red). The canonical suite does not compile
  it: after any screen-signature change, run that task locally before
  pushing. (MemorizeScreen left the set in 1.27, so its signature is no
  longer compiled by the instrumentation source — signature changes there
  now surface only through CI's build legs, not the capture legs.)
- **`ScrollState.animateScrollTo` kills the app when the animation actually runs.** Declared `Unit`, it passes its `$completion` straight through to `animateScrollBy` (declared `Float`): when the scroll suspends ≥1 frame, the resumption receives a Float where a Unit was promised — `ClassCastException`. Call the Float-typed pair instead — `scrollTo` (jump) / `animateScrollBy` (animated). The lazy/pager equivalents (`scrollToItem`, `animateScrollToItem`, `scrollToPage`, `animateScrollToPage`) are proper state machines and safe.
- **`FitText` beside a fixed sibling in a `Row` must be measured LAST.** Row measures children left to right against the width that remains: a `FitText` placed BEFORE the sibling sees the full row width, declines to shrink, and the sibling then overflows the slot. Give such a `FitText` `Modifier.weight(1f, fill = false)` so the fixed children are measured first. The same pattern appears in the share wordmark (seal + spacer before it) — already correct, keep it that way.
- **Modifier order matters:** `heightIn(max=X)` BEFORE `fillMaxHeight()`, or
  the cap is ignored; never pair `heightIn` on a Column child with
  `fillMaxHeight` on its children (expands to full screen, blanks the app).
  The same is true in nested scroll: a `NestedScrollConnection` must chain
  BEFORE (outside of) the `.verticalScroll` it guards — after it, the
  connection is a descendant and the scroller's own leftover never passes
  through it (this shipped the share-sheet shake for one release).
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
- No navigation rail or list-detail on tablets — both were built, compared,
  and set aside for the column.

## Known quirks & accepted limitations

- The widget's Arabic renders in the bundled HAFS — drawn into a bitmap
  by Canvas (which shapes vocalized text correctly), stepped down until the
  whole line box fits, since Glance Text can't wear bundled fonts.
  Latin falls back to the system serif; the notification still draws with
  system fonts and keeps `systemFontSafeArabic()` sanitization for الله.
- Counters always render Western digits via `%1$s` on purpose (`%d` follows
  device locale; ar/ur bidi reversed the pairs). Guarded by CounterFormatTest.
- WorkManager notification timing drifts a few minutes (system batching).
- At a 2.0 system font scale the detail plate's neighbour labels degrade to
  ellipsis ("Al…") — the keep-act labels crowd the end slots below even the
  0.4 floor. Reader-range scales always render the name whole; the truncation
  is FitText's pathological-scale insurance (accepted 1.20).
- A local debug APK won't install over a CI artifact and vice versa
  (different signers — INSTALL_FAILED_UPDATE_INCOMPATIBLE), and uninstalling
  wipes DataStore progress. Device-test from the CI artifact.
- The app deliberately has no SnackbarHost (reset has no Undo; some failures
  surface as Toast). Don't assume one exists.
- ScreenshotTest cannot run on local android-37.1 images (Espresso
  InputManager reflection error) — listing captures come from CI. Local
  fallback for one-off scenes: driving the real app over adb (`uiautomator dump` → match
  text or content-desc → `input tap` → `exec-out screencap`) with the debug
  APK installed, or ScreenshotTest on API ≤ 35 images (the test now saves to
  the AGP additional-test-output dir, not files/screenshots).
Local adb gotchas:
- **Two capture nondeterminisms (proven 1.22):** the scenes render the
  SCREENS directly, so MainActivity's bottom bar never appears — tab-label
  changes are invisible to the sets. And flashcards/quiz show SHUFFLED
  content (unseeded `Random`), so those two PNGs legitimately differ on
  every run. Diff old/new PNGs before assuming a regression.
  - Search lives in the home bar: stop an upward scroll to reveal it (or
    re-tap NAMES), tap the magnifier; a live query
    persists until cleared — tap the bar's ✕ ("Close search") or Back before tapping rows you expected from the full list.
  - Match text EXACTLY, not by substring ("NAMES" also occurs inside
    "99 names still to learn").
  - uiautomator dumps go stale during animations/IME — retry until the node
    appears; dismiss the keyboard (BACK) before tapping rows; Gboard's toolbar
    panel swallows taps aimed through it.
  - The swiftshader SystemUI ANR appears seconds after launch — loop checks,
    else cold-boot (`-no-window -no-snapshot`; poll for page content, never
    background colour). Better: `adb shell settings put global
    hide_error_dialogs 1` up front — while the dialog is up, dumps fail and
    `Wait` can never be found by text.
  - The list starts 1 Allah, 2 Al-Ahad, 3 Al-A'laa … (source order):
    Ar-Rahmaan is NOT near the top. For detail/share scenes use search
    ("Aleem" → Al-Aleem, longest meaning, scrollbar thumb visible) or the row
    "Allah".
