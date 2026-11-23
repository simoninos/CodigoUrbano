package com.cookingplastic.triskeledu.ui.validation

import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp

//Niveles y temas (RN-31 a RN-33). Solo lógica: recibe números y devuelve el nivel, así se prueba sin Android
object ReglaNivel {

    //desde el nivel 8 el nombre ya no cambia
    private val nombres = listOf("Visitante", "Explorador", "Descubridor", "Guía urbano", "Cronista", "Guardián", "Sabio", "Maestro IA")

    //RN-31: puntos con que se alcanza el nivel n = 35 × (n−1) × n / 2 → 0, 35, 105, 210, 350, 525, 735, 980…
    fun umbral(nivel: Int): Int = 35 * (nivel - 1) * nivel / 2

    //el nivel más alto cuyo umbral ya se alcanzó; sin tope
    fun numeroDesde(puntos: Int): Int {
        var nivel = 1
        while (umbral(nivel + 1) <= puntos) nivel++
        return nivel
    }

    fun nombre(nivel: Int): String = nombres[(nivel - 1).coerceIn(0, nombres.lastIndex)]

    fun nivelDesde(puntos: Int): NivelUsuario {
        val numero = numeroDesde(puntos)
        return NivelUsuario(
            numero = numero,
            nombre = nombre(numero),
            puntosDesde = umbral(numero),
            puntosSiguiente = umbral(numero + 1),
            puntosTotales = puntos
        )
    }

    //RN-32 y RN-33: cuentan el nivel actual o el más alto que alcanzó, el mayor de los dos.
    //Del nivel 9 en adelante no se desbloquea nada más. El administrador tiene todos (LVL-05)
    fun temasDesbloqueados(nivelActual: Int, nivelMaximoAlcanzado: Int, rol: RolUsuario): Set<TemaApp> {
        if (rol == RolUsuario.ADMINISTRADOR) return TemaApp.entries.toSet()
        val nivel = maxOf(nivelActual, nivelMaximoAlcanzado)
        return TemaApp.entries.filter { it.nivelRequerido <= nivel }.toSet()
    }

    //el tema que desbloquea un nivel; null del 9 en adelante
    fun temaDelNivel(nivel: Int): TemaApp? = TemaApp.entries.find { it.nivelRequerido == nivel }
}
