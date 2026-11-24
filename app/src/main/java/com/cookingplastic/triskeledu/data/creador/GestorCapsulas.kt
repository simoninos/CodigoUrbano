package com.cookingplastic.triskeledu.data.creador

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaCapsula
import com.cookingplastic.triskeledu.data.remoto.mensajeParaUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

//Resultado de una acción del creador: listo, o el motivo por el que no se hizo
sealed interface ResultadoCapsula {
    data object Listo : ResultadoCapsula
    data class Error(val mensaje: String) : ResultadoCapsula
}

//Acciones del creador sobre sus cápsulas (CRE-02 a CRE-08). Revisa las reglas de ReglaCapsula y recién
//entonces cambia la cápsula y su punto. Los ViewModels de P17, P17-B, P18, P18-B y P21 lo comparten
class GestorCapsulas(
    private val capsulaRepository: CapsulaRepository,
    private val puntoInteresRepository: PuntoInteresRepository,
    //RN-24: avisa al administrador cuando un creador deja un punto listo para activar
    private val avisos: GeneradorNotificaciones? = null,
    private val ahora: () -> Long = System::currentTimeMillis
) {

    //CRE-02 y CRE-06: guarda la cápsula nueva como BORRADOR, o los cambios de una existente
    suspend fun guardar(capsula: Capsula): ResultadoCapsula = intentar {
        capsulaRepository.guardar(capsula)
        ResultadoCapsula.Listo
    }

    //CRE-05: el borrador pasa a PUBLICADA y queda libre para asociarlo a un punto
    suspend fun publicar(capsulaId: String): ResultadoCapsula = intentar {
        val capsula = capsulaRepository.obtenerPorId(capsulaId) ?: return@intentar NO_EXISTE
        ReglaCapsula.motivoNoPublicable(capsula)?.let { return@intentar ResultadoCapsula.Error(it) }
        capsulaRepository.guardar(capsula.copy(estado = EstadoCapsula.PUBLICADA, publicadaEn = ahora()))
        ResultadoCapsula.Listo
    }

    //CRE-05 y RN-21: con el punto activo no se puede; con el punto inactivo se desasocia
    //y el punto queda Sin cápsula. La cápsula vuelve a ser borrador
    suspend fun retirar(capsulaId: String): ResultadoCapsula = intentar {
        val capsula = capsulaRepository.obtenerPorId(capsulaId) ?: return@intentar NO_EXISTE
        val punto = ReglaCapsula.puntoDe(capsula, puntoInteresRepository.obtenerTodos().first())
        ReglaCapsula.motivoNoRetirable(capsula, punto)?.let { return@intentar ResultadoCapsula.Error(it) }
        //primero la cápsula: si el servidor no acepta el retiro, el punto conserva su cápsula.
        //El servidor suelta el punto en la misma operación; aquí se refleja en Room
        capsulaRepository.guardar(capsula.copy(estado = EstadoCapsula.BORRADOR, publicadaEn = null))
        punto?.let { puntoInteresRepository.guardar(it.copy(capsulaId = null, activo = false)) }
        ResultadoCapsula.Listo
    }

    //CRE-06: solo borradores que no estén en un punto
    suspend fun eliminar(capsulaId: String): ResultadoCapsula = intentar {
        val capsula = capsulaRepository.obtenerPorId(capsulaId) ?: return@intentar NO_EXISTE
        val punto = ReglaCapsula.puntoDe(capsula, puntoInteresRepository.obtenerTodos().first())
        ReglaCapsula.motivoNoEliminable(capsula, punto)?.let { return@intentar ResultadoCapsula.Error(it) }
        capsulaRepository.eliminar(capsulaId)
        ResultadoCapsula.Listo
    }

    //CRE-08 y RN-28: cápsula propia, publicada y libre, a un punto Sin cápsula. El punto queda Inactivo:
    //activarlo sigue siendo tarea del administrador (RN-14)
    suspend fun asociar(capsulaId: String, puntoId: String, usuario: Usuario): ResultadoCapsula = intentar {
        val capsula = capsulaRepository.obtenerPorId(capsulaId) ?: return@intentar NO_EXISTE
        val puntos = puntoInteresRepository.obtenerTodos().first()
        val punto = puntos.find { it.id == puntoId } ?: return@intentar ResultadoCapsula.Error("Este punto ya no existe")
        val capsulaDelPunto = punto.capsulaId?.let { capsulaRepository.obtenerPorId(it) }
        ReglaCapsula.motivoPuntoNoAsociable(punto, capsulaDelPunto)?.let { return@intentar ResultadoCapsula.Error(it) }
        ReglaCapsula.motivoNoAsociable(capsula, usuario, puntos)?.let { return@intentar ResultadoCapsula.Error(it) }
        val listo = punto.copy(capsulaId = capsula.id, activo = false)
        puntoInteresRepository.guardar(listo)
        if (usuario.rol == RolUsuario.CREADOR) {
            try {
                avisos?.puntoListo(listo, usuario)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                //el punto ya quedó asociado; el aviso no es imprescindible
            }
        }
        ResultadoCapsula.Listo
    }

    //BKD-03: si el servidor rechaza la acción (403, 409) o no responde (EXC-06), la pantalla recibe un Error con
    //el mensaje, igual que cuando falla una regla local, y en el teléfono no cambia nada
    private suspend fun intentar(accion: suspend () -> ResultadoCapsula): ResultadoCapsula = try {
        accion()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResultadoCapsula.Error(e.mensajeParaUsuario("No se pudo completar la acción, intenta de nuevo"))
    }

    private companion object {
        val NO_EXISTE = ResultadoCapsula.Error("Esta cápsula ya no existe")
    }
}
