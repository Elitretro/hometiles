package dev.kicker.hometiles.ui

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * what a hardware key types. three kinds of phone, three ways in.
 *
 * reported on 19.10.2026 from a qin f21 and a titan: the dialler took no digit key, and the
 * titan's keyboard typed nothing. a keypad sends `KEYCODE_1`, a numeric pad `NUMPAD_1`, and
 * a full keyboard reaches its digits through alt, which only the produced character shows.
 */
class HardwareKeysTest {

    @Test
    fun `the keypad digits type themselves`() {
        for (digit in 0..9) {
            assertEquals('0' + digit, dialCharFor(KeyEvent.KEYCODE_0 + digit, 0))
        }
    }

    @Test
    fun `the numeric pad types the same digits`() {
        for (digit in 0..9) {
            assertEquals('0' + digit, dialCharFor(KeyEvent.KEYCODE_NUMPAD_0 + digit, 0))
        }
    }

    @Test
    fun `star, pound and plus have keys of their own`() {
        assertEquals('*', dialCharFor(KeyEvent.KEYCODE_STAR, 0))
        assertEquals('*', dialCharFor(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, 0))
        assertEquals('#', dialCharFor(KeyEvent.KEYCODE_POUND, 0))
        assertEquals('+', dialCharFor(KeyEvent.KEYCODE_PLUS, 0))
        assertEquals('+', dialCharFor(KeyEvent.KEYCODE_NUMPAD_ADD, 0))
    }

    @Test
    fun `a keyboard reaches its digits through the character`() {
        // the titan: alt and w produce a one. the key code alone says w.
        assertEquals('1', dialCharFor(KeyEvent.KEYCODE_W, '1'.code))
        assertEquals('#', dialCharFor(KeyEvent.KEYCODE_Q, '#'.code))
    }

    @Test
    fun `a letter is no digit`() {
        assertNull(dialCharFor(KeyEvent.KEYCODE_W, 'w'.code))
        assertNull(dialCharFor(KeyEvent.KEYCODE_DPAD_CENTER, 0))
        assertNull(dialCharFor(KeyEvent.KEYCODE_SPACE, ' '.code))
    }

    @Test
    fun `letters and digits go into a search`() {
        assertEquals('c', searchCharFor('c'.code, ctrl = false))
        assertEquals('Ü', searchCharFor('Ü'.code, ctrl = false))
        assertEquals('7', searchCharFor('7'.code, ctrl = false))
    }

    @Test
    fun `space, controls and shortcuts stay out of a search`() {
        // space and enter select on a list; a held ctrl is a shortcut.
        assertNull(searchCharFor(' '.code, ctrl = false))
        assertNull(searchCharFor('\n'.code, ctrl = false))
        assertNull(searchCharFor(0, ctrl = false))
        assertNull(searchCharFor('c'.code, ctrl = true))
    }
}
