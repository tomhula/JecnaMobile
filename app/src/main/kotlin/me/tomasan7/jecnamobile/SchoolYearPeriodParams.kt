package me.tomasan7.jecnamobile

import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.schoolYear
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SchoolYearPeriodParamsSerializer::class)
data class SchoolYearPeriodParams(
    val schoolYear: SchoolYear,
    val periodId: Int
)

internal object SchoolYearPeriodParamsSerializer : KSerializer<SchoolYearPeriodParams>
{
    override val descriptor = PrimitiveSerialDescriptor("SchoolYearPeriodParams", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SchoolYearPeriodParams) = encoder.encodeString(
        "${value.schoolYear.firstCalendarYear}$DIVIDER${value.periodId}"
    )

    override fun deserialize(decoder: Decoder): SchoolYearPeriodParams
    {
        val string = decoder.decodeString()
        val split = string.split(DIVIDER, limit = 2)
        return SchoolYearPeriodParams(split[0].toInt().schoolYear(), split[1].toInt())
    }

    private const val DIVIDER = "|"
}
