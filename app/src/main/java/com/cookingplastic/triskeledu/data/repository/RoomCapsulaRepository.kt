package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository

class RoomCapsulaRepository(private val dao: CapsulaDao) : CapsulaRepository {

    override fun capsulas(): Flow<List<Capsula>> = dao.capsulas()

    override suspend fun obtenerPorId(id: String): Capsula? = dao.obtenerPorId(id)

    override fun observar(id: String): Flow<Capsula?> = dao.observar(id)

    override suspend fun guardar(capsula: Capsula) = dao.guardar(capsula)

    override suspend fun eliminar(id: String) = dao.eliminar(id)
}
