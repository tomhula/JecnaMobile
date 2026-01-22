package me.tomasan7.jecnamobile.teachers

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.data.schoolStaff.TeachersPage
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import javax.inject.Inject

@HiltViewModel
class TeachersViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val repository: TeachersRepository
) : SubScreenViewModel<TeachersPage>(appContext)
{
    var uiState by mutableStateOf(TeachersState())
        private set

    /*init
    {
        if ((jecnaClient as WebJecnaClient).lastSuccessfulLoginTime != null)
            loadReal()
    }*/

    fun onFilterFieldValueChange(value: String) = changeUiState(filterFieldValue = value)

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        teachersPage: TeachersPage? = uiState.teachersPage,
        filterFieldValue: String = uiState.filterFieldValue,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            teachersPage = teachersPage,
            filterFieldValue = filterFieldValue,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }

    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_teachers)
    override val loadErrorMessage = appContext.getString(R.string.teachers_load_error)

    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)
    override fun setDataUiState(data: TeachersPage) = changeUiState(teachersPage = data)
    override suspend fun fetchRealData(): TeachersPage = repository.getTeachersPage()
}
