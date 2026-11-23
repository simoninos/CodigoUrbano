package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel

//Lo que se escribe a la derecha de cada nivel en P24
enum class EstadoFilaNivel {
    DESBLOQUEADO,
    NIVEL_ACTUAL,
    //todavía no lo alcanza: muestra "Faltan X pts"
    BLOQUEADO
}

//Una fila de "Niveles y temas": el nivel, desde cuántos puntos se alcanza y el tema que desbloquea
data class FilaNivel(
    val numero: Int,
    val nombre: String,
    val umbral: Int,
    val tema: TemaApp,
    val estado: EstadoFilaNivel,
    val faltanPuntos: Int
)

//P24 · Mi nivel (LVL-01)
data class NivelUiState(
    val nivel: NivelUsuario = ReglaNivel.nivelDesde(0),
    val filas: List<FilaNivel> = emptyList(),
    val nombreUsuario: String = "",
    val cargando: Boolean = true
) {
    //null desde que no hay siguiente nivel con umbral (no pasa: los niveles no tienen tope)
    val faltanParaSiguiente: Int?
        get() = nivel.puntosSiguiente?.let { (it - nivel.puntosTotales).coerceAtLeast(0) }

    //avance entre el umbral del nivel actual y el del siguiente, de 0 a 1
    val avance: Float
        get() = avanceNivel(nivel)
}

//también lo usa la tarjeta del mapa (LVL-04)
fun avanceNivel(nivel: NivelUsuario): Float {
    val siguiente = nivel.puntosSiguiente ?: return 1f
    val tramo = siguiente - nivel.puntosDesde
    if (tramo <= 0) return 1f
    return ((nivel.puntosTotales - nivel.puntosDesde).toFloat() / tramo).coerceIn(0f, 1f)
}
