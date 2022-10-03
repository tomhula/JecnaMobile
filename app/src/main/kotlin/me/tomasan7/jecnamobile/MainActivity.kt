package me.tomasan7.jecnamobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnaapi.web.JecnaWebClient
import me.tomasan7.jecnamobile.destinations.LoginScreenDestination
import me.tomasan7.jecnamobile.destinations.MainScreenDestination
import me.tomasan7.jecnamobile.destinations.NetworkErrorScreenDestination
import me.tomasan7.jecnamobile.repository.AuthRepository
import me.tomasan7.jecnamobile.ui.theme.JecnaMobileTheme
import me.tomasan7.jecnamobile.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity()
{
    @Inject
    lateinit var jecnaClient: JecnaWebClient

    @Inject
    lateinit var authRepository: AuthRepository

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContent {

            val startRoute = remember {
                if (!authRepository.exists())
                    LoginScreenDestination
                else
                {
                    val loginResult = runBlocking {
                        try
                        {
                            if (jecnaClient.login(authRepository.get()!!))
                            {
                                LoginResult.SUCCESS
                            }
                            else
                                LoginResult.AUTH_ERROR
                        }
                        catch (e: Exception)
                        {
                            LoginResult.NETWORK_ERROR
                        }
                    }

                    when (loginResult)
                    {
                        LoginResult.SUCCESS       -> MainScreenDestination
                        LoginResult.AUTH_ERROR    -> LoginScreenDestination
                        LoginResult.NETWORK_ERROR -> NetworkErrorScreenDestination
                    }
                }
            }

            val settingsState by settingsAsStateAwaitFirst()

            JecnaMobileTheme(isAppInDarkTheme(settingsState)) {

                val backgroundColor = MaterialTheme.colorScheme.background

                /* Copied from https://google.github.io/accompanist/systemuicontroller/ */
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isAppInDarkTheme(settingsState)

                DisposableEffect(systemUiController, useDarkIcons) {
                    // Update all the system bar colors to be transparent, and use
                    // dark icons if we're in light theme
                    systemUiController.setSystemBarsColor(
                        color = backgroundColor,
                        isNavigationBarContrastEnforced = false,
                        darkIcons = useDarkIcons
                    )

                    // setStatusBarColor() and setNavigationBarColor() also exist

                    onDispose {}
                }

                DestinationsNavHost(navGraph = NavGraphs.root,
                                    modifier = Modifier.fillMaxSize().background(backgroundColor),
                                    startRoute = startRoute)
            }
        }
    }

    private enum class LoginResult
    {
        SUCCESS,
        NETWORK_ERROR,
        AUTH_ERROR
    }
}