package com.beerdeal.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.beerdeal.core.BeerRecord
import com.beerdeal.core.DealCandidate
import com.beerdeal.core.DealEngine
import com.beerdeal.core.TagParser
import java.util.Locale

/**
 * Feature #2 of the roadmap: the ranked deals screen, with manual tag entry
 * standing in for camera capture (feature #1) until OCR lands.
 *
 * Uses only platform widgets — no androidx — so the app builds both with the
 * standard AGP toolchain and in network-restricted environments where
 * Google's Maven repository is unreachable.
 */
class MainActivity : Activity() {

    private val db = listOf(
        BeerRecord(1, "Miller Lite", "Molson Coors", 4.2, aliases = listOf("miller light")),
        BeerRecord(2, "Busch Light", "Anheuser-Busch", 4.1),
        BeerRecord(3, "Bud Light", "Anheuser-Busch", 4.2),
        BeerRecord(4, "Coors Light", "Molson Coors", 4.2),
        BeerRecord(5, "Pabst Blue Ribbon", "Pabst Brewing", 4.7, aliases = listOf("pbr")),
        BeerRecord(6, "Modelo Especial", "Grupo Modelo", 4.4),
        BeerRecord(7, "Corona Extra", "Grupo Modelo", 4.6),
        BeerRecord(8, "Hazy Little Thing", "Sierra Nevada", 6.7),
        BeerRecord(9, "Voodoo Ranger Imperial IPA", "New Belgium", 9.0),
        BeerRecord(10, "Elysian Space Dust", "Elysian Brewing", 8.2, aliases = listOf("space dust ipa")),
    )

    private val sampleTags = listOf(
        "MILLER LITE\n30 PK 12 OZ CANS\n\$22.99",
        "VOODOO RANGER IMPERIAL IPA\n12 PK 12 OZ\n\$18.99",
        "BUSCH LIGHT 12PK 12OZ 2/\$20",
    )

    private val engine = DealEngine(db)
    private val deals = mutableListOf<DealCandidate>()

    private lateinit var tagInput: EditText
    private lateinit var statusText: TextView
    private lateinit var resultsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tagInput = findViewById<EditText>(R.id.tagInput)!!
        statusText = findViewById<TextView>(R.id.statusText)!!
        resultsText = findViewById<TextView>(R.id.resultsText)!!

        findViewById<Button>(R.id.addButton)!!.setOnClickListener { addTag() }
        findViewById<Button>(R.id.samplesButton)!!.setOnClickListener { loadSamples() }
        findViewById<Button>(R.id.clearButton)!!.setOnClickListener { clearAll() }

        render()
    }

    private fun addTag() {
        val raw = tagInput.text.toString().trim()
        if (raw.isEmpty()) {
            status(getString(R.string.status_empty_input))
            return
        }
        val tag = TagParser.parse(raw)
        val candidate = engine.toCandidate(tag)
        if (candidate == null) {
            val missing = buildList {
                if (tag.productName == null) add("name")
                if (tag.priceCents == null) add("price")
                if (tag.unitVolumeOz == null) add("size (oz/ml)")
            }
            status(
                if (missing.isNotEmpty()) {
                    getString(R.string.status_missing_fields, missing.joinToString(", "))
                } else {
                    getString(R.string.status_no_match, tag.productName)
                }
            )
            return
        }
        deals += candidate
        tagInput.text.clear()
        status(getString(R.string.status_added, candidate.displayName))
        render()
    }

    private fun loadSamples() {
        deals += sampleTags.mapNotNull { engine.toCandidate(TagParser.parse(it)) }
        status(getString(R.string.status_samples_loaded))
        render()
    }

    private fun clearAll() {
        deals.clear()
        status("")
        render()
    }

    private fun status(message: String) {
        statusText.text = message
    }

    private fun render() {
        if (deals.isEmpty()) {
            resultsText.text = getString(R.string.empty_state)
            return
        }
        val ranked = engine.rank(deals)
        resultsText.text = buildString {
            ranked.forEachIndexed { index, deal ->
                appendLine("#${index + 1}  ${deal.displayName}")
                appendLine(
                    "    ${dollars(deal.priceCents.toDouble())} · " +
                        "${deal.packCount} × ${trim(deal.unitVolumeOz)}oz · " +
                        "${trim(deal.abv)}% ABV"
                )
                appendLine(
                    "    ${dollars(deal.centsPerAlcoholOz)}/oz alcohol · " +
                        "${dollars(deal.centsPerOz)}/oz · " +
                        "match ${(deal.matchConfidence * 100).toInt()}%"
                )
                appendLine()
            }
        }.trimEnd()
    }

    private fun dollars(cents: Double): String =
        String.format(Locale.US, "$%.2f", cents / 100.0)

    private fun trim(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
