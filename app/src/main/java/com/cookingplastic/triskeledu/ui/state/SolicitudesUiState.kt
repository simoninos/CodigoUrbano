package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Usuario

//Lo que el administrador está por confirmar en el diálogo
data class DecisionSolicitud(
    val usuario: Usuario,
    val aprobar: Boolean
)

//P22 · Solicitudes de creador (ADM-10)
data class SolicitudesUiState(
    val pendientes: List<Usuario> = emptyList(),
    val porConfirmar: DecisionSolicitud? = null,
    val procesando: Boolean = false,
    //resultado de la última respuesta, o "Esta solicitud ya fue respondida" (EXC-09)
    val mensaje: String? = null,
    val cargando: Boolean = true
)
