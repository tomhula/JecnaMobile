package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.JecnaClient
import javax.inject.Inject

class StudentProfileRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : StudentProfileRepository
{
    override suspend fun getCurrentStudent() = jecnaClient.getStudentProfile()
    override suspend fun getLocker() = jecnaClient.getLocker()
}
