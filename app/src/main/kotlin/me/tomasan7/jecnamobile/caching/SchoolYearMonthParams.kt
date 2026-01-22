package me.tomasan7.jecnamobile.caching

import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.schoolYear
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SchoolYearMonthParamsSerializer::class)
data class SchoolYearMonthParams(
    val schoolYear: SchoolYear,
    val month: Month
)

internal object SchoolYearMonthParamsSerializer : KSerializer<SchoolYearMonthParams>
{
    override val descriptor = PrimitiveSerialDescriptor("SchoolYearMonthParams", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SchoolYearMonthParams) = encoder.encodeString(
        "${value.schoolYear.firstCalendarYear}$DIVIDER${value.month.number}"
    )

    override fun deserialize(decoder: Decoder): SchoolYearMonthParams
    {
        val string = decoder.decodeString()
        val split = string.split(DIVIDER, limit = 2)
        return SchoolYearMonthParams(split[0].toInt().schoolYear(), Month(split[1].toInt()))
    }

    private const val DIVIDER = "|"
}
