package me.tomasan7.jecnamobile.student

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.student.Locker
import io.github.tomhula.jecnaapi.data.student.Student
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import javax.inject.Inject

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val jecnaClient: JecnaClient,
    private val repository: StudentProfileRepository
) : SubScreenViewModel<Student>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_student_profile)
    override val loadErrorMessage = appContext.getString(R.string.profile_load_error)

    var uiState by mutableStateOf(StudentProfileState())
        private set
    
    override suspend fun fetchRealData() = repository.getCurrentStudent()
    override fun setDataUiState(data: Student) = changeUiState(student = data)
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    override fun loadReal()
    {
        super.loadReal()
        loadLocker()
    }

    fun loadLocker()
    {
        if (uiState.lockerLoading || uiState.locker != null)
            return

        changeUiState(lockerLoading = true, lockerError = null)

        viewModelScope.launch {
            try
            {
                val locker = repository.getLocker()
                if (locker == null) {
                    changeUiState(locker = null, lockerError = appContext.getString(R.string.profile_locker_load_error))
                } else {
                    changeUiState(locker = locker, lockerError = null)
                }
            }
            catch (e: Exception)
            {
                changeUiState(lockerError = appContext.getString(R.string.profile_locker_load_error))
                e.printStackTrace()
            }
            finally
            {
                changeUiState(lockerLoading = false)
            }
        }
    }

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        student: Student? = uiState.student,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
        locker: Locker? = uiState.locker,
        lockerLoading: Boolean = uiState.lockerLoading,
        lockerError: String? = uiState.lockerError,
    )
    {
        uiState = StudentProfileState(
            loading = loading,
            student = student,
            snackBarMessageEvent = snackBarMessageEvent,
            locker = locker,
            lockerLoading = lockerLoading,
            lockerError = lockerError,
        )
    }

    fun createImageRequest(path: String): ImageRequest = ImageRequest.Builder(appContext).apply {
        data(WebJecnaClient.getUrlForPath(path))
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        setHeader(HttpHeaders.Cookie, sessionCookie.toHeaderString())
        (jecnaClient as WebJecnaClient).userAgent?.let { setHeader(HttpHeaders.UserAgent, it) }
    }.build()

    private fun getSessionCookieBlocking() = runBlocking { (jecnaClient as WebJecnaClient).getSessionCookie() }

    private fun Cookie.toHeaderString() = "$name=$value"
}
