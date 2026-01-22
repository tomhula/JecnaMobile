package me.tomasan7.jecnamobile.mainscreen

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import io.github.tomhula.jecnaapi.web.AuthenticationException
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.login.AuthRepository
import javax.inject.Inject

private const val LOG_TAG = "MainScreenViewmodel"

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @param:ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val jecnaClient: JecnaClient,
    private val canteenClient: CanteenClient
) : ViewModel(), DefaultLifecycleObserver
{
    var navigateToLoginEvent: StateEvent by mutableStateOf(consumed)
        private set

    private var lastKnownNetwork: Network? = null
    private val connectivityManager = getSystemService(appContext, ConnectivityManager::class.java) as ConnectivityManager
    private val networkAvailabilityCallback = NetworkAvailabilityCallback()

    override fun onStart(owner: LifecycleOwner)
    {
        super.onStart(owner)
        Log.d(LOG_TAG, "Registering network availability listener")
        registerNetworkAvailabilityListener()
    }

    override fun onStop(owner: LifecycleOwner)
    {
        super.onStop(owner)
        Log.d(LOG_TAG, "Unregistering network availability listener")
        unregisterNetworkAvailabilityListener()
    }

    fun tryLogin()
    {
        val hasBeenLoggedIn = (jecnaClient as WebJecnaClient).lastSuccessfulLoginTime != null
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
    }

    inner class NetworkAvailabilityCallback : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: Network)
        {
            if (network != lastKnownNetwork)
            {
                lastKnownNetwork = network
                onNetworkAvailable()
            }
        }

        override fun onLost(network: Network)
        {
            lastKnownNetwork = null
        }
    }
}
