package com.cookingplastic.triskeledu.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.PathParser
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.ui.theme.ColoresApp
import com.cookingplastic.triskeledu.ui.state.CamaraMapa
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// Centro del mapa mientras no hay ubicación del usuario: La Moneda (M-06)
private val CENTRO_INICIAL = GeoPoint(-33.44294, -70.65388)
private const val ZOOM_INICIAL = 17.0
private const val RADIO_ESCANEO_METROS = 100.0

// Trazos del marcador en una grilla de 44 x 52, iguales a los del mockup
private const val TRAZO_PIN = "M22 2C11 2 3 10 3 20c0 13 19 30 19 30s19-17 19-30C41 10 33 2 22 2z"
private const val TRAZO_CANDADO =
    "M17 19h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2zM18 19v-3a4 4 0 0 1 8 0v3"
private const val TRAZO_EDIFICIO = "M13 27h18M15 27v-8M20 27v-8M24 27v-8M29 27v-8M12 19h20l-10 -6z"
private const val TRAZO_CHECK = "M14 20l5 5 10 -10"
// ADM-11: ojo tachado, para los puntos Inactivos que solo ve el administrador
private const val TRAZO_OJO_TACHADO = "M12 21c3 -5 7 -7 10 -7s7 2 10 7c-3 5 -7 7 -10 7s-7 -2 -10 -7zM22 18a3 3 0 1 0 0.01 0M13 13l18 16"
// CRE-07: cámara de video tachada, para los puntos Sin cápsula
private const val TRAZO_VIDEO_TACHADO = "M13 17h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2zM25 21l6 -3v10l-6 -3M12 14l20 16"

// MAP-03: un latido dura 1,2 s y se dibuja en 12 pasos; los íconos de cada paso se guardan
private const val DURACION_LATIDO_MS = 1200L
private const val PASOS_LATIDO = 12
// espacio extra (en la grilla de 44 x 52) alrededor del pin para que quepa el halo
private const val MARGEN_HALO = 12f

// Color de la ubicación del usuario según el tema; también lo usa el botón "Centrar"
@Composable
fun colorUbicacionUsuario(): Color = ColoresApp.ubicacion

