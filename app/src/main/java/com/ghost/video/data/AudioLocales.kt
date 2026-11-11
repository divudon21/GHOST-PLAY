package com.ghost.video.data

import java.util.Locale

/**
 * Language list for the "Preferred audio language" preference.
 *
 * Ported from NextPlayer (GPL-3.0):
 * https://github.com/anilbeesetti/nextplayer/blob/main/feature/settings/src/main/java/dev/anilbeesetti/nextplayer/settings/utils/LocalesHelper.kt
 *
 * Media3 matches `setPreferredAudioLanguage` against ISO 639-2 three letter
 * codes (`Format.language`), which is why the key is `isO3Language` and not the
 * two letter ISO code.
 */
object AudioLocales {

    /** `displayName to iso3Code`, de-duplicated per code and sorted by name. */
    fun available(): List<Pair<String, String>> = try {
        Locale.getAvailableLocales()
            .map { it.displayLanguage to it.isO3Language }
            .distinctBy { it.second }
            .sortedBy { it.first }
    } catch (_: Exception) {
        emptyList()
    }

    /** Display name for a stored ISO 639-2 code; "" when it is unknown. */
    fun displayName(code: String): String = try {
        if (code.isBlank()) "" else
            Locale.getAvailableLocales().first { it.isO3Language == code }.displayLanguage
    } catch (_: Exception) {
        ""
    }
}
