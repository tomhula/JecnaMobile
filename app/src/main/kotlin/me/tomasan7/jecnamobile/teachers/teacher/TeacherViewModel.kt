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
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.data.schoolStaff.Teacher
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.parser.ParseException
import io.ktor.http.HttpHeaders
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.LoginStateProvider
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.SubScreenViewModel
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    @ApplicationContext
    appContext: Context,
    private val jecnaClient: JecnaClient,
    private val repository: TeachersRepository
) : SubScreenViewModel<Teacher>(appContext)
{
    override val parseErrorMessage = appContext.getString(R.string.error_unsupported_teacher)
    override val loadErrorMessage = appContext.getString(R.string.teacher_load_error)
    
    private lateinit var teacherReference: TeacherReference

    var uiState by mutableStateOf(TeacherState())
        private set

    /** Must be called before [enteredComposition]. */
    fun setTeacherReference(teacherReference: TeacherReference)
    {
        this.teacherReference = teacherReference
    }

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    override fun setDataUiState(data: Teacher) = changeUiState(teacher = data)
    override suspend fun fetchRealData() = repository.getTeacher(teacherReference)
    override fun showSnackBarMessage(message: String) = changeUiState(snackBarMessageEvent = triggered(message))
    override fun setLoadingUiState(loading: Boolean) = changeUiState(loading = loading)

    fun createImageRequest(path: String) = ImageRequest.Builder(appContext).apply {
        data(WebJecnaClient.getUrlForPath(path))
        crossfade(true)
        val sessionCookie = getSessionCookieBlocking() ?: return@apply
        setHeader(HttpHeaders.Cookie, sessionCookie.toHeaderString())
        (jecnaClient as WebJecnaClient).userAgent?.let { setHeader(HttpHeaders.UserAgent, it) }
    }.build()

    private fun getSessionCookieBlocking() = runBlocking { (jecnaClient as WebJecnaClient).getSessionCookie() }

    private fun Cookie.toHeaderString() = "$name=$value"

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