// Mapa de OpenStreetMap (osmdroid) dentro de Compose. osmdroid es una vista clásica de Android,
// por eso se inserta con AndroidView. Dibuja los marcadores de los puntos (M-02, M-03),
// la posición del usuario con el círculo de 100 m (M-05) y responde a los toques (M-09, M-11).
@Composable
fun MapaOsm(
    puntos: List<PuntoMapa>,
    ubicacionUsuario: Ubicacion?,
    seleccionadoId: String?,
    seguirUbicacion: Boolean,
    solicitudCentrar: Int,
    camaraInicial: CamaraMapa?,
    alTocarPunto: (String) -> Unit,
    alTocarMapa: () -> Unit,
    alGuardarCamara: (CamaraMapa) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val noche = ColoresApp.esNoche

    // Colores del tema en el formato ARGB que usa osmdroid
    val colores = ColoresMarcador(
        bloqueado = ColoresApp.bloqueado.toArgb(),
        desbloqueado = MaterialTheme.colorScheme.secondary.toArgb(),
        sobreDesbloqueado = MaterialTheme.colorScheme.onSecondary.toArgb(),
        completado = MaterialTheme.colorScheme.primary.toArgb(),
        sobreCompletado = MaterialTheme.colorScheme.onPrimary.toArgb()
    )
    val colorUbicacion = colorUbicacionUsuario().toArgb()
    val colorEtiqueta = MaterialTheme.colorScheme.surface.toArgb()
    // Los íconos guardados se distinguen por los colores con que se dibujaron: al cambiar de tema se crean de nuevo
    val firmaColores = "${colores.hashCode()}-$colorUbicacion-$colorEtiqueta"

    // Los listeners de osmdroid se crean una vez; rememberUpdatedState hace que siempre
    // llamen a la versión más reciente de estas funciones
    val alTocarPuntoActual by rememberUpdatedState(alTocarPunto)
    val alTocarMapaActual by rememberUpdatedState(alTocarMapa)
    val alGuardarCamaraActual by rememberUpdatedState(alGuardarCamara)

    // Los íconos se dibujan una sola vez por combinación de estado, tamaño y tema
    val iconos = remember { mutableMapOf<String, Drawable>() }

    // MAP-03: marcadores que laten ahora (con si están seleccionados) y el paso del latido.
    // No son estado de Compose: cambiar el ícono no vuelve a armar el mapa
    val reducirMovimiento = rememberReducirMovimiento()
    val latiendo = remember { mutableListOf<Pair<Marker, Boolean>>() }
    val pasoLatido = remember { intArrayOf(0) }
    fun iconoLatido(paso: Int, seleccionado: Boolean): Drawable =
        iconos.getOrPut("latido-$paso-$seleccionado-$firmaColores") {
            iconoLatiendo(context, paso, seleccionado, colores, colorUbicacion)
        }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            // osmdroid destruye las capas (y borra el ícono de cada marcador) cuando la vista sale
            // de la ventana. Se desactiva para que la destrucción ocurra solo en onDispose.
            setDestroyMode(false)
            // M-23: se abre donde quedó la última vez; la primera vez, en La Moneda
            controller.setZoom(camaraInicial?.zoom ?: ZOOM_INICIAL)
            controller.setCenter(camaraInicial?.let { GeoPoint(it.latitud, it.longitud) } ?: CENTRO_INICIAL)
        }
    }

    // Capas del mapa, de abajo hacia arriba
    val capas = remember {
        val toquesMapa = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                alTocarMapaActual()
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        val circulo = Polygon(mapView).apply {
            // El círculo no responde a toques, así no tapa a los marcadores
            setOnClickListener { _, _, _ -> false }
            isEnabled = false
        }
        val etiquetaRadio = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> false }
            isEnabled = false
        }
        val marcadores = FolderOverlay()
        val usuario = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> false }
            isEnabled = false
        }
        mapView.overlays.add(toquesMapa)
        mapView.overlays.add(circulo)
        mapView.overlays.add(etiquetaRadio)
        mapView.overlays.add(marcadores)
        mapView.overlays.add(usuario)
        CapasMapa(circulo, etiquetaRadio, marcadores, usuario)
    }

    // El mapa se pausa y se reanuda junto con la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }
    // Al salir de la pantalla se entrega la cámara antes de liberar el mapa (M-23)
    DisposableEffect(Unit) {
        onDispose {
            alGuardarCamaraActual(
                CamaraMapa(
                    latitud = mapView.mapCenter.latitude,
                    longitud = mapView.mapCenter.longitude,
                    zoom = mapView.zoomLevelDouble
                )
            )
            mapView.onDetach()
        }
    }

    // Los LaunchedEffect se ejecutan cada vez que el mapa entra en pantalla, no solo cuando su
    // clave cambia. Se guardan los valores con que entró para reaccionar solo a cambios nuevos.
    val camaraRestaurada = camaraInicial != null
    val ubicacionAlEntrar = remember { ubicacionUsuario }
    val centrarAlEntrar = remember { solicitudCentrar }
    val seleccionAlEntrar = remember { seleccionadoId }

    // M-06 y M-07: la primera ubicación centra el mapa; después lo sigue solo si seguirUbicacion
    var yaCentrado by remember { mutableStateOf(camaraRestaurada) }
    LaunchedEffect(ubicacionUsuario) {
        val u = ubicacionUsuario ?: return@LaunchedEffect
        if (camaraRestaurada && u == ubicacionAlEntrar) return@LaunchedEffect
        if (!yaCentrado || seguirUbicacion) {
            mapView.controller.animateTo(GeoPoint(u.latitud, u.longitud))
            yaCentrado = true
        }
    }
    // Botón "Centrar": solo cuando se presiona estando en el mapa
    LaunchedEffect(solicitudCentrar) {
        if (solicitudCentrar != centrarAlEntrar) {
            ubicacionUsuario?.let { mapView.controller.animateTo(GeoPoint(it.latitud, it.longitud)) }
        }
    }
    // MAP-03: mientras haya un punto en rango, su ícono cambia de paso; solo con la pantalla visible
    val hayQueLatir = !reducirMovimiento && puntos.any { it.enRango }
    LaunchedEffect(hayQueLatir, firmaColores) {
        if (!hayQueLatir) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(DURACION_LATIDO_MS / PASOS_LATIDO)
                pasoLatido[0] = (pasoLatido[0] + 1) % PASOS_LATIDO
                latiendo.forEach { (marcador, seleccionado) -> marcador.icon = iconoLatido(pasoLatido[0], seleccionado) }
                mapView.invalidate()
            }
        }
    }
    // Al elegir un punto, el mapa se mueve hacia él
    LaunchedEffect(seleccionadoId) {
        if (seleccionadoId == seleccionAlEntrar) return@LaunchedEffect
        puntos.find { it.punto.id == seleccionadoId }?.let {
            mapView.controller.animateTo(GeoPoint(it.punto.ubicacion.latitud, it.punto.ubicacion.longitud))
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mapa ->
            // M-17: osmdroid no trae teselas oscuras; en tema Noche se invierten los colores
            mapa.overlayManager.tilesOverlay.setColorFilter(if (noche) TilesOverlay.INVERT_COLORS else null)

            // Marcadores: se vuelven a crear con el estado actual de cada punto
            capas.marcadores.items.clear()
            latiendo.clear()
            puntos.forEach { puntoMapa ->
                val seleccionado = puntoMapa.punto.id == seleccionadoId
                val clave = "${puntoMapa.estado}-${puntoMapa.estadoAdmin}-$seleccionado-$firmaColores"
                val marcador = Marker(mapa).apply {
                    position = GeoPoint(puntoMapa.punto.ubicacion.latitud, puntoMapa.punto.ubicacion.longitud)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = when {
                        //MAP-03: en rango late; si se pide reducir el movimiento, queda quieto con el halo
                        puntoMapa.enRango && reducirMovimiento -> iconoLatido(PASO_HALO_QUIETO, seleccionado)
                        puntoMapa.enRango -> iconoLatido(pasoLatido[0], seleccionado)
                        else -> iconos.getOrPut(clave) {
                            iconoMarcador(context, puntoMapa.estado, seleccionado, colores, puntoMapa.sinCapsula, puntoMapa.inactivo)
                        }
                    }
                    title = puntoMapa.punto.nombre
                    // El toque abre la tarjeta del punto en vez del globo de osmdroid
                    setOnMarkerClickListener { _, _ ->
                        alTocarPuntoActual(puntoMapa.punto.id)
                        true
                    }
                }
                capas.marcadores.add(marcador)
                if (puntoMapa.enRango && !reducirMovimiento) latiendo += marcador to seleccionado
            }

            // Usuario, círculo de 100 m y su etiqueta
            if (ubicacionUsuario != null) {
                val densidad = context.resources.displayMetrics.density
                val centro = GeoPoint(ubicacionUsuario.latitud, ubicacionUsuario.longitud)

                capas.circulo.setPoints(Polygon.pointsAsCircle(centro, RADIO_ESCANEO_METROS))
                capas.circulo.fillPaint.color = ColorUtils.setAlphaComponent(colorUbicacion, 26)
                capas.circulo.outlinePaint.color = colorUbicacion
                capas.circulo.outlinePaint.strokeWidth = 2f * densidad
                capas.circulo.outlinePaint.pathEffect =
                    DashPathEffect(floatArrayOf(6f * densidad, 4f * densidad), 0f)
                capas.circulo.isEnabled = true

                // La etiqueta "100 m" va sobre el borde del círculo, hacia el noreste
                capas.etiquetaRadio.position = centro.destinationPoint(RADIO_ESCANEO_METROS, 45.0)
                capas.etiquetaRadio.icon = iconos.getOrPut("etiqueta-$firmaColores") {
                    etiquetaTexto(context, "100 m", colorUbicacion, colorEtiqueta)
                }
                capas.etiquetaRadio.isEnabled = true

                capas.usuario.position = centro
                capas.usuario.icon = iconos.getOrPut("usuario-$firmaColores") {
                    puntoUsuario(colorUbicacion, (22 * densidad).roundToInt())
                }
                capas.usuario.isEnabled = true
            } else {
                capas.circulo.isEnabled = false
                capas.etiquetaRadio.isEnabled = false
                capas.usuario.isEnabled = false
            }

            mapa.invalidate()
        }
    )
}

