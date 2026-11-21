package com.poseit.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Settings and the camera's quick settings share one list, so a value picked in one is highlighted in the other. */
class CountdownChoicesTest {

    @Test
    fun everyOfferedChoice_selectsItself() {
        COUNTDOWN_CHOICES.forEach { assertEquals(it, countdownChoiceFor(it)) }
    }

    @Test
    fun choicesCoverTheStoredRange() {
        // SettingsRepository stores 0..10 s.
        assertTrue(COUNTDOWN_CHOICES.first() == 0 && COUNTDOWN_CHOICES.last() == 10)
        assertEquals(AppSettings().countdownSeconds, countdownChoiceFor(AppSettings().countdownSeconds))
    }

    @Test
    fun valueNotOffered_selectsNearest() {
        assertEquals(5, countdownChoiceFor(6))
        assertEquals(10, countdownChoiceFor(9))
    }
}
