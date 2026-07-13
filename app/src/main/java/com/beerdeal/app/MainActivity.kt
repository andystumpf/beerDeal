package com.beerdeal.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beerdeal.core.BeerRecord
import com.beerdeal.core.DealEngine
import com.beerdeal.core.TagParser
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val sampleDb = listOf(
        BeerRecord(1, "Miller Lite", "Molson Coors", 4.2, aliases = listOf("miller light")),
        BeerRecord(2, "Busch Light", "Anheuser-Busch", 4.1),
        BeerRecord(3, "Hazy Little Thing", "Sierra Nevada", 6.7),
        BeerRecord(4, "Voodoo Ranger Imperial IPA", "New Belgium", 9.0),
    )

    private val sampleTags = listOf(
        "MILLER LITE\n30 PK 12 OZ CANS\n\$22.99",
        "VOODOO RANGER IMPERIAL IPA\n12 PK 12 OZ\n\$18.99",
        "BUSCH LIGHT 12PK 12OZ 2/\$20",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val engine = DealEngine(sampleDb)
        val candidates = sampleTags.mapNotNull { engine.toCandidate(TagParser.parse(it)) }
        val ranked = engine.rank(candidates)

        val output = buildString {
            appendLine(getString(R.string.sample_deals_header))
            appendLine()
            ranked.forEachIndexed { index, deal ->
                appendLine("${index + 1}. ${deal.displayName}")
                appendLine(
                    "   ${formatCents(deal.centsPerAlcoholOz)}/alcohol oz · " +
                        "${formatCents(deal.centsPerOz)}/beer oz · " +
                        "${deal.abv}% ABV"
                )
                appendLine()
            }
            appendLine(getString(R.string.camera_coming_soon))
        }

        findViewById<TextView>(R.id.resultsText).text = output.trimEnd()
    }

    private fun formatCents(cents: Double): String =
        String.format(Locale.US, "\$%.2f", cents / 100.0)
}
