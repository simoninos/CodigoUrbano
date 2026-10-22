package com.cookingplastic.triskeledu.ui.state

//Lo que la pantalla Mis lugares necesita para dibujarse
data class MisLugaresUiState(
    val totalPuntos: Int = 0,
    val lugaresEscaneados: Int = 0,
    val totalLugares: Int = 0,
    //lugares que el usuario ya escaneó, ya filtrados y ordenados
    val desbloqueados: List<LugarDesbloqueado> = emptyList(),
    //cuántos faltan por descubrir (no se muestran sus nombres)
    val bloqueados: Int = 0,
    val filtro: FiltroLugares = FiltroLugares.TODOS,
    val orden: OrdenLugares = OrdenLugares.RECIENTES,
    val cargando: Boolean = true,
    val mensajeError: String? = null
)

//Una fila de la lista "Desbloqueados"
data class LugarDesbloqueado(
    val puntoId: String,
    val nombre: String,
    val tipo: String,
    val puntos: Int,
    val maximo: Int,
    val estado: EstadoLugar,
    val escaneadoEn: Long
)

//Lo que se escribe bajo el nombre: "Recién descubierto", "Falta el video" o "Completado"
enum class EstadoLugar(val texto: String) {
    RECIEN_DESCUBIERTO("Recién descubierto"),
    FALTA_VIDEO("Falta el video"),
    COMPLETADO("Completado")
}

enum class FiltroLugares(val texto: String) {
    TODOS("Todos"),
    EN_CURSO("En curso"),
    COMPLETADOS("Completados")
}

enum class OrdenLugares(val texto: String) {
    RECIENTES("Más recientes"),
    NOMBRE("Nombre (A-Z)"),
    PUNTOS("Más puntos")
}