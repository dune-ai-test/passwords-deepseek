package com.aegis.vault.data

import android.content.Context

/**
 * The bundled EFF short word list (1,296 words, CC0) used by the
 * "Memorable" password mode.
 */
class Wordlist(private val context: Context) {

    @Volatile
    private var cached: List<String>? = null

    fun words(): List<String> {
        cached?.let { return it }
        val loaded = try {
            context.assets.open("eff_short_wordlist.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            }
        } catch (e: Exception) {
            FALLBACK
        }
        cached = loaded
        return loaded
    }

    private companion object {
        val FALLBACK = listOf(
            "anchor", "arrow", "beacon", "bridge", "candle", "castle", "cedar", "cobalt",
            "compass", "copper", "crystal", "delta", "ember", "falcon", "forest", "gadget",
            "galaxy", "garden", "harbor", "hazel", "island", "jungle", "kettle", "lantern",
            "legend", "lunar", "magnet", "meadow", "meteor", "mosaic", "nectar", "nomad",
            "oasis", "onyx", "orbit", "pebble", "phoenix", "pilot", "quartz", "raven",
            "rocket", "saffron", "sapphire", "shadow", "silver", "sparrow", "stellar",
            "summit", "tundra", "velvet", "vertex", "violet", "walnut", "willow", "zenith",
        )
    }
}
