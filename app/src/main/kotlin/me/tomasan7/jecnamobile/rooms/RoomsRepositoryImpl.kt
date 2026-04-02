package me.tomasan7.jecnamobile.rooms

import io.github.tomhula.jecnaapi.JecnaClient
import io.github.tomhula.jecnaapi.data.room.RoomReference

class RoomsRepositoryImpl(
    private val jecnaClient: JecnaClient
) : RoomsRepository
{
    override suspend fun getRoomsPage() = jecnaClient.getRoomsPage()

    override suspend fun getRoom(ref: RoomReference) = jecnaClient.getRoom(ref)
}
