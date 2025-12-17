package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.JecnaClient
import javax.inject.Inject

class StudentRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : StudentRepository
{
    override suspend fun getCurrentStudent() = jecnaClient.getStudentProfile()
}

