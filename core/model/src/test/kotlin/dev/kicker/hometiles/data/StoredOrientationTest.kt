package dev.kicker.hometiles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * the config stores the orientation by its name. issue #1 added a fourth one, and a phone
 * updated from 0.1.3 must keep the choice it had, with no migration.
 */
class StoredOrientationTest {

    @get:Rule
    val folder = TemporaryFolder()

    // the three names 0.1.3 wrote, as they stand in its config.json.
    @Test
    fun `a choice saved by the old version reads back unchanged`() {
        mapOf(
            "PORTRAIT" to ScreenOrientation.PORTRAIT,
            "LANDSCAPE" to ScreenOrientation.LANDSCAPE,
            "AUTO" to ScreenOrientation.AUTO,
        ).forEach { (stored, expected) ->
            val file = File(folder.root, "config-$stored.json")
            file.writeText("""{ "appearance": { "orientation": "$stored" } }""")
            val config = ConfigFile(file)
            assertEquals(stored, expected, config.read().appearance.orientation)
            assertFalse("$stored was rescued as unreadable", config.rescuedBroken)
        }
    }

    @Test
    fun `the new choice survives a round trip`() {
        val file = File(folder.root, "config.json")
        val config = ConfigFile(file)
        config.write(LauncherConfig(appearance = Appearance(orientation = ScreenOrientation.SYSTEM)))
        assertEquals(ScreenOrientation.SYSTEM, config.read().appearance.orientation)
    }
}
