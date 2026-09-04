# The Ninety Nine Names of Allah

A free, open-source, native Android app for reading and memorizing Al-Asma ul-Husna — the ninety nine names of Allah — with Arabic, transliteration, and meanings.

Based on the lecture of Sheikh Ibn Uthaymeen (Rahimahullah), as presented in *"The Ninety Nine Names of Allah: A Memorisation Tool with Transliteration and Meanings"*. Content curated at [muntasimulhaque.bearblog.dev/99-names](https://muntasimulhaque.bearblog.dev/99-names/).

Available on Google Play.

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=io.github.muntasimulhaque.ninetynine">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="70">
  </a>
</p>

## Screenshots

<p>
  <img src="docs/screenshots/phone/phone_home.png" width="205" alt="The home screen with the name of the day at the top">
  <img src="docs/screenshots/phone/phone_name.png" width="205" alt="A single name: Arabic, transliteration, meaning, and keep actions">
  <img src="docs/screenshots/phone/phone_flashcards-front.png" width="205" alt="Flashcards: the name side of the card">
  <img src="docs/screenshots/phone/phone_flashcards-back.png" width="205" alt="Flashcards: the meaning side of the card">
  <img src="docs/screenshots/phone/phone_quiz.png" width="205" alt="The ten-question quiz">
  <img src="docs/screenshots/phone/phone_memorize.png" width="205" alt="Memorization progress">
  <img src="docs/screenshots/phone/phone_bookmarks.png" width="205" alt="Bookmarks: the names you kept">
  <img src="docs/screenshots/phone/phone_share.png" width="205" alt="Share a name as a rendered card">
  <img src="docs/screenshots/phone/phone_settings.png" width="205" alt="Settings: theme, text size, and the daily name">
</p>

Tablet sets live beside the phone set in `docs/screenshots/` (`tablet7/`, `tablet10/`).

## Features

- **Read** — all 99 names with Arabic script set in KFGQPC Uthmanic Script HAFS — the typeface of the Madinah Mushaf — with transliteration and full meanings, plus scholarly notes (e.g. the distinction between Ar-Rahmaan and Ar-Raheem). Browse the list, swipe between names, and search by name, meaning, note, or number; on a name page the Name and its transliteration copy as one unit with a long-press, and the meaning below copies the same way.
- **Keep** — bookmark the names you turn to and find them together in their own tab, in the order they appear in the book. Separate from memorization: resetting your progress leaves your bookmarks alone.
- **Share** — turn any name into a beautifully rendered card (Arabic, transliteration, meaning) and share it as an image — or as plain text, for captions and notes.
- **Memorize** — flashcards with a flip animation and an "I know it / Still learning" loop, a ten-question quiz with a remembered best score and a revisit list naming what you missed, and a quiet progress count (no streaks, no gamification).
- **Daily** — a "Name of the Day" that rotates deterministically through all 99, shown on the home screen, as an optional notification at a time you choose, and as a resizable home-screen widget in the app's emerald-and-gold livery, its Name set in the bundled Mushaf typeface.
- **Considered** — warm paper light theme, dark, and true-black AMOLED; adjustable text size; quiet haptics; a tab bar that floats as a soft pill with a gentle shadow; bundled KFGQPC Uthmanic Script HAFS (Arabic) and Spectral (Latin) typefaces; predictive back; edge-to-edge.
- **Pure** — no ads, no analytics, no tracking, and **no INTERNET permission**, so the app cannot open a network connection at all. The only permission it declares for itself is notifications, and only if you turn the daily name on; the remaining manifest permissions come from Android's WorkManager library, which schedules the daily reminder. See [PRIVACY.md](PRIVACY.md).

## Found a mistake in the content?

Please say so — it is far more use as an issue than as a review. Open a
[content correction](https://github.com/muntasimulhaque/ninetynine/issues/new?template=content-correction.yml)
with the name's number (open the name; the counter at the top of its page
shows its position), what the app shows, and what it should say. From inside
the app, About → Send feedback opens an email instead.

Transliteration in particular has no single correct convention, and this app
follows its source rather than standardising it — so if a spelling looks wrong
to you, it is worth saying which convention you are going by.

## Building

1. Open the project in a recent Android Studio (AGP 9 requires the 2025.2 line or newer), or run `./gradlew assembleDebug` — the wrapper is committed, so nothing needs installing first.
2. Run on a device or emulator (minimum Android 7.0, API 24).
3. For a release build: **Build → Generate Signed App Bundle**.

Unit tests run with `./gradlew :app:testDebugUnitTest`: the daily-name rotation, quiz
generation, search and its highlight ranges, deck building, the flashcard and quiz
session contracts, and a guard over `assets/names.json` itself —
that it holds 99 sequential entries, no blank or duplicate fields, NFC-normalized
Arabic, and not one character the bundled Mushaf typeface cannot draw.

## Architecture

Single-module Kotlin app. Jetpack Compose + Material 3 with a small design system (theme, type scale, shared components), Navigation Compose with activity- and screen-scoped ViewModels, DataStore for progress and settings, WorkManager for the daily schedule, Glance for the widget, kotlinx.serialization for the bundled `assets/names.json`. No DI framework, no database — the content is a static JSON asset, which also makes translations straightforward (swap the asset per locale).

## License

**The code is MIT** — see [LICENSE](LICENSE). Take it, build on it, ship it.

**The content is not ours to license.** The text in `app/src/main/assets/`
(`names.json`, `intro.txt`) is reproduced from the lecture of Sheikh Ibn
Uthaymeen (Rahimahullah) as presented in the book named above. It is included
here for the benefit of anyone seeking to learn the names, with attribution
intact — the MIT grant covers the software around it, not that text.

One deliberate departure: the source's transliteration is inconsistent with
itself in eight places — a doubled consonant left single, a long vowel written
short — and those eight have been regularised to the convention the source
follows everywhere else (#28, #32, #44, #48, #80, #87, #94, #95). Nothing else
in the text has been altered.

**Bundled fonts** are under their own terms: Spectral under the SIL Open Font
License, and KFGQPC Uthmanic Script HAFS distributed free by the King Fahd
Glorious Quran Printing Complex and bundled unmodified — its license permits
use, copying and distribution but not modification or derivative artwork, so
the app's ٩٩ mark is drawn from Noto Naskh Arabic (OFL) instead. Both licenses
are in `app/src/main/assets/fonts/`.
