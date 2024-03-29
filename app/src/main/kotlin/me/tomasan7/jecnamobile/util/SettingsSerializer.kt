package me.tomasan7.jecnamobile.util

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tomasan7.jecnamobile.settings.Settings
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<Settings>
{
    override val defaultValue: Settings
        get() = Settings()

    override suspend fun readFrom(input: InputStream): Settings
    {
        return try {
            Json.decodeFromString(input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
        finally
        {
            withContext(Dispatchers.IO) {
                input.close()
            }
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream)
    {
        withContext(Dispatchers.IO) {
            output.use {
                it.write(Json.encodeToString(t).encodeToByteArray())
            }
        }
    }
}
