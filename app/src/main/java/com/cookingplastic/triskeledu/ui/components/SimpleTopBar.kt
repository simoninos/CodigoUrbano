package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.Monumento
import com.cookingplastic.triskeledu.data.model.TipoMonumento
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.navigation.Destination
import com.cookingplastic.triskeledu.ui.state.MonumentoUiState
import com.cookingplastic.triskeledu.ui.state.UsuarioUiState
import com.cookingplastic.triskeledu.ui.viewmodels.MonumentoViewModel
import com.cookingplastic.triskeledu.ui.viewmodels.UsuarioViewModel

@Preview
@Composable
fun SimpleTopBarPreview(){
    val monumentos = mutableListOf<Monumento>()
    val monumento1 = Monumento(1, "La moneda", -33.4429, -70.6540, TipoMonumento.MONUMENTO)
    val monumento2 = Monumento(2, "Museo de Arte Precolombino", -33.4382, -70.6519, TipoMonumento.MONUMENTO)
    val monumento3 = Monumento(3, "Museo Bellas Artes", -33.4360, -70.6436, TipoMonumento.MONUMENTO)

    monumentos.add(monumento1)
    monumentos.add(monumento2)
    monumentos.add(monumento3)

    val mockUsuarioUiState = UsuarioUiState(
        usuario = Usuario("Simoninos", 30, 5)
    )

    val mockMonumentoUiState = MonumentoUiState(
        monumentos = monumentos
    )


    Column(modifier = Modifier
        .safeContentPadding()
    ){
        SimpleTopBarContent(Destination.MAPA, mockUsuarioUiState, mockMonumentoUiState)
        SimpleTopBarContent(Destination.QR,mockUsuarioUiState, mockMonumentoUiState)
        SimpleTopBarContent(Destination.MONUMENTOS,mockUsuarioUiState, mockMonumentoUiState)
    }
}

@Composable
fun SimpleTopBar (
    destination: Destination,
    viewModelUsuario: UsuarioViewModel = viewModel(),
    viewModelMonumento: MonumentoViewModel = viewModel(),
){
    val estadoUsuario by viewModelUsuario.estado.collectAsStateWithLifecycle()
    val estadoMonumento by viewModelMonumento.estado.collectAsStateWithLifecycle()

    SimpleTopBarContent(destination, estadoUsuario, estadoMonumento)
}

@Composable
fun SimpleTopBarContent (
    destination: Destination,
    estadoUsuario: UsuarioUiState,
    estadoMonumento: MonumentoUiState
){
    val colorFondo = when (destination) {
        Destination.MAPA -> MaterialTheme.colorScheme.primary
        Destination.QR -> Color.Transparent
        Destination.MONUMENTOS -> Color.Transparent
        else -> Color.Transparent
    }

    Surface(color = colorFondo){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement
                .SpaceBetween
        ) {
            when (destination) {
                Destination.MAPA -> {
                    MapOverlay(true, estadoUsuario.usuario.username, estadoUsuario.usuario.puntos)
                    Boton(onClicked = {}, icon = Icons.Default.Settings)
                }

                Destination.QR -> {
                    Spacer(Modifier.weight(1f))
                    Boton(onClicked = {}, icon = Icons.Default.Settings)
                }

                Destination.MONUMENTOS -> {
                        Text("Mis puntos: ${estadoUsuario.usuario.puntos}")
                        Column{
                            Text("Lugares Escaneados")
                            Text("${estadoUsuario.usuario.monumentosEscaneados} de ${estadoMonumento.totalMonumentos}")
                        }
                }
            }
        }
    }
}

