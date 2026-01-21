package me.tomasan7.jecnamobile

import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.parser.ParseException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import java.net.UnknownHostException

abstract class SubScreenViewModel<T>(
    @param:ApplicationContext
    protected val appContext: Context,
) : ViewModel()
{
    abstract val parseErrorMessage: String
    abstract val loadErrorMessage: String
    
    var loadRealJob: Job? = null
    
    private val loginBroadcastReceiver = createBroadcastReceiver { _, intent ->
        val first = intent.getBooleanExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, false)

        if (loadRealJob == null || loadRealJob!!.isCompleted)
        {
            if (!first)
                showSnackBarMessage(appContext.getString(R.string.back_online))
            loadReal()
        }
    }

    abstract suspend fun fetchRealData(): T

    abstract fun showSnackBarMessage(message: String)

    abstract fun setLoadingUiState(loading: Boolean)

    abstract fun setDataUiState(data: T)

    fun enteredComposition()
    {
        appContext.registerReceiver(
            loginBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )
        loadReal()
    }

    fun leftComposition()
    {
        loadRealJob?.cancel()
        appContext.unregisterReceiver(loginBroadcastReceiver)
    }
    
    fun reload() = if (loadRealJob?.isCompleted ?: true) loadReal() else Unit
    
    fun loadReal()
    {
        loadRealJob?.cancel()

        setLoadingUiState(true)

        loadRealJob = viewModelScope.launch {
            try
            {
                setDataUiState(fetchRealData())
            }
            catch (_: UnresolvedAddressException)
            {
                showSnackBarMessage(appContext.getString(R.string.no_internet_connection))
            }
            catch (_: UnknownHostException)
            {
                showSnackBarMessage(appContext.getString(R.string.no_internet_connection))
            }
            catch (e: ParseException)
            {
                showSnackBarMessage(parseErrorMessage)
                e.printStackTrace()
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                showSnackBarMessage(loadErrorMessage)
                e.printStackTrace()
            }
            finally
            {
                setLoadingUiState(false)
            }
        }
    }
}
