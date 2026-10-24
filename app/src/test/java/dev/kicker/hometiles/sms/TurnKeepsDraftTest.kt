package dev.kicker.hometiles.sms

import dev.kicker.hometiles.Quelltext
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * turning the phone rebuilds the messages screen. with plain `remember` the open
 * conversation and the unsent draft were gone afterwards, back on the list. measured on the
 * jelly2 avd on 24.10.2026, in 0.1.3 as well; issue #1 made turning more common.
 */
class TurnKeepsDraftTest {

    private val text = Quelltext.file("dev/kicker/hometiles/sms/SmsActivity.kt").readText()

    @Test
    fun `the open conversation and the draft survive a turn`() {
        listOf("openThread", "openAddress", "justSentTo", "draft").forEach { name ->
            assertTrue(
                "$name is not saveable, a turn of the phone loses it",
                Regex("""var $name by rememberSaveable\b""").containsMatchIn(text),
            )
        }
    }
}
