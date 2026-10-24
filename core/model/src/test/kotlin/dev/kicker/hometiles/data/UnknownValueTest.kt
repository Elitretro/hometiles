package dev.kicker.hometiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * a value this release does not know costs that one setting, not the whole setup.
 *
 * 0.1.3 met the "SYSTEM" that 0.1.4 saved for the orientation, put the whole config aside as
 * unreadable and started the wizard (jelly2 avd, 24.10.2026). that version cannot be fixed
 * any more; from here on a later one can add a choice without the same happening again.
 */
class UnknownValueTest {

    @get:Rule
    val folder = TemporaryFolder()

    // what a later release might write: a choice this one has never heard of, beside
    // settings it knows.
    private val later = """
        {
          "screens": [ { "id": "s1", "name": "Mine" } ],
          "homeScreenId": "s1",
          "appearance": { "orientation": "SOMETHING_NEWER", "theme": "LIGHT" }
        }
    """.trimIndent()

    @Test
    fun `the config file keeps the rest`() {
        val file = File(folder.root, "config.json").apply { writeText(later) }
        val config = ConfigFile(file)
        val read = config.read()
        assertFalse("the setup was put aside as unreadable", config.rescuedBroken)
        assertEquals("Mine", read.screens.single().name)
        assertEquals(ThemeName.LIGHT, read.appearance.theme)
        assertEquals(Appearance().orientation, read.appearance.orientation)
    }

    // file and backup each had a setup of their own; a third one would miss the coercion.
    @Test
    fun `there is one json setup for the config`() {
        val setups = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }
            .filter { Regex("""Json\s*\{""").containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertEquals(listOf("ConfigJson.kt"), setups)
    }

    @Test
    fun `an imported backup keeps the rest`() {
        val read = ConfigTransfer.import(later)
        assertTrue("the backup was refused", read != null)
        assertEquals("Mine", read!!.screens.single().name)
        assertEquals(ThemeName.LIGHT, read.appearance.theme)
        assertEquals(Appearance().orientation, read.appearance.orientation)
    }
}
