package dev.kicker.hometiles.ui

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * whatever can be pressed shows the focus. `PLAN.md` 10.3.1.
 *
 * compose makes every `clickable` a focus target and the d-pad moves through them, but it
 * draws nothing. only the tiles carried a focus border; every row, key and button in the
 * menus was a focus target without a mark. on a qin f21 (reported 19.10.2026) that looked
 * like a list that only scrolls.
 *
 * so each `clickable` and its relatives stands behind a `focusBorder`, or behind its own
 * `onFocusChanged` as the tile does, a few lines further up in the same chain. the drawn
 * result is measured by `tools/focus-visible.py`; this rule keeps a new button from
 * slipping past it.
 */
class FocusVisibleTest {

    private val pressable = Regex("""\.(clickable|combinedClickable|selectable|toggleable)\s*[({]""")

    /** "file|line as written" to why it shows no border. */
    private val allowed = mapOf(
        "MainActivity.kt|.clickable { onDismiss() }," to
            "the whole background behind the enlarged label and behind the call-or-write " +
            "question, there to close them on a tap beside. by key both are led: the " +
            "label closes on any key, the question moves between its two rows and closes " +
            "on back. the background never holds the focus.",
    )

    private data class Hit(val file: String, val line: String, val shown: Boolean)

    private fun hits(): List<Hit> = Quelltext.files().flatMap { file ->
        val lines = file.readLines().filterNot { Quelltext.isCommentLine(it) }
        lines.indices.filter { pressable.containsMatchIn(lines[it]) }.map { i ->
            // the chain above the call, up to where the modifier starts.
            val chain = lines.subList(maxOf(0, i - 8), i + 1).joinToString("\n")
            Hit(
                file.name,
                lines[i].trim(),
                "focusBorder(" in chain || "onFocusChanged" in chain,
            )
        }
    }

    @Test
    fun `everything that can be pressed shows the focus`() {
        val bare = hits()
            .filterNot { it.shown }
            .filterNot { "${it.file}|${it.line}" in allowed }
            .map { "${it.file}: ${it.line}" }
        assertEquals(
            "pressable without a focus border. by key nobody sees it is about to be " +
                "pressed: `.focusBorder(shape)` directly in front of it, or into `allowed` " +
                "with a reason.",
            emptyList<String>(),
            bare,
        )
    }

    /**
     * a material text field shows its focus as a line of two dp in the accent colour. against
     * eight dp everywhere else that read as no focus at all, measured in the sos numbers and
     * the blocked senders on 19.10.2026.
     */
    @Test
    fun `every text field shows the focus like a button`() {
        val bare = Quelltext.files().flatMap { file ->
            val lines = file.readLines().filterNot { Quelltext.isCommentLine(it) }
            lines.indices
                .filter { "OutlinedTextField(" in lines[it] && !lines[it].trim().startsWith("import") }
                .filterNot { i -> lines.subList(i, minOf(lines.size, i + 30)).any { "focusBorder(" in it } }
                .map { i -> "${file.name}:${i + 1}" }
        }
        assertEquals(
            "a text field without the focus border. by key the thin outline is easy to miss: " +
                "`.focusBorder(shape)` at the end of its modifier.",
            emptyList<String>(),
            bare,
        )
    }

    @Test
    fun `every exception still exists`() {
        val present = hits().map { "${it.file}|${it.line}" }.toSet()
        assertEquals(
            "an exception for a line that is gone. take it out, or it waves the next one " +
                "through unseen.",
            emptyList<String>(),
            allowed.keys.filterNot { it in present },
        )
    }

    @Test
    fun `the rule finds what it looks for`() {
        val count = hits().size
        assertTrue("only $count pressable places found - does the rule still search?", count >= 12)
    }
}
