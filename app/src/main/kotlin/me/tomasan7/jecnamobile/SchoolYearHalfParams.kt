package me.tomasan7.jecnamobile

import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import kotlinx.datetime.Month
import kotlinx.datetime.number
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SchoolYearHalfParamsSerializer::class)
data class SchoolYearHalfParams(
    val schoolYear: SchoolYear,
    val schoolYearHalf: SchoolYearHalf
)

internal object SchoolYearHalfParamsSerializer : KSerializer<SchoolYearHalfParams>
{
    override val descriptor = PrimitiveSerialDescriptor("SchoolYearHalfParams", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SchoolYearHalfParams) = encoder.encodeString(
        "${value.schoolYear.firstCalendarYear}$DIVIDER${value.schoolYearHalf}"
    )

    override fun deserialize(decoder: Decoder): SchoolYearHalfParams
    {
        val string = decoder.decodeString()
        val split = string.split(DIVIDER, limit = 2)
        return SchoolYearHalfParams(SchoolYear(split[0].toInt()), SchoolYearHalf.valueOf(split[1]))
    }

    private const val DIVIDER = "|"
}
