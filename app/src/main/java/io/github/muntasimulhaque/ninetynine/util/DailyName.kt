package io.github.muntasimulhaque.ninetynine.util

import java.util.TimeZone

/** Deterministic daily-name rotation: local epoch day % 99, shared by banner, widget, notification. */
object DailyName {

    const val COUNT = 99
    private const val DAY_MILLIS = 86_400_000L

    /** Returns the name number (1..99) for the local calendar day containing [nowMillis]. */
    fun numberFor(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        // floorDiv, not /: plain division truncates toward zero, so for a
        // (hypothetical) pre-epoch instant the day count would skip a day.
        // floorDiv always rounds down, making the day count correct for
        // every representable instant. floorMod pairs with it — % is a
        // remainder, so a negative day would otherwise map to a negative
        // slot and wrap to the wrong name.
        val localDays = Math.floorDiv(nowMillis + timeZone.getOffset(nowMillis), DAY_MILLIS)
        return Math.floorMod(localDays, COUNT) + 1
    }
}
