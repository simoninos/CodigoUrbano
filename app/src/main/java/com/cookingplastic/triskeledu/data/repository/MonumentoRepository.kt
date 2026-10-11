package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Monumento
import com.cookingplastic.triskeledu.data.model.TipoMonumento

class MonumentoRepository{
    fun getMonumentos(): List<Monumento> {
        val monumentos = mutableListOf<Monumento>()
        val monumento1 = Monumento(1, "La moneda", -33.4429, -70.6540, TipoMonumento.MONUMENTO)
        val monumento2 = Monumento(2, "Museo de Arte Precolombino", -33.4382, -70.6519, TipoMonumento.MONUMENTO)
        val monumento3 = Monumento(3, "Museo Bellas Artes", -33.4360, -70.6436, TipoMonumento.MONUMENTO)

        monumentos.add(monumento1)
        monumentos.add(monumento2)
        monumentos.add(monumento3)

        return monumentos
    }

    fun getMonumentoById(id: Int): Monumento?{
        val monumentos = getMonumentos()

        return monumentos.find { it.id == id}
    }

    fun getCantidadMonumentos(): Int{
        val totalMonumentos = getMonumentos().size

        return totalMonumentos
    }
}
