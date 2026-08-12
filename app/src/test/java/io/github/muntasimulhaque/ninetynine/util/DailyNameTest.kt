package io.github.muntasimulhaque.ninetynine.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.TimeZone

class DailyNameTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun epochDayZeroIsFirstName() {
        assertEquals(1, DailyName.numberFor(0L, utc))
    }

    @Test
    fun walksThroughAllNamesAndWraps() {
        val day = 86_400_000L
        assertEquals(99, DailyName.numberFor(98 * day, utc))
        assertEquals(1, DailyName.numberFor(99 * day, utc))
        assertEquals(2, DailyName.numberFor(100 * day, utc))
    }

    @Test
    fun sameLocalDayGivesSameName() {
        val midnight = 1_704_067_200_000L // 2024-01-01T00:00:00Z
        assertEquals(
            DailyName.numberFor(midnight, utc),
            DailyName.numberFor(midnight + 23 * 3_600_000L, utc),
        )
    }

    @Test
    fun adjacentDaysDiffer() {
        val day = 86_400_000L
        val midnight = 1_704_067_200_000L
        assertNotEquals(DailyName.numberFor(midnight, utc), DailyName.numberFor(midnight + day, utc))
    }

    @Test
    fun timezoneCanShiftTheDay() {
        val justAfterMidnightUtc = 1_704_067_200_000L + 1_800_000L // 2024-01-01T00:30:00Z
        val stillYesterday = TimeZone.getTimeZone("Etc/GMT+12")
        assertNotEquals(
            DailyName.numberFor(justAfterMidnightUtc, utc),
            DailyName.numberFor(justAfterMidnightUtc, stillYesterday),
        )
    }

    /**
     * Plain division truncates toward zero, so a pre-epoch day previously
     * landed on the wrong name. floorDiv/floorMod keep the rotation exact for
     * every instant: the day before epoch day 0 must be the name before #1,
     * which is #99.
     */
    @Test
    fun preEpochDayCountsBackwardsCorrectly() {
        val day = 86_400_000L
        assertEquals(99, DailyName.numberFor(-day, utc))
        assertEquals(98, DailyName.numberFor(-2 * day, utc))
        // A timezone east of UTC makes a pre-epoch instant fall on a later
        // local day than UTC would; the offset must be applied before the
        // (floor) division, exactly as for positive instants.
        val east = TimeZone.getTimeZone("Etc/GMT-14")
        assertEquals(
            DailyName.numberFor(0L, east),
            DailyName.numberFor(-1L, east),
        )
    }
}
