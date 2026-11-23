package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.RolUsuario

//RN-15: qué puntos se dibujan en el mapa según el rol. Solo lógica: se prueba sin Android
object ReglaMapa {

    //el cliente solo ve los Activos; el creador y el administrador, además los Sin cápsula (CRE-07).
    //Los Inactivos solo los ve el administrador, si tiene encendido "Mostrar puntos inactivos" (ADM-11)
    fun puntoVisible(estado: EstadoAdminPunto, rol: RolUsuario?, mostrarInactivos: Boolean = true): Boolean = when (estado) {
        EstadoAdminPunto.ACTIVO -> true
        EstadoAdminPunto.SIN_CAPSULA -> rol == RolUsuario.CREADOR || rol == RolUsuario.ADMINISTRADOR
        EstadoAdminPunto.INACTIVO -> rol == RolUsuario.ADMINISTRADOR && mostrarInactivos
    }
}
