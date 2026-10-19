package dev.kicker.hometiles.ui

import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.kicker.hometiles.ui.theme.LocalBigPalette
import dev.kicker.hometiles.ui.theme.contrastRatio

/**
 * width of the focus border, on tiles and everywhere else. clearly more than the blink
 * border of a tile, so two borders on one screen never look alike. `PLAN.md` 10.3.1.
 */
const val FOCUS_BORDER_DP = 8f

/**
 * the focus border for everything that is not a tile: rows, buttons, keypad keys.
 *
 * compose moves the focus with the d-pad by itself but draws nothing for it. a keypad
 * phone then shows a list that seems only to scroll (qin f21, reported 19.10.2026).
 *
 * goes directly in front of the `clickable` it belongs to: `onFocusChanged` sees the focus
 * target that follows it in the chain.
 *
 * [fill] is the surface the border lies on, for rows that paint their own. the preview of the
 * light theme is light grey, and the ink of the dark theme on it was measured by
 * `tools/focus-visible.py` as no border at all (19.10.2026). see [focusInkFor].
 */
fun Modifier.focusBorder(shape: Shape, fill: Color? = null): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val palette = LocalBigPalette.current
    val ink = Color(focusInkFor(argb(palette.onBackground), argb(palette.background), fill?.let(::argb)))
    this
        .then(if (focused) Modifier.border(FOCUS_BORDER_DP.dp, ink, shape) else Modifier)
        .onFocusChanged { focused = it.isFocused }
}

/**
 * the colour of the focus border: the theme's ink, or its background where the ink would
 * vanish on the surface. of the two the one further from the fill, which reaches 3:1 for any
 * fill in all three themes (`FocusInkTest`).
 */
fun focusInkFor(onBackground: Long, background: Long, fill: Long?): Long = when {
    fill == null -> onBackground
    contrastRatio(onBackground, fill) >= contrastRatio(background, fill) -> onBackground
    else -> background
}

private fun argb(colour: Color): Long = colour.toArgb().toLong() and 0xFFFFFFFFL
