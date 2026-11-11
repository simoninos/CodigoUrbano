package com.cookingplastic.triskeledu.ui.screens.configuracion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.ui.components.AvisoPermisoNotificaciones
import com.cookingplastic.triskeledu.ui.components.AvisoUbicacionSegundoPlano
import com.cookingplastic.triskeledu.ui.components.BotonMapa
import com.cookingplastic.triskeledu.ui.components.Interruptor
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel

//se conecta al ViewModel, avisa cuando hay que navegar y anima la entrada
@Composable
fun ConfiguracionScreen(
    alCerrarSesion: () -> Unit,
    alVolver: () -> Unit,
    //CRE-01: solo el creador ve la tarjeta que abre P17
    alAbrirMisCapsulas: () -> Unit = {},
    //PER-02: la tarjeta del perfil abre P15
    alAbrirPerfil: () -> Unit = {},
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
            alVolver = alVolver,
            alAbrirMisCapsulas = alAbrirMisCapsulas,
            alAbrirPerfil = alAbrirPerfil,
        )
    }
}

//solo dibuja; las previews lo muestran sin animación ni ViewModel
@Composable
fun ConfiguracionContent(
    estado: ConfiguracionUiState,
    onEvent: (ConfiguracionUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    alAbrirMisCapsulas: () -> Unit = {},
    alAbrirPerfil: () -> Unit = {}
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
        //PER-02: tarjeta del perfil (P08, P08-Cre y P08-Admin): abre P15
        if (estado.nombreUsuario.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TarjetaPerfil(estado = estado, alPresionar = alAbrirPerfil)
        }
        //CRE-01: el creador además tiene la tarjeta "Mis cápsulas"
        if (estado.esCreador) {
            Spacer(modifier = Modifier.height(12.dp))
            TarjetaMisCapsulas(
                publicadas = estado.capsulasPublicadas,
                borradores = estado.capsulasBorrador,
                alPresionar = alAbrirMisCapsulas
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        //LVL-03: el tema y el interruptor Día/Noche van por separado; Noche aplica a cualquier tema
        TituloSeccion(texto = "Apariencia")
        SeccionApariencia(
            tema = estado.tema,
            temasDesbloqueados = estado.temasDesbloqueados,
            alElegir = { onEvent(ConfiguracionUiEvent.OnTemaChange(it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Interruptor(
            titulo = "Modo noche",
            marcado = estado.preferencias.temaNoche,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnTemaNocheChange(it)) },
            descripcion = "Usa la versión oscura del tema elegido"
        )

        Spacer(modifier = Modifier.height(16.dp))
        TituloSeccion(texto = "Notificaciones")
        //EXC-03: sin permiso, explica que igual quedan en el historial y cómo activarlas
        AvisoPermisoNotificaciones(modifier = Modifier.padding(bottom = 8.dp))
        if (estado.esAdministrador) {
            //P08-Admin: los avisos que le sirven al administrador (RN-24)
            Interruptor(
                titulo = "Nuevas solicitudes de creador",
                marcado = estado.preferencias.notifSolicitudes,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifSolicitudesChange(it)) },
                descripcion = "Avisa cuando alguien pide ser creador"
            )
            Interruptor(
                titulo = "Puntos listos para activar",
                marcado = estado.preferencias.notifPuntosListos,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifPuntosListosChange(it)) },
                descripcion = "Avisa cuando un creador asocia una cápsula a un punto"
            )
        } else {
            Interruptor(
                titulo = "Lugares cercanos",
                marcado = estado.preferencias.notifLugaresCercanos,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifLugaresCercanosChange(it)) },
                descripcion = "Avisa cuando estés cerca de un punto"
            )
            //RNF-03 y EXC-13: cómo recibirlo también con la app minimizada
            if (estado.preferencias.notifLugaresCercanos) {
                AvisoUbicacionSegundoPlano(modifier = Modifier.padding(vertical = 8.dp))
            }
            Interruptor(
                titulo = "Nuevas cápsulas",
                marcado = estado.preferencias.notifNuevasCapsulas,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnNotifNuevasCapsulasChange(it)) },
                descripcion = "Avisa cuando haya cápsulas nuevas"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TituloSeccion(texto = "Mapa")
        Interruptor(
            titulo = "Seguir mi ubicación",
            marcado = estado.preferencias.seguirUbicacion,
            alCambiar = { onEvent(ConfiguracionUiEvent.OnSeguirUbicacionChange(it)) },
            descripcion = "El mapa se mueve contigo"
        )
        if (estado.esAdministrador) {
            //ADM-11: los inactivos solo los ve el administrador, y puede ocultarlos
            Interruptor(
                titulo = "Mostrar puntos inactivos",
                marcado = estado.preferencias.mostrarInactivos,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnMostrarInactivosChange(it)) },
                descripcion = "Los puntos completos que todavía no están activos"
            )
        } else {
            Interruptor(
                titulo = "Mostrar lugares bloqueados",
                marcado = estado.preferencias.mostrarBloqueados,
                alCambiar = { onEvent(ConfiguracionUiEvent.OnMostrarBloqueadosChange(it)) },
                descripcion = "También muestra los que aún no desbloqueas"
            )
        }

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(24.dp))

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

//nombre, etiqueta del rol (creador o administrador), "Editar perfil" y el nivel (LVL-05: el admin no tiene)
@Composable
private fun TarjetaPerfil(estado: ConfiguracionUiState, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(estado.nombreUsuario.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(estado.nombreUsuario, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        estado.esAdministrador -> EtiquetaRol("Administrador")
                        estado.esCreador -> EtiquetaRol("Creador")
                    }
                    Text(
                        text = listOfNotNull("Editar perfil", estado.nivel?.let { "Nivel ${it.numero}" }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun EtiquetaRol(texto: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) {
        Text(text = texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

//CRE-01: abre P17 con los totales de cápsulas publicadas y en borrador
@Composable
private fun TarjetaMisCapsulas(publicadas: Int, borradores: Int, alPresionar: () -> Unit) {
    Surface(
        onClick = alPresionar,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Videocam, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Mis cápsulas", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${textoCantidad(publicadas, "publicada", "publicadas")} · ${textoCantidad(borradores, "borrador", "borradores")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

private fun textoCantidad(cantidad: Int, singular: String, plural: String) = "$cantidad ${if (cantidad == 1) singular else plural}"

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
    TriskeleduTheme(tema = TemaApp.CERRO_SANTA_LUCIA) {
        Surface {
            ConfiguracionContent(
                estado = ConfiguracionUiState(
                    preferencias = Preferencias(usuarioId = "1", tema = TemaApp.CERRO_SANTA_LUCIA),
                    cargando = false,
                    nombreUsuario = "cliente_prueba",
                    tema = TemaApp.CERRO_SANTA_LUCIA,
                    temasDesbloqueados = ReglaNivel.temasDesbloqueados(2, 2, RolUsuario.CLIENTE),
                    nivel = ReglaNivel.nivelDesde(60)
                ),
                onEvent = {},
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
                    esAdministrador = true,
                    temasDesbloqueados = TemaApp.entries.toSet()
                ),
                onEvent = {},
                alVolver = {},
            )
        }
    }
}
