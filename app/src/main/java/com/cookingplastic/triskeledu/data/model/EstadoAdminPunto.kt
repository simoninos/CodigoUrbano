package com.cookingplastic.triskeledu.data.model

//Estado del punto que ve el administrador (RN-27). No se guarda: se calcula desde el punto y su cápsula
enum class EstadoAdminPunto(val etiqueta: String) {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    SIN_CAPSULA("Sin cápsula");

    companion object {
        //Sin cápsula si no tiene una cápsula publicada; si la tiene, según el campo activo
        fun desde(punto: PuntoInteres, capsula: Capsula?): EstadoAdminPunto = when {
            capsula == null || capsula.id != punto.capsulaId || capsula.estado != EstadoCapsula.PUBLICADA -> SIN_CAPSULA
            punto.activo -> ACTIVO
            else -> INACTIVO
        }
    }
}