// Referencias a las capas que se actualizan cuando cambia el estado
private class CapasMapa(
    val circulo: Polygon,
    val etiquetaRadio: Marker,
    val marcadores: FolderOverlay,
    val usuario: Marker
)

private data class ColoresMarcador(
    val bloqueado: Int,
    val desbloqueado: Int,
    val sobreDesbloqueado: Int,
    val completado: Int,
    val sobreCompletado: Int
)

// Dibuja el marcador del mockup: el pin con el color del estado y su ícono interior
// (candado, edificio o check). El seleccionado mide 52 x 62 dp; los demás, 44 x 52 dp.
// CRE-07: un punto Sin cápsula va amarillo, con borde punteado y la cámara tachada.
// ADM-11: un punto Inactivo va gris, con borde punteado y el ojo tachado
private fun iconoMarcador(
    context: Context,
    estado: EstadoPunto,
    seleccionado: Boolean,
    colores: ColoresMarcador,
    sinCapsula: Boolean = false,
    inactivo: Boolean = false
): Drawable {
    val densidad = context.resources.displayMetrics.density
    val escala = densidad * if (seleccionado) 52f / 44f else 1f
    val bitmap = Bitmap.createBitmap(
        (44 * escala).roundToInt(),
        (52 * escala).roundToInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    canvas.scale(escala, escala)

    val blanco = android.graphics.Color.WHITE
    val (relleno, contorno, trazoIcono) = when {
        sinCapsula -> Triple(colores.desbloqueado, colores.sobreDesbloqueado, TRAZO_VIDEO_TACHADO)
        inactivo -> Triple(colores.bloqueado, blanco, TRAZO_OJO_TACHADO)
        estado == EstadoPunto.BLOQUEADO -> Triple(colores.bloqueado, blanco, TRAZO_CANDADO)
        estado == EstadoPunto.DESBLOQUEADO -> Triple(colores.desbloqueado, colores.sobreDesbloqueado, TRAZO_EDIFICIO)
        else -> Triple(colores.completado, colores.sobreCompletado, TRAZO_CHECK)
    }

    val pin = PathParser.createPathFromPathData(TRAZO_PIN)
    canvas.drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = relleno
    })
    canvas.drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.ROUND
        color = contorno
        if (sinCapsula || inactivo) pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
    })
    canvas.drawPath(PathParser.createPathFromPathData(trazoIcono), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = contorno
    })
    return BitmapDrawable(context.resources, bitmap)
}

