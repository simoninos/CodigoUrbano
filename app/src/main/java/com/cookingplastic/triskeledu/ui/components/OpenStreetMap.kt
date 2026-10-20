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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.PathParser
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cookingplastic.triskeledu.data.model.EstadoPunto
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.ui.state.PuntoMapa
import com.cookingplastic.triskeledu.ui.theme.Bloqueado
import com.cookingplastic.triskeledu.ui.theme.BloqueadoNoche
import com.cookingplastic.triskeledu.ui.theme.UbicacionNoche
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.roundToInt
import com.cookingplastic.triskeledu.ui.theme.Ubicacion as ColorUbicacionDia

// Centro del mapa mientras no hay ubicación del usuario: La Moneda (M-06)
private val CENTRO_INICIAL = GeoPoint(-33.44294, -70.65388)
private const val ZOOM_INICIAL = 17.0
private const val RADIO_ESCANEO_METROS = 60.0

// Trazos del marcador en una grilla de 44 x 52, iguales a los del mockup
private const val TRAZO_PIN = "M22 2C11 2 3 10 3 20c0 13 19 30 19 30s19-17 19-30C41 10 33 2 22 2z"
private const val TRAZO_CANDADO =
    "M17 19h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2zM18 19v-3a4 4 0 0 1 8 0v3"
private const val TRAZO_EDIFICIO = "M13 27h18M15 27v-8M20 27v-8M24 27v-8M29 27v-8M12 19h20l-10 -6z"
private const val TRAZO_CHECK = "M14 20l5 5 10 -10"

// true si el tema activo es Noche (fondo oscuro)
@Composable
private fun esTemaNoche(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

// Color de la ubicación del usuario según el tema; también lo usa el botón "Centrar"
@Composable
fun colorUbicacionUsuario(): Color = if (esTemaNoche()) UbicacionNoche else ColorUbicacionDia

// Mapa de OpenStreetMap (osmdroid) dentro de Compose. osmdroid es una vista clásica de Android,
// por eso se inserta con AndroidView. Dibuja los marcadores de los puntos (M-02, M-03),
// la posición del usuario con el círculo de 60 m (M-05) y responde a los toques (M-09, M-11).
@Composable
fun MapaOsm(
    puntos: List<PuntoMapa>,
    ubicacionUsuario: Ubicacion?,
    seleccionadoId: String?,
    seguirUbicacion: Boolean,
    solicitudCentrar: Int,
    alTocarPunto: (String) -> Unit,
    alTocarMapa: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val noche = esTemaNoche()

    // Colores del tema en el formato ARGB que usa osmdroid
    val colores = ColoresMarcador(
        bloqueado = (if (noche) BloqueadoNoche else Bloqueado).toArgb(),
        desbloqueado = MaterialTheme.colorScheme.secondary.toArgb(),
        sobreDesbloqueado = MaterialTheme.colorScheme.onSecondary.toArgb(),
        completado = MaterialTheme.colorScheme.primary.toArgb(),
        sobreCompletado = MaterialTheme.colorScheme.onPrimary.toArgb()
    )
    val colorUbicacion = colorUbicacionUsuario().toArgb()
    val colorEtiqueta = MaterialTheme.colorScheme.surface.toArgb()

    // Los listeners de osmdroid se crean una vez; rememberUpdatedState hace que siempre
    // llamen a la versión más reciente de estas funciones
    val alTocarPuntoActual by rememberUpdatedState(alTocarPunto)
    val alTocarMapaActual by rememberUpdatedState(alTocarMapa)

    // Los íconos se dibujan una sola vez por combinación de estado, tamaño y tema
    val iconos = remember { mutableMapOf<String, Drawable>() }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(ZOOM_INICIAL)
            controller.setCenter(CENTRO_INICIAL)
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
    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    // M-06 y M-07: la primera ubicación centra el mapa; después lo sigue solo si seguirUbicacion
    var yaCentrado by remember { mutableStateOf(false) }
    LaunchedEffect(ubicacionUsuario) {
        val u = ubicacionUsuario ?: return@LaunchedEffect
        if (!yaCentrado || seguirUbicacion) {
            mapView.controller.animateTo(GeoPoint(u.latitud, u.longitud))
            yaCentrado = true
        }
    }
    // Botón "Centrar"
    LaunchedEffect(solicitudCentrar) {
        if (solicitudCentrar > 0) {
            ubicacionUsuario?.let { mapView.controller.animateTo(GeoPoint(it.latitud, it.longitud)) }
        }
    }
    // Al elegir un punto, el mapa se mueve hacia él
    LaunchedEffect(seleccionadoId) {
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
            puntos.forEach { puntoMapa ->
                val seleccionado = puntoMapa.punto.id == seleccionadoId
                val clave = "${puntoMapa.estado}-$seleccionado-$noche"
                val marcador = Marker(mapa).apply {
                    position = GeoPoint(puntoMapa.punto.ubicacion.latitud, puntoMapa.punto.ubicacion.longitud)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = iconos.getOrPut(clave) {
                        iconoMarcador(context, puntoMapa.estado, seleccionado, colores)
                    }
                    title = puntoMapa.punto.nombre
                    // El toque abre la tarjeta del punto en vez del globo de osmdroid
                    setOnMarkerClickListener { _, _ ->
                        alTocarPuntoActual(puntoMapa.punto.id)
                        true
                    }
                }
                capas.marcadores.add(marcador)
            }

            // Usuario, círculo de 60 m y su etiqueta
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

                // La etiqueta "60 m" va sobre el borde del círculo, hacia el noreste
                capas.etiquetaRadio.position = centro.destinationPoint(RADIO_ESCANEO_METROS, 45.0)
                capas.etiquetaRadio.icon = iconos.getOrPut("etiqueta-$noche") {
                    etiquetaTexto(context, "60 m", colorUbicacion, colorEtiqueta)
                }
                capas.etiquetaRadio.isEnabled = true

                capas.usuario.position = centro
                capas.usuario.icon = iconos.getOrPut("usuario-$noche") {
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

private class ColoresMarcador(
    val bloqueado: Int,
    val desbloqueado: Int,
    val sobreDesbloqueado: Int,
    val completado: Int,
    val sobreCompletado: Int
)

// Dibuja el marcador del mockup: el pin con el color del estado y su ícono interior
// (candado, edificio o check). El seleccionado mide 52 x 62 dp; los demás, 44 x 52 dp.
private fun iconoMarcador(
    context: Context,
    estado: EstadoPunto,
    seleccionado: Boolean,
    colores: ColoresMarcador
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
    val (relleno, contorno, trazoIcono) = when (estado) {
        EstadoPunto.BLOQUEADO -> Triple(colores.bloqueado, blanco, TRAZO_CANDADO)
        EstadoPunto.DESBLOQUEADO -> Triple(colores.desbloqueado, colores.sobreDesbloqueado, TRAZO_EDIFICIO)
        EstadoPunto.COMPLETADO -> Triple(colores.completado, colores.sobreCompletado, TRAZO_CHECK)
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

// Punto de la ubicación del usuario: círculo del color de ubicación con borde blanco
private fun puntoUsuario(color: Int, tamanoPx: Int): Drawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(tamanoPx / 5, android.graphics.Color.WHITE)
        setSize(tamanoPx, tamanoPx)
    }

// Etiqueta de texto con fondo redondeado, usada para "60 m"
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
