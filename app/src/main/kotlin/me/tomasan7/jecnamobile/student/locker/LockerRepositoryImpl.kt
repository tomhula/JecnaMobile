package me.tomasan7.jecnamobile.student.locker

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.student.Locker
import javax.inject.Inject

class LockerRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : LockerRepository {
    override suspend fun getLocker(): Locker? = jecnaClient.getLocker()
}
