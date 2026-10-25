package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow

class RoomCapsulaRepository(private val dao: CapsulaDao) : CapsulaRepository {

    override fun capsulas(): Flow<List<Capsula>> = dao.capsulas()

    override suspend fun obtenerPorId(id: String): Capsula? = dao.obtenerPorId(id)
}
