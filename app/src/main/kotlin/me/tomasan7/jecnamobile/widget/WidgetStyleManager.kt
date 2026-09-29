package me.tomasan7.jecnamobile.widget

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.tomasan7.jecnamobile.settings.Settings

@Serializable
data class WidgetColorsExport(
    val backgroundColor: String = "#000000",
    val borderColor: String = "#FF6584",
    val dayHeaderColor: String = "#18181A",
    val dayHeaderTextColor: String = "#FF6584",
    val cellBackgroundColor: String = "#000000",
    val cellTextColor: String = "#FFFFFF",
    val teacherTextColor: String = "#FFFFFF",
    val classroomTextColor: String = "#FFFFFF",
    val groupTextColor: String = "#FFFFFF",
    val activeHourBorderColor: String = "#FF1493",
    val activeHourFillColor: String = "#33FF1493",
    val substitutionColor: String = "#FFA726",
    val substitutionTextColor: String = "#FFA726",
    val substitutionBackgroundColor: String = "#4DFFA726"
)

@Serializable
data class WidgetStyleExport(
    val version: Int = 1,
    val name: String = "Vlastní styl",
    val theme: Settings.WidgetTheme = Settings.WidgetTheme.CUSTOM,
    val solidHeaderBackground: Boolean = false,
    val hideHeader: Boolean = false,
    val hideBorder: Boolean = false,
    val hideClass: Boolean = false,
    val hideTeachers: Boolean = false,
    val periodHeaderMode: Settings.PeriodHeaderMode = Settings.PeriodHeaderMode.NUMBER_ONLY,
    val cellScale: Float = 1.0f,
    val showLastUpdatedTime: Boolean = true,
    val alwaysShowLastUpdated: Boolean = true,
    val colors: WidgetColorsExport = WidgetColorsExport()
)

object WidgetStyleManager
{
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun colorToHex(color: Long): String
    {
        val uColor = color and 0xFFFFFFFFL
        val alpha = (uColor shr 24) and 0xFF
        return if (alpha != 0xFFL)
        {
            String.format("#%08X", uColor)
        }
        else
        {
            String.format("#%06X", uColor and 0x00FFFFFFL)
        }
    }

    fun hexToColor(hex: String, default: Long): Long
    {
        val clean = hex.trim().removePrefix("#")
        if (clean.equals("transparent", ignoreCase = true) || clean.equals("none", ignoreCase = true) || clean.equals("pruhledna", ignoreCase = true) || clean == "0")
        {
            return 0x00000000L
        }
        return when (clean.length)
        {
            6 -> clean.toLongOrNull(16)?.let { 0xFF000000L or it } ?: default
            8 -> clean.toLongOrNull(16) ?: default
            4 -> {
                val a = clean[0]
                val r = clean[1]
                val g = clean[2]
                val b = clean[3]
                "$a$a$r$r$g$g$b$b".toLongOrNull(16) ?: default
            }
            3 -> {
                val r = clean[0]
                val g = clean[1]
                val b = clean[2]
                "$r$r$g$g$b$b".toLongOrNull(16)?.let { 0xFF000000L or it } ?: default
            }
            else -> default
        }
    }

    /**
     * Exports the given widget settings into a human-readable JSON string.
     */
    fun exportToJson(settings: Settings.WidgetSettings, name: String = "Vlastní styl"): String
    {
        val colors = if (settings.theme == Settings.WidgetTheme.CUSTOM) {
            settings.customColors
        } else {
            TimetableWidgetRenderer.getEffectiveColors(settings)
        }

        val colorsExport = WidgetColorsExport(
            backgroundColor = colorToHex(colors.backgroundColor),
            borderColor = colorToHex(colors.borderColor),
            dayHeaderColor = colorToHex(colors.dayHeaderColor),
            dayHeaderTextColor = colorToHex(colors.dayHeaderTextColor),
            cellBackgroundColor = colorToHex(colors.cellBackgroundColor),
            cellTextColor = colorToHex(colors.cellTextColor),
            teacherTextColor = colorToHex(colors.teacherTextColor),
            classroomTextColor = colorToHex(colors.classroomTextColor),
            groupTextColor = colorToHex(colors.groupTextColor),
            activeHourBorderColor = colorToHex(colors.activeHourBorderColor),
            activeHourFillColor = colorToHex(colors.activeHourFillColor),
            substitutionColor = colorToHex(colors.substitutionColor),
            substitutionTextColor = colorToHex(colors.substitutionTextColor),
            substitutionBackgroundColor = colorToHex(colors.substitutionBackgroundColor)
        )

        val export = WidgetStyleExport(
            version = 1,
            name = name,
            theme = Settings.WidgetTheme.CUSTOM,
            solidHeaderBackground = settings.solidHeaderBackground,
            hideHeader = settings.hideHeader,
            hideBorder = settings.hideBorder,
            hideClass = settings.hideClass,
            hideTeachers = settings.hideTeachers,
            periodHeaderMode = settings.periodHeaderMode,
            cellScale = settings.cellScale,
            showLastUpdatedTime = settings.showLastUpdatedTime,
            alwaysShowLastUpdated = settings.alwaysShowLastUpdated,
            colors = colorsExport
        )

        return json.encodeToString(WidgetStyleExport.serializer(), export)
    }