// MAP-03: el marcador bloqueado en rango, en un paso del latido. El pin crece y se achica desde la punta
// (hasta un 15 %) y detrás se abre un halo del color de la ubicación que se desvanece.
// El bitmap es más grande que el del pin para que quepa el halo; la punta queda a la misma altura del borde
// inferior, así el marcador no salta al empezar a latir
private const val PASO_HALO_QUIETO = 4

private fun iconoLatiendo(
    context: Context,
    paso: Int,
    seleccionado: Boolean,
    colores: ColoresMarcador,
    colorHalo: Int
): Drawable {
    val densidad = context.resources.displayMetrics.density
    val escala = densidad * if (seleccionado) 52f / 44f else 1f
    val avance = paso.toFloat() / PASOS_LATIDO
    val bitmap = Bitmap.createBitmap(
        ((44 + 2 * MARGEN_HALO) * escala).roundToInt(),
        ((52 + MARGEN_HALO) * escala).roundToInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    canvas.scale(escala, escala)
    canvas.translate(MARGEN_HALO, MARGEN_HALO)

    //halo: crece desde la cabeza del pin y se desvanece
    val radio = 18f + 14f * avance
    canvas.drawCircle(22f, 20f, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(colorHalo, (110 * (1f - avance)).roundToInt())
    })
    canvas.drawCircle(22f, 20f, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = ColorUtils.setAlphaComponent(colorHalo, (200 * (1f - avance)).roundToInt())
    })

    //pin bloqueado con el candado, escalado desde la punta (22, 50)
    val crecimiento = 1f + 0.15f * sin(PI * avance).toFloat()
    canvas.save()
    canvas.scale(crecimiento, crecimiento, 22f, 50f)
    val pin = PathParser.createPathFromPathData(TRAZO_PIN)
    canvas.drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colores.bloqueado
    })
    canvas.drawPath(pin, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.WHITE
    })
    canvas.drawPath(PathParser.createPathFromPathData(TRAZO_CANDADO), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = android.graphics.Color.WHITE
    })
    canvas.restore()
    return BitmapDrawable(context.resources, bitmap)
}

