package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

//BKD-03: el CRUD de puntos del administrador (P09 y P10) contra ms-contenido. Primero escribe en el servidor,
//que vuelve a revisar el rol y las reglas (RN-14, RN-30); solo si acepta, guarda en Room la versión que devolvió.
//Si el servidor rechaza, el error (ErrorBackend) llega al ViewModel con su mensaje y en el teléfono no cambia nada.
//Las lecturas siguen saliendo de Room, que se llena desde el servidor con ActualizadorContenido
//Sin conexión no se edita nada: si el cambio no necesita llamar al servidor (guardar sin cambios), igual se
//revisa que responda (EstadoServidor.exigir), para que el teléfono nunca quede con cambios que el servidor no tiene
class PuntosConBackend(
    private val local: PuntoInteresRepository,
    private val api: BackendApi,
    private val estado: EstadoServidor? = null,
    //ADM-05: el administrador puede quitarle la cápsula a un punto desde P10; el creador no (lo hace al retirarla)
    private val esAdministrador: suspend () -> Boolean = { false }
) : PuntoInteresRepository by local {

    override suspend fun guardar(punto: PuntoInteres) {
        val anterior = local.obtenerPorId(punto.id).first()
        var servidor: PuntoDto? = null
        if (anterior == null) {
            //ADM-08: punto nuevo; el servidor lo crea Inactivo o Sin cápsula con el mismo id del teléfono
            servidor = llamar { api.crearPunto(PuntoEnvioDto.desde(punto)) }
        } else if (cambiaronLosDatos(anterior, punto) || (quitaLaCapsula(anterior, punto) && esAdministrador())) {
            servidor = llamar { api.editarPunto(punto.id, PuntoEnvioDto.desde(punto)) }
        } else if (punto.capsulaId != null && punto.capsulaId != anterior.capsulaId) {
            //CRE-08: el creador solo asocia su cápsula (no puede editar el resto del punto: 403)
            servidor = llamar { api.asociarCapsula(punto.id, AsociarDto(punto.capsulaId)) }
        }
        //el creador quita la cápsula al retirarla: el servidor ya soltó el punto (CapsulasConBackend.retirar)
        if (punto.activo != (servidor?.activo ?: anterior?.activo ?: false)) {
            servidor = llamar { api.cambiarActivo(punto.id, ActivoDto(punto.activo)) }
        }
        if (servidor == null) estado?.exigir()
        local.guardar(servidor?.aPunto() ?: punto)
    }

    private fun quitaLaCapsula(anterior: PuntoInteres, nuevo: PuntoInteres) =
        anterior.capsulaId != null && nuevo.capsulaId == null

    //ADM-09: el servidor responde 409 con lo que falta si el punto no puede activarse (RN-14)
    override suspend fun cambiarActivo(id: String, activo: Boolean): Boolean {
        val servidor = llamar { api.cambiarActivo(id, ActivoDto(activo)) }
        local.guardar(servidor.aPunto())
        return true
    }

    //RN-30: el servidor responde 409 si alguien ya escaneó el punto, aunque este teléfono no lo sepa
    override suspend fun eliminar(id: String) {
        llamarSinCuerpo { api.eliminarPunto(id) }
        local.eliminar(id)
    }

    //nombre, descripción, tipo, ubicación, imagen, enlace o QR: lo que solo edita el administrador
    private fun cambiaronLosDatos(anterior: PuntoInteres, nuevo: PuntoInteres): Boolean =
        anterior.copy(capsulaId = nuevo.capsulaId, activo = nuevo.activo, actualizadoEn = nuevo.actualizadoEn) != nuevo
}

//BKD-03: las cápsulas del creador (P17 a P18-B) contra ms-contenido, con el mismo esquema que PuntosConBackend.
//GestorCapsulas cambia el estado con guardar(); aquí cada cambio de estado se traduce a su endpoint
class CapsulasConBackend(
    private val local: CapsulaRepository,
    private val api: BackendApi,
    private val estado: EstadoServidor? = null
) : CapsulaRepository by local {

    override suspend fun guardar(capsula: Capsula) {
        val anterior = local.obtenerPorId(capsula.id)
        var llamo = true
        if (anterior == null) {
            //CRE-02: el servidor la crea como BORRADOR del creador del token
            llamar { api.crearCapsula(CapsulaEnvioDto.desde(capsula)) }
        } else if (CapsulaEnvioDto.desde(anterior) != CapsulaEnvioDto.desde(capsula)) {
            llamar { api.editarCapsula(capsula.id, CapsulaEnvioDto.desde(capsula)) }
        } else {
            llamo = false
        }
        val estadoServidor = anterior?.estado ?: EstadoCapsula.BORRADOR
        if (capsula.estado != estadoServidor) {
            //CRE-05: publicar, o retirar (el servidor suelta el punto si estaba inactivo, RN-21)
            llamar { if (capsula.estado == EstadoCapsula.PUBLICADA) api.publicarCapsula(capsula.id) else api.retirarCapsula(capsula.id) }
            llamo = true
        }
        //sin conexión no se guarda ni siquiera una cápsula sin cambios
        if (!llamo) estado?.exigir()
        local.guardar(capsula)
    }

    override suspend fun eliminar(id: String) {
        llamarSinCuerpo { api.eliminarCapsula(id) }
        local.eliminar(id)
    }
}

//Lo que se trajo del servidor en una actualización
data class ResumenActualizacion(val capsulas: Int, val puntos: Int)

//Trae las cápsulas y los puntos que el usuario puede ver (según su rol, RN-15) y los deja en Room, que es lo que
//leen las pantallas. Recibe los repositorios de Room directo, no los ConBackend: si no, cada fila volvería al servidor
class ActualizadorContenido(
    private val api: BackendApi,
    private val capsulasLocales: CapsulaRepository,
    private val puntosLocales: PuntoInteresRepository
) {
    suspend fun actualizar(): ResumenActualizacion {
        //primero las cápsulas: los puntos las referencian (clave foránea en Room)
        val capsulas = llamar { api.capsulas() }.count { guardarSinFallar { capsulasLocales.guardar(it.aCapsula()) } }
        val puntos = llamar { api.puntos() }.count { guardarSinFallar { puntosLocales.guardar(it.aPunto()) } }
        return ResumenActualizacion(capsulas, puntos)
    }

    //una fila que no calza con Room (por ejemplo, la cápsula de un creador que este teléfono no conoce) se salta;
    //las demás se guardan igual
    private suspend fun guardarSinFallar(guardar: suspend () -> Unit): Boolean = try {
        guardar()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}
