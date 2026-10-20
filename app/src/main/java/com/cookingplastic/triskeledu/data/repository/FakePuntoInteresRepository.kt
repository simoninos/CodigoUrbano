package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.puedeActivarse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Versión de prueba: guarda los puntos en memoria a partir de DatosPrueba.
// Los cambios se emiten al instante a quien escucha, pero se pierden al cerrar la app.
class FakePuntoInteresRepository(
    puntosIniciales: List<PuntoInteres> = DatosPrueba.puntos
) : PuntoInteresRepository {

    private val puntos = MutableStateFlow(puntosIniciales)

    override fun obtenerActivos(): Flow<List<PuntoInteres>> =
        puntos.map { lista -> lista.filter { it.activo } }

    override fun obtenerTodos(): Flow<List<PuntoInteres>> = puntos

    override fun obtenerPorId(id: String): Flow<PuntoInteres?> =
        puntos.map { lista -> lista.find { it.id == id } }

    override suspend fun obtenerPorCodigoQr(codigo: String): PuntoInteres? =
        puntos.value.find { it.codigoQr == codigo }

    // Se crea siempre una lista nueva: StateFlow solo emite cuando el valor cambia,
    // y una lista modificada por dentro seguiría siendo la misma para él
    override suspend fun guardar(punto: PuntoInteres) {
        val actualizado = punto.copy(actualizadoEn = System.currentTimeMillis())
        puntos.update { lista ->
            if (lista.any { it.id == punto.id }) {
                lista.map { if (it.id == punto.id) actualizado else it }
            } else {
                lista + actualizado
            }
        }
    }

    override suspend fun eliminar(id: String) {
        puntos.update { lista -> lista.filterNot { it.id == id } }
    }

    override suspend fun cambiarActivo(id: String, activo: Boolean): Boolean {
        val punto = puntos.value.find { it.id == id } ?: return false
        if (activo && !punto.puedeActivarse()) return false
        guardar(punto.copy(activo = activo))
        return true
    }
}
