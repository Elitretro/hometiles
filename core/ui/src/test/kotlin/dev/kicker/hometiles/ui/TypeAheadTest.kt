package dev.kicker.hometiles.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * letters typed faster than a frame all reach the search.
 *
 * "fr" typed into the contacts read "r", "ber" read "er": 11 of 20 bursts on the jelly2 avd,
 * 24.10.2026, in 0.1.3 and 0.1.4 alike. the list and the field each built on the value of
 * the last frame. the field also counts its edits from its own last text, not from ours.
 */
class TypeAheadTest {

    @Test
    fun `two letters from the list before the next frame both stay`() {
        val typeAhead = TypeAhead("")
        assertEquals("f", typeAhead.type('f'))
        assertEquals("fr", typeAhead.type('r'))
    }

    // the measured case: "b" from the list, then the field has the focus but was drawn
    // with "" and reports "e", then "er".
    @Test
    fun `the field typing on before it was drawn keeps the first letter`() {
        val typeAhead = TypeAhead("")
        assertEquals("b", typeAhead.type('b'))
        assertEquals("be", typeAhead.edited("e"))
        assertEquals("ber", typeAhead.edited("er"))
    }

    @Test
    fun `a field that is up to date is taken as it is`() {
        val typeAhead = TypeAhead("")
        typeAhead.drawn("ber")
        assertEquals("bers", typeAhead.edited("bers"))
        assertEquals("ber", typeAhead.edited("ber"))
        assertEquals("bXer", typeAhead.edited("bXer"))
        assertEquals("", typeAhead.edited(""))
    }

    // the clear button empties the search from outside.
    @Test
    fun `a value drawn from outside counts again`() {
        val typeAhead = TypeAhead("fr")
        typeAhead.drawn("")
        assertEquals("a", typeAhead.type('a'))
    }

    private val source = File("src/main/kotlin/dev/kicker/hometiles/ui/BigSearchField.kt").readText()

    @Test
    fun `list and field both write through TypeAhead`() {
        assertTrue("letters from the list bypass TypeAhead", "typeAhead.type(" in source)
        assertTrue("edits in the field bypass TypeAhead", "typeAhead.edited(" in source)
        assertTrue("the drawn value never reaches TypeAhead", "typeAhead.drawn(value)" in source)
    }
}
