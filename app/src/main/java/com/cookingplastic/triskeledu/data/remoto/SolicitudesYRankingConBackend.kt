package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.security.HashContrasena
import com.cookingplastic.triskeledu.data.simulacion.ResultadoSolicitud
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.ui.screens.ranking.FilaRanking
import com.cookingplastic.triskeledu.ui.screens.ranking.PeriodoRanking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.UUID

//ADM-10 y RN-29: las solicitudes de creador las responde el administrador contra ms-usuarios, que evita que
//dos administradores respondan la misma (409, EXC-09). La notificación al usuario la sigue dejando la versión
//local (SolicitudesSimuladas), que recibe aquí como "local"
class SolicitudesConBackend(
    private val local: SolicitudesRepository,
    private val dao: UsuarioDao,
    private val api: BackendApi
) : SolicitudesRepository by local {

    //P22 muestra lo de Room; al abrirla se traen del servidor las cuentas pendientes que este teléfono no conoce.
    //Sin conexión se muestran las que ya estaban guardadas
    override fun pendientes(): Flow<List<Usuario>> = local.pendientes().onStart {
        try {
            llamar { api.solicitudesPendientes() }.forEach { dto ->
                val existente = dao.buscarPorId(dto.id)
                if (existente == null) {
                    //cuenta creada en otro teléfono: su contraseña no se conoce, así que la copia local queda con una
                    //clave al azar (no puede iniciar sesión sin conexión en este teléfono, pero sí con el servidor)
                    val azar = withContext(Dispatchers.Default) { HashContrasena.hashear(UUID.randomUUID().toString()) }
                    dao.guardarUsuario(dto.aUsuario(azar.hash, azar.sal))
                } else {
                    dao.guardarUsuario(dto.aUsuario(existente.claveHash, existente.claveSal))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //EXC-06: sin servidor queda la lista local
        }
    }

    override suspend fun aprobar(usuarioId: String, adminId: String) = responder(usuarioId, adminId, aprobar = true)

    override suspend fun rechazar(usuarioId: String, adminId: String) = responder(usuarioId, adminId, aprobar = false)

    private suspend fun responder(usuarioId: String, adminId: String, aprobar: Boolean): ResultadoSolicitud {
        try {
            llamar { if (aprobar) api.aprobarSolicitud(usuarioId) else api.rechazarSolicitud(usuarioId) }
        } catch (e: ErrorBackend) {
            if (e.codigo == 409) return ResultadoSolicitud.YA_RESPONDIDA
            throw e
        }
        //el servidor ya la resolvió: se refleja en Room y se deja la notificación al usuario
        if (aprobar) local.aprobar(usuarioId, adminId) else local.rechazar(usuarioId, adminId)
        return ResultadoSolicitud.RESUELTA
    }
}

//LUG-06 (P20): el ranking de todos los usuarios lo calcula ms-progreso. El teléfono solo conoce a sus usuarios,
//así que sin servidor el RankingViewModel vuelve a calcularlo con lo que hay en Room
class RankingServidor(private val api: BackendApi) {

    suspend fun ranking(periodo: PeriodoRanking, usuarioActualId: String?, cantidad: Int): Pair<List<FilaRanking>, FilaRanking?> {
        val respuesta = llamar { api.ranking(if (periodo == PeriodoRanking.ESTE_MES) "MES" else "SIEMPRE") }
        val primeros = respuesta.filas.take(cantidad).map { it.aFila(usuarioActualId) }
        //"···" y su fila: solo si el usuario conectado no aparece entre los primeros
        val suya = respuesta.miPosicion?.takeIf { mia -> primeros.none { it.usuarioId == mia.usuarioId } }?.aFila(usuarioActualId)
        return primeros to suya
    }

    private fun FilaRankingDto.aFila(actual: String?) = FilaRanking(
        posicion = posicion,
        usuarioId = usuarioId,
        username = username ?: "Usuario",
        puntos = puntos,
        esActual = usuarioId == actual
    )
}
