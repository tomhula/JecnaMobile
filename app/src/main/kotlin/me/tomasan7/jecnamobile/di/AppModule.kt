package me.tomasan7.jecnamobile.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.stevekk11.api.SubstitutionClient
import io.github.tomhula.jecnaapi.CanteenClient
import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient
import me.tomasan7.jecnamobile.LoginStateProvider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module(includes = [AppModuleBindings::class, CacheRepositoriesModule::class])
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
    
    @Provides
    @Singleton
    fun provideSubstitutionClient() = SubstitutionClient().apply {
        setEndpointUrl("https://jecnarozvrh.jzitnik.dev/versioned/v1")
    }
    
    @Provides
    @Singleton
    fun provideLoginStateProvider(jecnaClient: JecnaClient) = object : LoginStateProvider {
        override val afterFirstLogin: Boolean
                get() = (jecnaClient as WebJecnaClient).autoLoginAuth != null
    }
}
