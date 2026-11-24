package com.cookingplastic.triskeledu.domain.rules

//ADM-08: el QR de un punto nuevo se genera solo al guardarlo, con el mismo formato de los datos de prueba
//(TRISKEL-PI-001, TRISKEL-PI-002…). Solo lógica: se prueba sin Android
object ReglaQr {

    private const val PREFIJO = "TRISKEL-PI-"
    private val patron = Regex("""^TRISKEL-PI-(\d+)$""")

    //EXC-04: true si el código tiene el formato del juego, aunque su punto ya no exista
    fun esCodigoDelJuego(codigo: String): Boolean = patron.matches(codigo.trim())

    //el siguiente número libre: uno más que el mayor que ya existe. Nunca repite uno usado (el índice es único)
    fun siguienteCodigo(existentes: Collection<String>): String {
        val mayor = existentes.mapNotNull { patron.find(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0
        return PREFIJO + (mayor + 1).toString().padStart(3, '0')
    }
}
