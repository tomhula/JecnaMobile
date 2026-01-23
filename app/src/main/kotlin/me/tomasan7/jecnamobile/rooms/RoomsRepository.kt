package me.tomasan7.jecnamobile.rooms

import io.github.tomhula.jecnaapi.data.room.Room
import io.github.tomhula.jecnaapi.data.room.RoomReference
import io.github.tomhula.jecnaapi.data.room.RoomsPage

interface RoomsRepository
{
    suspend fun getRoomsPage(): RoomsPage
    suspend fun getRoom(ref: RoomReference): Room
}
