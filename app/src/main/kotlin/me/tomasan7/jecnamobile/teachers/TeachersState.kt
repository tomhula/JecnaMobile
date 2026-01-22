package me.tomasan7.jecnamobile.teachers

import android.icu.text.Collator
import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.schoolStaff.TeachersPage
import me.tomasan7.jecnamobile.util.removeAccent
import java.util.*

@Immutable
data class TeachersState(
    val loading: Boolean = false,
    val teachersPage: TeachersPage? = null,
    val filterFieldValue: String = "",
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val teacherReferencesSorted = teachersPage?.teachersReferences
        ?.sortedWith(compareBy(Collator.getInstance(Locale.forLanguageTag("cs"))) { it.fullName.surname() })

    val teacherReferencesSortedFiltered = teacherReferencesSorted
        ?.filter { it.fullName.removeAccent().contains(filterFieldValue.removeAccent(), ignoreCase = true) || it.tag.contains(filterFieldValue, ignoreCase = true) }

    companion object
    {
        private const val NAME_CHAR = "[ěščřžýáíéóúůďťňĎŇŤŠČŘŽÝÁÍÉÚŮĚÓa-zA-Z]"
        private val SURNAME_REGEX = Regex("""(?:\w+\. )*$NAME_CHAR+ (?<surname>$NAME_CHAR+).*""")

        private fun String.surname() = SURNAME_REGEX.matchEntire(this)?.groups?.get("surname")?.value ?: this
    }
}
