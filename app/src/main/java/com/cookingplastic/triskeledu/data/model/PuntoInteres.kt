package com.cookingplastic.triskeledu.data.model

import java.util.UUID

data class PuntoInteres(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val descripcion: String?,
    val tipo: TipoPuntoInteres,
    val ubicacion: Ubicacion,
    val imagenUri: String?,
    val descubreMasUrl: String?,
    val codigoQr: String?,
    val capsulaId: String?,
    val activo: Boolean = false,
    val actualizadoEn: Long = System.currentTimeMillis()
)