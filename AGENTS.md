# AGENTS.md

Ninety Nine Names: a free, offline Android app for reading and memorizing the
99 names of Allah in Arabic, with transliteration and meaning. Single-module
Kotlin, Jetpack Compose + Material 3, Navigation Compose, DataStore,
WorkManager, Glance, kotlinx.serialization. The content is
`app/src/main/assets/names.json`.

**The code is the source of truth.** Why a thing is the way it is lives in
the KDoc and comments next to that thing, and settled choices are tagged
`owner decision, <version>` so `git grep "owner decision"` finds them. This
file carries only what code cannot say: policy, why-this-not-that, the repo's
private vocabulary, and the release runbook. If this file and the code
disagree, the code wins; fix this file in the same change. Do not grow this
into a second codebase written in prose. Keep in mind this file is public:
no secrets, no private owner context.

## Start of every session: pull

The owner works from several machines, so this checkout is one of several.
`git fetch` and pull `main` before reading any other file or running any
command, without asking, and work on that head.

## Hard rules

Things no compiler enforces; violating one is a release blocker.

- **Never the Internet.** No INTERNET permission, ever: the app must be
  incapable of opening a connection. `POST_NOTIFICATIONS` is the only
  self-declared permission (the rest come from WorkManager), and `INTERNET`
  stays 0 in the merged manifest. Fonts and content are bundled; the few
  URLs in the app open an external browser.
