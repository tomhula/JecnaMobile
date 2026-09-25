package me.tomasan7.jecnamobile.widget

import me.tomasan7.jecnamobile.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetStyleManagerTest
{
    @Test
    fun testColorToHex()
    {
        // Opaque color should produce #RRGGBB
        assertEquals("#FF6584", WidgetStyleManager.colorToHex(0xFFFF6584L))
        assertEquals("#000000", WidgetStyleManager.colorToHex(0xFF000000L))

        // Fully transparent color should produce #00000000
        assertEquals("#00000000", WidgetStyleManager.colorToHex(0x00000000L))

        // Semi-transparent color should produce #AARRGGBB
        assertEquals("#80000000", WidgetStyleManager.colorToHex(0x80000000L))
        assertEquals("#33FF1493", WidgetStyleManager.colorToHex(0x33FF1493L))
    }

    @Test
    fun testHexToColorKeywords()
    {
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("transparent", 0xFFFFFFFFL))
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("#transparent", 0xFFFFFFFFL))
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("none", 0xFFFFFFFFL))
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("pruhledna", 0xFFFFFFFFL))
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("0", 0xFFFFFFFFL))
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("#0", 0xFFFFFFFFL))
    }

    @Test
    fun testHexToColorFormats()
    {
        // 6 digits
        assertEquals(0xFFFF6584L, WidgetStyleManager.hexToColor("#FF6584", 0L))
        assertEquals(0xFF000000L, WidgetStyleManager.hexToColor("#000000", 0L))

        // 8 digits (ARGB)
        assertEquals(0x00000000L, WidgetStyleManager.hexToColor("#00000000", 0xFFFFFFFFL))
        assertEquals(0x80FF6584L, WidgetStyleManager.hexToColor("#80FF6584", 0L))

        // 4 digits (#ARGB -> #AARRGGBB)
        assertEquals(0x88AABBCCL, WidgetStyleManager.hexToColor("#8ABC", 0L))
        assertEquals(0x00FFFFFFL, WidgetStyleManager.hexToColor("#0FFF", 0L))

        // 3 digits (#RGB -> #FFRRGGBB)
        assertEquals(0xFF112233L, WidgetStyleManager.hexToColor("#123", 0L))
    }

    @Test
    fun testExportAndParseRoundTripWithTransparency()
    {
        val initialSettings = Settings.WidgetSettings(
            theme = Settings.WidgetTheme.CUSTOM,
            customColors = Settings.WidgetColors(
                backgroundColor = 0x00000000L, // fully transparent
                borderColor = 0x80FF6584L,    // semi-transparent
                dayHeaderColor = 0x00000000L,
                dayHeaderTextColor = 0xFFFFFFFFL
            )
        )

        val exportedJson = WidgetStyleManager.exportToJson(initialSettings, "Transparent Style")
        val parsed = WidgetStyleManager.parseJson(exportedJson)

        assertNotNull(parsed)
        assertEquals(0x00000000L, parsed!!.customColors.backgroundColor)
        assertEquals(0x80FF6584L, parsed.customColors.borderColor)
        assertEquals(0x00000000L, parsed.customColors.dayHeaderColor)
        assertEquals(0xFFFFFFFFL, parsed.customColors.dayHeaderTextColor)
    }
}
