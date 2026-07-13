package com.beerdeal.core

/**
 * A parsed shelf tag before enrichment. Fields are nullable because OCR
 * is messy — the UI should prompt the user to fill gaps rather than fail.
 */
data class ParsedTag(
    val rawText: String,
    val productName: String?,
    val priceCents: Int?,
    val packCount: Int?,        // e.g. 12 in "12pk"
    val unitVolumeOz: Double?,  // e.g. 12.0 in "12oz cans"
)

/**
 * A known beer in the local ABV database.
 */
data class BeerRecord(
    val id: Long,
    val name: String,           // canonical name, e.g. "Miller Lite"
    val brewery: String,
    val abv: Double,            // 4.2 means 4.2%
    val aliases: List<String> = emptyList(), // OCR-friendly variants
)

/**
 * A fully-resolved deal candidate, ready for ranking.
 */
data class DealCandidate(
    val displayName: String,
    val priceCents: Int,
    val packCount: Int,
    val unitVolumeOz: Double,
    val abv: Double,
    val matchConfidence: Double, // 0.0–1.0 from the fuzzy matcher
) {
    val totalVolumeOz: Double get() = packCount * unitVolumeOz

    /** Standard shopping metric: dollars per fluid ounce of beer. */
    val centsPerOz: Double get() = priceCents / totalVolumeOz

    /**
     * The headline metric: dollars per ounce of pure ethanol.
     * totalVolumeOz * (abv / 100) = ounces of alcohol in the package.
     */
    val centsPerAlcoholOz: Double get() = priceCents / (totalVolumeOz * (abv / 100.0))
}
