package me.tomasan7.jecnamobile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.tomasan7.jecnaapi.CanteenClient
import me.tomasan7.jecnaapi.JecnaClient
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module(includes = [AppModuleBindings::class])
@InstallIn(SingletonComponent::class)
internal object AppModule
{
    @Provides
    @Singleton
    /* Long timeout because attendances page takes the server around 11s to respond */
    fun provideJecnaClient() = JecnaClient(autoLogin = true, userAgent = "JM", requestTimout = 12.seconds)

    @Provides
    @Singleton
    fun provideCanteenClient() = CanteenClient(autoLogin = true, userAgent = "JM")
}
