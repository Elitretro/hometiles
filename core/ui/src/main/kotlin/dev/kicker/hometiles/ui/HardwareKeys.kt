package dev.kicker.hometiles.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * the character a hardware key stands for when a number is being entered, or null.
 *
 * three ways in, because phones differ: a keypad phone sends `KEYCODE_1`, a numeric pad
 * `KEYCODE_NUMPAD_1` and a phone with a full keyboard such as the titan reaches the digits
 * through alt, which shows only in the character the key produces with its modifiers.
 *
 * plain ints and no `KeyEvent`, so the mapping can be held to its promise without a device.
 */
fun dialCharFor(keyCode: Int, unicodeChar: Int): Char? {
    when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> return '0' + (keyCode - KeyEvent.KEYCODE_0)
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ->
            return '0' + (keyCode - KeyEvent.KEYCODE_NUMPAD_0)
        KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> return '*'
        KeyEvent.KEYCODE_POUND -> return '#'
        KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> return '+'
    }
    val typed = unicodeChar.toChar()
    return if (unicodeChar > 0 && typed in DIAL_CHARS) typed else null
}

private const val DIAL_CHARS = "0123456789*#+"

/** the same for a whole event, with the modifiers it was pressed with. */
fun dialChar(event: KeyEvent): Char? = dialCharFor(event.keyCode, event.getUnicodeChar(event.metaState))

/**
 * the character a key adds to a search, or null.
 *
 * letters and digits only. space, enter and the arrows have work of their own on a list
 * screen, and a key held with ctrl is a shortcut, not a letter.
 */
fun searchCharFor(unicodeChar: Int, ctrl: Boolean): Char? {
    if (ctrl || unicodeChar <= 0) return null
    val typed = unicodeChar.toChar()
    return if (typed.isLetterOrDigit()) typed else null
}

/**
 * the search text while letters come faster than the screen is drawn. see BigSearchField.
 *
 * two writers share it: letters typed from the list, and the field once it has the focus.
 * each built on the value of the last frame, so between two frames one wiped the other's
 * letter. here both build on [text], and the field's edits count from what the field itself
 * last held.
 */
class TypeAhead(private var text: String) {
    private var inField = text

    val current: String get() = text

    /** a letter typed from the list. */
    fun type(letter: Char): String = (text + letter).also { text = it }

    /**
     * an edit the field reports. letters it added at the end go on top of [text]; anything
     * else, deleting or typing mid-word, is taken as the field has it. that only happens at
     * the pace of a hand, when nothing is pending.
     */
    fun edited(edited: String): String {
        text = if (edited.startsWith(inField)) text + edited.removePrefix(inField) else edited
        inField = edited
        return text
    }

    /** the value the screen was drawn with. the field holds it from now on. */
    fun drawn(value: String) {
        text = value
        inField = value
    }
}

/**
 * keys that belong to the whole screen: they arrive whether anything has the focus or not.
 *
 * compose hands a key only to the focused node and its parents. after a tap nothing holds
 * the focus (touch mode), so a digit pressed on the dialler went nowhere. registered at the
 * activity, which gets every key compose did not take, the newest handler first; a handler
 * returns true for what it took, on key down and on key up alike, so nothing below sees half
 * a press.
 */
@Composable
fun ScreenKeys(enabled: Boolean = true, handler: (KeyEvent) -> Boolean) {
    val activity = LocalContext.current.homeTilesActivity() ?: return
    val current by rememberUpdatedState(handler)
    DisposableEffect(activity, enabled) {
        val remove = if (enabled) activity.addScreenKeys { current(it) } else ({})
        onDispose { remove() }
    }
}

private tailrec fun Context.homeTilesActivity(): HomeTilesActivity? = when (this) {
    is HomeTilesActivity -> this
    is ContextWrapper -> baseContext.homeTilesActivity()
    else -> null
}
