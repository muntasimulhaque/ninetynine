# Play Store listing: copy these in

Updated each release (version field and What's-new notes included). Everything
below is ready to paste into the Play Console. The **app name is exactly 30 characters** (Play's limit),
so do not add a word or a stray space.

---

## App identity

- **Application ID (package name):** `io.github.muntasimulhaque.ninetynine`
- **Privacy policy URL:** `https://muntasimulhaque.github.io/ninetynine/privacy-policy.html`
- **Version (this release):** versionName `1.33`, versionCode `43`

---

## App name (Play listing title)

```
The Ninety Nine Names of Allah
```

## Short description (≤ 80 characters: 69 here)

```
Memorize the 99 Names (Asma ul Husna) offline. No ads, no tracking.
```

## Full description

```
Meet the ninety-nine Names of Allah, one every morning, in the Arabic of the Madinah Mushaf, with the meaning in plain English.

Open the app and today's Name is already on the page. Tomorrow it is the next one. In ninety-nine days you have met them all, and you decided almost nothing: the app does the choosing and the counting.

Nothing here arrives over a network. All ninety-nine Names, both typefaces and every meaning live inside the app, and the app asks Android for no Internet permission at all, so it cannot send anything anywhere, not your progress, not a crash report. Airplane mode changes nothing: every screen still works.

WHAT IS IN IT

Each Name is fully vocalized Arabic set in KFGQPC Uthmanic Script HAFS, the typeface of the Madinah Mushaf published by the King Fahd Glorious Quran Printing Complex, with its transliteration and its meaning drawn from the lecture of Sheikh Ibn Uthaymeen (Rahimahullah). Where a distinction needs care, the note is there, three of the 99 carry one, among them the difference between Ar-Rahmaan and Ar-Raheem.

READING

All ninety-nine, in order, each on its own page. Turn from one page to the next, or arrive from the list or from your bookmarks. Long-press a Name to copy it, or its meaning.

THE NAME OF THE DAY

One Name, every day: on the app's own page, in a notification at the hour you choose, and as a widget you can resize to sit on your home screen. All three carry the same Name, and none of them comes round again until the ninety-nine are done.

MEMORIZING

Flashcards, ten at a time. See the Name, recall the meaning, tap the card to turn it over. Swipe right for "I know it", left for "Still learning", and take the last card back if your thumb got ahead of you.

Then ten questions: match each Name to its meaning, and at the end see the ones you missed, each one tap away from its page. Your best score is kept for you.

Your progress is a count of the Names you have marked as learned, and nothing else. No streaks, no badges, no message scolding you for staying away. Bookmarks are their own shelf: the Names you keep stay kept, and clearing your progress leaves them exactly where they were.

SEARCHING

A name, a word from a meaning, a note, or a number. Spelling is forgiven (Qayyum finds Al-Qayyoom), and a match is shown in the row that earned it, so you can see why the list answered.

SHARING

Any Name can leave as a card with the Arabic, the name and the meaning on it, or as plain text for a caption or a note of your own.

TYPE AND THEMES

Light, dark and true-black (AMOLED) themes, and a text size you set yourself, the Arabic grows with the English, so both stay comfortable together.

FREE, AND PRIVATE

No ads. No accounts. No in-app purchases. No analytics, no tracking, no third party libraries. Plenty of apps that call themselves private mean they promise not to look; this one cannot look; it has no way to reach the network at all. The code is open source under the MIT license, so anyone can read exactly what it does.

May Allah make us among those who learn, memorize, understand and act upon His beautiful Names.
```

## What's new (version 1.33)

```
Nothing in the app itself changed.
This release only tidies the code underneath: the same 99 Names and the same screens, split into smaller modules that are easier to keep correct. Every screen was captured before and after and renders identically.
```

## What's new (version 1.32)

```
The text you get from “Share text” on a Name page reads cleaner:

• The name line carries the transliteration alone; the short meaning no longer repeats beside it.
• The shared text is now left-aligned, so it sits like a caption in the app you paste it into.
```

## What's new (version 1.31)

```
Typesetting and wording refinements throughout: the app's sentences now use ordinary punctuation (commas, colons, semicolons, parentheses) in place of long dashes, in the interface, the introduction and the listing copy.
```

## What's new (version 1.30)

```
Fixes for rare hiccups on a slow start, plus refinements:

• Opening the Quiz on a slow start could close the app; it now waits for the round.
• Quiz and Flashcards no longer flash a “not loaded” message on the way in.
• Numbers in sentences use the app’s Western digits on every phone language.
• A long Name keeps its whole spelling in the list at a large text size.
• The list scroll thumb stops above the floating bar, not behind it.
```

## What's new (version 1.29)

```
Fixed: tapping the Name of the Day notification could open the page you had last left the app on, instead of the Name shown in the notification. Tapping the notification now always opens that day's Name.
```

## What's new (version 1.28)

```
Fixed: tapping the home-screen widget could open the wrong Name (the page you had last left the app on) instead of the Name of the Day shown on the widget. Tapping the widget now always opens that day's Name.
```

## What's new (version 1.27)

```
Cold starts now land straight in your chosen theme, no light flash for Dark and Black readers. Also:

• A better-balanced flashcard on tall screens.
• Search text survives an interrupted session.
• Fixed stray wording on the About page, in the introduction, and in one Name's meaning.
• A clearer message when sharing cannot start.
```

## What's new (version 1.26)

```
Nothing in the app itself changed.
The GitHub front page now describes the whole app, including the quiz's revisit list.
```

## What's new (version 1.25)

```
Quieter under the hood:

• Bad saved data (a corrupted setting,
backup or quiz round) now falls back
safely instead of closing the app.
• Reminders, text size, flashcards and
quiz all validate what they are given.
• Stricter handling of background work
and sharing, with no new permissions.
```

## What's new (version 1.24)

```
Nothing in the app itself changed.

This release only refreshes the
GitHub front page: current
screenshots and a tidier README.
```

## What's new (version 1.23)

```
Our first public release, welcome!

Read and memorize the Ninety Nine Names of
Allah at your own pace: a Name of the Day,
flashcards, a short quiz, bookmarks and
daily reminder, all offline and private.

Small polish throughout: steadier screen
headings on tablets, and hardening under
the hood for a calmer, more reliable read.
```

## What's new (version 1.22)

```
Small labels, easier to read:

• The bottom bar's tabs (Names,
  Memorize, Bookmarks, Settings)
  and the name page's Learned and
  Bookmark labels are now set in
  normal case, easier to recognise
  at a glance.
• The wide-tracked capitals remain
  on the quiet annotations, like
  NAME OF THE DAY.
• Label text now behaves correctly
  on devices set to any language.
```

## What's new (version 1.21)

```
The name page's bottom bar now floats
like the main tab bar:

• On a long meaning, the text scrolls
  beneath the bar at the foot of the
  page, the same floating read as the
  lists, instead of stopping above it.
• The end of the page still lifts fully
  clear of the bar, and the bar itself
  is unchanged.

All 99 Names and your progress are
unchanged.
```

## What's new (version 1.20)

```
Two fixes on the pages you read most:

• Re-tapping Memorize, Names,
  Bookmarks or Settings in the bottom
  bar now always returns you to the
  top of the page. On some devices it
  could close the app instead; that
  crash is gone.
• On a name page the next Name's
  label now shrinks to fit, exactly
  like the previous one, so no Name
  is ever cut off, at any text size.

All 99 Names and your progress are
unchanged.
```

## What's new (version 1.19)

```
Small refinements to the pages you
use most:

• Re-tap Memorize, Bookmarks or
  Settings in the bottom bar and the
  page returns to its top, the same
  answer Names has always given.
• On a name page, the learned and
  bookmark acts now carry short
  labels and rest in the bar's quiet
  grey, matching the share icon.
• The previous and next Names on
  the page foot shrink to fit
  instead of cutting off, both
  arrows always stay visible.

All 99 Names and your progress are
unchanged.
```

## What's new (version 1.18)

```
Settings now lives in the bottom bar,
beside Names, Memorize and Bookmarks;
one tap, in the same place on every
screen. The name page gains a floating
capsule: previous and next, Mark as
learned and bookmark sit together at
the foot of the page, always within
reach on even the longest meanings.
Search stays in the home bar, where
the freed corner lets the app's title
set a little larger. All 99 Names and
your progress are unchanged.
```

## What's new (version 1.17)

```
The page now ends the way it reads:

• The bottom of every list clears the
  tab bar, the 99th Name is fully
  visible when you scroll to the end.
• The bar rests the same way in every
  theme: a soft shadow in dark and
  true-black, and no more thin outline.
• Search, flashcards, quiz and the
  widget are unchanged, as are all 99
  Names.
```

## What's new (version 1.16)

```
A tidier fit at the foot of the page:

• The tab bar (its pill, icons and
  labels) is a little smaller, so it
  sits quietly under the book.
• Tapping a tab now answers with a
  capsule highlight, matching the pill
  bar itself.
• Search, flashcards, quiz and the
  widget are unchanged, as are all 99
  Names.
```

## What's new (version 1.15)

```
The tab bar now floats as a smooth
pill:

• The bar lifts off the page as a capsule,
  same colour as the paper, a soft
  shadow, rounded ends.
• The quiz options and the deck menu
  match the book's own corners.
• The home-screen widget reads again
  in the app's own typefaces.
• All 99 Names are unchanged.
```

## What's new (version 1.14)

```
A quieter frame around the book:

• The tab bar now floats on its own
squircle, and the lists scroll beneath
it.
• The home-screen widget is redrawn:
softer corners, lighter Latin type,
tidier spacing.
• Smoother flashcard and quiz
transitions.
• Larger touch targets and clearer
screen-reader speech.

All 99 Names are unchanged.
```

## What's new (version 1.13)

```
This update is about tablets:

• On 7" and 10" screens, the title,
  search and the tab bar now sit inside
  the same page margins as the content.
  The whole screen reads as one open
  page of the book.
• The flashcards deck menu matches the
  app's paper.
• Phones are unchanged.
```

## What's new (version 1.12)

```
The corner of every tab is simpler:

• The ⋮ menu is gone. One tap on the gear
opens Settings directly.
• About (the hadith, the source, the
colophon) now sits at the foot of
Settings, one tap after it.
• Screen readers speak each control more
clearly.

All 99 Names are unchanged.
```

## What's new (version 1.11)

```
The book now prints in a larger format on
tablets:

• On 7-inch and 10-inch devices, all type (the
Names, the meanings, the bar) scales up with
the screen, so the page reads comfortably at
tablet distance.
• Phones are unchanged, and your text-size
setting keeps working exactly as before.

All 99 Names are unchanged.
```

## What's new (version 1.10)

```
The book now prints in a larger format on
tablets:

• On 7-inch and 10-inch devices, all type (the
Names, the meanings, the bar) scales up with
the screen, so the page reads comfortably at
tablet distance.
• Phones are unchanged, and your text-size
setting keeps working exactly as before.

All 99 Names are unchanged.
```

## What's new (version 1.9)

```
A quieter morning, a clearer page:

• The daily reminder now comes with the app: one
"allow" and the Name of the Day arrives each
morning. It stays silent, and Settings turns it
off any time.
• The lists carry their numbers again, so "I've
memorized up to 19" has an anchor on the page.
• The Name of the Day card no longer repeats
itself when expanded.

All 99 Names are unchanged.
```

## What's new (version 1.8)

```
Care where you meet the book:

• The daily notification now opens into the Name's
emerald card, the same plate as the app and widget.
• On tablets, every screen keeps the book's page
proportions instead of stretching edge to edge.
• TalkBack reads the Names in an Arabic voice, and
progress now announces itself.
• Finish the book and the ٩٩ settles in, quietly.

All 99 Names are unchanged.
```

## What's new (version 1.7)

```
Search moves to the app bar:

• Stop a scroll with an upward pull, tap the magnifier,
and search from anywhere, no scrolling back needed.
• Back now steps out of search one layer at a time
instead of leaving the app.
• The share card no longer trembles when pushed upward.
• On a name page, long-press copies the Name itself
(Arabic and transliteration) alongside the meaning.
• Re-tapping Names lands at the very top, bar included.

All 99 Names are unchanged.
```

## What's new (version 1.6)

```
Polish where the hands and eyes rest:

• The share sheet no longer keeps shaking if you push
the preview upward a few times; it settles.
• A larger title on the home screen, kept neatly within
its bar at every text size.
• The search field is now a quiet rounded plate: its own
space above and below, set apart from the list.

All 99 Names are unchanged.
```

## What's new (version 1.5)

```
A quieter room for the same book:

• Search is now a quiet field at the head of the list:
always there, no icon to find.
• Name pages carry both keeps in one place: a gold check
marks a Name learned, beside the bookmark.
• List rows lose their numbers; flashcards lose their
instructions, the gestures teach themselves.
• Calmer motion throughout.
• The widget's Arabic now renders in the Madinah Mushaf's
HAFS script.

All 99 Names are unchanged.
```

## What's new (version 1.4)

```
A small, warm correction:

• The Name on each Name page now wears a warmer, brighter gold: closer
to the hero card's emerald-and-gold, and easier on the eye at every
text size.

All 99 Names are unchanged.
```

## What's new (version 1.3)

```
A quieter, more exacting coat of paint:

• Every corner icon (search, ⓘ, share, the gear, back) now wears one
lighter weight.
• Tab titles sit at a book's running-head size on all three tabs.
• Finer progress line; the disabled quiz button no longer sits like mud.
• Theme swatches get a paper mat; list rows breathe a touch more.
• A refined launcher icon; widget, notification and app share one
emerald-and-gold.

All 99 Names are unchanged.
```

## What's new (version 1.2)

```
The app answers back, quietly, wherever a finger or an eye asks:

• Search paints matching letters gold.
• A dragged flashcard shows its verdict, and ticks at the point of no return.
• Beating your quiz best earns a gold "New best".
• The text-size specimen grows live under the slider.
• Long-press the launcher icon for Flashcards or Quiz.
• Share a Name as text too; hold a meaning to copy it.
• Night readers get a night splash; theme rows wear swatches.

All 99 Names are unchanged.
```

## What's new (version 1.1)

```
A round of motion and moments, everywhere you felt a cut:

• Quiz questions turn like pages now, and your score
counts itself up, a perfect round earns the app's gold seal.
• The learned count rolls to meet you when you return having learned more.
• A fruitless search offers "Clear search" right there.
• Long lists carry the reading pages' quiet position thumb.
• The day's name card turns over gently at midnight.
• The daily notification wears the app's emerald.

All 99 Names are unchanged.
```

## What's new (version 1.0)

```
Fixes for readers on older phones, and finer speech for TalkBack:

• Android 7: the launcher icon now appears correctly.
• Android 8 to 11: the widget picker now shows a preview of the Name of the
Day widget instead of the app icon.
• TalkBack now announces what tapping the day's name card, the learned count
and the notification time does.

All 99 Names are unchanged.
```

## Category, content & data

- **Category:** Education (or Books & Reference; either fits)
- **Ads:** None
- **In-app purchases:** None
- **Data safety:** "No data collected". The app has **no INTERNET permission**
  in its manifest, so this is provable and safe to declare.
- **Content rating (IARC):** answer the questionnaire honestly (educational /
  devotional content; no violence, sexual, or mature content).

## Privacy policy

The app collects no data, so you can declare "the app does not collect any
data" in the form. If Play asks for a privacy policy URL anyway, publish this
(you can host it on your blog or a GitHub Page) and paste the link:

```
# Privacy Policy

The Ninety Nine Names of Allah does not collect, store, transmit, or share any
personal data.

The app works fully offline. It has no network access, no advertising, no
analytics, no account system, and no third-party services. Everything you do in
the app (the Names you bookmark, mark as learned, your theme and text-size
preferences) is stored only on your own device and never leaves it.

If you have any questions, contact muntasim.haque@gmail.com.
```

## Contact / support

- Developer email: `muntasim.haque@gmail.com`