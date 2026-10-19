package dev.kicker.hometiles.ui

import androidx.compose.ui.graphics.Color
import dev.kicker.hometiles.ui.theme.contrastRatio
import dev.kicker.hometiles.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * the focus border can be seen on whatever it lies on.
 *
 * the rows of the theme choice show the other themes, so in the dark theme a light grey row
 * stands in the list, and the white focus border on it was measured as no border at all
 * (`tools/focus-visible.py`, 19.10.2026). this runs every fill of every theme under every
 * theme, the previews included.
 *
 * 3:1 is wcag 1.4.11 for a part of an interface that has to be seen.
 */
class FocusInkTest {

    private fun Color.argb(): Long {
        fun ch(v: Float) = (v.coerceIn(0f, 1f) * 255f).toLong().coerceIn(0, 255)
        return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
    }

    @Test
    fun `the focus border reaches 3 to 1 on every fill`() {
        val palettes = themesAndSystem().map { (theme, dark) -> theme to paletteFor(theme, dark) }
        val fills = palettes.flatMap { (_, p) -> p.allSurfaces().map { it.fill } + p.background }
            .map { it.argb() }
            .distinct()
        val faint = palettes.flatMap { (theme, p) ->
            fills.mapNotNull { fill ->
                val ink = focusInkFor(p.onBackground.argb(), p.background.argb(), fill)
                val ratio = contrastRatio(ink, fill)
                if (ratio >= 3.0) null else "%s: border #%06X on #%06X only %.2f:1".format(
                    theme, ink and 0xFFFFFF, fill and 0xFFFFFF, ratio,
                )
            }
        }.distinct()
        assertEquals("a focus border that vanishes on its own row", emptyList<String>(), faint)
    }

    @Test
    fun `without a fill it is the theme's ink`() {
        val p = paletteFor(themesAndSystem().first().first, true)
        assertEquals(p.onBackground.argb(), focusInkFor(p.onBackground.argb(), p.background.argb(), null))
    }
}
