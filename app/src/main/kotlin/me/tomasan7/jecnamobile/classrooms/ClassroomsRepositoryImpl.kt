package me.tomasan7.jecnamobile.classrooms

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import javax.inject.Inject

class ClassroomsRepositoryImpl @Inject constructor(
    private val jecnaClient: JecnaClient
) : ClassroomsRepository
{
    override suspend fun getClassroomsPage() = jecnaClient.getClassroomsPage()

    override suspend fun getClassroom(ref: ClassroomReference) = jecnaClient.getClassroom(ref)
}

