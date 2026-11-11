package com.cookingplastic.triskeledu.domain.repository

import com.cookingplastic.triskeledu.data.model.Capsula
import kotlinx.coroutines.flow.Flow

interface CapsulaRepository {
    fun capsulas(): Flow<List<Capsula>>
    suspend fun obtenerPorId(id: String): Capsula? //lo dejamos con ? Por si ID no existe y así devuelva null

    //CRE-05: la misma cápsula, avisando cada cambio; null si se eliminó
    fun observar(id: String): Flow<Capsula?>

    //CRE-02 y CRE-06: crea la cápsula o reemplaza sus datos
    suspend fun guardar(capsula: Capsula)

    suspend fun eliminar(id: String)
}