- **`applicationId io.github.muntasimulhaque.ninetynine` is final.**
- **Three name strings, never merged:** `launcher_name`, `app_title`,
  `store_title` in `res/values/strings.xml`. The comment there explains each;
  `store_title` is exactly 30 characters (Play's limit) and is kept in step
  with the Play Console by hand.
- **Content stays the app's own.** Do not re-extract `names.json` from the
  blog and do not re-add title clauses to meanings; `NamesAssetTest` guards
  the rest. Transliteration follows the source except the eight places
  listed in README.md.
- **No AI attribution anywhere.** No Co-Authored-By trailers, no "generated
  with" footers, no name in contributors, commits or code. Before pushing,
  grep the log for `claude|co-authored|generated with|ai-attribution`; after
  pushing, `git ls-remote origin` shows only `refs/heads/main`.
- **No em dashes anywhere** (owner decision, 1.31): use a comma, semicolon,
  colon or parentheses. `NoEmDashTest` scans the app sources and the repo
  documents and fails the build; bundled licence notices are the one
  exception, being quoted verbatim.
- **Tool calls must be native**, never XML/DSML/card-formatted text
  (`<invoke>` and `<parameter>` are strictly prohibited).

## Build, test, verify

```bash
# the canonical suite
./gradlew :app:testDebugUnitTest :app:assembleDebug

# everything CI runs
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

- Verify by the process exit code, never by grepping piped output.
- `local.properties` is gitignored and easy to lose: recreate it with
  `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` (or the machine's
  real SDK path) or the first build fails. The Gradle wrapper is committed.
- Toolchain: JDK 17, AGP 9.3.0, Kotlin 2.4.10, Gradle 9.5.0, compile and
  target SDK 37, min 24. AGP has built-in Kotlin, so the `kotlin-android`
  plugin is deliberately absent and compiler options live in the top-level
  `kotlin { compilerOptions }` block. AGP auto-installs the android-37.0
  platform it wants; android-37.1 does not satisfy it.
- Machine notes: on Dev Pro, JDK 17 is at
  `C:\Users\Dev Pro\.jdks\jdk-17.0.19+10`, the SDK at
  `C:\Users\Dev Pro\AppData\Local\Android\Sdk`, and three API 35 AVDs are
  installed (`Pixel_4_35`, `Nexus_7_35`, `Pixel_C_35`, all `google_apis`
  x86_64); a third box (Windows profile `user`) has JDK 17 at
  `C:\Users\user\jdk\jdk-17.0.20.1+1`, the SDK at
  `C:\Users\user\android-sdk`, and a working API 35 `Pixel_4` AVD on which
  `ScreenshotTest` runs locally. `ScreenshotTest` cannot run on android-37.1
  images; listing captures come from CI regardless.

## Release hand-off

Every push to `main` is a Play release candidate.

1. **Version**: raise `versionName` by 0.1 and `versionCode` by 1 in
   `app/build.gradle.kts`, then update the version line in
   `docs/play-listing.md`. Read the current values from the file, never from
   this document.
2. **What's new notes** go in `docs/play-listing.md`, following the house
   rules in that file's header.
3. **Verify locally** with the full CI suite above.
4. **Commit and push**, then confirm CI is green:
   `gh run view <run-id> -R muntasimulhaque/ninetynine`. With the raw Actions
   API, `head_sha` matches only the full 40-character SHA; a short SHA reads
   as "running".
5. **Build the signed AAB and verify it**: `./gradlew :app:bundleRelease`
   (signing is probed for in `app/build.gradle.kts`; an absent keystore means
   an unsigned build, so check), then
   `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`
   ("jar verified"; the PKIX warning on the self-signed upload key is
   normal). Only if no keystore is available, use the CI artifact and say so.
6. **Hand over**: copy the bundle to
   `releases/ninetynine-<version>-vc<code>.aab` (hand-off copies live in the
   repo, never on the Desktop), and paste the notes verbatim as plain flowing
   text: no code fence, no indentation, each bullet one unbroken line. Delete
   the copy from `releases/` once the owner confirms the Play submission;
   the App Bundle Explorer retains it.
7. **Screenshots, decide explicitly every time.** If visible UI changed,
   refresh the complete phone, 7-inch and 10-inch sets in `docs/screenshots/`
   from the `screenshots.yml` run:
   `gh run download <run-id> -R muntasimulhaque/ninetynine -n store-screenshots-phone -D docs/screenshots/phone`
   (likewise `tablet7`, `tablet10`). Delete the stale set first: download
   overwrites files but never removes them. The tablet sets are uploaded to
   Play by hand. If nothing visible changed, say
   "no new screenshots needed" and why. Never capture a listing set by hand;
   local adb capture is for interactive checks only.

## CI

- `build.yml` (push to `main`, PRs): unit tests, `lintDebug` (fails on new
  issues), debug APK, minified unsigned release so R8 runs on every change;
  uploads the debug APK for 7 days.
- `screenshots.yml` (pushes touching UI files, manual dispatch): the
  canonical eight-scene `ScreenshotTest` set on phone, 7-inch and 10-inch
  API 35 emulators; artifacts named `store-screenshots-*`.
- Workflows are SHA-pinned and read-only (`permissions: contents: read`);
  keep both.

## Map

Navigate with this, then read the file's own KDoc before changing it.

| Path | What is there |
| --- | --- |
| `app/build.gradle.kts` | version, R8 rules, keystore probing (all commented) |
| `app/src/androidTest/java/.../ScreenshotTest.kt` | the canonical Play scene set; its KDoc lists the scenes |
| `app/src/main/assets/` | `names.json` (the content), `intro.txt`, bundled fonts and licences |
| `MainActivity.kt`, `NamesApp.kt` | entry points: deep links, shortcuts, splash gate, daily re-anchor, notification ask |
| `data/` | `Name`, `NamesRepository`, `Prefs` (DataStore; read before touching) |
| `util/` | pure logic, unit-tested: DailyName, DeckBuilder, QuizBuilder, SearchFilter, Highlight, ShareText |
| `daily/` | Glance widget, notification plate, WorkManager schedule |
| `ui/` | `App` (routes, transitions), `NamesViewModel`, `BottomBar`, one package per screen |
| `ui/theme/` | colors, type ramp, motion, haptics, shapes |
| `ui/theme/components/` | the shared furniture; `PageParts.kt` first; reuse, never re-draw |
| `app/src/test/` | JUnit guards: asset invariants, the em dash ban, pure logic, ViewModels |
| `docs/` | `play-listing.md`, icons, privacy policy, the CI screenshot sets (the phone set is also README's thumbnails) |
| `.github/workflows/` | `build.yml`, `screenshots.yml` |

## Glossary

Terms the code comments use as private vocabulary.

- **the book**: the whole content, `names.json` plus `intro.txt`.
- **title / meaning**: the short line and the full text. The reading rule
  (guarded by `NamesAssetTest`) is that the title shows only where the full
  meaning does not, and the meaning always opens with the title clause.
- **plate**: a raised surface wearing the emerald-and-gold identity (hero
  card, flashcard front, quiz card, share card, notification plate), and by
  extension the floating capsules.
- **folio**: a list row's number, the list's coordinate system for
  memorization ("I've memorized up to 19").
- **the column**: the centred width cap on wide screens, in three sizes:
  `readingMeasure` (prose), `pageMeasure` (pages), `barMeasure` (chrome).
- **device factor**: the 1.0 / 1.125 / 1.25 multiplier by smallest width,
  folded into the reading scale so a tablet prints the same book larger.
- **keep-acts**: Learned and Bookmark, the two acts on the detail plate.
- **the two voices**: tracked wide caps for annotation (overlines, counters,
  running heads), mixed case at `tabLabelStyle()` for chrome the reader taps.
  Prose is `bodySmall` or larger; never set a sentence in
  `labelMedium`/`labelSmall`.
- **ready gate**: the ViewModel flag separating "nothing decided yet" from
  "no round exists"; never index a round before it reads ready.
- **the house push**: the standard pushed-screen transition (fade GENTLE plus
  a gentle rise); content turns use it, tab switches crossfade.
- **seal**: the `MarkSeal` ring holding the square-Kufic mark, worn by the
  three earned moments.

## Decisions: do not reopen without approval

Appealable; bring a genuinely better idea to the owner and, if approved,
implement it and update this list.

- No DI framework, no database: the content is a static asset.
- The app is a book that asks almost no decisions. Rejected on that rule: a
  spaced-repetition queue, a review screen, reverse flashcards, a sticky
  learned control, a first-run explainer, a first-run epigraph page. It opens
  on the list.
- No grid view (built, removed).
- No navigation rail and no list-detail on tablets: the column.
- Settings is the fourth tab, quietest and rightmost; About is a nav row at
  its foot.
- Search lives in the home bar only, and Back unwinds search one layer per
  press before it can leave a top-level tab.
- The daily reminder is on by default; consent is the system permission
  dialog, never prose.
- No SnackbarHost: reset has no Undo, some failures surface as a Toast.
- Scroll thumbs are position cues, not draggable fast-scrollers.
- `displayMedium` is deliberately unused in `Type.kt`; ask before deleting.
- Store screenshots are the canonical CI scenes; never re-derive them.

## Traps with no code home

- Files are CRLF. When patching, include the following line so an old_text
  ending in a newline still matches.
- For repo-wide sweeps use `git grep`: the checkout path contains spaces, and
  it sees tracked files only.
- After changing a screen's signature, run
  `./gradlew :app:compileDebugAndroidTestKotlin` locally: `ScreenshotTest`
  renders screens directly, and the canonical suite does not compile it.
- After deleting UI code, re-grep unused imports; the project holds a
  zero-warning standard and lint fails on new issues.
- Screenshot diffs are noisy in three known ways: flashcards and quiz render
  shuffled content, and the Settings swatch rings can differ by a hair in a
  few pixels. A real change moves ink across a plate, not a few sub-pixel
  edges.
- A local debug APK will not install over a CI artifact (different signers),
  and uninstalling wipes DataStore progress: device-test from the CI artifact.
- Pre-API 26 devices need the real bitmap mipmaps; `mipmap-anydpi-v26` alone
  does not resolve below 26.
- When porting proven code, diff against the source; never retype from
  memory.
