package com.cookingplastic.triskeledu.navigation

/*
* Rutas de las pantallas que no son pestañas de la barra inferior.
* Las rutas que reciben un dato lo declaran entre llaves, por ejemplo "detalle/{puntoId}".
* Las funciones arman la ruta con el dato real, para no escribirla a mano al navegar.
*/
object Rutas {
    // P00: primera pantalla sin sesión (ACC-01)
    const val BIENVENIDA = "bienvenida"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    // P23: cuenta con la solicitud de creador en revisión (RN-29)
    const val SOLICITUD_REVISION = "solicitud_revision"

    // Nombre del argumento que llevan Detalle y Admin editar
    const val ARG_PUNTO_ID = "puntoId"

    const val DETALLE = "detalle/{$ARG_PUNTO_ID}"
    fun detalle(puntoId: String) = "detalle/$puntoId"

    // P13: selfie en un punto desbloqueado (ACT-03)
    const val SELFIE = "selfie/{$ARG_PUNTO_ID}"
    fun selfie(puntoId: String) = "selfie/$puntoId"

    // P12: reproductor de cápsula. puntoId y vistaPrevia son opcionales (P18-B no tiene punto)
    const val ARG_CAPSULA_ID = "capsulaId"
    const val ARG_VISTA_PREVIA = "vistaPrevia"
    const val VIDEO = "video/{$ARG_CAPSULA_ID}?$ARG_PUNTO_ID={$ARG_PUNTO_ID}&$ARG_VISTA_PREVIA={$ARG_VISTA_PREVIA}"
    fun video(capsulaId: String, puntoId: String?, vistaPrevia: Boolean = false): String =
        "video/$capsulaId?$ARG_VISTA_PREVIA=$vistaPrevia" + (puntoId?.let { "&$ARG_PUNTO_ID=$it" } ?: "")

    const val CONFIGURACION = "configuracion"
    // P15: mi perfil (PER-02), desde la tarjeta del perfil en Configuración
    const val PERFIL = "perfil"
    // P16: historial de notificaciones (PER-03), desde la campana
    const val NOTIFICACIONES = "notificaciones"

    // ---------- Creador de contenido (CRE) ----------
    // P17: Mis cápsulas
    const val CAPSULAS = "capsulas"
    // P17-B: detalle de una cápsula publicada
    const val CAPSULA_DETALLE = "capsula/{$ARG_CAPSULA_ID}"
    fun capsulaDetalle(capsulaId: String) = "capsula/$capsulaId"
    // P18: sin capsulaId crea una cápsula nueva
    const val CAPSULA_EDITAR = "capsula_editar?$ARG_CAPSULA_ID={$ARG_CAPSULA_ID}"
    fun capsulaEditar(capsulaId: String? = null) =
        if (capsulaId == null) "capsula_editar" else "capsula_editar?$ARG_CAPSULA_ID=$capsulaId"
    // P18-B: vista previa y publicar (usa el reproductor de P12 en modo vista previa)
    const val CAPSULA_PREVIA = "capsula_previa/{$ARG_CAPSULA_ID}"
    fun capsulaPrevia(capsulaId: String) = "capsula_previa/$capsulaId"
    // P21: asociar una cápsula a un punto Sin cápsula
    const val ASOCIAR_CAPSULA = "asociar_capsula/{$ARG_PUNTO_ID}"
    fun asociarCapsula(puntoId: String) = "asociar_capsula/$puntoId"

    // P22: solicitudes de creador (administrador)
    const val SOLICITUDES = "solicitudes"

    // P24: mi nivel (LVL-01), se abre desde la tarjeta del usuario en el mapa
    const val NIVEL = "nivel"

    // P20: ranking (LUG-06), desde "Ver ranking ›" en Mis descubrimientos
    const val RANKING = "ranking"

    // P10: crear o editar punto. La lista del administrador es la pestaña Mis lugares (P07-Admin)
    // El argumento es opcional: sin puntoId el formulario crea un punto nuevo
    const val ADMIN_EDITAR = "admin/editar?$ARG_PUNTO_ID={$ARG_PUNTO_ID}"
    fun adminEditar(puntoId: String? = null) =
        if (puntoId == null) "admin/editar" else "admin/editar?$ARG_PUNTO_ID=$puntoId"
}