// P10 (mini mapa) y P19 (selector de ubicación): el mismo mapa de OpenStreetMap, sin marcadores de puntos.
// interactivo = false: solo muestra el lugar (en P10 un toque abre P19). interactivo = true: se mueve con
// el dedo, un toque lo centra ahí, y avisa el centro nuevo cada vez que cambia
@Composable
fun MapaSimple(
    latitud: Double,
    longitud: Double,
    modifier: Modifier = Modifier,
    zoom: Double = 16.0,
    interactivo: Boolean = false,
    alMoverCentro: (latitud: Double, longitud: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val noche = ColoresApp.esNoche
    val alMoverCentroActual by rememberUpdatedState(alMoverCentro)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(interactivo)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setDestroyMode(false)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(latitud, longitud))
            if (interactivo) {
                // un toque mueve el centro (y con él el pin) a ese lugar
                overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let { controller.animateTo(it) }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }))
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        alMoverCentroActual(mapCenter.latitude, mapCenter.longitude)
                        return false
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean = false
                })
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observador)
            mapView.onDetach()
        }
    }
    // el mini mapa sigue a las coordenadas que se escriben a mano o llegan de P19
    if (!interactivo) {
        LaunchedEffect(latitud, longitud) { mapView.controller.setCenter(GeoPoint(latitud, longitud)) }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mapa -> mapa.overlayManager.tilesOverlay.setColorFilter(if (noche) TilesOverlay.INVERT_COLORS else null) }
    )
}

// Punto de la ubicación del usuario: círculo del color de ubicación con borde blanco
private fun puntoUsuario(color: Int, tamanoPx: Int): Drawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(tamanoPx / 5, android.graphics.Color.WHITE)
        setSize(tamanoPx, tamanoPx)
    }

// Etiqueta de texto con fondo redondeado, usada para "100 m"
private fun etiquetaTexto(context: Context, texto: String, colorTexto: Int, colorFondo: Int): Drawable {
    val densidad = context.resources.displayMetrics.density
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * densidad
        isFakeBoldText = true
        color = colorTexto
    }
    val margenX = 8f * densidad
    val margenY = 2f * densidad
    val ancho = (pintura.measureText(texto) + 2 * margenX).roundToInt()
    val alto = (pintura.fontMetrics.let { it.descent - it.ascent } + 2 * margenY).roundToInt()
    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawRoundRect(
        RectF(0f, 0f, ancho.toFloat(), alto.toFloat()),
        10f * densidad,
        10f * densidad,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorFondo }
    )
    canvas.drawText(texto, margenX, margenY - pintura.fontMetrics.ascent, pintura)
    return BitmapDrawable(context.resources, bitmap)
}
