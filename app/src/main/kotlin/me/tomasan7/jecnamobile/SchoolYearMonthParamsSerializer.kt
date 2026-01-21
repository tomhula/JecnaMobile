package me.tomasan7.jecnamobile

import io.github.tomhula.jecnaapi.util.SchoolYear
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
        return SchoolYearMonthParams(SchoolYear(split[0].toInt()), Month(split[1].toInt()))
    }

    private const val DIVIDER = "|"
}
