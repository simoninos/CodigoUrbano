package com.cookingplastic.triskeledu.data.model

//RN-25: lista cerrada de categorías; la etiqueta es lo que se muestra en pantalla
enum class CategoriaCapsula(val etiqueta: String) {
    QUE_ES_IA("¿Qué es la IA?"),
    IA_VIDA_DIARIA("IA en la vida diaria"),
    IA_CIUDAD("IA y la ciudad"),
    IA_RESPONSABLE("IA responsable"),
    HISTORIA_PATRIMONIO("Historia y patrimonio"),
}
