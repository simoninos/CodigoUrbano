package com.cookingplastic.triskeledu.navigation

/*
* Rutas de las pantallas que no son pestañas de la barra inferior.
* Las rutas que reciben un dato lo declaran entre llaves, por ejemplo "detalle/{puntoId}".
* Las funciones arman la ruta con el dato real, para no escribirla a mano al navegar.
*/
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"

    // Nombre del argumento que llevan Detalle, Lugar encontrado y Admin editar
    const val ARG_PUNTO_ID = "puntoId"

    const val DETALLE = "detalle/{$ARG_PUNTO_ID}"
    fun detalle(puntoId: String) = "detalle/$puntoId"

    const val ENCONTRADO = "encontrado/{$ARG_PUNTO_ID}"
    fun encontrado(puntoId: String) = "encontrado/$puntoId"

    const val CONFIGURACION = "configuracion"

    const val ADMIN_LISTA = "admin"

    // El argumento es opcional: sin puntoId el formulario crea un punto nuevo
    const val ADMIN_EDITAR = "admin/editar?$ARG_PUNTO_ID={$ARG_PUNTO_ID}"
    fun adminEditar(puntoId: String? = null) =
        if (puntoId == null) "admin/editar" else "admin/editar?$ARG_PUNTO_ID=$puntoId"
}
