package me.tomasan7.jecnamobile.classrooms

import io.github.tomhula.jecnaapi.data.classroom.Room
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import io.github.tomhula.jecnaapi.data.classroom.ClassroomPage

interface ClassroomsRepository
{
    suspend fun getClassroomsPage(): ClassroomPage
    suspend fun getClassroom(ref: ClassroomReference): Room
}

