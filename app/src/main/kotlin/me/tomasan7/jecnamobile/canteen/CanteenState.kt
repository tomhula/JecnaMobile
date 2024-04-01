package me.tomasan7.jecnamobile.canteen

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import me.tomasan7.jecnaapi.data.canteen.DayMenu
import java.time.LocalTime

@Immutable
data class CanteenState(
    val loading: Boolean = false,
    val orderInProcess: Boolean = false,
    val credit: Float? = null,
    val menu: Set<DayMenu> = emptySet(),
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)
{
    val menuSorted = menu/*.filter { it.items.isNotEmpty() }*/.sortedBy { it.day }

    companion object
    {
        /**
         * The time after which you canteen no longer hands out food.
         */
        private val FOOD_HAND_OUT_END_TIME = LocalTime.of(14, 30)
    }
}
