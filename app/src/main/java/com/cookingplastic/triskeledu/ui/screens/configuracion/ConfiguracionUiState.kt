package com.cookingplastic.triskeledu.ui.screens.configuracion

import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.TemaApp

//Lo que muestra la pantalla de configuración
data class ConfiguracionUiState(
    //los 5 interruptores parten con los valores por defecto de Preferencias. El usuarioId vacío dura solo mientras carga
    val preferencias: Preferencias = Preferencias(usuarioId = ""),
    //true hasta que llegan las preferencias guardadas
    val cargando: Boolean = true,
    val nombreUsuario: String = "",
    //el botón de administrador solo aparece si esto es true
    val esAdministrador: Boolean = false,
    //CRE-01: P08-Cre muestra la etiqueta "Creador" y la tarjeta "Mis cápsulas" con sus totales
    val esCreador: Boolean = false,
    val capsulasPublicadas: Int = 0,
    val capsulasBorrador: Int = 0,
    //LVL-03: el tema elegido y los que puede elegir (RN-33); el administrador tiene los 8 (LVL-05)
    val tema: TemaApp = TemaApp.CLASICO,
    val temasDesbloqueados: Set<TemaApp> = setOf(TemaApp.CLASICO),
    //null para el administrador, que no tiene nivel
    val nivel: NivelUsuario? = null,
    //lo contrario de sesionIniciada: cuando pasa a true, se vuelve al login
    val sesionCerrada: Boolean = false,
    val mensajeError: String? = null
)
