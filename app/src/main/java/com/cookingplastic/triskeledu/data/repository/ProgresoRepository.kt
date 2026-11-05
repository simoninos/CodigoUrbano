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

    //crea la fila con +10 puntos (RN-01). Si ya existía, la devuelve sin sumar de nuevo
    suspend fun registrarEscaneo(usuarioId: String, puntoId: String, ubicacion: Ubicacion): ProgresoPunto

    //suma de los puntos de todos los lugares del usuario
    fun totalPuntos(usuarioId: String): Flow<Int>

    //RN-02 y RN-35: guarda la fecha en que vio la cápsula y suma 50. Devuelve true si los sumó ahora;
    //false si ya la había visto o si todavía no escanea el lugar
    suspend fun registrarVistaCapsula(usuarioId: String, puntoId: String): Boolean

    //ACT-03: guarda la selfie (dirección y fecha) y suma 20. Devuelve true si los sumó ahora;
    //false si ya tenía selfie en este lugar o si todavía no lo escanea
    suspend fun registrarSelfie(usuarioId: String, puntoId: String, selfieUri: String): Boolean

    //CRE-05: cuántos usuarios vieron completa la cápsula de este punto (filas con los 50 puntos)
    fun vistasCompletas(puntoId: String): Flow<Int>

    //P07-Admin y RN-30: cuántos usuarios escanearon cada punto (puntoId -> escaneos). Un punto que no
    //aparece nunca fue escaneado y se puede eliminar
    fun escaneosPorPunto(): Flow<Map<String, Int>>

    //SIM-03: filas del usuario que el servidor todavía no confirma (sincronizado = false)
    suspend fun pendientesDe(usuarioId: String): List<ProgresoPunto>

    //SIM-03: el servidor aceptó estas filas
    suspend fun marcarSincronizados(ids: List<String>)

    //EXC-08: el servidor rechazó el escaneo: se quita la fila con todos sus puntos y el lugar vuelve a bloqueado
    suspend fun quitar(id: String)

    //LUG-06: todas las filas de todos los usuarios del teléfono, para el ranking
    fun todas(): Flow<List<ProgresoPunto>>
}