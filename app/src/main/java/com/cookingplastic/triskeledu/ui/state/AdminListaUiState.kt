package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.PuntoInteres
import java.text.Normalizer

data class AdminListaUiState(
    val puntos: List<PuntoInteres> = emptyList(),
    val busqueda: String = "",
    // Punto que espera la confirmación para eliminarse; null = no hay diálogo abierto (A-10)
    val puntoAEliminar: PuntoInteres? = null,
    val mensaje: String? = null,
    val cargando: Boolean = true
) {
    // A-04: filtra por nombre sin distinguir mayúsculas ni tildes
    val puntosFiltrados: List<PuntoInteres>
        get() {
            val buscado = normalizar(busqueda)
            return if (buscado.isEmpty()) puntos else puntos.filter { normalizar(it.nombre).contains(buscado) }
        }

    // A-03
    val totalActivos: Int
        get() = puntos.count { it.activo }
}

// "Museo Histórico" -> "museo historico": quita tildes y pasa a minúsculas
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
