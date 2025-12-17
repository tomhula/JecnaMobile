package me.tomasan7.jecnamobile.mainscreen

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.web.AuthenticationException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.student.StudentProfileRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val jecnaClient: JecnaClient,
    private val canteenClient: CanteenClient,
    private val studentRepository: StudentProfileRepository
) : ViewModel() {

    var navigateToLoginEvent: StateEvent by mutableStateOf(consumed)
        private set

    // Only used for welcome text in the drawer
    var currentStudentName: String? by mutableStateOf(null)
        private set
    var currentStudentClassName: String? by mutableStateOf(null)
        private set

    var showLockerDialog: Boolean by mutableStateOf(false)
        private set

    private val connectivityManager =
        getSystemService(appContext, ConnectivityManager::class.java) as ConnectivityManager
    private val networkAvailabilityCallback = NetworkAvailabilityCallback()

    private val loginBroadcastReceiver = createBroadcastReceiver { _, _ ->
        // Once we know login succeeded, (re)load the student
        loadCurrentStudent()
    }

    private val prefs = appContext.getSharedPreferences("main_screen_cache", Context.MODE_PRIVATE)
    private val CACHE_NAME_KEY = "student_cache_name"
    private val CACHE_CLASS_KEY = "student_cache_class"
    private val CACHE_TIME_KEY = "student_cache_time"
    private val CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(168) // 168 hours

    init {
        registerNetworkAvailabilityListener()

        // If we already have a successful login, we can load the student immediately.
        if (jecnaClient.lastSuccessfulLoginTime != null)
            loadCurrentStudent()

        // Otherwise, wait for a successful login broadcast.
        appContext.registerReceiver(
            loginBroadcastReceiver,
            android.content.IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun loadCurrentStudent() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val cachedName = prefs.getString(CACHE_NAME_KEY, null)
                val cachedClassName = prefs.getString(CACHE_CLASS_KEY, null)
                val cachedTime = prefs.getLong(CACHE_TIME_KEY, 0L)

                if (cachedName != null && cachedClassName != null && (now - cachedTime) < CACHE_DURATION_MS) {
                    // Use cached values directly for welcome text
                    currentStudentName = cachedName
                    currentStudentClassName = cachedClassName
                } else {
                    val student = studentRepository.getCurrentStudent()

                    currentStudentName = student.fullName
                    currentStudentClassName = student.className

                    prefs.edit()
                        .putString(CACHE_NAME_KEY, student.fullName)
                        .putString(CACHE_CLASS_KEY, student.className)
                        .putLong(CACHE_TIME_KEY, now)
                        .apply()
                }
            } catch (_: Exception) {
                // Ignore, will just not show welcome text
            }
        }
    }

    fun tryLogin()
    {
        val hasBeenLoggedIn = jecnaClient.lastSuccessfulLoginTime != null
        val auth = jecnaClient.autoLoginAuth ?: authRepository.get()

        if (auth == null)
        {
            navigateToLoginEvent = triggered
            return
        }

        viewModelScope.launch {
            val loginResult = try
            {
                jecnaClient.login(auth).also {
                    if (!it)
                        Toast.makeText(appContext, R.string.login_error_credentials_saved, Toast.LENGTH_SHORT).show()
                }
            }
            /* This method (tryLogin()) should only run, when internet is available, but if it fails anyway,
            * just leave it as is and try again on new broadcast. */
            catch (e: UnresolvedAddressException)
            {
                return@launch
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                Toast.makeText(appContext, R.string.login_error_unknown, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                false
            }

            if (loginResult)
                broadcastSuccessfulLogin(!hasBeenLoggedIn)
        }
    }

    private fun onNetworkAvailable()
    {
        broadcastNetworkAvailable()
        tryLogin()
    }

    private fun broadcastSuccessfulLogin(first: Boolean = false)
    {
        val intent = Intent(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION)
        intent.putExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, first)
        intent.`package` = appContext.packageName
        appContext.sendBroadcast(intent)
    }

    private fun broadcastNetworkAvailable()
    {
        val intent = Intent(JecnaMobileApplication.NETWORK_AVAILABLE_ACTION)
        intent.`package` = appContext.packageName
        appContext.sendBroadcast(intent)
    }

    fun logout()
    {
        runBlocking {
            launch {
                try
                {
                    jecnaClient.logout()
                }
                // Already logged out
                catch (_: AuthenticationException) {}
            }
            launch {
                try
                {
                    canteenClient.logout()
                }
                // Already logged out
                catch (_: AuthenticationException) {}
            }
        }

        authRepository.clear()
    }

    fun onLoginEventConsumed()
    {
        navigateToLoginEvent = consumed
    }

    private fun registerNetworkAvailabilityListener()
    {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkAvailabilityCallback)
    }

    private fun unregisterNetworkAvailabilityListener()
    {
        connectivityManager.unregisterNetworkCallback(networkAvailabilityCallback)
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }

    override fun onCleared() = unregisterNetworkAvailabilityListener()

    inner class NetworkAvailabilityCallback : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: android.net.Network) = onNetworkAvailable()
    }

    fun onLockerClick() {
        showLockerDialog = true
    }

    fun onLockerDialogDismiss() {
        showLockerDialog = false
    }
}
