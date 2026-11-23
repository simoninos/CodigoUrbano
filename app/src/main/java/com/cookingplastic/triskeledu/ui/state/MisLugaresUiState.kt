package com.cookingplastic.triskeledu.ui.state

//Lo que Mis descubrimientos (P07-A a P07-D) necesita para dibujarse. Todo se calcula desde ProgresoPunto
data class MisLugaresUiState(
    val totalPuntos: Int = 0,
    //"Lugares escaneados X de Y" y los tres cuadros "X de Y" (Y = puntos activos, LUG-01)
    val lugaresEscaneados: Int = 0,
    val totalLugares: Int = 0,
    val qrEscaneados: Int = 0,
    val capsulasVistas: Int = 0,
    val selfiesTomadas: Int = 0,
    //qué lista se muestra; la vista abierta se ve rellena con el color primario
    val vista: VistaDescubrimientos = VistaDescubrimientos.LUGARES,
    //las cuatro listas, ya filtradas y ordenadas. Solo traen lo que el usuario desbloqueó (RN-23)
    val desbloqueados: List<LugarDesbloqueado> = emptyList(),
    val capsulas: List<CapsulaDescubierta> = emptyList(),
    val qrs: List<QrEscaneado> = emptyList(),
    val selfies: List<SelfieTomada> = emptyList(),
    //lo que falta en la vista abierta; aquí sí van los nombres de los lugares (RN-23)
    val porDescubrir: List<LugarPorDescubrir> = emptyList(),
    val filtro: FiltroLugares = FiltroLugares.TODOS,
    val orden: OrdenLugares = OrdenLugares.RECIENTES,
    //"Por descubrir" pidió abrir el mapa en un lugar: la pantalla navega y lo vuelve a false
    val irAlMapa: Boolean = false,
    val cargando: Boolean = true,
    val mensajeError: String? = null
)

//P07-A: una fila de la lista "Desbloqueados"
data class LugarDesbloqueado(
    val puntoId: String,
    val nombre: String,
    val tipo: String,
    val puntos: Int,
    val maximo: Int,
    val estado: EstadoLugar,
    val escaneadoEn: Long
)

//P07-B: la cápsula de un lugar escaneado; vista = ya la vio completa
data class CapsulaDescubierta(
    val puntoId: String,
    val capsulaId: String,
    val titulo: String,
    val categoria: String,
    val nombrePunto: String,
    val vista: Boolean,
    val fecha: Long
)

//P07-C: un QR escaneado, con la distancia a la que se escaneó
data class QrEscaneado(
    val puntoId: String,
    val nombre: String,
    val escaneadoEn: Long,
    val distanciaMetros: Int
)

//P07-D: una selfie tomada; solo las ve su autor (RN-10)
data class SelfieTomada(
    val puntoId: String,
    val nombre: String,
    val selfieUri: String?,
    val selfieEn: Long
)

data class LugarPorDescubrir(val puntoId: String, val nombre: String)

//Las cuatro vistas de Mis descubrimientos, con el título de la lista y cómo se nombra lo que falta
enum class VistaDescubrimientos(val tituloLista: String, val singular: String, val plural: String, val pista: String) {
    LUGARES("Desbloqueados", "lugar", "lugares", "Encuéntralo en el mapa y escanea su QR"),
    CAPSULAS("Desbloqueados", "cápsula", "cápsulas", "Escanea el QR del lugar para desbloquearla"),
    QR("QR escaneados", "QR", "QR", "Encuéntralo en el mapa y escanéalo"),
    SELFIES("Mis selfies", "selfie", "selfies", "Tómatela en el lugar después de escanear su QR");

    //LUG-03: "Plaza de Armas", "Cápsula de Plaza de Armas", "QR de…" o "Selfie en…"
    fun textoPorDescubrir(nombre: String): String = when (this) {
        LUGARES -> nombre
        CAPSULAS -> "Cápsula de $nombre"
        QR -> "QR de $nombre"
        SELFIES -> "Selfie en $nombre"
    }
}

//Lo que se escribe bajo el nombre (LUG-02): "Recién descubierto", "Falta la cápsula", "Falta la selfie" o "Completado"
enum class EstadoLugar(val texto: String) {
    RECIEN_DESCUBIERTO("Recién descubierto"),
    FALTA_CAPSULA("Falta la cápsula"),
    FALTA_SELFIE("Falta la selfie"),
    COMPLETADO("Completado")
}

//Sirve para las cuatro listas: en cápsulas, "En curso" son las por ver y "Completados" las vistas;
//en las demás, según si el lugar está completo
enum class FiltroLugares(val texto: String) {
    TODOS("Todos"),
    EN_CURSO("En curso"),
    COMPLETADOS("Completados")
}

enum class OrdenLugares(val texto: String) {
    RECIENTES("Más recientes"),
    NOMBRE("Nombre (A-Z)"),
    PUNTOS("Más puntos")
}
