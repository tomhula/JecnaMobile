package me.tomasan7.jecnamobile.canteen

import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.WebCanteenClient
import io.github.tomhula.jecnaapi.data.canteen.DayMenu
import io.github.tomhula.jecnaapi.data.canteen.ExchangeItem
import io.github.tomhula.jecnaapi.data.canteen.MenuItem
import io.github.tomhula.jecnaapi.parser.ParseException
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.now
import me.tomasan7.jecnamobile.util.settingsDataStore
import javax.inject.Inject

@HiltViewModel
class CanteenViewModel @Inject constructor(
    @param:ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val canteenClient: CanteenClient,
) : ViewModel()
{
    var uiState by mutableStateOf(CanteenState())
        private set
    private var loadMenuJob: Job? = null
    private var loginJob: Job? = null
    private var loginInProcess = false
    private val awaitedDays = mutableSetOf<LocalDate>()

    private val networkAvailableBroadcastReceiver = createBroadcastReceiver { _, _ ->
        if (!loginInProcess)
            if ((canteenClient as WebCanteenClient).lastSuccessfulLoginAuth == null)
                loginCanteenClient()
            else if (uiState.menu.size < getDays().size)
                loadMenu()

        showMessage(R.string.back_online)
    }

    init
    {
        loginCanteenClient()
    }

    private fun loginCanteenClient()
    {
        loginInProcess = true
        loginJob = viewModelScope.launch {
            changeUiState(loading = true)

            val auth = authRepository.get()

            if (auth != null)
                try
                {
                    canteenClient.login(auth)
                }
                catch (e: Exception)
                {
                    showMessage(R.string.canteen_login_error)
                    e.printStackTrace()
                }
                finally
                {
                    loginInProcess = false
                }
            else
            {
                loginInProcess = false
                showMessage(R.string.canteen_login_error)
            }

            if (uiState.menu.size < getDays().size)
                loadMenu()
            else
                changeUiState(loading = false)
        }
    }

    fun enteredComposition()
    {
        appContext.registerReceiver(
            networkAvailableBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.NETWORK_AVAILABLE_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun leftComposition()
    {
        loadMenuJob?.cancel()
        appContext.unregisterReceiver(networkAvailableBroadcastReceiver)
    }

    fun orderMenuItem(menuItem: MenuItem, dayMenuDate: LocalDate)
    {
        if (uiState.orderInProcess)
            return

        if (!menuItem.isEnabled)
            return

        changeUiState(orderInProcess = true)

        viewModelScope.launch {
            try
            {
                val newCredit = canteenClient.order(menuItem)

                when (newCredit)
                {
                    null -> showMessage(R.string.error_order)
                    -1f -> changeUiState(loading = false, credit = null)
                    else ->
                    {
                        val newDayMenu = canteenClient.getDayMenu(dayMenuDate)
                        updateMenu(newDayMenu)
                        changeUiState(loading = false, credit = newCredit)
                    }
                }
            }
            catch (e: UnresolvedAddressException)
            {
                e.printStackTrace()
                showMessage(R.string.no_internet_connection)
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_order)
            }

            changeUiState(orderInProcess = false)
        }
    }


    fun orderExchangeItem(item: ExchangeItem)
    {
        if (uiState.orderInProcess)
            return

        changeUiState(orderInProcess = true)

        viewModelScope.launch {
            try
            {
                val newCredit = canteenClient.order(item)
                if (newCredit == null)
                    showMessage(R.string.error_order)
                else
                    loadExchangeData()
            }
            catch (e: UnresolvedAddressException)
            {
                e.printStackTrace()
                showMessage(R.string.no_internet_connection)
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_order)
            }

            changeUiState(orderInProcess = false)
        }
    }

    fun putMenuItemOnExchange(menuItem: MenuItem, dayMenuDate: LocalDate)
    {
        if (uiState.orderInProcess)
            return

        if (!menuItem.isOrdered)
            return

        if (menuItem.isEnabled)
            return

        changeUiState(orderInProcess = true)

        viewModelScope.launch {
            try
            {
                canteenClient.putOnExchange(menuItem)
                val newDayMenu = canteenClient.getDayMenu(dayMenuDate)
                updateMenu(newDayMenu)
                changeUiState(loading = false)
            }
            catch (e: UnresolvedAddressException)
            {
                showMessage(R.string.no_internet_connection)
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_order)
            }

            changeUiState(orderInProcess = false)
        }
    }

    fun loadMenu()
    {
        changeUiState(loading = true)

        loadMenuJob?.cancel()

        viewModelScope.launch {
            loginJob?.join()
            val days = getDays()
            awaitedDays.addAll(days)
            loadMenuJob = canteenClient.getMenuAsync(days)
                .onEach { addDayMenu(it) }
                .catch { e -> showMenuLoadErrorMessage(e, R.string.error_load); e.printStackTrace() }
                .onCompletion {
                    changeUiState(loading = false)
                    awaitedDays.removeAll(days.toSet())
                }
                .launchIn(viewModelScope)

            viewModelScope.launch {
                try
                {
                    val credit = canteenClient.getCredit()
                    changeUiState(credit = credit)
                }
                catch (e: UnresolvedAddressException)
                {
                    showMessage(R.string.no_internet_connection)
                }
                catch (e: ParseException)
                {
                    e.printStackTrace()
                    showMessage(R.string.error_unsupported_credit)
                }
                catch (e: Exception)
                {
                    e.printStackTrace()
                    showMessage(R.string.error_load_credit)
                }
            }
        }
    }

    private suspend fun loadExchangeData() {
        val exchange = canteenClient.getExchange()
            .groupBy { it.day }
            .map { (day, items) -> ExchangeDay(day, items) }
            .sortedBy { it.day }
        changeUiState(exchange = exchange, loading = false)
    }

    fun fetchExchange()
    {
        changeUiState(loading = true)

        viewModelScope.launch {
            loginJob?.join()

            try
            {
                loadExchangeData()
            }
            catch (e: UnresolvedAddressException)
            {
                showMessage(R.string.no_internet_connection)
                changeUiState(loading = false)
            }
            catch (e: ParseException)
            {
                e.printStackTrace()
                showMessage(R.string.error_unsupported_menu)
                changeUiState(loading = false)
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_load)
                changeUiState(loading = false)
            }
        }
    }

    private fun getDays(): List<LocalDate>
    {
        val result = generateSequence(LocalDate.now()) { it.plus(1, DateTimeUnit.DAY) }
            .filterNot { it.isWeekend() }
            .take(DAYS_TO_LOAD_COUNT)
            .toList()

        return result
    }

    fun showMenuLoadErrorMessage(e: Throwable, @StringRes default: Int) = when (e)
    {
        is UnresolvedAddressException -> showMessage(R.string.no_internet_connection)
        is ParseException             -> showMessage(R.string.error_unsupported_menu)
        else                          -> showMessage(default)
    }

    fun showMessage(@StringRes message: Int) =
        changeUiState(snackBarMessageEvent = triggered(appContext.getString(message)))

    fun loadMoreDayMenus(count: Int)
    {
        if (uiState.loading)
            return

        if (uiState.menu.isEmpty())
            return

        val currentLatestDay = uiState.menuSorted.lastOrNull()?.day ?: LocalDate.now()

        val newDays = generateSequence(currentLatestDay.plus(1, DateTimeUnit.DAY)) { it.plus(1, DateTimeUnit.DAY) }
            .filterNot { it.isWeekend() }
            .take(count)
            .toList()

        val newNewDays = newDays
            // Filters any days, that are already loaded.
            .filter { day -> uiState.menu.none { it.day == day } }
            // Filters any days, that are already loading.
            .filter { day -> day !in awaitedDays }

        awaitedDays.addAll(newNewDays)

        loadMenuJob = canteenClient.getMenuAsync(newNewDays)
            .onEach { addDayMenu(it) }
            .catch { e -> showMenuLoadErrorMessage(e, R.string.error_load); e.printStackTrace(); }
            .onCompletion { awaitedDays.removeAll(newNewDays.toSet()) }
            .launchIn(viewModelScope)
    }

    fun LocalDate.isWeekend() = dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun reloadMenu() = loadMenu()

    fun reloadExchange() = fetchExchange()

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    fun onHelpDialogShownAutomatically() = viewModelScope.launch {
        appContext.settingsDataStore.updateData {
            it.copy(
                canteenHelpSeen = true
            )
        }
    }

    private fun updateMenu(newDayMenu: DayMenu)
    {
        val menu = uiState.menu.map { if (it.day == newDayMenu.day) newDayMenu else it }.toMutableSet()
        changeUiState(menu = menu)
    }

    private fun addDayMenu(newDayMenu: DayMenu)
    {
        Log.d("CanteenViewModel", "addDayMenu: $newDayMenu")
        awaitedDays.remove(newDayMenu.day)
        val menu = uiState.menu.toMutableSet()
        menu.removeIf { it.day == newDayMenu.day }
        menu.add(newDayMenu)
        changeUiState(menu = menu)
    }

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        orderInProcess: Boolean = uiState.orderInProcess,
        menu: Set<DayMenu> = uiState.menu,
        exchange: List<ExchangeDay> = uiState.exchange,
        credit: Float? = uiState.credit,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            menu = menu,
            exchange = exchange,
            credit = credit,
            orderInProcess = orderInProcess,
            snackBarMessageEvent = snackBarMessageEvent,
        )
    }

    companion object
    {
        private const val DAYS_TO_LOAD_COUNT = 7
    }
}
