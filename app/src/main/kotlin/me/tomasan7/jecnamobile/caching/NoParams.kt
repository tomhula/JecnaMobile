package me.tomasan7.jecnamobile.caching

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = NoParamsSerializer::class)
data object NoParams

internal object NoParamsSerializer : KSerializer<NoParams>
{
    override val descriptor = PrimitiveSerialDescriptor("NoParams", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NoParams) = encoder.encodeString("NoParams")

    override fun deserialize(decoder: Decoder) = NoParams
}
