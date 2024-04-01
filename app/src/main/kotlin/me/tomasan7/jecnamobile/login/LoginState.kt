package me.tomasan7.jecnamobile.login

import androidx.compose.runtime.Immutable

@Immutable
data class LoginState(
    val isLoading: Boolean = false,
    val username: String = "",
    val password: String = "",
    val usernameBlankError: Boolean = false,
    val passwordBlankError: Boolean = false,
    val rememberAuth: Boolean = false,
    val canHitLogin: Boolean = false,
    val loginResult: LoginResult? = null
)

sealed interface LoginResult
{
    object Success : LoginResult
    sealed interface Error : LoginResult
    {
        object InvalidCredentials : Error
        object NoInternetConnection : Error
        object Unknown : Error
    }
}
