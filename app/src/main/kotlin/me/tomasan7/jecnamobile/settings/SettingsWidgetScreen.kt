package me.tomasan7.jecnamobile.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.ui.component.HorizontalDivider
import me.tomasan7.jecnamobile.ui.component.RadioGroup
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
import me.tomasan7.jecnamobile.util.settingsDataStore
import me.tomasan7.jecnamobile.widget.*
import io.github.tomhula.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.caching.SchoolYearPeriodParams
import me.tomasan7.jecnamobile.timetable.TimetableCacheRepository
import me.tomasan7.jecnamobile.timetable.TimetableData
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWidgetScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    targetWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
)
{
    val settings by settingsAsStateAwaitFirst()
    val context = LocalContext.current

    val timetableRepository: TimetableRepository = koinInject()
    val timetableCacheRepository: TimetableCacheRepository = koinInject()

    val cachedTimetableData by produceState<TimetableData?>(initialValue = null) {
        value = try {
            val params = SchoolYearPeriodParams(SchoolYear.current(), SchoolYearPeriodParams.CURRENT_PERIOD_ID)
            timetableCacheRepository.getCache(params)?.data
        } catch (_: Exception) {
            null
        }
    }

    val studentClass by produceState<String?>(initialValue = null, key1 = cachedTimetableData) {
        val fromRepo = try {
            timetableRepository.getStudentClassName()
        } catch (_: Exception) {
            null
        }
        value = fromRepo ?: TimetableWidgetRenderer.extractClassName(cachedTimetableData)
    }

    val detectedGroupSubjects = remember(cachedTimetableData) {
        TimetableWidgetRenderer.extractGroupSubjects(cachedTimetableData)
    }

    val allConfigurableSubjects = remember(detectedGroupSubjects, settings.subjectGroups) {
        val map = detectedGroupSubjects.associateBy { it.subjectShort }.toMutableMap()
        settings.subjectGroups.forEach { (sub, grp) ->
            if (!map.containsKey(sub)) {
                map[sub] = TimetableWidgetRenderer.SubjectGroupInfo(
                    subjectShort = sub,
                    subjectFull = null,
                    availableGroups = listOf(grp)
                )
            }
        }
        map.values.sortedBy { it.subjectShort }
    }

    val allUniqueAvailableGroups = remember(allConfigurableSubjects) {
        allConfigurableSubjects.flatMap { it.availableGroups }.toSet()
    }
    val hasHalves = remember(allUniqueAvailableGroups) {
        allUniqueAvailableGroups.any { it == "1/2" || it == "2/2" }
    }
    val hasThirds = remember(allUniqueAvailableGroups) {
        allUniqueAvailableGroups.any { it == "1/3" || it == "2/3" || it == "3/3" }
    }
    val hasFourths = remember(allUniqueAvailableGroups) {
        allUniqueAvailableGroups.any { it == "1/4" || it == "2/4" || it == "3/4" || it == "4/4" }
    }
    var showAllSubjects by remember { mutableStateOf(false) }
    var selectedEditorDay by remember { mutableStateOf(DayOfWeek.MONDAY) }

    var subjectToEdit by remember { mutableStateOf<TimetableWidgetRenderer.SubjectGroupInfo?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    var selectedWidgetId by remember(targetWidgetId) { mutableIntStateOf(targetWidgetId) }

    var previewMode by remember {
        mutableStateOf(
            if (targetWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val awm = AppWidgetManager.getInstance(context)
                val info = awm.getAppWidgetInfo(targetWidgetId)
                when {
                    info?.provider?.className?.contains("TodayRow") == true -> PreviewWidgetMode.TODAY_ROW
                    info?.provider?.className?.contains("Day") == true -> PreviewWidgetMode.DAY
                    info?.provider?.className?.contains("CurrentHour") == true -> PreviewWidgetMode.CURRENT_HOUR
                    else -> PreviewWidgetMode.WEEK
                }
            } else {
                PreviewWidgetMode.WEEK
            }
        )
    }

    LaunchedEffect(selectedWidgetId) {
        if (selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val awm = AppWidgetManager.getInstance(context)
            val info = awm.getAppWidgetInfo(selectedWidgetId)
            when {
                info?.provider?.className?.contains("TodayRow") == true -> previewMode = PreviewWidgetMode.TODAY_ROW
                info?.provider?.className?.contains("Day") == true -> previewMode = PreviewWidgetMode.DAY
                info?.provider?.className?.contains("CurrentHour") == true -> previewMode = PreviewWidgetMode.CURRENT_HOUR
                else -> previewMode = PreviewWidgetMode.WEEK
            }
        }
    }

    val installedWidgets = remember {
        val awm = AppWidgetManager.getInstance(context)
        val week = awm.getAppWidgetIds(ComponentName(context, TimetableWeekWidgetReceiver::class.java))
        val day = awm.getAppWidgetIds(ComponentName(context, TimetableDayWidgetReceiver::class.java))
        val todayRow = awm.getAppWidgetIds(ComponentName(context, TimetableTodayRowWidgetReceiver::class.java))
        val currentHour = awm.getAppWidgetIds(ComponentName(context, TimetableCurrentHourWidgetReceiver::class.java))

        buildList {
            week.forEach { add(it to "Celý týden (#$it)") }
            day.forEach { add(it to "Dnešní den (#$it)") }
            todayRow.forEach { add(it to "Dnes 1 řádek (#$it)") }
            currentHour.forEach { add(it to "Aktuální hodina (#$it)") }
        }
    }

    val widgetSettings = if (selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        settings.getWidgetSettings(selectedWidgetId)
    } else {
        settings.widgetSettings
    }

    val coroutineScope = rememberCoroutineScope()
    val updateWidgetSettings: ((Settings.WidgetSettings) -> Settings.WidgetSettings) -> Unit = { transform ->
        coroutineScope.launch {
            val updatedSettings = context.settingsDataStore.updateData { current ->
                if (selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    current.withWidgetSettings(selectedWidgetId, transform)
                } else {
                    val newGlobal = transform(current.widgetSettings)
                    val updatedPerWidget = current.perWidgetSettings.mapValues { (_, ws) -> transform(ws) }
                    current.copy(widgetSettings = newGlobal, perWidgetSettings = updatedPerWidget)
                }
            }
            TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
        }
    }

    val effectiveColors = remember(widgetSettings) {
        TimetableWidgetRenderer.getEffectiveColors(widgetSettings)
    }

    var isAdvancedColors by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widgety rozvrhu") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportovat styl")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Importovat styl")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sticky / Pinned Top Section with Preview
            Surface(
                tonalElevation = 1.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Per-widget configuration indicator or widget picker
                    if (targetWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Konfigurujete konkrétní widget (ID #$targetWidgetId)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else if (installedWidgets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WIDGET:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            FilterChip(
                                selected = selectedWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID,
                                onClick = { selectedWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID },
                                label = { Text("Výchozí", fontSize = 12.sp) }
                            )
                            installedWidgets.forEach { (id, label) ->
                                FilterChip(
                                    selected = selectedWidgetId == id,
                                    onClick = { selectedWidgetId = id },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Live Widget Preview Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ŽIVÝ NÁHLED WIDGETU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        val bgAlpha = (effectiveColors.backgroundColor shr 24) and 0xFFL
                        if (widgetSettings.hideBorder || bgAlpha == 0L)
                        {
                            Text(
                                text = "Průhledný (na tapetě)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else if (bgAlpha < 255L)
                        {
                            Text(
                                text = "Poloprůhledný (${(bgAlpha * 100) / 255}% na tapetě)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Preview layout selector chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PreviewWidgetMode.entries.forEach { mode ->
                            FilterChip(
                                selected = previewMode == mode,
                                onClick = { previewMode = mode },
                                label = { Text(mode.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    WidgetLivePreviewCard(
                        widgetSettings = widgetSettings,
                        colors = effectiveColors,
                        studentClass = studentClass,
                        cachedTimetableData = cachedTimetableData,
                        subjectGroups = settings.subjectGroups,
                        previewMode = previewMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Scrollable Content Below
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Widget Theme
                SettingsSection(title = "Barevný motiv") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    RadioGroup(
                        options = Settings.WidgetTheme.entries,
                        selectedOption = widgetSettings.theme,
                        onSelectionChange = { theme ->
                            updateWidgetSettings { it.copy(theme = theme) }
                        }
                    ) {
                        when (it)
                        {
                            Settings.WidgetTheme.LIGHT -> "Světlý režim"
                            Settings.WidgetTheme.DARK -> "Tmavý režim"
                            Settings.WidgetTheme.CUSTOM -> "Vlastní barvy"
                        }
                    }
                }
            }

            // Section 2: Custom Colors (visible when CUSTOM is active)
            if (widgetSettings.theme == Settings.WidgetTheme.CUSTOM)
            {
                SettingsSection(title = "Nastavení vlastních barev") {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mode Switch: Simple vs Advanced
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = !isAdvancedColors,
                                onClick = { isAdvancedColors = false },
                                label = {
                                    Text(
                                        text = "Jednoduché barvy",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isAdvancedColors,
                                onClick = { isAdvancedColors = true },
                                label = {
                                    Text(
                                        text = "Pokročilé (všechny)",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Quick Presets
                        Text(
                            text = "Rychlé předvolby:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                selected = widgetSettings.customColors.borderColor == 0xFF30363DL && widgetSettings.customColors.backgroundColor == 0xFF000000L,
                                onClick = {
                                    // Dark AMOLED
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = Settings.WidgetColors(
                                                backgroundColor = 0xFF000000L,
                                                borderColor = 0xFF30363DL,
                                                dayHeaderColor = 0xFF161B22L,
                                                dayHeaderTextColor = 0xFFE6EDF3L,
                                                cellBackgroundColor = 0xFF0D1117L,
                                                cellTextColor = 0xFFE6EDF3L,
                                                teacherTextColor = 0xFF8B949EL,
                                                classroomTextColor = 0xFF8B949EL,
                                                groupTextColor = 0xFF8B949EL,
                                                activeHourBorderColor = 0xFF2F81F7L,
                                                activeHourFillColor = 0x332F81F7L,
                                                substitutionColor = 0xFFE3B341L,
                                                substitutionTextColor = 0xFFE3B341L,
                                                substitutionBackgroundColor = 0x4DE3B341L
                                            )
                                        )
                                    }
                                },
                                label = { Text("Tmavý režim") }
                            )

                            FilterChip(
                                selected = widgetSettings.customColors.borderColor == 0xFFD0D7DEL,
                                onClick = {
                                    // White
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = Settings.WidgetColors(
                                                backgroundColor = 0xFFFFFFFFL,
                                                borderColor = 0xFFD0D7DEL,
                                                dayHeaderColor = 0xFFF1F3F5L,
                                                dayHeaderTextColor = 0xFF1F2328L,
                                                cellBackgroundColor = 0xFFF6F8FAL,
                                                cellTextColor = 0xFF1F2328L,
                                                teacherTextColor = 0xFF57606AL,
                                                classroomTextColor = 0xFF57606AL,
                                                groupTextColor = 0xFF57606AL,
                                                activeHourBorderColor = 0xFF0969DAL,
                                                activeHourFillColor = 0x330969DAL,
                                                substitutionColor = 0xFFD97706L,
                                                substitutionTextColor = 0xFFD97706L,
                                                substitutionBackgroundColor = 0x33D97706L
                                            )
                                        )
                                    }
                                },
                                label = { Text("Čistě bílá") }
                            )

                            FilterChip(
                                selected = (widgetSettings.customColors.backgroundColor and 0xFF000000L) == 0L && (widgetSettings.customColors.cellBackgroundColor and 0xFF000000L) == 0L,
                                onClick = {
                                    // Fully Transparent
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = Settings.WidgetColors(
                                                backgroundColor = 0x00000000L,
                                                borderColor = 0x4DFFFFFFL,
                                                dayHeaderColor = 0x00000000L,
                                                dayHeaderTextColor = 0xFFFFFFFFL,
                                                cellBackgroundColor = 0x00000000L,
                                                cellTextColor = 0xFFFFFFFFL,
                                                teacherTextColor = 0xCCFFFFFFL,
                                                classroomTextColor = 0xCCFFFFFFL,
                                                groupTextColor = 0xCCFFFFFFL,
                                                activeHourBorderColor = 0xFF00E5FFL,
                                                activeHourFillColor = 0x3300E5FFL,
                                                substitutionColor = 0xFFFFA726L,
                                                substitutionTextColor = 0xFFFFA726L,
                                                substitutionBackgroundColor = 0x4DFFA726L
                                            )
                                        )
                                    }
                                },
                                label = { Text("Průhledný") }
                            )

                            FilterChip(
                                selected = widgetSettings.customColors.backgroundColor == 0x80000000L && widgetSettings.customColors.cellBackgroundColor == 0x33FFFFFFL,
                                onClick = {
                                    // Frosted Dark Glass / Semi-transparent
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = Settings.WidgetColors(
                                                backgroundColor = 0x80000000L,
                                                borderColor = 0x66FFFFFFL,
                                                dayHeaderColor = 0x4D000000L,
                                                dayHeaderTextColor = 0xFFFFFFFFL,
                                                cellBackgroundColor = 0x33FFFFFFL,
                                                cellTextColor = 0xFFFFFFFFL,
                                                teacherTextColor = 0xCCFFFFFFL,
                                                classroomTextColor = 0xCCFFFFFFL,
                                                groupTextColor = 0xCCFFFFFFL,
                                                activeHourBorderColor = 0xFFFF6584L,
                                                activeHourFillColor = 0x40FF6584L,
                                                substitutionColor = 0xFFFFD600L,
                                                substitutionTextColor = 0xFFFFD600L,
                                                substitutionBackgroundColor = 0x4DFFD600L
                                            )
                                        )
                                    }
                                },
                                label = { Text("Poloprůhledný") }
                            )
                        }

                        HorizontalDivider()

                        if (!isAdvancedColors)
                        {
                            // SIMPLE MODE: 4 high-level color pickers
                            ColorPickerRow(
                                label = "Hlavní barva motivu a ohraničení",
                                selectedColor = Color(widgetSettings.customColors.borderColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0xFFFF6584L, // Pink
                                    0xFFFF1493L, // Deep Pink
                                    0xFF00E5FFL, // Cyan
                                    0xFF76FF03L, // Neon Green
                                    0xFFFFD600L, // Yellow
                                    0xFFFFFFFFL, // White
                                    0xFF30363DL  // Gray
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                borderColor = color,
                                                dayHeaderTextColor = if ((color and 0xFF000000L) == 0L) it.customColors.dayHeaderTextColor else color
                                            )
                                        )
                                    }
                                }
                            )

                            ColorPickerRow(
                                label = "Pozadí widgetu a buněk",
                                selectedColor = Color(widgetSettings.customColors.backgroundColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0x80000000L, // Semi-transparent black (50%)
                                    0xFF000000L, // Black
                                    0xFF121212L, // Dark Gray
                                    0xFF1A1A2EL, // Dark Navy
                                    0xFF1B0024L, // Dark Purple
                                    0xFF212529L, // Charcoal
                                    0xFFFFFFFFL  // White
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                backgroundColor = color,
                                                cellBackgroundColor = color,
                                                dayHeaderColor = if ((color and 0xFF000000L) == 0L) 0x00000000L else if (((color shr 24) and 0xFFL) < 255L) ((color and 0x00FFFFFFL) or 0x66000000L) else it.customColors.dayHeaderColor
                                            )
                                        )
                                    }
                                }
                            )

                            ColorPickerRow(
                                label = "Barva textu v rozvrhu (všechny texty)",
                                selectedColor = Color(widgetSettings.customColors.cellTextColor),
                                colorOptions = listOf(
                                    0xFFFFFFFFL, // White
                                    0xFFE6EDF3L, // Light Gray
                                    0xFFFF6584L, // Pink
                                    0xFF00E5FFL, // Cyan
                                    0xFF76FF03L, // Lime
                                    0xFFFFD600L, // Yellow
                                    0xFF000000L  // Black
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                cellTextColor = color,
                                                teacherTextColor = color,
                                                classroomTextColor = color,
                                                groupTextColor = color
                                            )
                                        )
                                    }
                                }
                            )

                            ColorPickerRow(
                                label = "Barva textu suplování",
                                selectedColor = Color(widgetSettings.customColors.substitutionTextColor),
                                colorOptions = listOf(
                                    0xFFFFA726L, // Orange
                                    0xFFFFD600L, // Amber
                                    0xFFFFFFFFL, // White
                                    0xFFFF5252L, // Red
                                    0xFF00E5FFL, // Cyan
                                    0xFFFF4081L, // Pink
                                    0xFF76FF03L  // Lime
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                substitutionTextColor = color,
                                                substitutionColor = color
                                            )
                                        )
                                    }
                                }
                            )

                            ColorPickerRow(
                                label = "Barva pozadí suplování",
                                selectedColor = Color(widgetSettings.customColors.substitutionBackgroundColor),
                                supportAlpha = true,
                                colorOptions = listOf(
                                    0x4DFFA726L, // Tinted orange (default)
                                    0x4DFFD600L, // Tinted amber
                                    0x4DFF5252L, // Tinted red
                                    0x4D00E5FFL, // Tinted cyan
                                    0x4DFF4081L, // Tinted pink
                                    0x00000000L, // Transparent (no background)
                                    0xFF000000L  // Solid black
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(substitutionBackgroundColor = color)
                                        )
                                    }
                                }
                            )
                        }
                        else
                        {
                            // ADVANCED MODE: Granular color pickers
                            // 1. Subject name
                            ColorPickerRow(
                                label = "Barva textu předmětů (v buňkách)",
                                selectedColor = Color(widgetSettings.customColors.cellTextColor),
                                colorOptions = listOf(
                                    0xFFFFFFFFL,
                                    0xFFE6EDF3L,
                                    0xFFFF6584L,
                                    0xFF00E5FFL,
                                    0xFF76FF03L,
                                    0xFFFFD600L,
                                    0xFFFFA726L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(cellTextColor = color))
                                    }
                                }
                            )

                            // 2. Teacher name
                            ColorPickerRow(
                                label = "Barva textu vyučujících (zkratky učitelů)",
                                selectedColor = Color(widgetSettings.customColors.teacherTextColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent / Skrýt
                                    0xFFFFFFFFL,
                                    0xFFE6EDF3L,
                                    0xFF8B949EL,
                                    0xFFFF6584L,
                                    0xFF00E5FFL,
                                    0xFF76FF03L,
                                    0xFFFFD600L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(teacherTextColor = color))
                                    }
                                }
                            )

                            // 3. Classroom number
                            ColorPickerRow(
                                label = "Barva textu učeben (čísla místností)",
                                selectedColor = Color(widgetSettings.customColors.classroomTextColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent / Skrýt
                                    0xFFFFFFFFL,
                                    0xFFE6EDF3L,
                                    0xFF8B949EL,
                                    0xFFFF6584L,
                                    0xFF00E5FFL,
                                    0xFF76FF03L,
                                    0xFFFFD600L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(classroomTextColor = color))
                                    }
                                }
                            )

                            // 4. Group name
                            ColorPickerRow(
                                label = "Barva textu označení skupin (např. 1/2)",
                                selectedColor = Color(widgetSettings.customColors.groupTextColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent / Skrýt
                                    0xFFFFFFFFL,
                                    0xFFE6EDF3L,
                                    0xFF8B949EL,
                                    0xFFFF6584L,
                                    0xFF00E5FFL,
                                    0xFF76FF03L,
                                    0xFFFFD600L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(groupTextColor = color))
                                    }
                                }
                            )

                            // 5. Header text color
                            ColorPickerRow(
                                label = "Barva textu záhlaví dnů a hodin",
                                selectedColor = Color(widgetSettings.customColors.dayHeaderTextColor),
                                colorOptions = listOf(
                                    0xFFFF6584L,
                                    0xFF00E5FFL,
                                    0xFFFFFFFFL,
                                    0xFFE6EDF3L,
                                    0xFFFFD600L,
                                    0xFFFF9100L,
                                    0xFF76FF03L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(dayHeaderTextColor = color))
                                    }
                                }
                            )

                            // 6. Header background color
                            ColorPickerRow(
                                label = "Pozadí lišty záhlaví a dnů",
                                selectedColor = Color(widgetSettings.customColors.dayHeaderColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0xFFFF6584L, // Pink accent
                                    0xFF00E5FFL, // Cyan accent
                                    0xFF18181AL, // Very Dark
                                    0xFF161B22L, // Slate Dark
                                    0xFF0A192FL, // Navy Dark
                                    0xFF252528L, // Muted Gray
                                    0xFF000000L, // Black
                                    0xFFF1F3F5L  // Light Gray
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(dayHeaderColor = color))
                                    }
                                }
                            )

                            // 7. Border & grid color
                            ColorPickerRow(
                                label = "Ohraničení a mřížka rozvrhu",
                                selectedColor = Color(widgetSettings.customColors.borderColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0xFFFF6584L,
                                    0xFFFF1493L,
                                    0xFF00E5FFL,
                                    0xFF76FF03L,
                                    0xFFFFD600L,
                                    0xFFFFFFFFL,
                                    0xFF616161L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(borderColor = color))
                                    }
                                }
                            )

                            // 8. Background color
                            ColorPickerRow(
                                label = "Pozadí celého widgetu",
                                selectedColor = Color(widgetSettings.customColors.backgroundColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0x80000000L, // Semi-transparent black
                                    0xFF000000L,
                                    0xFF121212L,
                                    0xFF1A1A2EL,
                                    0xFF1B0024L,
                                    0xFF212529L,
                                    0xFFFFFFFFL
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(backgroundColor = color))
                                    }
                                }
                            )

                            // 9. Cell background color
                            ColorPickerRow(
                                label = "Pozadí jednotlivých buněk",
                                selectedColor = Color(widgetSettings.customColors.cellBackgroundColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent
                                    0x33FFFFFFL, // Semi-transparent white
                                    0xFF000000L,
                                    0xFF0D1117L,
                                    0xFF121212L,
                                    0xFF1A1A2EL,
                                    0xFFF6F8FAL,
                                    0xFFFFFFFFL
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(customColors = it.customColors.copy(cellBackgroundColor = color))
                                    }
                                }
                            )

                            // 10. Active hour highlight
                            ColorPickerRow(
                                label = "Aktivní hodina (zvýraznění)",
                                selectedColor = Color(widgetSettings.customColors.activeHourBorderColor),
                                colorOptions = listOf(
                                    0x00000000L, // Transparent / Bez zvýraznění
                                    0xFFFF1493L,
                                    0xFF00E5FFL,
                                    0xFF00E676L,
                                    0xFFFF9100L,
                                    0xFF2979FFL,
                                    0xFFE040FBL
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                activeHourBorderColor = color,
                                                activeHourFillColor = if ((color and 0xFF000000L) == 0L) 0x00000000L else ((color and 0x00FFFFFFL) or 0x33000000L)
                                            )
                                        )
                                    }
                                }
                            )

                            // 11. Substitutions text color
                            ColorPickerRow(
                                label = "Barva textu suplování",
                                selectedColor = Color(widgetSettings.customColors.substitutionTextColor),
                                colorOptions = listOf(
                                    0xFFFFA726L,
                                    0xFFFFD600L,
                                    0xFFFFFFFFL,
                                    0xFFFF5252L,
                                    0xFF00E5FFL,
                                    0xFFFF4081L,
                                    0xFF76FF03L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(
                                                substitutionTextColor = color,
                                                substitutionColor = color
                                            )
                                        )
                                    }
                                }
                            )

                            // 12. Substitutions background color
                            ColorPickerRow(
                                label = "Barva pozadí suplování",
                                selectedColor = Color(widgetSettings.customColors.substitutionBackgroundColor),
                                supportAlpha = true,
                                colorOptions = listOf(
                                    0x4DFFA726L,
                                    0x4DFFD600L,
                                    0x4DFF5252L,
                                    0x4D00E5FFL,
                                    0x4DFF4081L,
                                    0x00000000L,
                                    0xFF000000L
                                ),
                                onColorSelected = { color ->
                                    updateWidgetSettings {
                                        it.copy(
                                            customColors = it.customColors.copy(substitutionBackgroundColor = color)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section: Style Import & Export
            SettingsSection(title = "Import a export stylu") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sdílejte svůj styl s ostatními nebo si zálohujte nastavení vzhledu do souboru (.json).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Exportovat", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Importovat", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Section 3: Content Toggles
            SettingsSection(title = "Zobrazení prvků v rozvrhu") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingToggleRow(
                        title = "Filtrovat mé skupiny na velkém rozvrhu",
                        subtitle = "Na velkém týdenním widgetu zobrazit pouze hodiny vaší vybrané skupiny místo všech rozdělených skupin",
                        checked = widgetSettings.filterMyGroups,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(filterMyGroups = checked) }
                        }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = "Barevné pozadí záhlaví (dny a hodiny)",
                        subtitle = "Vyplní záhlaví dnů a hodin plnou barvou motivu (jako v původním růžovém rozvrhu)",
                        checked = widgetSettings.solidHeaderBackground,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(solidHeaderBackground = checked) }
                        }
                    )

                    HorizontalDivider()

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Zobrazení v záhlaví hodin",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Zvolte, co se zobrazí v horním záhlaví každé vyučovací hodiny",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        val currentMode = widgetSettings.getEffectivePeriodHeaderMode()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Settings.PeriodHeaderMode.NUMBER_ONLY to "Jen číslo",
                                Settings.PeriodHeaderMode.TIME_ONLY to "Jen čas",
                                Settings.PeriodHeaderMode.BOTH to "Obojí"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = currentMode == mode,
                                    onClick = {
                                        updateWidgetSettings {
                                            it.copy(
                                                periodHeaderMode = mode,
                                                showTimeRange = (mode == Settings.PeriodHeaderMode.TIME_ONLY)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    SettingToggleRow(
                        title = if (!studentClass.isNullOrBlank()) "Skrýt kód třídy (např. $studentClass)" else "Skrýt kód třídy",
                        subtitle = "Odstraní označení třídy z každého pole rozvrhu pro čistší vzhled",
                        checked = widgetSettings.hideClass,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(hideClass = checked) }
                        }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = "Skrýt zkratky učitelů",
                        subtitle = "Nezobrazovat zkratku vyučujícího v levém horním rohu",
                        checked = widgetSettings.hideTeachers,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(hideTeachers = checked) }
                        }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = if (!studentClass.isNullOrBlank()) "Skrýt lištu s nadpisem (Rozvrh $studentClass)" else "Skrýt lištu s nadpisem (Rozvrh)",
                        subtitle = "Odstraní horní lištu s názvem a rozvrh vyplní celou výšku widgetu",
                        checked = widgetSettings.hideHeader,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(hideHeader = checked) }
                        }
                    )

                    HorizontalDivider()

                    SettingToggleRow(
                        title = "Vypnout černé ohraničení (průhledné pozadí)",
                        subtitle = "Odstraní černý okrajový rámeček a widget se roztáhne přes celou plochu",
                        checked = widgetSettings.hideBorder,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(hideBorder = checked) }
                        }
                    )

                    HorizontalDivider()

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Velikost písma v buňkách",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Upravit velikost písma a škálování obsahu v rozvrhu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0.85f to "Kompaktní",
                                1.0f to "Normální",
                                1.15f to "Větší",
                                1.30f to "Velké"
                            ).forEach { (scale, label) ->
                                val isSelected = kotlin.math.abs(widgetSettings.cellScale - scale) < 0.05f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        updateWidgetSettings { it.copy(cellScale = scale) }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section: Subject Groups Configuration
            SettingsSection(title = "Moje skupiny předmětů") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nastavte si svou skupinu. V malých widgetech a na 1 řádku se pak zobrazí pouze vaše hodiny.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick global selector for 1/2
                    if (hasHalves)
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Skupina pro půlené předměty (1/2, 2/2)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (settings.userProfileGroup != null) {
                                    Text(
                                        text = "(Z profilu: ${settings.userProfileGroup})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val currentHalvesGroup = remember(settings.subjectGroups, allConfigurableSubjects, settings.userProfileGroup) {
                                val halfSubjects = allConfigurableSubjects.filter { it.availableGroups.any { g -> g == "1/2" || g == "2/2" } }
                                val groups = halfSubjects.mapNotNull { settings.subjectGroups[it.subjectShort] }.toSet()
                                if (groups.size == 1) groups.first() else settings.userProfileGroup
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentHalvesGroup == null,
                                    onClick = {
                                        coroutineScope.launch {
                                            val updatedSettings = context.settingsDataStore.updateData { current ->
                                                val newMap = current.subjectGroups.toMutableMap()
                                                allConfigurableSubjects
                                                    .filter { it.availableGroups.any { g -> g == "1/2" || g == "2/2" } }
                                                    .forEach { newMap.remove(it.subjectShort) }
                                                current.copy(
                                                    userProfileGroup = null,
                                                    widgetSettings = current.widgetSettings.copy(userProfileGroup = null),
                                                    perWidgetSettings = current.perWidgetSettings.mapValues { (_, ws) -> ws.copy(userProfileGroup = null) },
                                                    subjectGroups = newMap
                                                )
                                            }
                                            TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                        }
                                    },
                                    label = { Text("Všechny") }
                                )
                                listOf("1/2", "2/2").forEach { grp ->
                                    FilterChip(
                                        selected = currentHalvesGroup == grp,
                                        onClick = {
                                            coroutineScope.launch {
                                                val updatedSettings = context.settingsDataStore.updateData { current ->
                                                    val newMap = current.subjectGroups.toMutableMap()
                                                    allConfigurableSubjects
                                                        .filter { it.availableGroups.any { g -> g == "1/2" || g == "2/2" } }
                                                        .forEach { newMap[it.subjectShort] = grp }
                                                    current.copy(
                                                        userProfileGroup = grp,
                                                        widgetSettings = current.widgetSettings.copy(userProfileGroup = grp),
                                                        perWidgetSettings = current.perWidgetSettings.mapValues { (_, ws) -> ws.copy(userProfileGroup = grp) },
                                                        subjectGroups = newMap
                                                    )
                                                }
                                                TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                            }
                                        },
                                        label = { Text(grp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    // Quick global selector for 1/3 (only shown if 1/3 subjects exist in schedule)
                    if (hasThirds)
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Skupina pro třetinové předměty (1/3, 2/3, 3/3)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val currentThirdsGroup = remember(settings.subjectGroups, allConfigurableSubjects) {
                                val thirdSubjects = allConfigurableSubjects.filter { it.availableGroups.any { g -> g == "1/3" || g == "2/3" || g == "3/3" } }
                                val groups = thirdSubjects.mapNotNull { settings.subjectGroups[it.subjectShort] }.toSet()
                                if (groups.size == 1) groups.first() else null
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentThirdsGroup == null,
                                    onClick = {
                                        coroutineScope.launch {
                                            val updatedSettings = context.settingsDataStore.updateData { current ->
                                                val newMap = current.subjectGroups.toMutableMap()
                                                allConfigurableSubjects
                                                    .filter { it.availableGroups.any { g -> g == "1/3" || g == "2/3" || g == "3/3" } }
                                                    .forEach { newMap.remove(it.subjectShort) }
                                                current.copy(subjectGroups = newMap)
                                            }
                                            TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                        }
                                    },
                                    label = { Text("Všechny") }
                                )
                                listOf("1/3", "2/3", "3/3").forEach { grp ->
                                    FilterChip(
                                        selected = currentThirdsGroup == grp,
                                        onClick = {
                                            coroutineScope.launch {
                                                val updatedSettings = context.settingsDataStore.updateData { current ->
                                                    val newMap = current.subjectGroups.toMutableMap()
                                                    allConfigurableSubjects
                                                        .filter { it.availableGroups.any { g -> g == "1/3" || g == "2/3" || g == "3/3" } }
                                                        .forEach { newMap[it.subjectShort] = grp }
                                                    current.copy(subjectGroups = newMap)
                                                }
                                                TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                            }
                                        },
                                        label = { Text(grp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    // Quick global selector for 1/4 (if exists)
                    if (hasFourths)
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Skupina pro čtvrtinové předměty (1/4, 2/4, 3/4, 4/4)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val currentFourthsGroup = remember(settings.subjectGroups, allConfigurableSubjects) {
                                val fourthSubjects = allConfigurableSubjects.filter { it.availableGroups.any { g -> g.endsWith("/4") } }
                                val groups = fourthSubjects.mapNotNull { settings.subjectGroups[it.subjectShort] }.toSet()
                                if (groups.size == 1) groups.first() else null
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentFourthsGroup == null,
                                    onClick = {
                                        coroutineScope.launch {
                                            val updatedSettings = context.settingsDataStore.updateData { current ->
                                                val newMap = current.subjectGroups.toMutableMap()
                                                allConfigurableSubjects
                                                    .filter { it.availableGroups.any { g -> g.endsWith("/4") } }
                                                    .forEach { newMap.remove(it.subjectShort) }
                                                current.copy(subjectGroups = newMap)
                                            }
                                            TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                        }
                                    },
                                    label = { Text("Všechny") }
                                )
                                listOf("1/4", "2/4", "3/4", "4/4").forEach { grp ->
                                    FilterChip(
                                        selected = currentFourthsGroup == grp,
                                        onClick = {
                                            coroutineScope.launch {
                                                val updatedSettings = context.settingsDataStore.updateData { current ->
                                                    val newMap = current.subjectGroups.toMutableMap()
                                                    allConfigurableSubjects
                                                        .filter { it.availableGroups.any { g -> g.endsWith("/4") } }
                                                        .forEach { newMap[it.subjectShort] = grp }
                                                    current.copy(subjectGroups = newMap)
                                                }
                                                TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                                            }
                                        },
                                        label = { Text(grp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // More button: Podrobné nastavení po předmětech
                    OutlinedButton(
                        onClick = { showAllSubjects = !showAllSubjects },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (showAllSubjects) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (showAllSubjects) "Skrýt seznam jednotlivých předmětů" else "Zobrazit všechny předměty (${allConfigurableSubjects.size})")
                    }

                    if (showAllSubjects)
                    {
                        if (allConfigurableSubjects.isEmpty())
                        {
                            Text(
                                text = "Zatím nebyly z rozvrhu detekovány žádné dělené předměty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        else
                        {
                            allConfigurableSubjects.forEach { info ->
                                val currentGroup = settings.subjectGroups[info.subjectShort]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { subjectToEdit = info }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val label = if (!info.subjectFull.isNullOrBlank()) {
                                            "${info.subjectShort} (${info.subjectFull})"
                                        } else {
                                            info.subjectShort
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val groupText = if (currentGroup != null) {
                                            "Nastavená skupina: $currentGroup"
                                        } else {
                                            "Skupina nevybrána (všechny)"
                                        }
                                        Text(
                                            text = groupText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (currentGroup != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    SuggestionChip(
                                        onClick = { subjectToEdit = info },
                                        label = {
                                            Text(
                                                text = currentGroup ?: "Zvolit",
                                                fontWeight = if (currentGroup != null) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                                HorizontalDivider()
                            }
                        }

                        OutlinedButton(
                            onClick = { showAddSubjectDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Přidat předmět ručně")
                        }
                    }
                }
            }

            // Section 4: Refresh Action
            SettingsSection(title = "Aktualizace") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vynutit obnovení rozvrhu",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val lastRefreshed = widgetSettings.lastRefreshedTimestamp?.let { "Naposledy: $it" } ?: "Zatím neaktualizováno"
                            Text(
                                text = lastRefreshed,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                TimetableWidgetUpdater.triggerManualRefresh(context)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Obnovit")
                        }
                    }
                }
            }

            // Section 5: Testing & Simulation (Debug)
            SettingsSection(title = "Testování a simulace (Debug)") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingToggleRow(
                        title = "Zapnout simulaci vyučovacích hodin",
                        subtitle = "Umožní přepsat aktuální stav rozvrhu a vyzkoušet různé hodiny, přestávky a suplování ve widgetech",
                        checked = widgetSettings.debugSimulationEnabled,
                        onCheckedChange = { checked ->
                            updateWidgetSettings { it.copy(debugSimulationEnabled = checked) }
                        }
                    )

                    if (widgetSettings.debugSimulationEnabled)
                    {
                        HorizontalDivider()

                        // 1. Simulated Day selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Simulovaný den v týdnu",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    null to "Auto (Dnes)",
                                    DayOfWeek.MONDAY.name to "Pondělí",
                                    DayOfWeek.TUESDAY.name to "Úterý",
                                    DayOfWeek.WEDNESDAY.name to "Středa",
                                    DayOfWeek.THURSDAY.name to "Čtvrtek",
                                    DayOfWeek.FRIDAY.name to "Pátek"
                                ).forEach { (dayName, label) ->
                                    FilterChip(
                                        selected = widgetSettings.debugSimulatedDay == dayName,
                                        onClick = {
                                            updateWidgetSettings { it.copy(debugSimulatedDay = dayName) }
                                        },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 2. Period index selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Právě probíhající hodina",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    -1 to "Před školou",
                                    0 to "1. hod",
                                    1 to "2. hod",
                                    2 to "3. hod",
                                    3 to "4. hod",
                                    4 to "5. hod",
                                    5 to "6. hod",
                                    6 to "7. hod",
                                    7 to "8. hod",
                                    -2 to "Konec školy"
                                ).forEach { (idx, label) ->
                                    FilterChip(
                                        selected = widgetSettings.debugPeriodIndex == idx,
                                        onClick = {
                                            updateWidgetSettings { it.copy(debugPeriodIndex = idx) }
                                        },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 3. Break vs Lesson state & minutes remaining
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Stav hodiny a zbývající čas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple(false, 35, "V hodině (35 min)"),
                                    Triple(false, 20, "V hodině (20 min)"),
                                    Triple(false, 5, "Brzké přepnutí (5 min)"),
                                    Triple(true, 5, "Přestávka (5 min)"),
                                    Triple(true, 10, "Velká přestávka (10 min)")
                                ).forEach { (isBreak, mins, label) ->
                                    val isSelected = widgetSettings.debugIsBreak == isBreak && widgetSettings.debugMinutesRemaining == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            updateWidgetSettings {
                                                it.copy(debugIsBreak = isBreak, debugMinutesRemaining = mins)
                                            }
                                        },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 4. Fallback substitution type
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Výchozí typ suplování",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "Normální výuka",
                                    1 to "Odpadá",
                                    2 to "Změna / Suplování",
                                    3 to "Volno"
                                ).forEach { (type, label) ->
                                    FilterChip(
                                        selected = widgetSettings.debugSubstitutionType == type,
                                        onClick = {
                                            updateWidgetSettings { it.copy(debugSubstitutionType = type) }
                                        },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 5. Custom Week Schedule Editor
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vlastní úpravy hodin pro celý týden",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val activeCount = widgetSettings.debugWeekOverrides.size
                                    Text(
                                        text = if (activeCount > 0) "Aktivních vlastních úprav: $activeCount" else "Žádné úpravy (používá se rozvrh ze školy)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (widgetSettings.debugWeekOverrides.isNotEmpty())
                                {
                                    TextButton(
                                        onClick = {
                                            updateWidgetSettings { it.copy(debugWeekOverrides = emptyMap()) }
                                        }
                                    ) {
                                        Text("Resetovat", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Day tabs for Po..Pá
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    DayOfWeek.MONDAY to "Po",
                                    DayOfWeek.TUESDAY to "Út",
                                    DayOfWeek.WEDNESDAY to "St",
                                    DayOfWeek.THURSDAY to "Čt",
                                    DayOfWeek.FRIDAY to "Pá"
                                ).forEach { (day, label) ->
                                    val dayOverridesCount = (0..7).count { pIdx ->
                                        (widgetSettings.debugWeekOverrides["${day.name}_$pIdx"] ?: 0) != 0
                                    }
                                    FilterChip(
                                        selected = selectedEditorDay == day,
                                        onClick = { selectedEditorDay = day },
                                        label = {
                                            Text(
                                                text = if (dayOverridesCount > 0) "$label ($dayOverridesCount)" else label,
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedEditorDay == day) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // List of 8 periods for selected day
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (0..7).forEach { pIdx ->
                                    val key = "${selectedEditorDay.name}_$pIdx"
                                    val currentVal = widgetSettings.debugWeekOverrides[key] ?: 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${pIdx + 1}. hod",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.width(48.dp)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                0 to "Normál",
                                                1 to "Odpadá",
                                                2 to "Změna",
                                                3 to "Volno"
                                            ).forEach { (v, lbl) ->
                                                FilterChip(
                                                    selected = currentVal == v,
                                                    onClick = {
                                                        val updatedMap = widgetSettings.debugWeekOverrides.toMutableMap()
                                                        if (v == 0) {
                                                            updatedMap.remove(key)
                                                        } else {
                                                            updatedMap[key] = v
                                                        }
                                                        updateWidgetSettings { it.copy(debugWeekOverrides = updatedMap) }
                                                    },
                                                    label = {
                                                        Text(lbl, fontSize = 11.sp)
                                                    },
                                                    modifier = Modifier.height(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog)
    {
        WidgetStyleExportDialog(
            widgetSettings = widgetSettings,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog)
    {
        WidgetStyleImportDialog(
            onStyleImported = { imported ->
                updateWidgetSettings {
                    it.copy(
                        theme = imported.theme,
                        solidHeaderBackground = imported.solidHeaderBackground,
                        hideHeader = imported.hideHeader,
                        hideBorder = imported.hideBorder,
                        hideClass = imported.hideClass,
                        hideTeachers = imported.hideTeachers,
                        periodHeaderMode = imported.periodHeaderMode,
                        cellScale = imported.cellScale,
                        customColors = imported.customColors
                    )
                }
            },
            onDismiss = { showImportDialog = false }
        )
    }

    subjectToEdit?.let { info ->
        GroupSelectDialog(
            info = info,
            currentGroup = settings.subjectGroups[info.subjectShort],
            onGroupSelected = { grp ->
                coroutineScope.launch {
                    val updatedSettings = context.settingsDataStore.updateData { current ->
                        val newGroups = current.subjectGroups.toMutableMap()
                        if (grp == null) {
                            newGroups.remove(info.subjectShort)
                        } else {
                            newGroups[info.subjectShort] = grp
                        }
                        current.copy(subjectGroups = newGroups)
                    }
                    TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                }
            },
            onDismiss = { subjectToEdit = null }
        )
    }

    if (showAddSubjectDialog)
    {
        AddSubjectGroupDialog(
            onSubjectAdded = { subject, grp ->
                coroutineScope.launch {
                    val updatedSettings = context.settingsDataStore.updateData { current ->
                        val newGroups = current.subjectGroups.toMutableMap()
                        newGroups[subject] = grp
                        current.copy(subjectGroups = newGroups)
                    }
                    TimetableWidgetUpdater.updateAllWidgets(context, updatedSettings)
                }
            },
            onDismiss = { showAddSubjectDialog = false }
        )
    }
}
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Modifier.checkerboardBackground(
    tileSizeDp: Float = 5f,
    darkColor: Color = Color(0xFF262A30),
    lightColor: Color = Color(0xFF383E47)
): Modifier = this.drawBehind {
    val tileSize = tileSizeDp * density
    val numCols = kotlin.math.ceil(size.width / tileSize).toInt()
    val numRows = kotlin.math.ceil(size.height / tileSize).toInt()
    for (r in 0 until numRows)
    {
        for (c in 0 until numCols)
        {
            val color = if ((r + c) % 2 == 0) lightColor else darkColor
            drawRect(
                color = color,
                topLeft = Offset(c * tileSize, r * tileSize),
                size = Size(
                    tileSize.coerceAtMost(size.width - c * tileSize),
                    tileSize.coerceAtMost(size.height - r * tileSize)
                )
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    selectedColor: Color,
    colorOptions: List<Long>,
    onColorSelected: (Long) -> Unit,
    supportAlpha: Boolean = true
)
{
    var showDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val a = (selectedColor.toArgb() ushr 24) and 0xFF
                val rgb = selectedColor.toArgb() and 0x00FFFFFF
                val hexStr = when {
                    a == 0 -> "Průhledná (#00000000)"
                    a < 255 -> String.format("#%02X%06X (%d%%)", a, rgb, (a * 100) / 255)
                    else -> String.format("#%06X", rgb)
                }
                Text(
                    text = "Klikněte pro libovolnou barvu ($hexStr)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.clickable { showDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCurTransparent = selectedColor.alpha == 0f || (selectedColor.toArgb() and 0xFF000000.toInt()) == 0
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .then(
                                if (isCurTransparent || selectedColor.alpha < 1f) {
                                    Modifier.checkerboardBackground(tileSizeDp = 3f)
                                } else {
                                    Modifier
                                }
                            )
                            .background(selectedColor)
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurTransparent) {
                            Text(
                                text = "Ø",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Výběr barvy",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            colorOptions.forEach { colorVal ->
                val color = Color(colorVal)
                val isSelected = if (supportAlpha) {
                    selectedColor.toArgb() == colorVal.toInt()
                } else {
                    (selectedColor.toArgb() and 0x00FFFFFF) == (colorVal.toInt() and 0x00FFFFFF)
                }
                val isTransparentVal = colorVal == 0x00000000L || (colorVal and 0xFF000000L) == 0L
                val isSemiVal = ((colorVal shr 24) and 0xFFL) < 255L
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (isTransparentVal || isSemiVal) {
                                Modifier.checkerboardBackground(tileSizeDp = 4f)
                            } else {
                                Modifier
                            }
                        )
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorVal) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        if (isTransparentVal) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            val isLight = (((colorVal shr 16) and 0xFF) * 0.299 + ((colorVal shr 8) and 0xFF) * 0.587 + (colorVal and 0xFF) * 0.114) > 128
                            val hasAlpha = ((colorVal shr 24) and 0xFF) > 100
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isLight && hasAlpha) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isTransparentVal) {
                        Text(
                            text = "Ø",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Dedicated button to open picker for any custom color
            OutlinedIconButton(
                onClick = { showDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Vybrat libovolnou barvu",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDialog)
    {
        ColorPickerDialog(
            title = label,
            initialColor = selectedColor,
            supportAlpha = supportAlpha,
            onColorSelected = {
                onColorSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    supportAlpha: Boolean = true
)
{
    val argb = remember(initialColor) { initialColor.toArgb() }
    val initA = remember(argb) { if (supportAlpha) (argb ushr 24) and 0xFF else 255 }
    val initR = remember(argb) { (argb shr 16) and 0xFF }
    val initG = remember(argb) { (argb shr 8) and 0xFF }
    val initB = remember(argb) { argb and 0xFF }

    var a by remember { mutableFloatStateOf(initA.toFloat()) }
    var r by remember { mutableFloatStateOf(initR.toFloat()) }
    var g by remember { mutableFloatStateOf(initG.toFloat()) }
    var b by remember { mutableFloatStateOf(initB.toFloat()) }
    var hexText by remember {
        mutableStateOf(
            if (supportAlpha) String.format("%02X%02X%02X%02X", initA, initR, initG, initB)
            else String.format("%02X%02X%02X", initR, initG, initB)
        )
    }

    val curA = if (supportAlpha) a.roundToInt().coerceIn(0, 255) else 255
    val curR = r.roundToInt().coerceIn(0, 255)
    val curG = g.roundToInt().coerceIn(0, 255)
    val curB = b.roundToInt().coerceIn(0, 255)
    val curColorLong = if (supportAlpha && curA == 0) 0x00000000L else {
        ((curA.toLong() and 0xFFL) shl 24) or
        ((curR.toLong() and 0xFFL) shl 16) or
        ((curG.toLong() and 0xFFL) shl 8) or
        (curB.toLong() and 0xFFL)
    }
    val curColor = Color(curColorLong)

    val quickPalette = remember {
        listOf(
            0x00000000L, 0x80000000L, 0xFFFF1744L, 0xFFFF5252L, 0xFFFF4081L, 0xFFFF6584L, 0xFFE040FBL, 0xFF7C4DFFL,
            0xFF536DFEL, 0xFF2979FFL, 0xFF00E5FFL, 0xFF1DE9B6L, 0xFF00E676L, 0xFF76FF03L, 0xFFFFEA00L, 0xFFFFD600L,
            0xFFFF9100L, 0xFFFF5722L, 0xFFFFFFFFL, 0xFFE6EDF3L, 0xFFB0BEC5L, 0xFF757575L, 0xFF37474FL, 0xFF000000L
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Comparison preview: original vs new
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    val initLight = (initR * 0.299 + initG * 0.587 + initB * 0.114) > 128
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(initialColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Původní",
                            fontSize = 11.sp,
                            color = if (initLight && (!supportAlpha || initA > 100)) Color.Black else Color.White
                        )
                    }

                    val curLight = (curR * 0.299 + curG * 0.587 + curB * 0.114) > 128
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Checkerboard background to make transparency obvious
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .checkerboardBackground(tileSizeDp = 5f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(curColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val newLabel = when {
                                curA == 0 -> "Průhledná (0%)"
                                curA < 255 -> String.format("Nová (%d%% krytí)", (curA * 100) / 255)
                                else -> String.format("Nová (#%02X%02X%02X)", curR, curG, curB)
                            }
                            Text(
                                text = newLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (curLight && curA > 120) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Quick opacity chips
                if (supportAlpha)
                {
                    Text(
                        text = "Rychlá průhlednost / krytí:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1.0f to "100%",
                            0.75f to "75%",
                            0.50f to "50%",
                            0.25f to "25%",
                            0.0f to "Průhledná (0%)"
                        ).forEach { (ratio, chipLabel) ->
                            val targetA = (ratio * 255).roundToInt()
                            val isSel = kotlin.math.abs(curA - targetA) <= 3
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    a = targetA.toFloat()
                                    if (targetA == 0) {
                                        r = 0f
                                        g = 0f
                                        b = 0f
                                        hexText = "00000000"
                                    } else {
                                        hexText = String.format("%02X%02X%02X%02X", targetA, curR, curG, curB)
                                    }
                                },
                                label = { Text(chipLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // HEX input field
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val clean = input.trim().removePrefix("#")
                        if (clean.equals("transparent", ignoreCase = true) || clean.equals("none", ignoreCase = true) || clean.equals("pruhledna", ignoreCase = true) || clean == "0")
                        {
                            if (supportAlpha) a = 0f
                            r = 0f
                            g = 0f
                            b = 0f
                        }
                        else if (clean.length == 8)
                        {
                            clean.toLongOrNull(16)?.let { num ->
                                if (supportAlpha) a = ((num ushr 24) and 0xFF).toFloat()
                                r = ((num shr 16) and 0xFF).toFloat()
                                g = ((num shr 8) and 0xFF).toFloat()
                                b = (num and 0xFF).toFloat()
                            }
                        }
                        else if (clean.length == 4)
                        {
                            val aC = clean[0]
                            val rC = clean[1]
                            val gC = clean[2]
                            val bC = clean[3]
                            "$aC$aC$rC$rC$gC$gC$bC$bC".toLongOrNull(16)?.let { num ->
                                if (supportAlpha) a = ((num ushr 24) and 0xFF).toFloat()
                                r = ((num shr 16) and 0xFF).toFloat()
                                g = ((num shr 8) and 0xFF).toFloat()
                                b = (num and 0xFF).toFloat()
                            }
                        }
                        else if (clean.length == 6)
                        {
                            clean.toLongOrNull(16)?.let { num ->
                                r = ((num shr 16) and 0xFF).toFloat()
                                g = ((num shr 8) and 0xFF).toFloat()
                                b = (num and 0xFF).toFloat()
                            }
                        }
                        else if (clean.length == 3)
                        {
                            val rC = clean[0]
                            val gC = clean[1]
                            val bC = clean[2]
                            "$rC$rC$gC$gC$bC$bC".toLongOrNull(16)?.let { num ->
                                r = ((num shr 16) and 0xFF).toFloat()
                                g = ((num shr 8) and 0xFF).toFloat()
                                b = (num and 0xFF).toFloat()
                            }
                        }
                    },
                    label = { Text(if (supportAlpha) "HEX kód (RRGGBB nebo AARRGGBB)" else "HEX kód") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Alpha & RGB sliders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (supportAlpha)
                    {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Krytí / Průhlednost (A)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            val percent = (curA * 100) / 255
                            Text("$percent% ($curA)", style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = a,
                            onValueChange = {
                                a = it
                                hexText = String.format("%02X%02X%02X%02X", it.roundToInt().coerceIn(0, 255), curR, curG, curB)
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Red
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Červená (R)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$curR", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = r,
                        onValueChange = {
                            r = it
                            hexText = if (supportAlpha) {
                                String.format("%02X%02X%02X%02X", curA, it.roundToInt().coerceIn(0, 255), curG, curB)
                            } else {
                                String.format("%02X%02X%02X", it.roundToInt().coerceIn(0, 255), curG, curB)
                            }
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF5252),
                            activeTrackColor = Color(0xFFFF5252)
                        )
                    )

                    // Green
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Zelená (G)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$curG", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = g,
                        onValueChange = {
                            g = it
                            hexText = if (supportAlpha) {
                                String.format("%02X%02X%02X%02X", curA, curR, it.roundToInt().coerceIn(0, 255), curB)
                            } else {
                                String.format("%02X%02X%02X", curR, it.roundToInt().coerceIn(0, 255), curB)
                            }
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4CAF50),
                            activeTrackColor = Color(0xFF4CAF50)
                        )
                    )

                    // Blue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Modrá (B)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("$curB", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = b,
                        onValueChange = {
                            b = it
                            hexText = if (supportAlpha) {
                                String.format("%02X%02X%02X%02X", curA, curR, curG, it.roundToInt().coerceIn(0, 255))
                            } else {
                                String.format("%02X%02X%02X", curR, curG, it.roundToInt().coerceIn(0, 255))
                            }
                        },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2196F3),
                            activeTrackColor = Color(0xFF2196F3)
                        )
                    )
                }

                // Quick Palette
                Text(
                    text = "Paleta odstínů:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // 24 swatches in 3 rows
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickPalette.chunked(8).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            chunk.forEach { swatchVal ->
                                val swatchColor = Color(swatchVal)
                                val isSwatchTrans = swatchVal == 0x00000000L || (swatchVal and 0xFF000000L) == 0L
                                val isSwatchSemi = ((swatchVal shr 24) and 0xFFL) < 255L
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSwatchTrans || isSwatchSemi) {
                                                Modifier.checkerboardBackground(tileSizeDp = 3.5f)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .background(swatchColor)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                                        .clickable {
                                            if (swatchVal == 0x00000000L) {
                                                a = 0f
                                                r = 0f
                                                g = 0f
                                                b = 0f
                                                hexText = if (supportAlpha) "00000000" else "000000"
                                            } else {
                                                val sA = if (supportAlpha && ((swatchVal shr 24) and 0xFF) != 0xFFL) {
                                                    ((swatchVal shr 24) and 0xFF).toFloat()
                                                } else {
                                                    a
                                                }
                                                val sR = ((swatchVal shr 16) and 0xFF).toFloat()
                                                val sG = ((swatchVal shr 8) and 0xFF).toFloat()
                                                val sB = (swatchVal and 0xFF).toFloat()
                                                a = sA
                                                r = sR
                                                g = sG
                                                b = sB
                                                hexText = if (supportAlpha) {
                                                    String.format("%02X%02X%02X%02X", sA.toInt(), sR.toInt(), sG.toInt(), sB.toInt())
                                                } else {
                                                    String.format("%02X%02X%02X", sR.toInt(), sG.toInt(), sB.toInt())
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSwatchTrans) {
                                        Text(
                                            text = "Ø",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(curColorLong) }
            ) {
                Text("Použít")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

enum class PreviewWidgetMode(val label: String)
{
    WEEK("Týden"),
    TODAY_ROW("1 řádek"),
    DAY("Den"),
    CURRENT_HOUR("Hodina")
}

@Composable
private fun WidgetLivePreviewCard(
    widgetSettings: Settings.WidgetSettings,
    colors: Settings.WidgetColors,
    studentClass: String?,
    cachedTimetableData: TimetableData? = null,
    subjectGroups: Map<String, String> = emptyMap(),
    previewMode: PreviewWidgetMode = PreviewWidgetMode.WEEK,
    modifier: Modifier = Modifier
)
{
    val bgAlpha = (colors.backgroundColor shr 24) and 0xFFL
    val isExplicitlyTransparent = bgAlpha == 0L
    val isSemiTransparent = bgAlpha in 1L..254L
    val isBgTransparent = isExplicitlyTransparent || (widgetSettings.hideBorder && !isSemiTransparent)
    val bgColor = if (isBgTransparent) Color.Transparent else Color(colors.backgroundColor)
    val showBorder = !widgetSettings.hideBorder && (colors.borderColor and 0xFF000000L) != 0L
    val showHeader = !widgetSettings.hideHeader && (previewMode == PreviewWidgetMode.WEEK || previewMode == PreviewWidgetMode.DAY)
    val borderColor = Color(colors.borderColor)
    val cellTextColor = Color(colors.cellTextColor)
    val isSemiOrTransparent = isBgTransparent || isSemiTransparent

    val now = remember { Clock.System.now() }
    val localNow = remember(now) { now.toLocalDateTime(TimeZone.currentSystemDefault()) }
    val (targetDay, targetDate, titlePrefix) = remember(cachedTimetableData, localNow) {
        TimetableWidgetUpdater.calculateTargetDay(cachedTimetableData?.page?.timetable, localNow)
    }
    val effectiveTargetDay = if (widgetSettings.debugSimulationEnabled) {
        TimetableWidgetRenderer.getSimulatedDay(widgetSettings, localNow)
    } else {
        targetDay
    }
    val effectiveTitlePrefix = if (widgetSettings.debugSimulationEnabled) "Simulace" else titlePrefix

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSemiOrTransparent) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E2638), Color(0xFF111726), Color(0xFF1A2333))
                        )
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (showBorder || !isBgTransparent) bgColor else Color.Transparent,
            border = if (showBorder) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
            shadowElevation = if (showBorder && !isBgTransparent) 4.dp else 0.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 6.dp,
                    start = 10.dp,
                    end = 10.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (only for Week and Day when hideHeader is false)
                if (showHeader)
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val titleText = when (previewMode) {
                            PreviewWidgetMode.DAY -> {
                                val dayName = TimetableWidgetRenderer.getShortDayName(effectiveTargetDay)
                                "$effectiveTitlePrefix · $dayName ${targetDate.dayOfMonth}. ${targetDate.monthNumber}."
                            }
                            else -> if (studentClass.isNullOrBlank()) "Rozvrh hodin" else "Rozvrh $studentClass"
                        }
                        Text(
                            text = titleText,
                            color = cellTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val updatedTimeStr = widgetSettings.lastRefreshedTimestamp?.let { "Akt. $it" } ?: "Akt. 12:45"
                            Text(
                                text = updatedTimeStr,
                                color = cellTextColor.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Obnovit",
                                tint = cellTextColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Widget image preview
                BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(300)
                    val context = LocalContext.current

                    val tableHeightDp = when (previewMode) {
                        PreviewWidgetMode.WEEK -> 150.dp
                        PreviewWidgetMode.TODAY_ROW -> 58.dp
                        PreviewWidgetMode.DAY -> 200.dp
                        PreviewWidgetMode.CURRENT_HOUR -> 120.dp
                    }
                    val heightPx = with(density) { tableHeightDp.toPx() }.toInt().coerceAtLeast(80)

                    val previewBitmap = remember(previewMode, widgetSettings, colors, widthPx, heightPx, studentClass, subjectGroups, cachedTimetableData) {
                        when (previewMode) {
                            PreviewWidgetMode.WEEK -> {
                                if (cachedTimetableData != null) {
                                    val activeSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                                        cachedTimetableData.page.timetable.getLessonSpotsForDay(effectiveTargetDay)?.getOrNull(widgetSettings.debugPeriodIndex)
                                    } else {
                                        null
                                    }
                                    TimetableWidgetRenderer.renderWeekTimetableBitmap(
                                        context = context,
                                        timetableData = cachedTimetableData,
                                        settings = widgetSettings,
                                        width = widthPx,
                                        height = heightPx,
                                        activeSpot = activeSpot,
                                        subjectGroups = subjectGroups
                                    )
                                } else {
                                    TimetableWidgetRenderer.renderPreviewBitmap(
                                        context = context,
                                        settings = widgetSettings,
                                        width = widthPx,
                                        height = heightPx,
                                        userClassName = studentClass,
                                        subjectGroups = subjectGroups
                                    )
                                }
                            }
                            PreviewWidgetMode.TODAY_ROW -> {
                                val activeSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                                    cachedTimetableData?.page?.timetable?.getLessonSpotsForDay(effectiveTargetDay)?.getOrNull(widgetSettings.debugPeriodIndex)
                                } else {
                                    null
                                }
                                TimetableWidgetRenderer.renderTodayRowTimetableBitmap(
                                    context = context,
                                    timetableData = cachedTimetableData,
                                    settings = widgetSettings,
                                    targetDay = effectiveTargetDay,
                                    width = widthPx,
                                    height = heightPx,
                                    activeSpot = activeSpot,
                                    subjectGroups = subjectGroups
                                )
                            }
                            PreviewWidgetMode.DAY -> {
                                val activeSpot = if (widgetSettings.debugSimulationEnabled && widgetSettings.debugPeriodIndex >= 0) {
                                    cachedTimetableData?.page?.timetable?.getLessonSpotsForDay(effectiveTargetDay)?.getOrNull(widgetSettings.debugPeriodIndex)
                                } else {
                                    null
                                }
                                TimetableWidgetRenderer.renderDayTimetableBitmap(
                                    context = context,
                                    timetableData = cachedTimetableData,
                                    settings = widgetSettings,
                                    targetDay = effectiveTargetDay,
                                    width = widthPx,
                                    height = heightPx,
                                    activeSpot = activeSpot,
                                    subjectGroups = subjectGroups
                                )
                            }
                            PreviewWidgetMode.CURRENT_HOUR -> {
                                val squareSize = heightPx.coerceAtLeast(180)
                                TimetableWidgetRenderer.renderCurrentHourBitmap(
                                    context = context,
                                    timetableData = cachedTimetableData,
                                    settings = widgetSettings,
                                    now = localNow,
                                    width = squareSize,
                                    height = squareSize,
                                    subjectGroups = subjectGroups,
                                    userClassName = studentClass
                                )
                            }
                        }
                    }

                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Náhled rozvrhu",
                        modifier = if (previewMode == PreviewWidgetMode.CURRENT_HOUR) {
                            Modifier.size(tableHeightDp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .height(tableHeightDp)
                        }
                    )
                }

                // Bottom text (only when top header is hidden and not current hour)
                if (!showHeader && previewMode != PreviewWidgetMode.CURRENT_HOUR && previewMode != PreviewWidgetMode.TODAY_ROW)
                {
                    val lastRefreshed = widgetSettings.lastRefreshedTimestamp?.let { "Aktualizováno: $it" } ?: "Aktualizováno: 12:45"
                    Text(
                        text = lastRefreshed,
                        color = cellTextColor.copy(alpha = 0.75f),
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetStyleExportDialog(
    widgetSettings: Settings.WidgetSettings,
    onDismiss: () -> Unit
)
{
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val jsonText = remember(widgetSettings) {
        WidgetStyleManager.exportToJson(widgetSettings)
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(jsonText.toByteArray())
                }
                Toast.makeText(context, "Styl úspěšně uložen do souboru!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Chyba při ukládání souboru", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Exportovat styl widgetu", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Kód aktuálního stylu widgetu (JSON). Můžete ho zkopírovat, sdílet do zpráv nebo uložit do souboru:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonText))
                            Toast.makeText(context, "Kód stylu byl zkopírován do schránky!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kopírovat", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, jsonText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Sdílet styl widgetu"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sdílet", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        exportFileLauncher.launch("jecna-widget-styl.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Uložit jako soubor (.json)")
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = jsonText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít")
            }
        }
    )
}

@Composable
private fun WidgetStyleImportDialog(
    onStyleImported: (Settings.WidgetSettings) -> Unit,
    onDismiss: () -> Unit
)
{
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputJson by remember { mutableStateOf("") }

    val parsedSettings = remember(inputJson) {
        WidgetStyleManager.parseJson(inputJson)
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (content != null) {
                    inputJson = content
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Chyba při čtení souboru", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Importovat styl widgetu", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Vložte kód stylu (JSON) nebo načtěte soubor:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputJson,
                    onValueChange = { inputJson = it },
                    placeholder = { Text("Sem vložte JSON kód stylu...", fontSize = 12.sp) },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                inputJson = clip
                            } else {
                                Toast.makeText(context, "Schránka je prázdná", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vložit ze schránky", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vybrat soubor", fontSize = 11.sp)
                    }
                }

                // Validation status
                if (inputJson.isNotBlank()) {
                    if (parsedSettings != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Styl rozpoznán v pořádku! Můžete importovat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Neplatný formát stylu (zkontrolujte JSON).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = parsedSettings != null,
                onClick = {
                    if (parsedSettings != null) {
                        onStyleImported(parsedSettings)
                        Toast.makeText(context, "Styl byl úspěšně importován!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Text("Importovat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun GroupSelectDialog(
    info: TimetableWidgetRenderer.SubjectGroupInfo,
    currentGroup: String?,
    onGroupSelected: (String?) -> Unit,
    onDismiss: () -> Unit
)
{
    var customGroupText by remember { mutableStateOf("") }

    val options = remember(info.availableGroups) {
        if (info.availableGroups.isNotEmpty()) {
            info.availableGroups.distinct()
        } else {
            listOf("1/2", "2/2", "1/3", "2/3", "3/3", "1/4", "2/4", "3/4", "4/4")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Skupina pro předmět ${info.subjectShort}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Zvolte skupinu pro předmět ${info.subjectShort}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { grp ->
                        FilterChip(
                            selected = currentGroup == grp,
                            onClick = {
                                onGroupSelected(grp)
                                onDismiss()
                            },
                            label = {
                                Text(
                                    text = "Skupina $grp",
                                    fontWeight = if (currentGroup == grp) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Nebo zadejte vlastní označení:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customGroupText,
                        onValueChange = { customGroupText = it.trim() },
                        placeholder = { Text("např. 1/2 nebo A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = customGroupText.isNotBlank(),
                        onClick = {
                            onGroupSelected(customGroupText)
                            onDismiss()
                        }
                    ) {
                        Text("Uložit")
                    }
                }

                if (currentGroup != null)
                {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            onGroupSelected(null)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zrušit výběr (zobrazit všechny)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít")
            }
        }
    )
}

@Composable
private fun AddSubjectGroupDialog(
    onSubjectAdded: (String, String) -> Unit,
    onDismiss: () -> Unit
)
{
    var subjectText by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("1/2") }
    val commonGroups = listOf("1/2", "2/2", "1/3", "2/3", "3/3", "1/4", "2/4", "3/4", "4/4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Přidat dělený předmět", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Zadejte zkratku předmětu a vyberte svou skupinu:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it.uppercase().trim() },
                    label = { Text("Zkratka předmětu (např. WA, A, CIT, PSS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Skupina:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroup == grp,
                            onClick = { selectedGroup = grp },
                            label = { Text(grp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = subjectText.isNotBlank(),
                onClick = {
                    onSubjectAdded(subjectText, selectedGroup)
                    onDismiss()
                }
            ) {
                Text("Přidat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
