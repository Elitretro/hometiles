package dev.kicker.hometiles.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import android.os.Bundle
import android.view.KeyEvent
import dev.kicker.hometiles.data.ConfigStore
import dev.kicker.hometiles.data.Language

/**
 * common ground of every screen: wraps the context to the chosen language before any text
 * is looked up.
 *
 * a base class and not a call in each `onCreate`, because a forgotten call would leave
 * exactly one screen in the wrong language.
 */
abstract class HomeTilesActivity : ComponentActivity() {

    private var attachedLanguage: Language = Language.SYSTEM

    /**
     * counts how often this screen came back to the front.
     *
     * for everything the *system* grants: notification access, the sms role, the phone
     * role. those come from a system setting and send no result back, so a value read once
     * while drawing keeps claiming access is missing after it was granted.
     *
     * use as `remember(resumes.intValue) { ... }` around the call. a counter and not a
     * `Boolean`, because the same value set twice triggers no redraw.
     */
    val resumes = mutableIntStateOf(0)

    /** see [ScreenKeys]. */
    private val screenKeys = mutableListOf<(KeyEvent) -> Boolean>()

    /** returns the call that takes the handler out again. */
    fun addScreenKeys(handler: (KeyEvent) -> Boolean): () -> Unit {
        screenKeys += handler
        return { screenKeys -= handler }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // a copy: a handler may change the screen, and with it the list.
        if (screenKeys.toList().asReversed().any { it(event) }) return true
        return super.dispatchKeyEvent(event)
    }

    override fun attachBaseContext(newBase: Context) {
        attachedLanguage = ConfigStore.get(newBase).current.appearance.language
        super.attachBaseContext(AppLocale.wrap(newBase, attachedLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // one place instead of twelve times `portrait` in the manifest.
        requestedOrientation =
            Orientation.requested(ConfigStore.get(this).current.appearance.orientation)
    }

    override fun onResume() {
        super.onResume()
        resumes.intValue += 1
        val appearance = ConfigStore.get(this).current.appearance
        // a screen that was already running when language or orientation changed keeps the
        // old one; the home screen sits in the background the whole time.
        val wanted = Orientation.requested(appearance.orientation)
        if (requestedOrientation != wanted) requestedOrientation = wanted
        if (AppLocale.needsRecreate(attachedLanguage, appearance.language)) recreate()
    }
}
