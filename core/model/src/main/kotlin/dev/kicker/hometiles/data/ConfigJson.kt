package dev.kicker.hometiles.data

import kotlinx.serialization.json.Json

/**
 * the one json setup for the config file and the backup, which are the same format.
 *
 * `coerceInputValues`: a choice this release does not know falls back to its default instead
 * of making the whole file unreadable. 0.1.3 lost a phone's setup over one "SYSTEM" that 0.1.4
 * had saved. it covers single values, not entries inside a list.
 */
internal val configJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    prettyPrint = true
}
