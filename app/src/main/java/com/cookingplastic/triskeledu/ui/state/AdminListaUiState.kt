package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import java.text.Normalizer

//P07-Admin · Mis lugares del administrador (LUG-05, ADM-01). Reemplaza a la pantalla 9
data class AdminListaUiState(
    val puntos: List<PuntoInteres> = emptyList(),
    // RN-14: cápsula de cada punto por su id, para saber si está publicada
    val capsulas: Map<String, Capsula> = emptyMap(),
    // RN-30: escaneos de cada punto (puntoId -> cantidad); los que no están nunca se escanearon
    val puntosConProgreso: Map<String, Int> = emptyMap(),
    val busqueda: String = "",
    // null = Todos; si no, solo los de ese estado
    val filtro: EstadoAdminPunto? = null,
    // Punto que espera la confirmación para eliminarse; null = no hay diálogo abierto (A-10)
    val puntoAEliminar: PuntoInteres? = null,
    val mensaje: String? = null,
    val cargando: Boolean = true
) {
    // RN-27: Activo, Inactivo o Sin cápsula, calculado desde el punto y su cápsula
    fun estadoDe(punto: PuntoInteres): EstadoAdminPunto =
        EstadoAdminPunto.desde(punto, punto.capsulaId?.let { capsulas[it] })

    fun escaneosDe(punto: PuntoInteres): Int = puntosConProgreso[punto.id] ?: 0

    // A-04: filtra por nombre sin distinguir mayúsculas ni tildes, y por estado
    val puntosFiltrados: List<PuntoInteres>
        get() {
            val buscado = normalizar(busqueda)
            return puntos
                .filter { buscado.isEmpty() || normalizar(it.nombre).contains(buscado) }
                .filter { filtro == null || estadoDe(it) == filtro }
        }

    // cuántos hay de cada estado, para los filtros
    fun cantidad(estado: EstadoAdminPunto): Int = puntos.count { estadoDe(it) == estado }

    // A-03
    val totalActivos: Int
        get() = cantidad(EstadoAdminPunto.ACTIVO)
}

// "Museo Histórico" -> "museo historico": quita tildes y pasa a minúsculas
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
