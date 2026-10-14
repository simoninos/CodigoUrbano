package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion

class MonumentoRepository{
    suspend fun getMonumentos(): List<PuntoInteres> {
        val monumentos = mutableListOf<PuntoInteres>()

        val puntoInteres1 = PuntoInteres(
            nombre = "La Moneda",
            descripcion = null,
            tipo = TipoPuntoInteres.MONUMENTO,
            ubicacion = Ubicacion(-33.4429, -70.6540, 567.0),
            imagenUri = null,
            descubreMasUrl = null,
            codigoQr = null,
            capsulaId = null,
            activo = true,
            actualizadoEn = System.currentTimeMillis()
        )

        val puntoInteres2 = PuntoInteres(
            nombre = "Museo de Arte Precolombino",
            descripcion = null,
            tipo = TipoPuntoInteres.MONUMENTO,
            ubicacion = Ubicacion(-33.4382, -70.6519, 567.0),
            imagenUri = null,
            descubreMasUrl = null,
            codigoQr = null,
            capsulaId = null,
            activo = true,
            actualizadoEn = System.currentTimeMillis()
        )

        val puntoInteres3 = PuntoInteres(
            nombre = "Museo Bellas Artes",
            descripcion = null,
            tipo = TipoPuntoInteres.MONUMENTO,
            ubicacion = Ubicacion(-33.4360, -70.6436, 560.0),
            imagenUri = null,
            descubreMasUrl = null,
            codigoQr = null,
            capsulaId = null,
            activo = true,
            actualizadoEn = System.currentTimeMillis()
        )

        monumentos.add(puntoInteres1)
        monumentos.add(puntoInteres2)
        monumentos.add(puntoInteres3)

        return monumentos
    }

    suspend fun getMonumentoById(id: String): PuntoInteres?{
        val monumentos = getMonumentos()

        return monumentos.find { it.id == id }
    }
}
