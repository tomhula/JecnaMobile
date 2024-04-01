package me.tomasan7.jecnamobile.teachers.teacher

import android.content.Context
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.http.Cookie
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.data.schoolStaff.Teacher
import me.tomasan7.jecnaapi.data.schoolStaff.TeacherReference
import me.tomasan7.jecnaapi.parser.ParseException
import me.tomasan7.jecnaapi.web.jecna.JecnaWebClient
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import okhttp3.Headers
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val jecnaClient: JecnaClient,
    private val repository: TeachersRepository
) : ViewModel()
{
    private lateinit var teacherReference: TeacherReference

    var uiState by mutableStateOf(TeacherState())
        private set

    private var loadTeacherJob: Job? = null

    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadTeacherJob == null || loadTeacherJob!!.isCompleted)
        {
            if (!first)
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.back_online)))
            loadReal()
        }
    }

    fun enteredComposition(teacherReference: TeacherReference)
    {
        this.teacherReference = teacherReference

        if (this::teacherReference.isInitialized && jecnaClient.autoLoginAuth != null)
            loadReal()

        appContext.registerReceiver(
            loginBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun leftComposition()
    {
        loadTeacherJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data(JecnaWebClient.getUrlForPath(path))
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        headers(Headers.headersOf("Cookie", sessionCookie.toHeaderString()))
    }.build()

    private fun getSessionCookieBlocking() = runBlocking { jecnaClient.getSessionCookie() }

    private fun Cookie.toHeaderString() = "$name=$value"

    private fun loadReal()
    {
        loadTeacherJob?.cancel()

        changeUiState(loading = true)

        loadTeacherJob = viewModelScope.launch {
            try
            {
                changeUiState(teacher = repository.getTeacher(teacherReference))
            }
            catch (e: UnresolvedAddressException)
            {
                changeUiState(snackBarMessageEvent = triggered(getOfflineMessage()))
            }
            catch (e: ParseException)
            {
                changeUiState(
                    snackBarMessageEvent = triggered(appContext.getString(R.string.error_unsupported_teacher))
                )
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                changeUiState(snackBarMessageEvent = triggered(appContext.getString(R.string.teacher_load_error)))
                e.printStackTrace()
            }
            finally
            {
                changeUiState(loading = false)
            }
        }
    }

    private fun getOfflineMessage() = appContext.getString(R.string.no_internet_connection)

    fun reload() = if (!uiState.loading) loadReal() else Unit

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    fun changeUiState(
        loading: Boolean = uiState.loading,
        teacher: Teacher? = uiState.teacher,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent
    )
    {
        uiState = TeacherState(
            loading = loading,
            teacher = teacher,
            snackBarMessageEvent = snackBarMessageEvent
        )
    }
}
