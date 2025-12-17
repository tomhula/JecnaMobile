package me.tomasan7.jecnamobile.student.locker

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.data.student.Locker
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.R
import javax.inject.Inject

@HiltViewModel
class LockerDialogViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val lockerRepository: LockerRepository
) : ViewModel() {

    var locker: Locker? by mutableStateOf(null)
        private set

    var loading: Boolean by mutableStateOf(false)
        private set

    var error: String? by mutableStateOf(null)
        private set

    fun loadLocker() {
        if (loading || locker != null)
            return

        loading = true
        error = null

        viewModelScope.launch {
            try {
                locker = lockerRepository.getLocker()
                if (locker == null) {
                    error = appContext.getString(R.string.locker_load_error)
                }
            } catch (e: Exception) {
                error = appContext.getString(R.string.locker_load_error)
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }
}
