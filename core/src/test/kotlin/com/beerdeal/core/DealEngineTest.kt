package com.beerdeal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DealEngineTest {

    private val db = listOf(
        BeerRecord(1, "Miller Lite", "Molson Coors", 4.2, aliases = listOf("miller light")),
        BeerRecord(2, "Busch Light", "Anheuser-Busch", 4.1),
        BeerRecord(3, "Hazy Little Thing", "Sierra Nevada", 6.7),
        BeerRecord(4, "Voodoo Ranger Imperial IPA", "New Belgium", 9.0),
    )
    private val engine = DealEngine(db)

    @Test
    fun `parses a typical shelf tag`() {
        val tag = TagParser.parse("MILLER LITE\n30 PK 12 OZ CANS\n\$22.99")
        assertEquals(2299, tag.priceCents)
        assertEquals(30, tag.packCount)
        assertEquals(12.0, tag.unitVolumeOz!!, 0.001)
        assertEquals("MILLER LITE", tag.productName)
    }

    @Test
    fun `parses count-x-volume shorthand`() {
        val tag = TagParser.parse("Hazy Little Thing IPA 12x12oz \$17.49")
        assertEquals(12, tag.packCount)
        assertEquals(12.0, tag.unitVolumeOz!!, 0.001)
        assertEquals(1749, tag.priceCents)
    }

    @Test
    fun `does not mistake tallboy volume for price`() {
        val tag = TagParser.parse("VOODOO RANGER\n19.2 OZ CAN\n\$3.49")
        assertEquals(349, tag.priceCents)
        assertEquals(19.2, tag.unitVolumeOz!!, 0.001)
    }

    @Test
    fun `handles multibuy pricing`() {
        val tag = TagParser.parse("BUSCH LIGHT 12PK 12OZ 2/\$20")
        assertEquals(1000, tag.priceCents)
    }

    @Test
    fun `fuzzy matches OCR typos`() {
        val m = engine.match("MlLLER L1TE")
        assertNotNull(m)
        assertEquals("Miller Lite", m!!.record.name)
    }

    @Test
    fun `ranks by cost per alcohol ounce not sticker price`() {
        // 30-rack of 4.2% at $22.99 vs 12-pack of 9.0% at $18.99:
        // the imperial IPA is cheaper per ounce of alcohol despite fewer cans.
        val lite = engine.toCandidate(
            TagParser.parse("MILLER LITE\n30 PK 12 OZ CANS\n\$22.99")
        )!!
        val voodoo = engine.toCandidate(
            TagParser.parse("VOODOO RANGER IMPERIAL IPA\n12 PK 12 OZ\n\$18.99")
        )!!

        val ranked = engine.rank(listOf(lite, voodoo))
        assertEquals("Voodoo Ranger Imperial IPA", ranked[0].displayName)
        assertTrue(ranked[0].centsPerAlcoholOz < ranked[1].centsPerAlcoholOz)
    }

    @Test
    fun `incomplete tag returns null candidate for manual entry flow`() {
        val tag = TagParser.parse("SOME UNKNOWN CRAFT BEER")
        assertEquals(null, engine.toCandidate(tag))
    }
}
