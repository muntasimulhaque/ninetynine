# Plan of record — polish sweep (audit fixes)

Status of decisions, so nothing is lost between sessions.

## Already decided

- Baseline build: rerun after all edits; verify tests+lint+assembleDebug green.
- A1-A5, P1, P3, P4: DONE (search field 48dp + no double-announce, theme/switch
  rows 48dp, NOTE heading via SectionLabel, toggle semantics Role.Switch on
  learned/bookmark, FitText ellipsis insurance, quiz question->result house
  push, imePadding on Home, PredictiveBackHandler for search unwind).
- P5 (flashcards empty/done hard-cut): DO — same AnimatedContent pattern as P1.
- V1: slider track outlineVariant -> outline (matches HairlineProgress ruling).
- V2: deck menu OptionCheck RoundedCornerShape(4.dp) -> SquircleShape(4.dp).
- V3: DailyPlate spacing true-up to hero card (12->14, 6->2, epithet 16sp),
  name constants after ramp counterparts.
- V4: widget Latin in Spectral Light (bitmap path, like DailyPlate).
- V5: ArabicSize slots for widget/plate (Widget 38, Compact 18, Plate=Panel 48
  named usage).
- V6: hoist TabLabelStyle (bottom bar 10sp/1.2sp literals) into a named style.
- V7: scaledGap sweep on text-adjacent gaps (hero 14dp, Detail note 8dp,
  flashcards/quiz block gaps, About colophon) - keep touch-target padding fixed.
- Widget squircle plate: runtime-drawn n=4 plate at device radius, keep system
  radius decision, pre-31 gains corners too. Preview PNG already regenerated.
- Bottom bar: implement BOTH B (floating shadow, scroll-under + scrim) and C
  (flat border) behind a quick switch, build, take REAL screenshots on the
  emulator/CI, show the user, user picks ONE, discard the other, then settle.
- P2 (widget tap action description): EXCLUDED by user decision.

## Sequencing

1. P5, V1-V7 (small diffs, batch them)
2. Widget runtime squircle plate
3. Bottom bar both variants (behind a compile-time switch for screenshotting)
4. Full verification: testDebugUnitTest lintDebug assembleDebug
5. Real screenshots (emulator via adb if available on this machine, else CI
   screenshots.yml workflow_dispatch + gh run download)
6. User picks bar variant; discard the other; final rebuild
