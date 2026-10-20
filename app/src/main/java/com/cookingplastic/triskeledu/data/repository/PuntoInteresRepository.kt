package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.PuntoInteres
import kotlinx.coroutines.flow.Flow

// Contrato de los puntos de interés.
// Las lecturas de datos que cambian devuelven Flow; las escrituras son suspend.
interface PuntoInteresRepository {

    // Solo los puntos activos: los que ve el cliente
    fun obtenerActivos(): Flow<List<PuntoInteres>>

    // Todos los puntos, activos e inactivos: los que ve el administrador
    fun obtenerTodos(): Flow<List<PuntoInteres>>

    // Un punto por su id; emite null si no existe
    fun obtenerPorId(id: String): Flow<PuntoInteres?>

    // Lectura única para el escaneo: el punto cuyo QR tiene ese texto, o null
    suspend fun obtenerPorCodigoQr(codigo: String): PuntoInteres?

    // Crea el punto si su id no existe; si existe, lo reemplaza
    suspend fun guardar(punto: PuntoInteres)

    suspend fun eliminar(id: String)

    // Devuelve false si el id no existe o si se intenta activar un punto incompleto (RN-14)
    suspend fun cambiarActivo(id: String, activo: Boolean): Boolean
}
