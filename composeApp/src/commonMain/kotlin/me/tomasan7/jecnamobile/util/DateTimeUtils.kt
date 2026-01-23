package me.tomasan7.jecnamobile.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun LocalDate.Companion.now() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
