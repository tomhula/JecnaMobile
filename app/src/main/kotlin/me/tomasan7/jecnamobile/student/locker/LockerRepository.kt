package me.tomasan7.jecnamobile.student.locker

import io.github.tomhula.jecnaapi.data.student.Locker

interface LockerRepository {
    suspend fun getLocker(): Locker?
}
