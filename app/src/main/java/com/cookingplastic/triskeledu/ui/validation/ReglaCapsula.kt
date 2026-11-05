package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.EstadoAdminPunto
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.model.tieneEnlace

//Reglas de las cápsulas del creador (RN-20, RN-21, RN-26, RN-28). Cada función devuelve el motivo
//por el que no se puede hacer algo, o null si se puede. Solo lógica: se prueba sin Android
object ReglaCapsula {

    const val RETIRO_BLOQUEADO = "No se puede retirar: está en un punto activo. Pide al administrador que lo desactive."

    //el punto que tiene esta cápsula, si hay uno (RN-20: a lo más uno)
    fun puntoDe(capsula: Capsula, puntos: List<PuntoInteres>): PuntoInteres? = puntos.find { it.capsulaId == capsula.id }

    //CRE-08 y RN-28: propia (salvo el administrador), publicada y sin punto
    fun motivoNoAsociable(capsula: Capsula, usuario: Usuario, puntos: List<PuntoInteres>): String? {
        val punto = puntoDe(capsula, puntos)
        return when {
            usuario.rol != RolUsuario.ADMINISTRADOR && capsula.creadorId != usuario.id -> "Es de otro creador"
            punto != null -> "Ya está asociada a ${punto.nombre}"
            capsula.estado != EstadoCapsula.PUBLICADA -> "Es un borrador: publícala primero"
            else -> null
        }
    }

    //RN-28: solo se asocia a un punto Sin cápsula
    fun motivoPuntoNoAsociable(punto: PuntoInteres, capsulaActual: Capsula?): String? =
        if (EstadoAdminPunto.desde(punto, capsulaActual) != EstadoAdminPunto.SIN_CAPSULA) "Este punto ya tiene una cápsula" else null

    //ADM-07: el selector de P10 ofrece las cápsulas PUBLICADAS y libres, de cualquier creador, más la que
    //el punto ya tiene (si se está editando)
    fun capsulasParaPunto(capsulas: List<Capsula>, puntos: List<PuntoInteres>, puntoId: String?): List<Capsula> {
        val ocupadas = puntos.filter { it.id != puntoId }.mapNotNull { it.capsulaId }.toSet()
        return capsulas
            .filter { it.estado == EstadoCapsula.PUBLICADA && it.id !in ocupadas }
            .sortedBy { it.titulo }
    }

    //CRE-03 y RN-26: se publica un borrador que ya tiene su enlace de Drive
    fun motivoNoPublicable(capsula: Capsula): String? = when {
        capsula.estado == EstadoCapsula.PUBLICADA -> "Esta cápsula ya está publicada"
        !capsula.tieneEnlace() -> "Pega un enlace de Google Drive"
        capsula.duracionSegundos !in 1..ValidacionCapsula.DURACION_MAXIMA -> "La cápsula debe durar 3 minutos como máximo"
        else -> null
    }

    //CRE-05 y RN-21: no se retira de un punto activo
    fun motivoNoRetirable(capsula: Capsula, punto: PuntoInteres?): String? = when {
        capsula.estado != EstadoCapsula.PUBLICADA -> "Esta cápsula no está publicada"
        punto?.activo == true -> RETIRO_BLOQUEADO
        else -> null
    }

    //CRE-06 y RN-21: solo se elimina un borrador que no esté en un punto
    fun motivoNoEliminable(capsula: Capsula, punto: PuntoInteres?): String? = when {
        capsula.estado != EstadoCapsula.BORRADOR -> "Solo se pueden eliminar los borradores"
        punto != null -> "Está asociada a ${punto.nombre}"
        else -> null
    }
}
