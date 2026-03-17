package me.tomasan7.jecnamobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.LoginScreen
import me.tomasan7.jecnamobile.mainscreen.MainScreen
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

    override fun onCreate(savedInstanceState: Bundle?)
    {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val startState = if (authRepository.exists())
            AppState.Main
        else
            AppState.Login

        setContent {
            val settings by settingsAsStateAwaitFirst()
            val isAppInDarkTheme = isAppInDarkTheme(settings)
            
            var currentState by rememberMutableStateOf(startState)
            
            JecnaMobileTheme(isAppInDarkTheme) {
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) { targetState ->
                    when (targetState)
                    {
                        AppState.Login -> LoginScreen(
                            onLoginSuccess = { currentState = if (settings.hasSeenWelcomeScreen) AppState.Main else AppState.Welcome }
                        )
                        AppState.Welcome -> WelcomeScreen (
                            onWelcomeComplete = { currentState = AppState.Main }
                        )
                        AppState.Main -> MainScreen(
                            onNavigateToLogin = { currentState = AppState.Login }
                        )
                    }
                }
            }
        }
    }
}

private enum class AppState
{
    Login,
    Welcome,
    Main
}
