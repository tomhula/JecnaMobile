package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.data.student.Locker
import io.github.tomhula.jecnaapi.data.student.Student

interface StudentProfileRepository {
    suspend fun getCurrentStudent(): Student
    suspend fun getLocker(): Locker?
}
