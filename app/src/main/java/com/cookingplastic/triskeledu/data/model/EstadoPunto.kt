package com.cookingplastic.triskeledu.data.model

// Estado de un punto de interés para el usuario (RF-C05). No se guarda: se calcula desde su progreso.
// - BLOQUEADO: el usuario no ha escaneado el QR del punto.
// - DESBLOQUEADO: lo escaneó, pero le quedan actividades.
// - COMPLETADO: obtuvo todos los puntos posibles del lugar.
enum class EstadoPunto(val etiqueta: String) {
    BLOQUEADO("Bloqueado"),
    DESBLOQUEADO("Desbloqueado"),
    COMPLETADO("Completado");

    companion object {
        // Estado a partir de la fila de progreso del usuario en ese punto; null = aún no lo escanea.
        // Mapa y Detalle lo calculan igual que Mis lugares, con estaCompletado() de ProgresoPunto.
        fun desde(progreso: ProgresoPunto?): EstadoPunto = when {
            progreso == null -> BLOQUEADO
            progreso.estaCompletado() -> COMPLETADO
            else -> DESBLOQUEADO
        }
    }
}
