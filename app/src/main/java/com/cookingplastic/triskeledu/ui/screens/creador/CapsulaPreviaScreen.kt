package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.CapsulaPreviaUiEvent
import com.cookingplastic.triskeledu.ui.screens.video.VideoScreen
import com.cookingplastic.triskeledu.ui.screens.video.textoTiempo
import com.cookingplastic.triskeledu.ui.state.CapsulaPreviaUiState
import com.cookingplastic.triskeledu.ui.state.VideoUiState
import com.cookingplastic.triskeledu.ui.viewmodels.CapsulaPreviaViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.VideoViewModel

//P18-B · Vista previa y publicar (CRE-03, CRE-05). Usa el mismo reproductor de P12 con esVistaPrevia = true:
//libre y sin sumar puntos. Abajo dice si el enlace funciona y permite publicar
@Composable
fun CapsulaPreviaScreen(
    alVolver: () -> Unit,
    alPublicada: (String) -> Unit,
    videoViewModel: VideoViewModel = viewModel(factory = VideoViewModel.Factory),
    viewModel: CapsulaPreviaViewModel = viewModel(factory = CapsulaPreviaViewModel.Factory)
) {
    val video by videoViewModel.estado.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.publicada) {
        if (estado.publicada) video.capsula?.let { alPublicada(it.id) }
    }
    VideoScreen(
        alVolver = alVolver,
        viewModel = videoViewModel,
        pie = {
            PieVistaPrevia(
                video = video,
                estado = estado,
                alVolver = alVolver,
                alPublicar = {
                    viewModel.onEvent(CapsulaPreviaUiEvent.OnPublicar(videoCargado = video.error == null, duracionMs = video.duracionMs))
                }
            )
        }
    )
}

@Composable
private fun PieVistaPrevia(video: VideoUiState, estado: CapsulaPreviaUiState, alVolver: () -> Unit, alPublicar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        //el reproductor ya sabe cuánto dura: el archivo es público y se puede ver
        if (video.error == null && video.duracionMs > 0) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "El enlace funciona: el video es público y dura ${textoTiempo(video.duracionMs)}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        MensajeError(mensaje = estado.mensajeError)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = alVolver, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Volver") }
            if (estado.esBorrador) {
                Button(
                    onClick = alPublicar,
                    enabled = !estado.publicando && video.error == null,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) { Text(if (estado.publicando) "Publicando..." else "Publicar") }
            }
        }
    }
}
