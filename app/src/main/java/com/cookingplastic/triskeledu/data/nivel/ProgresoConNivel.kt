package com.cookingplastic.triskeledu.data.nivel

import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.estaCompletado
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

//Envuelve el ProgresoRepository de Room: hace lo mismo y, después de cada suma de puntos, revisa el nivel
//y si el lugar quedó completo (80 de 80, PER-04). "by progreso" delega todo lo demás sin cambios.
//Así ninguna pantalla tiene que acordarse de revisarlo
class ProgresoConNivel(
    private val progreso: ProgresoRepository,
    private val controlNivel: ControlNivel,
    //RN-24: "Logro" al completar un lugar; lo entrega GeneradorNotificaciones
    private val alCompletarLugar: suspend (usuarioId: String, puntoId: String) -> Unit = { _, _ -> }
) : ProgresoRepository by progreso {

    override suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto {
        val fila = progreso.registrarEscaneo(usuarioId, puntoId, ubicacion)
        revisarNivel(usuarioId)
        return fila
    }

    override suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String): Boolean {
        val sumo = progreso.registrarVistaCapsula(usuarioId, puntoId)
        if (sumo) despuesDeSumar(usuarioId, puntoId)
        return sumo
    }

    //ACT-03: la selfie también puede subir de nivel (P25) y completar el lugar
    override suspend fun registrarSelfie(usuarioId: String, puntoId: String, selfieUri: String): Boolean {
        val sumo = progreso.registrarSelfie(usuarioId, puntoId, selfieUri)
        if (sumo) despuesDeSumar(usuarioId, puntoId)
        return sumo
    }

    private suspend fun despuesDeSumar(usuarioId: String, puntoId: String) {
        revisarNivel(usuarioId)
        try {
            if (progreso.progresoDePunto(usuarioId, puntoId).first()?.estaCompletado() == true) alCompletarLugar(usuarioId, puntoId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //el aviso no es imprescindible: los puntos ya quedaron guardados
        }
    }

    //los puntos ya quedaron guardados: si falla la revisión del nivel, el escaneo o la vista no se pierden
    private suspend fun revisarNivel(usuarioId: String) {
        try {
            controlNivel.revisar(usuarioId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //se vuelve a revisar con la próxima suma de puntos
        }
    }
}
