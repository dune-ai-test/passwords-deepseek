package com.aegis.vault.data

import androidx.compose.ui.graphics.Color
import com.aegis.vault.ui.design.AegisColors
import kotlin.math.absoluteValue

data class Brand(val letter: String, val color: Color)

data class CardBrand(val name: String, val color: Color)

/**
 * Recognised service brands → letter badges with their brand colour,
 * plus credit-card network detection for the card visual.
 */
object Brands {

    private val known: Map<String, Brand> = mapOf(
        "github.com" to Brand("G", Color(0xFF24292F)),
        "gitlab.com" to Brand("G", Color(0xFFFC6D26)),
        "bitbucket.org" to Brand("B", Color(0xFF0052CC)),
        "google.com" to Brand("G", Color(0xFFEA4335)),
        "gmail.com" to Brand("G", Color(0xFFEA4335)),
        "youtube.com" to Brand("Y", Color(0xFFFF0000)),
        "figma.com" to Brand("F", Color(0xFFF24E1E)),
        "chase.com" to Brand("C", Color(0xFF117ACA)),
        "netflix.com" to Brand("N", Color(0xFFE50914)),
        "notion.so" to Brand("N", Color(0xFF475569)),
        "amazon.com" to Brand("A", Color(0xFFFF9900)),
        "spotify.com" to Brand("S", Color(0xFF1DB954)),
        "discord.com" to Brand("D", Color(0xFF5865F2)),
        "linkedin.com" to Brand("L", Color(0xFF0A66C2)),
        "dropbox.com" to Brand("D", Color(0xFF0061FF)),
        "apple.com" to Brand("A", Color(0xFF111111)),
        "icloud.com" to Brand("I", Color(0xFF3693F3)),
        "paypal.com" to Brand("P", Color(0xFF003087)),
        "venmo.com" to Brand("V", Color(0xFF3D95CE)),
        "coinbase.com" to Brand("C", Color(0xFF0052FF)),
        "binance.com" to Brand("B", Color(0xFFF0B90B)),
        "kraken.com" to Brand("K", Color(0xFF5741D9)),
        "metamask.io" to Brand("M", Color(0xFFF6851B)),
        "robinhood.com" to Brand("R", Color(0xFF00C805)),
        "wise.com" to Brand("W", Color(0xFF163300)),
        "revolut.com" to Brand("R", Color(0xFF0075EB)),
        "monzo.com" to Brand("M", Color(0xFFFF4F40)),
        "chime.com" to Brand("C", Color(0xFF1EC677)),
        "wellsfargo.com" to Brand("W", Color(0xFFD71E28)),
        "bankofamerica.com" to Brand("B", Color(0xFF012169)),
        "capitalone.com" to Brand("C", Color(0xFF004977)),
        "americanexpress.com" to Brand("A", Color(0xFF2E77BC)),
        "reddit.com" to Brand("R", Color(0xFFFF4500)),
        "x.com" to Brand("X", Color(0xFF111111)),
        "twitter.com" to Brand("T", Color(0xFF1DA1F2)),
        "instagram.com" to Brand("I", Color(0xFFE1306C)),
        "facebook.com" to Brand("F", Color(0xFF1877F2)),
        "microsoft.com" to Brand("M", Color(0xFF0067B8)),
        "outlook.com" to Brand("O", Color(0xFF0078D4)),
        "yahoo.com" to Brand("Y", Color(0xFF6001D2)),
        "protonmail.com" to Brand("P", Color(0xFF6D4AFF)),
        "proton.me" to Brand("P", Color(0xFF6D4AFF)),
        "steamcommunity.com" to Brand("S", Color(0xFF171A21)),
        "steampowered.com" to Brand("S", Color(0xFF171A21)),
        "epicgames.com" to Brand("E", Color(0xFF2A2A2A)),
        "twitch.tv" to Brand("T", Color(0xFF9146FF)),
        "pinterest.com" to Brand("P", Color(0xFFE60023)),
        "slack.com" to Brand("S", Color(0xFF4A154B)),
        "zoom.us" to Brand("Z", Color(0xFF2D8CFF)),
        "adobe.com" to Brand("A", Color(0xFFFA0F00)),
        "airbnb.com" to Brand("A", Color(0xFFFF5A5F)),
        "uber.com" to Brand("U", Color(0xFF111111)),
        "walmart.com" to Brand("W", Color(0xFF0071DC)),
        "target.com" to Brand("T", Color(0xFFCC0000)),
        "bestbuy.com" to Brand("B", Color(0xFF0046BE)),
        "hulu.com" to Brand("H", Color(0xFF1CE783)),
        "disneyplus.com" to Brand("D", Color(0xFF113CCF)),
        "whatsapp.com" to Brand("W", Color(0xFF25D366)),
        "telegram.org" to Brand("T", Color(0xFF26A5E4)),
        "signal.org" to Brand("S", Color(0xFF3A76F0)),
        "snapchat.com" to Brand("S", Color(0xFFF7D600)),
        "tiktok.com" to Brand("T", Color(0xFF111111)),
        "spotify.com" to Brand("S", Color(0xFF1DB954)),
        "stackoverflow.com" to Brand("S", Color(0xFFF48024)),
        "atlassian.com" to Brand("A", Color(0xFF0052CC)),
        "cloudflare.com" to Brand("C", Color(0xFFF38020)),
        "namecheap.com" to Brand("N", Color(0xFFDE3723)),
        "shopify.com" to Brand("S", Color(0xFF5E8E3E)),
        "etsy.com" to Brand("E", Color(0xFFF1641E)),
        "ebay.com" to Brand("E", Color(0xFFE53238)),
        "booking.com" to Brand("B", Color(0xFF003580)),
        "zillow.com" to Brand("Z", Color(0xFF006AFF)),
        "duolingo.com" to Brand("D", Color(0xFF58CC02)),
        "strava.com" to Brand("S", Color(0xFFFC4C02)),
    )

    /** Returns a badge for a website/domain string, or null when it isn't one. */
    fun forDomain(raw: String): Brand? {
        val cleaned = raw.trim().lowercase()
            .substringAfter("://")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(" ")
            .removePrefix("www.")
            .trim()
        if (cleaned.isBlank()) return null
        known[cleaned]?.let { return it }
        known.entries.firstOrNull { (domain, _) -> cleaned.endsWith(".$domain") }?.let { return it.value }
        return if (cleaned.contains(".")) {
            val letter = cleaned.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: return null
            Brand(letter, palette(cleaned))
        } else {
            null
        }
    }

    private fun palette(seed: String): Color {
        val index = (seed.hashCode().toLong().absoluteValue % AegisColors.letterPalette.size).toInt()
        return AegisColors.letterPalette[index]
    }

    fun cardBrand(number: String): CardBrand {
        val digits = number.filter { it.isDigit() }
        return when {
            digits.startsWith("4") -> CardBrand("VISA", Color(0xFF1A1F71))
            digits.startsWith("34") || digits.startsWith("37") -> CardBrand("AMEX", Color(0xFF2E77BC))
            digits.startsWith("5") || digits.startsWith("2") -> CardBrand("MASTERCARD", Color(0xFFEB001B))
            digits.startsWith("6") -> CardBrand("DISCOVER", Color(0xFFF76B1C))
            else -> CardBrand("CARD", Color(0xFF007AFF))
        }
    }
}
