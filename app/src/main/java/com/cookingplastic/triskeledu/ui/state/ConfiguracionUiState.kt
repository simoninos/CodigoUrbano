package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Preferencias

//Lo que muestra la pantalla de configuración
data class ConfiguracionUiState(
    //los 5 interruptores parten con los valores por defecto de Preferencias. El usuarioId vacío dura solo mientras carga
    val preferencias: Preferencias = Preferencias(usuarioId = ""),
    //true hasta que llegan las preferencias guardadas
    val cargando: Boolean = true,
    val nombreUsuario: String = "",
    //el botón de administrador solo aparece si esto es true
    val esAdministrador: Boolean = false,
    //lo contrario de sesionIniciada: cuando pasa a true, se vuelve al login
    val sesionCerrada: Boolean = false,
    val mensajeError: String? = null
)
