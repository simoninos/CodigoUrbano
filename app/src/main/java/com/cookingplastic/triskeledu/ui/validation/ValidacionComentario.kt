package com.cookingplastic.triskeledu.ui.validation

// D-17 (RN-18): un comentario lleva de 1 a 280 caracteres.
// Los espacios al principio y al final no cuentan, porque se quitan antes de guardar.
object ValidacionComentario {

    const val MAXIMO_CARACTERES = 280

    // Devuelve el mensaje de error, o null si el texto se puede publicar
    fun validar(texto: String): String? {
        val limpio = texto.trim()
        return when {
            limpio.isEmpty() -> "Escribe tu comentario"
            limpio.length > MAXIMO_CARACTERES -> "Máximo $MAXIMO_CARACTERES caracteres"
            else -> null
        }
    }
}