    /**
     * Parses a style JSON string into a WidgetSettings object or returns null if parsing fails.
     */
    fun parseJson(jsonString: String): Settings.WidgetSettings?
    {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return null

        // Attempt 1: Parse WidgetStyleExport (our clean format)
        try
        {
            val export = json.decodeFromString(WidgetStyleExport.serializer(), trimmed)
            val defColors = Settings.WidgetColors()
            val subTextColor = if (export.colors.substitutionTextColor != "#FFA726") {
                hexToColor(export.colors.substitutionTextColor, defColors.substitutionTextColor)
            } else {
                hexToColor(export.colors.substitutionColor, defColors.substitutionTextColor)
            }
            val subBgColor = if (export.colors.substitutionBackgroundColor != "#4DFFA726") {
                hexToColor(export.colors.substitutionBackgroundColor, defColors.substitutionBackgroundColor)
            } else {
                val sc = hexToColor(export.colors.substitutionColor, defColors.substitutionColor)
                (sc and 0x00FFFFFFL) or 0x4D000000L
            }

            val parsedColors = Settings.WidgetColors(
                backgroundColor = hexToColor(export.colors.backgroundColor, defColors.backgroundColor),
                borderColor = hexToColor(export.colors.borderColor, defColors.borderColor),
                dayHeaderColor = hexToColor(export.colors.dayHeaderColor, defColors.dayHeaderColor),
                dayHeaderTextColor = hexToColor(export.colors.dayHeaderTextColor, defColors.dayHeaderTextColor),
                cellBackgroundColor = hexToColor(export.colors.cellBackgroundColor, defColors.cellBackgroundColor),
                cellTextColor = hexToColor(export.colors.cellTextColor, defColors.cellTextColor),
                teacherTextColor = hexToColor(export.colors.teacherTextColor, defColors.teacherTextColor),
                classroomTextColor = hexToColor(export.colors.classroomTextColor, defColors.classroomTextColor),
                groupTextColor = hexToColor(export.colors.groupTextColor, defColors.groupTextColor),
                activeHourBorderColor = hexToColor(export.colors.activeHourBorderColor, defColors.activeHourBorderColor),
                activeHourFillColor = hexToColor(export.colors.activeHourFillColor, defColors.activeHourFillColor),
                substitutionColor = hexToColor(export.colors.substitutionColor, defColors.substitutionColor),
                substitutionTextColor = subTextColor,
                substitutionBackgroundColor = subBgColor
            )

            return Settings.WidgetSettings(
                theme = export.theme,
                solidHeaderBackground = export.solidHeaderBackground,
                hideHeader = export.hideHeader,
                hideBorder = export.hideBorder,
                hideClass = export.hideClass,
                hideTeachers = export.hideTeachers,
                periodHeaderMode = export.periodHeaderMode,
                cellScale = export.cellScale,
                showLastUpdatedTime = export.showLastUpdatedTime,
                alwaysShowLastUpdated = export.alwaysShowLastUpdated,
                customColors = parsedColors
            )
        }
        catch (e: Exception)
        {
            // Attempt 2: Direct raw Settings.WidgetSettings
            try
            {
                return json.decodeFromString(Settings.WidgetSettings.serializer(), trimmed)
            }
            catch (e2: Exception)
            {
                return null
            }
        }
    }
}
