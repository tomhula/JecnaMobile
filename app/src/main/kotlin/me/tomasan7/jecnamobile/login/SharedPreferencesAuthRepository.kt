package me.tomasan7.jecnamobile.login

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.web.Auth
import javax.inject.Inject

class SharedPreferencesAuthRepository @Inject constructor(
    @ApplicationContext
    appContext: Context
) : AuthRepository
{
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun get(): Auth?
    {
        val username = preferences.getString(USERNAME_KEY, null) ?: return null
        val password = preferences.getString(PASSWORD_KEY, null) ?: return null

        return Auth(username, password)
    }

    override fun set(auth: Auth)
    {
        with(preferences.edit()) {
            putString(USERNAME_KEY, auth.username)
            putString(PASSWORD_KEY, auth.password)
            apply()
        }
    }

    override fun clear()
    {
        with(preferences.edit()) {
            clear()
            apply()
        }
    }

    override fun exists() = preferences.contains(USERNAME_KEY) && preferences.contains(PASSWORD_KEY)

    companion object
    {
        private const val FILE_NAME = "auth"
        private const val USERNAME_KEY = "username"
        private const val PASSWORD_KEY = "password"
    }
}
