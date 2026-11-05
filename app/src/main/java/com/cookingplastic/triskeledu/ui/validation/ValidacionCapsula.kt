package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.CategoriaCapsula

//Validaciones del formulario P18 (ERS 4, RN-25 y RN-26). Cada una devuelve el mensaje de error o null
object ValidacionCapsula {

    const val TITULO_MINIMO = 3
    const val TITULO_MAXIMO = 80
    const val DESCRIPCION_MAXIMA = 300
    //RN-26: 3 minutos como máximo
    const val DURACION_MAXIMA = 180

    fun validarTitulo(titulo: String): String? {
        val limpio = titulo.trim()
        return when {
            limpio.isEmpty() -> "El título es obligatorio"
            limpio.length !in TITULO_MINIMO..TITULO_MAXIMO -> "El título debe tener entre $TITULO_MINIMO y $TITULO_MAXIMO caracteres"
            else -> null
        }
    }

    fun validarDescripcion(descripcion: String): String? =
        if (descripcion.trim().length > DESCRIPCION_MAXIMA) "Máximo $DESCRIPCION_MAXIMA caracteres" else null

    fun validarCategoria(categoria: CategoriaCapsula?): String? =
        if (categoria == null) "Elige una categoría" else null

    //RN-26: obligatorio y de Google Drive (drive.google.com/file/d/… o ?id=…)
    fun validarEnlace(enlace: String): String? =
        if (EnlaceVideo.idDeDrive(enlace.trim()) == null) "Pega un enlace de Google Drive" else null

    //en segundos, de 1 a 180
    fun validarDuracion(texto: String): String? {
        val segundos = texto.trim().toIntOrNull()
        return when {
            segundos == null || segundos < 1 -> "Ingresa la duración en segundos"
            segundos > DURACION_MAXIMA -> "La cápsula debe durar 3 minutos como máximo"
            else -> null
        }
    }
}
