package dev.kicker.hometiles.ui

import android.content.pm.ActivityInfo
import dev.kicker.hometiles.data.ScreenOrientation

/** screen rotation, in one place instead of twelve times in the manifest. `PLAN.md` 4.2. */
object Orientation {

    fun requested(orientation: ScreenOrientation): Int = when (orientation) {
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // SENSOR and not UNSPECIFIED: automatic must also turn while the system rotation
        // lock is off, otherwise the choice has no effect.
        ScreenOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        // USER turns with the sensor while the system rotation is on and stands still while
        // it is locked. issue #1.
        ScreenOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_USER
    }
}
