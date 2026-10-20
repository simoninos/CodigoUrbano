package com.cookingplastic.triskeledu.ui.screens.configuracion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.Interruptor
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.ConfiguracionUiEvent
import com.cookingplastic.triskeledu.ui.state.ConfiguracionUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.ConfiguracionViewModel

//se conecta al ViewModel, avisa cuando hay que navegar y anima la entrada
@Composable
fun ConfiguracionScreen(
    alCerrarSesion: () -> Unit,
    alAbrirAdmin: () -> Unit,
    alVolver: () -> Unit,
    viewModel: ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.sesionCerrada) {
        if (estado.sesionCerrada) alCerrarSesion()
    }

    //parte invisible y al dibujarse pide ser visible: eso dispara la animación una sola vez
    val entrada = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = entrada,
        //crece desde la esquina superior derecha, donde está el engranaje de Simon
        enter = scaleIn(transformOrigin = TransformOrigin(1f, 0f)) + fadeIn()
    ) {
        ConfiguracionContent(
            estado = estado,
            onEvent = viewModel::onEvent,
            alAbrirAdmin = alAbrirAdmin,
            alVolver = alVolver,
        )
    }
}

//solo dibuja; las previews lo muestran sin animación ni ViewModel
@Composable
fun ConfiguracionContent(
    estado: ConfiguracionUiState,
    onEvent: (ConfiguracionUiEvent) -> Unit,
    alAbrirAdmin: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            BotonMapa(icono = Icons.Default.Close, descripcion = "Cerrar Configuración", alPresionar = alVolver)
        }
        if (estado.nombreUsuario.isNotEmpty()) {
            Text(
                text = "Sesión iniciada como ${estado.nombreUsuario}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        TituloSeccion(texto = "Apariencia")
        //por ahora solo guarda temaNoche; que toda la app cambie de colores es la meta 13
        Interruptor(
            titulo = "Tema noche",
            marcado = estado.preferencias.temaNoche,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnTemaNocheChange(it)) },
            descripcion = "Colores oscuros para usar de noche"
        )

        Spacer(modifier = Modifier.height(16.dp))
        TituloSeccion(texto = "Notificaciones")
        Interruptor(
            titulo = "Lugares cercanos",
            marcado = estado.preferencias.notifLugaresCercanos,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifLugaresCercanosChange(it)) },
            descripcion = "Avisa cuando estés cerca de un punto"
        )
        Interruptor(
            titulo = "Nuevos videos",
            marcado = estado.preferencias.notifNuevosVideos,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifNuevosVideosChange(it)) },
            descripcion = "Avisa cuando haya cápsulas nuevas"
        )

        Spacer(modifier = Modifier.height(16.dp))
        TituloSeccion(texto = "Mapa")
        Interruptor(
            titulo = "Seguir mi ubicación",
            marcado = estado.preferencias.seguirUbicacion,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnSeguirUbicacionChange(it)) },
            descripcion = "El mapa se mueve contigo"
        )
        Interruptor(
            titulo = "Mostrar lugares bloqueados",
            marcado = estado.preferencias.mostrarBloqueados,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnMostrarBloqueadosChange(it)) },
            descripcion = "También muestra los que aún no desbloqueas"
        )

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(24.dp))

        //solo los administradores ven este botón
        if (estado.esAdministrador) {
            OutlinedButton(
                onClick = alAbrirAdmin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Administrar puntos")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { onEvent(ConfiguracionUiEvent.OnCerrarSesion) },
            //rojo del tema para que se note que es una acción importante
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar sesión")
        }
    }
}

//privada porque solo la usa esta pantalla
@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Preview(name = "Cliente", showBackground = true)
@Composable
private fun ConfiguracionClientePreview() {
    TriskeleduTheme {
        Surface {
            ConfiguracionContent(
                estado = ConfiguracionUiState(
                    preferencias = Preferencias(usuarioId = "1"),
                    cargando = false,
                    nombreUsuario = "cliente_prueba"
                ),
                onEvent = {},
                alAbrirAdmin = {},
                alVolver = {},
            )
        }
    }
}

@Preview(name = "Administrador (noche)", showBackground = true)
@Composable
private fun ConfiguracionAdminNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            ConfiguracionContent(
                //tema noche encendido, igual que el tema de esta preview
                estado = ConfiguracionUiState(
                    preferencias = Preferencias(usuarioId = "2", temaNoche = true),
                    cargando = false,
                    nombreUsuario = "admin_prueba",
                    esAdministrador = true
                ),
                onEvent = {},
                alAbrirAdmin = {},
                alVolver = {},
            )
        }
    }
}
