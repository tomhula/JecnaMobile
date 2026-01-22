package me.tomasan7.jecnamobile.canteen

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import io.github.tomhula.jecnaapi.data.canteen.DayMenu
import io.github.tomhula.jecnaapi.data.canteen.ExchangeItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Immutable
data class CanteenState(
    val loading: Boolean = false,
    val orderInProcess: Boolean = false,
    val credit: Float? = null,
    val menu: Set<DayMenu> = emptySet(),
    val exchange: List<ExchangeDay> = emptyList(),
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val menuSorted = menu/*.filter { it.items.isNotEmpty() }*/.sortedBy { it.day }
}

data class ExchangeDay(
    val day: LocalDate,
    val items: List<ExchangeItem>
)
