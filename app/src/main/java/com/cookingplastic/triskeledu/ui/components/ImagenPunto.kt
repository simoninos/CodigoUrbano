package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.cookingplastic.triskeledu.R
import com.cookingplastic.triskeledu.data.local.DatosPrueba

// Imagen de un punto de interés, compartida por el Detalle y el formulario del administrador.
// La imagen de prueba de DatosPrueba es un vector (punto_placeholder.xml) que Coil no puede leer
// desde su dirección "android.resource://", así que se dibuja directo como recurso.
// Si una foto real no carga, se muestra la misma imagen de reemplazo y nunca queda un espacio vacío.
@Composable
fun ImagenPunto(
    imagenUri: String?,
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    val reemplazo = painterResource(R.drawable.punto_placeholder)
    if (imagenUri.isNullOrBlank() || imagenUri == DatosPrueba.IMAGEN_PRUEBA) {
        Image(
            painter = reemplazo,
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Coil carga la imagen desde el teléfono sin bloquear la pantalla
        AsyncImage(
            model = imagenUri,
            contentDescription = descripcion,
            placeholder = reemplazo,
            error = reemplazo,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
