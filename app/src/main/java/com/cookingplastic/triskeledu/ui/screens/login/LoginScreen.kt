package com.cookingplastic.triskeledu.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.ui.components.BotonEnviar
import com.cookingplastic.triskeledu.ui.components.EnfocarPrimerError
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.SesionUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoSesion
import com.cookingplastic.triskeledu.ui.state.SesionUiState
import com.cookingplastic.triskeledu.ui.state.UsuarioDemo
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.SesionViewModel

@Composable
//Screen se conecta a ViewModel
fun LoginScreen(
    alIniciarSesion: () -> Unit,
    alOlvidarClave: () -> Unit,
    alCrearCuenta: () -> Unit,
    //RN-29: la cuenta tiene la solicitud de creador pendiente (P23)
    alSolicitudPendiente: () -> Unit,
    viewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

      //como un Composable se lee muchas veces, no queremos que se llame alIniciarSesion todo el rato. Por eso se usa LaunchedEffect, haciendo que se ejecute solo cuando cambia la clave
    LaunchedEffect(estado.sesionIniciada) {
        if (estado.sesionIniciada) alIniciarSesion()
    }
    LaunchedEffect(estado.solicitudPendiente) {
        if (estado.solicitudPendiente) {
            viewModel.onEvent(SesionUiEvent.OnSolicitudPendienteVista)
            alSolicitudPendiente()
        }
    }

    LoginContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alOlvidarClave = alOlvidarClave,
        alCrearCuenta = alCrearCuenta
    )
}

//Content dibuja recibiendo el estado y la función para avisar eventos
@Composable
fun LoginContent(
    estado: SesionUiState,
    onEvent: (SesionUiEvent) -> Unit,
    alOlvidarClave: () -> Unit,
    alCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier
) {
    //RN-18: el foco va al primer campo con error cada vez que se toca "Iniciar sesión"
    val focos = remember { CampoSesion.entries.associateWith { FocusRequester() } }
    EnfocarPrimerError(intento = estado.intentoEnvio, campo = estado.campoConError, focos = focos)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = estado.correo,
            onValueChange = { onEvent(SesionUiEvent.OnCorreoChange(it)) },
            label = { Text("Correo") },
            singleLine = true,
            isError = estado.errorCorreo != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoSesion.CORREO))
        )
        MensajeError(mensaje = estado.errorCorreo)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = estado.clave,
            onValueChange = { onEvent(SesionUiEvent.OnClaveChange(it)) },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = estado.errorClave != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(SesionUiEvent.OnIniciarSesion) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoSesion.CLAVE))
        )
        MensajeError(mensaje = estado.errorClave)

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(16.dp))

        //RN-18: siempre responde; atenuado si faltan datos y, al tocarlo, muestra los errores
        BotonEnviar(
            texto = "Iniciar sesión",
            alPresionar = { onEvent(SesionUiEvent.OnIniciarSesion) },
            atenuado = !estado.puedeIniciar,
            cargando = estado.cargando,
            textoCargando = "Ingresando..."
        )

        TextButton(onClick = alOlvidarClave) {
            Text(text = "Olvidé mi contraseña")
        }
        TextButton(onClick = alCrearCuenta) {
            Text(text = "Crear cuenta")
        }

        Spacer(modifier = Modifier.height(16.dp))
        AccesoRapido(usuarios = estado.usuariosDemo, alElegir = { onEvent(SesionUiEvent.OnAccesoRapido(it)) })
    }
}

//ACC-08: rellena el correo y la contraseña de un usuario de prueba para la demo. No inicia sesión
@Composable
private fun AccesoRapido(usuarios: List<UsuarioDemo>, alElegir: (String) -> Unit) {
    val admin = usuarios.find { it.rol == RolUsuario.ADMINISTRADOR }
    val creador = usuarios.find { it.rol == RolUsuario.CREADOR }
    var listaAbierta by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "Acceso rápido (demo)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { admin?.let { alElegir(it.correo) } },
                enabled = admin != null,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text("Admin") }
            OutlinedButton(
                onClick = { creador?.let { alElegir(it.correo) } },
                enabled = creador != null,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text("Creador") }
            //Box: el menú se abre debajo de este botón
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { listaAbierta = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Usuarios")
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = listaAbierta, onDismissRequest = { listaAbierta = false }) {
                    //uno por nivel y la cuenta en revisión; el administrador tiene su propio botón
                    usuarios.filter { it.rol != RolUsuario.ADMINISTRADOR }.forEach { usuario ->
                        DropdownMenuItem(
                            text = { Text(usuario.etiqueta) },
                            onClick = {
                                alElegir(usuario.correo)
                                listaAbierta = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private val usuariosDemoPreview = listOf(
    UsuarioDemo("usuaria_nueva", "usuaria.nueva@ejemplo.cl", RolUsuario.CLIENTE, 1, false),
    UsuarioDemo("camila_explora", "camila.explora@ejemplo.cl", RolUsuario.CLIENTE, 3, false),
    UsuarioDemo("contenido_ia", "contenido@ejemplo.cl", RolUsuario.CREADOR, 5, false),
    UsuarioDemo("admin_triskel", "admin@ejemplo.cl", RolUsuario.ADMINISTRADOR, null, false),
    UsuarioDemo("pendiente_demo", "pendiente.demo@ejemplo.cl", RolUsuario.CLIENTE, null, true)
)

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun LoginVacioPreview() {
    TriskeleduTheme {
        Surface {
            LoginContent(
                estado = SesionUiState(usuariosDemo = usuariosDemoPreview),
                onEvent = {},
                alOlvidarClave = {},
                alCrearCuenta = {}
            )
        }
    }
}

@Preview(name = "Con errores", showBackground = true)
@Composable
private fun LoginErroresPreview() {
    TriskeleduTheme {
        Surface {
            LoginContent(
                estado = SesionUiState(
                    correo = "cliente@",
                    errorCorreo = "Ingresa un correo válido",
                    errorClave = "Ingresa tu contraseña",
                    usuariosDemo = usuariosDemoPreview
                ),
                onEvent = {},
                alOlvidarClave = {},
                alCrearCuenta = {}
            )
        }
    }
}

@Preview(name = "Credenciales incorrectas (noche)", showBackground = true)
@Composable
private fun LoginIncorrectoNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            LoginContent(
                estado = SesionUiState(
                    correo = "cliente@triskeledu.cl",
                    clave = "otraClave",
                    mensajeError = "Correo o contraseña incorrectos",
                    usuariosDemo = usuariosDemoPreview
                ),
                onEvent = {},
                alOlvidarClave = {},
                alCrearCuenta = {}
            )
        }
    }
}
