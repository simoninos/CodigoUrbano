package com.cookingplastic.triskeledu.ui.validation

//Reglas del enlace de una cápsula. Solo texto: se prueba sin Android
object EnlaceVideo {

    //formas en que Drive comparte un archivo: /file/d/ID/... o ?id=ID
    private val patronesDrive = listOf(
        Regex("""drive\.google\.com/file/d/([\w-]+)"""),
        Regex("""drive\.google\.com/(?:open|uc)\?(?:[^#]*&)?id=([\w-]+)""")
    )

    //id del archivo de Drive; null si no es un enlace de Drive
    fun idDeDrive(enlace: String): String? =
        patronesDrive.firstNotNullOfOrNull { it.find(enlace)?.groupValues?.get(1) }

    //el enlace de compartir abre una página, no el video: el reproductor necesita la descarga directa
    fun aReproducible(enlace: String): String =
        idDeDrive(enlace)?.let { "https://drive.google.com/uc?export=download&id=$it" } ?: enlace.trim()

    //solo los videos de internet la necesitan; el clip incluido en la app (android.resource://) no (RN-36)
    fun necesitaInternet(enlace: String): Boolean =
        enlace.startsWith("http://") || enlace.startsWith("https://")
}
