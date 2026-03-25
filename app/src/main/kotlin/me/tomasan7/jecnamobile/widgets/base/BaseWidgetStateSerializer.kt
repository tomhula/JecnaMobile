package me.tomasan7.jecnamobile.widgets.base

import android.util.Log
import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

abstract class BaseWidgetStateSerializer<T>(
    private val kSerializer: KSerializer<T>,
    private val logTag: String
) : Serializer<T> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    abstract override val defaultValue: T

    override suspend fun readFrom(input: InputStream): T {
        return try {
            json.decodeFromString(kSerializer, input.readBytes().decodeToString())
        } catch (e: Exception) {
            Log.e(logTag, "Error reading state, falling back to default", e)
            defaultValue
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(kSerializer, t).encodeToByteArray())
    }
}
