package com.cookingplastic.triskeledu.data.notificaciones

import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

//Envuelve el PuntoInteresRepository de Room: hace lo mismo y, cuando un punto pasa a Activo (desde el
//interruptor de P07-Admin o al guardar P10), deja el aviso "Nueva cápsula" (RN-24). Lo demás se delega
class PuntosConAvisos(
    private val puntos: PuntoInteresRepository,
    private val generador: GeneradorNotificaciones
) : PuntoInteresRepository by puntos {

    override suspend fun cambiarActivo(id: String, activo: Boolean): Boolean {
        val antes = puntos.obtenerPorId(id).first()
        val cambio = puntos.cambiarActivo(id, activo)
        if (cambio && activo && antes?.activo == false) avisar(antes.copy(activo = true))
        return cambio
    }

    override suspend fun guardar(punto: PuntoInteres) {
        val antes = puntos.obtenerPorId(punto.id).first()
        puntos.guardar(punto)
        if (punto.activo && antes?.activo != true) avisar(punto)
    }

    //el punto ya quedó guardado: si el aviso falla, no se deshace nada
    private suspend fun avisar(punto: PuntoInteres) {
        try {
            generador.nuevaCapsula(punto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //sin aviso esta vez
        }
    }
}
