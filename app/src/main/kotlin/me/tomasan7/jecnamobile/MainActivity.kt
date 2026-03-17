package me.tomasan7.jecnamobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.chrynan.parcelable.core.getParcelableExtra
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.ExperimentalSerializationApi
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.LoginScreen
import me.tomasan7.jecnamobile.mainscreen.MainScreen
import me.tomasan7.jecnamobile.navigation.AppDestination
import me.tomasan7.jecnamobile.settings.isAppInDarkTheme
import me.tomasan7.jecnamobile.ui.theme.JecnaMobileTheme
import me.tomasan7.jecnamobile.util.rememberMutableStateOf
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
import me.tomasan7.jecnamobile.welcome.WelcomeScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity()
{
    @Inject
    lateinit var authRepository: AuthRepository

    @OptIn(ExperimentalSerializationApi::class)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val startDestination = if (authRepository.exists())
            AppState.Main
        else
            AppState.Login

        val navigateTo = intent.getParcelableExtra(
            name = EXTRA_NAVIGATE_TO,
            deserializer = AppDestination.serializer()
        )

        setContent {
            val settings by settingsAsStateAwaitFirst()
            val isAppInDarkTheme = isAppInDarkTheme(settings)
            
            var currentDestination by rememberMutableStateOf(startDestination)
            
            JecnaMobileTheme(isAppInDarkTheme) {
                val backgroundColor = MaterialTheme.colorScheme.background

                NavDisplay(
                    backStack = remember(currentDestination) { listOf(currentDestination) },
                    onBack = {},
                    entryProvider = { key ->
                        when (key)
                        {
                            AppState.Login -> NavEntry(key) { 
                                LoginScreen(onLoginSuccess = { 
                                    currentDestination = if (settings.hasSeenWelcomeScreen) AppState.Main else AppState.Welcome 
                                }) 
                            }
                            AppState.Welcome -> NavEntry(key) {
                                WelcomeScreen(onWelcomeComplete = {
                                    currentDestination = AppState.Main
                                })
                            }
                            AppState.Main -> NavEntry(key) { 
                                MainScreen(
                                    onNavigateToLogin = { currentDestination = AppState.Login },
                                    initialNavigateTo = navigateTo
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().background(backgroundColor)
                )
            }
        }
    }
}

private enum class AppState: NavKey
{
    Login,
    Welcome,
    Main
}

const val EXTRA_NAVIGATE_TO = "navigate_to"
