package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto

//Reglas de los puntos para el administrador (RN-27, RN-30, ADM-09). Solo lógica: se prueba sin Android
object ReglaPunto {

    const val ELIMINAR_CON_PROGRESO = "Ya hay usuarios con progreso aquí: desactívalo en vez de eliminarlo"
    const val ACTIVAR_SIN_CAPSULA = "Asocia una cápsula publicada para activarlo"

    //RN-30: solo se elimina un punto que nadie escaneó; si no, se desactiva y no se pierden los puntos de nadie
    fun motivoNoEliminable(escaneos: Int): String? = if (escaneos > 0) ELIMINAR_CON_PROGRESO else null

    //RN-27 y ADM-09: con Sin cápsula el interruptor queda bloqueado
    fun interruptorBloqueado(estado: EstadoAdminPunto): Boolean = estado == EstadoAdminPunto.SIN_CAPSULA
}
