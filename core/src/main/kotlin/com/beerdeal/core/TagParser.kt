package com.beerdeal.core

/**
 * Extracts price, pack count, and unit volume from raw OCR text of a shelf tag.
 *
 * Design notes:
 * - Operates on the full OCR blob for one tag (per-tag capture mode).
 *   ML Kit returns text blocks; join their lines with '\n' before calling.
 * - Everything is best-effort. Missing fields stay null and the UI asks.
 * - Prices: handles "$18.99", "18.99", "1899" (some tags omit the decimal),
 *   and "2/$15" multi-buy pricing (uses per-unit price).
 */
object TagParser {

    // "$18.99" or "18.99" — requires decimal to avoid matching pack counts
    private val PRICE_DECIMAL = Regex("""\$?\s*(\d{1,3})\.(\d{2})\b""")

    // "2/$15" or "2 for $15" multi-buy
    private val PRICE_MULTIBUY = Regex("""(\d+)\s*(?:/|for)\s*\$\s*(\d+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)

    // "12pk", "12 pk", "12-pack", "12 pack", "30 PACK"
    private val PACK_COUNT = Regex("""\b(\d{1,2})\s*[- ]?\s*(?:pk|pack|ct|cnt|count)\b""", RegexOption.IGNORE_CASE)

    // "12oz", "12 oz", "16 OZ", "19.2oz" (tallboy singles), "750ml"
    private val VOLUME_OZ = Regex("""\b(\d{1,2}(?:\.\d)?)\s*[- ]?\s*(?:oz|fl\.?\s*oz)\b""", RegexOption.IGNORE_CASE)
    private val VOLUME_ML = Regex("""\b(\d{3,4})\s*ml\b""", RegexOption.IGNORE_CASE)

    // Common count-x-volume shorthand: "12x12oz", "24 x 12 oz"
    private val COUNT_X_VOLUME = Regex("""\b(\d{1,2})\s*[xX×]\s*(\d{1,2}(?:\.\d)?)\s*(?:oz)?\b""")

    private const val ML_PER_OZ = 29.5735

    fun parse(rawText: String): ParsedTag {
        val text = rawText.trim()

        var packCount: Int? = null
        var unitVolumeOz: Double? = null

        // Try "12x12oz" shorthand first — it pins both fields at once
        COUNT_X_VOLUME.find(text)?.let {
            packCount = it.groupValues[1].toInt()
            unitVolumeOz = it.groupValues[2].toDouble()
        }

        if (packCount == null) {
            packCount = PACK_COUNT.find(text)?.groupValues?.get(1)?.toInt()
        }
        if (unitVolumeOz == null) {
            unitVolumeOz = VOLUME_OZ.find(text)?.groupValues?.get(1)?.toDouble()
                ?: VOLUME_ML.find(text)?.groupValues?.get(1)?.toInt()?.let { ml -> ml / ML_PER_OZ }
        }

        val priceCents = parsePriceCents(text)
        val productName = extractProductName(text)

        return ParsedTag(
            rawText = rawText,
            productName = productName,
            priceCents = priceCents,
            packCount = packCount,
            unitVolumeOz = unitVolumeOz,
        )
    }

    private fun parsePriceCents(text: String): Int? {
        PRICE_MULTIBUY.find(text)?.let {
            val qty = it.groupValues[1].toInt()
            val total = it.groupValues[2].toDouble()
            if (qty > 0) return ((total / qty) * 100).toInt()
        }
        // Prefer a price with a $ sign if multiple decimals appear (avoids
        // matching "19.2oz" volume as a price)
        val candidates = PRICE_DECIMAL.findAll(text).toList()
        val dollarSigned = candidates.firstOrNull { it.value.contains('$') }
        val best = dollarSigned ?: candidates.firstOrNull { m ->
            // Reject matches immediately followed by a volume unit
            val tail = text.substring(m.range.last + 1).trimStart()
            !tail.startsWith("oz", ignoreCase = true) && !tail.startsWith("ml", ignoreCase = true)
        }
        return best?.let { it.groupValues[1].toInt() * 100 + it.groupValues[2].toInt() }
    }

    /**
     * Heuristic: the product name is usually the longest line that isn't
     * dominated by digits/units. Good enough to seed the fuzzy matcher.
     */
    private fun extractProductName(text: String): String? {
        return text.lines()
            .map { it.trim() }
            .filter { it.length >= 4 }
            .filterNot { looksLikeSpecLine(it) }
            .maxByOrNull { it.length }
    }

    /** Price, pack-size, and volume lines are spec data — not the brand name. */
    private fun looksLikeSpecLine(line: String): Boolean {
        if (PRICE_MULTIBUY.containsMatchIn(line)) return true
        if (PACK_COUNT.containsMatchIn(line)) return true
        if (VOLUME_OZ.containsMatchIn(line)) return true
        if (VOLUME_ML.containsMatchIn(line)) return true
        if (COUNT_X_VOLUME.containsMatchIn(line)) return true
        if (PRICE_DECIMAL.containsMatchIn(line) && line.contains('$')) return true
        val digitRatio = line.count { it.isDigit() }.toDouble() / line.length
        return digitRatio > 0.4
    }
}
