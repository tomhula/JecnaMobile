package me.tomasan7.jecnamobile

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.WebJecnaClient

class JecnaClientLoginStateProvider(private val jecnaClient: JecnaClient) : LoginStateProvider
{
    override val afterFirstLogin: Boolean
        get() = (jecnaClient as WebJecnaClient).autoLoginAuth != null
}
