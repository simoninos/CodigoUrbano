package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow

interface CapsulaRepository {
    fun capsulas(): Flow<List<Capsula>>
    suspend fun obtenerPorId(id: String): Capsula? //lo dejamos con ? Por si ID no existe y así devuelva null
}
