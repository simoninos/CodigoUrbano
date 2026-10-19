package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres

object Validaciones {

    private val patronCorreo = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$""")
    private val patronUsername = Regex("""^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9_]{3,20}$""")
    private val patronEnlace = Regex("""^https://\S+$""")


    //Aquí no irán si el correo o username está en uso porque ya existe en repositorio
    fun validarCorreo(correo: String): String? =
        if (patronCorreo.matches(correo)) null else "Ingresa un correo válido"

    fun validarClaveLogin(clave: String): String? =
        if (clave.isBlank()) "Ingresa tu contraseña" else null

    fun validarUsername(username: String): String? =
        if (patronUsername.matches(username)) null else "Usa de 3 a 20 caracteres: letras, números o _"

    fun validarClaveRegistro(clave: String): String? {
        val largoOk = clave.length >= 8
        val tieneLetra = clave.any { it.isLetter() }
        val tieneNumero = clave.any { it.isDigit() }
        return if (largoOk && tieneLetra && tieneNumero) null else "Mínimo 8 caracteres, con letras y números"
    }

    fun validarRepetirClave(clave: String, repetida: String): String? =
        if (clave == repetida) null else "Las contraseñas no coinciden"

    fun validarTerminos(aceptados: Boolean): String? =
        if (aceptados) null else "Debes aceptar los términos"

    fun validarNombrePunto(nombre: String): String? =
        if (nombre.trim().length in 3..80) null else "El nombre es obligatorio"

    fun validarTipo(tipo: TipoPuntoInteres?): String? =
        if (tipo != null) null else "Elige un tipo"

    fun validarLatitud(texto: String): String? {
        val latitud = texto.toDoubleOrNull()
        return if (latitud != null && latitud in -90.0..90.0) null else "Latitud fuera de rango"
    }

    fun validarLongitud(texto: String): String? {
        val longitud = texto.toDoubleOrNull()
        return if (longitud != null && longitud in -180.0..180.0) null else "Longitud fuera de rango"
    }

    fun validarAltitud(texto: String): String? =
        if (texto.toDoubleOrNull() != null) null else "La altitud debe ser un número"

    fun validarDescubreMas(enlace: String): String? =
        if (enlace.isBlank() || patronEnlace.matches(enlace)) null else "Ingresa un enlace válido"

    fun validarDescripcion(descripcion: String): String? =
        if (descripcion.length <= 500) null else "Máximo 500 caracteres"
}
