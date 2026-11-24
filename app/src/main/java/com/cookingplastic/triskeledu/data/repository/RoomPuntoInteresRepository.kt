package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.CapsulaDao
import com.cookingplastic.triskeledu.data.local.dao.PuntoInteresDao
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.puedeActivarse
import kotlinx.coroutines.flow.Flow
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository

// Versión con Room: los puntos quedan guardados en SQLite y siguen ahí al cerrar la app.
// Implementa la misma interfaz que la Fake, así que las pantallas no cambian al usarla.
class RoomPuntoInteresRepository(
    private val dao: PuntoInteresDao,
    //para revisar la cápsula del punto antes de activarlo (RN-14)
    private val capsulaDao: CapsulaDao
) : PuntoInteresRepository {

    override fun obtenerActivos(): Flow<List<PuntoInteres>> = dao.obtenerActivos()

    override fun obtenerTodos(): Flow<List<PuntoInteres>> = dao.obtenerTodos()

    override fun obtenerPorId(id: String): Flow<PuntoInteres?> = dao.obtenerPorId(id)

    override suspend fun obtenerPorCodigoQr(codigo: String): PuntoInteres? = dao.obtenerPorCodigoQr(codigo)

    override suspend fun guardar(punto: PuntoInteres) {
        dao.guardar(punto.copy(actualizadoEn = System.currentTimeMillis()))
    }

    override suspend fun eliminar(id: String) {
        dao.eliminar(id)
    }

    override suspend fun cambiarActivo(id: String, activo: Boolean): Boolean {
        val punto = dao.buscarPorId(id) ?: return false
        // RN-14: solo se activa un punto completo con su cápsula publicada; desactivar siempre está permitido
        val capsula = punto.capsulaId?.let { capsulaDao.obtenerPorId(it) }
        if (activo && !punto.puedeActivarse(capsula)) return false
        dao.cambiarActivo(id, activo, System.currentTimeMillis())
        return true
    }
}
