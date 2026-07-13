package com.beerdeal.core

/**
 * Matches OCR'd product names against the local ABV database and ranks deals.
 *
 * Matching strategy (same tiered approach as a search normalizer):
 *   1. Normalize both sides (lowercase, strip punctuation, collapse spaces,
 *      drop noise words like "beer", "lager", "cans").
 *   2. Exact normalized match, then alias match, then token-overlap score
 *      with a Levenshtein tiebreak for OCR typos ("MlLLER L1TE").
 */
class DealEngine(private val database: List<BeerRecord>) {

    private val noiseWords = setOf("beer", "cans", "can", "btls", "bottles", "case", "the")

    fun normalize(s: String): String =
        fixOcrConfusables(s)
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in noiseWords }
            .joinToString(" ")

    /** Common ML Kit misreads: 1↔l/i, 0↔o. Applied before tokenization. */
    private fun fixOcrConfusables(s: String): String =
        s.map { c ->
            when (c) {
                '0' -> 'o'
                '1' -> 'i'
                else -> c
            }
        }.joinToString("")

    data class Match(val record: BeerRecord, val confidence: Double)

    fun match(ocrName: String): Match? {
        val query = normalize(ocrName)
        if (query.isBlank()) return null

        // Tier 1: exact normalized name or alias
        database.firstOrNull { normalize(it.name) == query }?.let { return Match(it, 1.0) }
        database.firstOrNull { rec -> rec.aliases.any { normalize(it) == query } }
            ?.let { return Match(it, 0.95) }

        // Tier 2: token overlap + edit distance
        val queryTokens = query.split(" ").toSet()
        val scored = database.mapNotNull { rec ->
            val recTokens = normalize(rec.name).split(" ").toSet()
            val overlap = queryTokens.intersect(recTokens).size.toDouble()
            val union = queryTokens.union(recTokens).size.toDouble()
            val jaccard = if (union > 0) overlap / union else 0.0
            val editSim = 1.0 - levenshtein(query, normalize(rec.name)).toDouble() /
                maxOf(query.length, normalize(rec.name).length)
            val score = 0.6 * jaccard + 0.4 * editSim
            if (score >= 0.5) Match(rec, score) else null
        }
        return scored.maxByOrNull { it.confidence }
    }

    /**
     * Builds a candidate from a parsed tag if all required fields resolve.
     * Returns null when data is missing — the UI should surface these tags
     * for manual completion instead of silently dropping them.
     */
    fun toCandidate(tag: ParsedTag): DealCandidate? {
        val price = tag.priceCents ?: return null
        val volume = tag.unitVolumeOz ?: return null
        val name = tag.productName ?: return null
        val m = match(name) ?: return null
        // Singles (19.2oz tallboys, bombers) have no pack marking; default 1
        val count = tag.packCount ?: 1

        return DealCandidate(
            displayName = m.record.name,
            priceCents = price,
            packCount = count,
            unitVolumeOz = volume,
            abv = m.record.abv,
            matchConfidence = m.confidence,
        )
    }

    /** Primary ranking: cheapest alcohol first. */
    fun rank(candidates: List<DealCandidate>): List<DealCandidate> =
        candidates.sortedBy { it.centsPerAlcoholOz }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            curr.copyInto(prev)
        }
        return prev[b.length]
    }
}
