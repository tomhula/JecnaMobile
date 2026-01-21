package me.tomasan7.jecnamobile

import kotlinx.serialization.Serializable

@Serializable(with = NoParamsSerializer::class)
data object NoParams
