package com.cookingplastic.triskeledu.ui.components

import java.time.Instant
import java.time.ZoneId

private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

//"Hoy, 11:20", "Ayer, 09:05" o "30 oct, 16:45", como en el mockup de Mis descubrimientos
fun fechaCorta(ms: Long, ahora: Long = System.currentTimeMillis(), zona: ZoneId = ZoneId.systemDefault()): String {
    val fecha = Instant.ofEpochMilli(ms).atZone(zona)
    //atZone y no LocalDate.ofInstant: ese último no existe en Android 8 a 11
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    val hora = "%02d:%02d".format(fecha.hour, fecha.minute)
    return when (fecha.toLocalDate()) {
        hoy -> "Hoy, $hora"
        hoy.minusDays(1) -> "Ayer, $hora"
        else -> "${fecha.dayOfMonth} ${meses[fecha.monthValue - 1]}, $hora"
    }
}

//"31 oct 2026", sin hora (P22: fecha de la solicitud)
fun fechaConAnio(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String {
    val fecha = Instant.ofEpochMilli(ms).atZone(zona)
    return "${fecha.dayOfMonth} ${meses[fecha.monthValue - 1]} ${fecha.year}"
}

//"oct 2026" (P15: "Desde oct 2026")
fun mesYAnio(ms: Long, zona: ZoneId = ZoneId.systemDefault()): String {
    val fecha = Instant.ofEpochMilli(ms).atZone(zona)
    return "${meses[fecha.monthValue - 1]} ${fecha.year}"
}
