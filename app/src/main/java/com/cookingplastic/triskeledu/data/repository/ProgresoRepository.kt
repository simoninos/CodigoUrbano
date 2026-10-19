package com.cookingplastic.triskeledu.data.repository
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import kotlinx.coroutines.flow.Flow

//Contrato del progreso: lo usan Escanear, Lugar encontrado y Mis lugares (Catalina), y Mapa y Detalle (Simon)
//Lo que lee datos que cambian devuelve Flow; lo que escribe es suspend
interface ProgresoRepository {
    //todos los lugares que el usuario ya escaneó
    fun progresoDe(usuarioId: String): Flow<List<ProgresoPunto>>

    //el progreso de un lugar; null si el usuario todavía no lo escanea (bloqueado)
    fun progresoDePunto(usuarioId: String, puntoId: String): Flow<ProgresoPunto?>

    //crea la fila con +5 puntos. Si ya existía, la devuelve sin sumar de nuevo
    suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto

    //suma de los puntos de todos los lugares del usuario
    fun totalPuntos(usuarioId: String): Flow<Int>
}