package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.model.Notificacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

//Historial de notificaciones (PER-03): lo que se guardó, aunque se haya descartado del sistema
interface NotificacionRepository {
    //de la más nueva a la más antigua
    fun notificacionesDe(usuarioId: String): Flow<List<Notificacion>>
    //el contador de la campana
    fun sinLeer(usuarioId: String): Flow<Int>
    suspend fun insertar(notificacion: Notificacion)
    suspend fun marcarLeida(id: String)
    suspend fun marcarTodasLeidas(usuarioId: String)
}

class RoomNotificacionRepository(private val dao: NotificacionDao) : NotificacionRepository {
    override fun notificacionesDe(usuarioId: String): Flow<List<Notificacion>> = dao.notificacionesDe(usuarioId)
    override fun sinLeer(usuarioId: String): Flow<Int> = dao.sinLeer(usuarioId)
    override suspend fun insertar(notificacion: Notificacion) = dao.insertar(notificacion)
    override suspend fun marcarLeida(id: String) = dao.marcarLeida(id)
    override suspend fun marcarTodasLeidas(usuarioId: String) = dao.marcarTodasLeidas(usuarioId)
}

//Versión de prueba: en memoria
class FakeNotificacionRepository(iniciales: List<Notificacion> = emptyList()) : NotificacionRepository {

    private val lista = MutableStateFlow(iniciales)

    //para revisar en las pruebas qué se guardó
    val todas: List<Notificacion>
        get() = lista.value

    override fun notificacionesDe(usuarioId: String): Flow<List<Notificacion>> =
        lista.map { todas -> todas.filter { it.usuarioId == usuarioId }.sortedByDescending { it.creadaEn } }

    override fun sinLeer(usuarioId: String): Flow<Int> = lista.map { todas -> todas.count { it.usuarioId == usuarioId && !it.leida } }

    override suspend fun insertar(notificacion: Notificacion) {
        lista.update { todas -> todas.filterNot { it.id == notificacion.id } + notificacion }
    }

    override suspend fun marcarLeida(id: String) {
        lista.update { todas -> todas.map { if (it.id == id) it.copy(leida = true) else it } }
    }

    override suspend fun marcarTodasLeidas(usuarioId: String) {
        lista.update { todas -> todas.map { if (it.usuarioId == usuarioId) it.copy(leida = true) else it } }
    }
}
