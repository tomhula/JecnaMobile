package me.tomasan7.jecnamobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.DestinationsNavHost
import dagger.hilt.android.AndroidEntryPoint
import me.tomasan7.jecnamobile.destinations.LoginScreenDestination
import me.tomasan7.jecnamobile.destinations.MainScreenDestination
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.settings.isAppInDarkTheme
import me.tomasan7.jecnamobile.ui.theme.JecnaMobileTheme
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
            MainScreenDestination
        else
            LoginScreenDestination

        setContent {
            val settings by settingsAsStateAwaitFirst()
            val isAppInDarkTheme = isAppInDarkTheme(settings)

            JecnaMobileTheme(isAppInDarkTheme) {
                val backgroundColor = MaterialTheme.colorScheme.background

                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    start = startDestination,
                    modifier = Modifier.fillMaxSize().background(backgroundColor)
                )
            }
        }
    }
}
