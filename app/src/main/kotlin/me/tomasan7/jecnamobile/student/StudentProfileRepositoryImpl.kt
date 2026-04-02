package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.JecnaClient

class StudentProfileRepositoryImpl(
    private val jecnaClient: JecnaClient
) : StudentProfileRepository
{
    override suspend fun getCurrentStudent() = jecnaClient.getStudentProfile()
    override suspend fun getLocker() = jecnaClient.getLocker()
    override suspend fun getCertificates() = jecnaClient.getStudentCertificates()
}
