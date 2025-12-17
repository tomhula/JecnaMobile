package me.tomasan7.jecnamobile.student

import io.github.tomhula.jecnaapi.data.student.Student

interface StudentRepository
{
    suspend fun getCurrentStudent(): Student
}

