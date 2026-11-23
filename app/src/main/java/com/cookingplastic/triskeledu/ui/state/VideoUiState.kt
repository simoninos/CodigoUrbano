package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula

//RN-08: bloqueado = no se puede adelantar (la primera vez); libre = barra para adelantar y retroceder
enum class ModoReproductor {
    BLOQUEADO,
    LIBRE
}

//Estado de P12 (reproductor de cápsula) y de la vista previa del creador (P18-B)
data class VideoUiState(
    val capsula: Capsula? = null,
    //subtítulo "La Moneda · ¿Qué es la IA?"; null en la vista previa sin punto
    val nombrePunto: String? = null,
    //lo que recibe el reproductor: el clip incluido o la descarga directa de Drive
    val enlace: String? = null,
    val modo: ModoReproductor = ModoReproductor.BLOQUEADO,
    //RN-35: la cápsula no tiene enlace; no hay reproductor, solo el aviso
    val sinEnlace: Boolean = false,
    //P18-B: siempre libre y nunca suma
    val esVistaPrevia: Boolean = false,
    //se guardan aquí para retomar en el mismo punto al girar o volver del segundo plano (RNF-07)
    val posicionMs: Long = 0,
    val duracionMs: Long = 0,
    val reproduciendo: Boolean = true,
    val terminada: Boolean = false,
    //EXC-05 y EXC-11: se cortó internet mientras se veía
    val sinConexion: Boolean = false,
    //cambia en cada "Reintentar" para que la pantalla vuelva a cargar el video
    val intento: Int = 0,
    val error: String? = null,
    //puntos que sumó esta vez (50) o 0 si ya se habían sumado
    val puntosGanados: Int = 0,
    val yaVista: Boolean = false,
    val cargando: Boolean = true
) {
    val puedeAdelantar: Boolean
        get() = modo == ModoReproductor.LIBRE
}
