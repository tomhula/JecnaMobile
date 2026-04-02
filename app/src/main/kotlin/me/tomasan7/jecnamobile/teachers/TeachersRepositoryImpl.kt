package me.tomasan7.jecnamobile.teachers

import io.github.tomhula.jecnaapi.JecnaClient

class TeachersRepositoryImpl(
    private val jecnaClient: JecnaClient
) : TeachersRepository
{
    override suspend fun getTeachersPage() = jecnaClient.getTeachersPage()

    override suspend fun getTeacher(tag: String) = jecnaClient.getTeacher(tag)
}
