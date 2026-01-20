package me.tomasan7.jecnamobile.util

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class CachedData<T>(
    val data: T,
    val timestamp: Instant = Clock.System.now()
)
