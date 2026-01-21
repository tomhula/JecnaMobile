package me.tomasan7.jecnamobile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module(includes = [AppModuleBindings::class])
@InstallIn(SingletonComponent::class)
internal object AppModule
{
    @Provides
    @Singleton
    /* Long timeout because attendances page takes the server around 11s to respond */
    fun provideJecnaClient() = JecnaClient(autoLogin = true, userAgent = "JM", requestTimeout = 15.seconds)

    @Provides
    @Singleton
    fun provideCanteenClient() = CanteenClient(autoLogin = true, userAgent = "JM")
}
