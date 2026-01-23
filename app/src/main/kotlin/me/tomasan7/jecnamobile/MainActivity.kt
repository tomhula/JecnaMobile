package me.tomasan7.jecnamobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.LoginScreen
import me.tomasan7.jecnamobile.mainscreen.MainScreen
import me.tomasan7.jecnamobile.settings.isAppInDarkTheme
import me.tomasan7.jecnamobile.ui.theme.JecnaMobileTheme
import me.tomasan7.jecnamobile.ui.theme.jecnaThemeDarkColors
import me.tomasan7.jecnamobile.ui.theme.jecnaThemeLightColors
import me.tomasan7.jecnamobile.util.rememberMutableStateOf
import me.tomasan7.jecnamobile.util.settingsAsStateAwaitFirst
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

        val startDestination = if (authRepository.exists())
            AppDestination.Main
        else
            AppDestination.Login

        setContent {
            val settings by settingsAsStateAwaitFirst()
            val isAppInDarkTheme = isAppInDarkTheme(settings)
            
            var currentDestination by rememberMutableStateOf(startDestination)

            val useDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val lightColorScheme = if (useDynamicColors) dynamicLightColorScheme(LocalContext.current) else jecnaThemeLightColors
            val darkColorScheme = if (useDynamicColors) dynamicDarkColorScheme(LocalContext.current) else jecnaThemeDarkColors
            
            JecnaMobileTheme(
                darkTheme = isAppInDarkTheme,
                lightColorScheme = lightColorScheme,
                darkColorScheme = darkColorScheme
            ) {
                val backgroundColor = MaterialTheme.colorScheme.background

                NavDisplay(
                    backStack = remember(currentDestination) { listOf(currentDestination) },
                    onBack = {},
                    entryProvider = { key ->
                        when (key)
                        {
                            AppDestination.Login -> NavEntry(key) { 
                                LoginScreen(onLoginSuccess = { currentDestination = AppDestination.Main }) 
                            }
                            AppDestination.Main -> NavEntry(key) { 
                                MainScreen(onNavigateToLogin = { currentDestination = AppDestination.Login })
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().background(backgroundColor)
                )
            }
        }
    }
}

private enum class AppDestination: NavKey
{
    Login,
    Main
}
