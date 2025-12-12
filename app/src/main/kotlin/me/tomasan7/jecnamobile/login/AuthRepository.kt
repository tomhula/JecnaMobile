package me.tomasan7.jecnamobile.login

import io.github.tomhula.jecnaapi.web.Auth

interface AuthRepository
{
    fun get(): Auth?
    fun set(auth: Auth)
    fun set(username: String, password: String) = set(Auth(username, password))
    fun clear()
    fun exists(): Boolean
}
