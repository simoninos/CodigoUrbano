package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OpenStreetMap(
    modifier: Modifier = Modifier,
    latitud: Double,
    longitud: Double,
    zoomLevel: Double = 15.0,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).apply {
                // Tipo de mapa predeterminado (Mapnik)
                setTileSource(TileSourceFactory.MAPNIK)
                // Permitir zoom con dos dedos (Pinch to zoom)
                setMultiTouchControls(true)

                // Posición inicial
                val startPoint = GeoPoint(latitud, longitud)
                controller.setZoom(zoomLevel)
                controller.setCenter(startPoint)

                // Agregar un marcador opcional
                val marker = Marker(this).apply {
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Ubicación inicial"
                }
                overlays.add(marker)
            }
        },
        update = { mapView ->
            // Actualizar la vista si cambian las coordenadas desde Compose
            val point = GeoPoint(latitud, longitud)
            mapView.controller.setCenter(point)
        }
    )
}