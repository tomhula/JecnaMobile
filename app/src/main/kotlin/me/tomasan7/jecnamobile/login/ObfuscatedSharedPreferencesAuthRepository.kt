package me.tomasan7.jecnamobile.login

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.web.Auth
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.random.Random
import androidx.core.content.edit

private const val LOG_TAG = "AuthRepository"

class ObfuscationSharedPreferencesAuthRepository @Inject constructor(
    @ApplicationContext
    appContext: Context
) : AuthRepository
{
    private val seedFile = appContext.filesDir.resolve("seed")
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private fun getSeed(): Long?
    {
        if (seedFile.length() <= 0)
            return null

        return DataInputStream(seedFile.inputStream()).use {
            it.readLong()
        }
    }

    private fun createAndGetSeed(): Long
    {
        val seed = Random.nextLong()
        DataOutputStream(seedFile.outputStream()).use {
            it.writeLong(seed)
        }
        Log.d(LOG_TAG, "Created new auth seed")
        return seed
    }

    private fun ByteArray.transform(seed: Long): ByteArray
    {
        val result = ByteArray(this.size)
        val random = Random(seed)

        for (i in this.indices)
        {
            val mask = random.nextInt(256).toByte()
            result[i] = (this[i].toInt() xor mask.toInt()).toByte()
        }
        return result
    }
    
    private fun String.obfuscate(seed: Long): String
    {
        val bytes = this.toByteArray()
        val transformed = bytes.transform(seed)
        val stringBytes = Base64.encodeToByteArray(transformed)
        val string = String(stringBytes, Charsets.UTF_8)
        return string
    }
    
    private fun String.deobfuscate(seed: Long): String
    {
        val bytes = Base64.decode(this)
        val transformed = bytes.transform(seed)
        val string = String(transformed, Charsets.UTF_8)
        return string
    }
        
    private fun isLegacy(): Boolean = seedFile.length() <= 0 && this.exists()

    override fun get(): Auth?
    {
        val username: String
        val password: String
        
        if (isLegacy())
        {
            Log.d(LOG_TAG, "Reading legacy auth")
            username = preferences.getString(USERNAME_LEGACY_KEY, null) ?: return null
            password = preferences.getString(PASSWORD_LEGACY_KEY, null) ?: return null
            Log.d(LOG_TAG, "Clearing legacy auth")
            clear()
            set(Auth(username, password))
        }
        else
        {
            Log.d(LOG_TAG, "Reading auth")
            val seed = getSeed() ?: return null
            username = preferences.getString(USERNAME_KEY, null)?.deobfuscate(seed) ?: return null
            password = preferences.getString(PASSWORD_KEY, null)?.deobfuscate(seed) ?: return null
        }

        return Auth(username, password)
    }

    override fun set(auth: Auth)
    {
        val seed = getSeed() ?: createAndGetSeed()

        Log.d(LOG_TAG, "Writing auth")
        
        preferences.edit {
            putString(USERNAME_KEY, auth.username.obfuscate(seed))
            putString(PASSWORD_KEY, auth.password.obfuscate(seed))
        }
    }

    override fun clear()
    {
        Log.d(LOG_TAG, "Clearing auth")
        preferences.edit {
            clear()
        }
        Log.d(LOG_TAG, "Clearing seed")
        seedFile.delete()
    }

    override fun exists() = (preferences.contains(USERNAME_LEGACY_KEY) && preferences.contains(PASSWORD_LEGACY_KEY)) 
            || (preferences.contains(USERNAME_KEY) && preferences.contains(PASSWORD_KEY))

    companion object
    {
        private const val FILE_NAME = "auth"
        private const val USERNAME_LEGACY_KEY = "username"
        private const val PASSWORD_LEGACY_KEY = "password"
        private const val USERNAME_KEY = "never_gonna"
        private const val PASSWORD_KEY = "give_you_up"
    }
}
