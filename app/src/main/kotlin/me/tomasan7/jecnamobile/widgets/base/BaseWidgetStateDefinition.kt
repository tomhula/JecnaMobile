package me.tomasan7.jecnamobile.widgets.base

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import java.io.File

abstract class BaseWidgetStateDefinition<T>(
    private val filePrefix: String,
    private val serializer: Serializer<T>
) : GlanceStateDefinition<T> {

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<T> {
        return DataStoreFactory.create(
            serializer = serializer,
            produceFile = { context.dataStoreFile(filePrefix + fileKey) }
        )
    }

    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(filePrefix + fileKey)
}
