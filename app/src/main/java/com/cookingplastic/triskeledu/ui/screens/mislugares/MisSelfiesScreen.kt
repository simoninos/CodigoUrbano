package com.cookingplastic.triskeledu.ui.screens.mislugares

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.ui.components.fechaCorta
import com.cookingplastic.triskeledu.ui.state.SelfieTomada

//P07-D (LUG-08): las selfies del usuario. Son privadas: solo las ve su autor (RN-10).
//La cámara para tomarlas llega en la etapa 8; mientras tanto se ven las de los datos de prueba
@Composable
fun ListaSelfies(selfies: List<SelfieTomada>, alVerDetalle: (String) -> Unit, textoVacio: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selfies.isEmpty()) TextoVacio(textoVacio)
        selfies.forEach { selfie -> TarjetaSelfie(selfie = selfie, alPresionar = { alVerDetalle(selfie.puntoId) }) }
    }
}

@Composable
private fun TarjetaSelfie(selfie: SelfieTomada, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            //miniatura: la foto, o un ícono de persona en las selfies de prueba
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.size(56.dp)) {
                if (selfie.selfieUri != null && selfie.selfieUri != DatosPrueba.IMAGEN_PRUEBA) {
                    AsyncImage(
                        model = selfie.selfieUri,
                        contentDescription = "Selfie en ${selfie.nombre}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = selfie.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Tomada: ${fechaCorta(selfie.selfieEn)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EtiquetaPuntos(puntos = PUNTOS_SELFIE)
                Etiqueta(texto = "Privada")
            }
        }
    }
}
