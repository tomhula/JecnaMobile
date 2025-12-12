package me.tomasan7.jecnamobile.teachers

import io.github.tomhula.jecnaapi.data.schoolStaff.Teacher
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeachersPage

interface TeachersRepository
{
    suspend fun getTeachersPage(): TeachersPage
    suspend fun getTeacher(tag: String): Teacher
    suspend fun getTeacher(ref: TeacherReference): Teacher = getTeacher(ref.tag)
}
