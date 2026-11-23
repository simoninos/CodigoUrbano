package com.cookingplastic.triskeledu.ui.screens.perfil

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.cookingplastic.triskeledu.ui.components.mesYAnio
import com.cookingplastic.triskeledu.ui.event.PerfilUiEvent
import com.cookingplastic.triskeledu.ui.screens.creador.Etiqueta
import com.cookingplastic.triskeledu.ui.state.CampoPerfil
import com.cookingplastic.triskeledu.ui.state.PerfilUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ReglaNivel
import com.cookingplastic.triskeledu.ui.viewmodels.PerfilViewModel

//P15 · Mi perfil (PER-02): se llega desde la tarjeta del perfil en Configuración; "Volver" regresa ahí
@Composable
fun PerfilScreen(
    alVolver: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PerfilContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver)
}

@Composable
fun PerfilContent(
    estado: PerfilUiState,
    onEvent: (PerfilUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focos = remember { CampoPerfil.entries.associateWith { FocusRequester() } }
    EnfocarPrimerError(intento = estado.intentoEnvio, campo = estado.campoConError, focos = focos)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onEvent(PerfilUiEvent.OnMensajeMostrado)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                Text("Mi perfil", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
            Spacer(modifier = Modifier.height(8.dp))
            TarjetaCuenta(estado)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = estado.username,
                onValueChange = { onEvent(PerfilUiEvent.OnUsernameChange(it)) },
                label = { Text("Nombre de usuario") },
                singleLine = true,
                isError = estado.errorUsername != null,
                supportingText = if (estado.errorUsername == null) ({ Text("3 a 20 caracteres: letras, números o _") }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoPerfil.USERNAME))
            )
            MensajeError(mensaje = estado.errorUsername)
            //el correo no se cambia en esta versión: se muestra para que lo vea
            OutlinedTextField(
                value = estado.correo,
                onValueChange = {},
                label = { Text("Correo") },
                readOnly = true,
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Cambiar contraseña", style = MaterialTheme.typography.titleMedium)
            CampoClave(
                valor = estado.claveActual,
                alCambiar = { onEvent(PerfilUiEvent.OnClaveActualChange(it)) },
                etiqueta = "Contraseña actual",
                error = estado.errorClaveActual,
                modifier = Modifier.focusRequester(focos.getValue(CampoPerfil.CLAVE_ACTUAL))
            )
            CampoClave(
                valor = estado.claveNueva,
                alCambiar = { onEvent(PerfilUiEvent.OnClaveNuevaChange(it)) },
                etiqueta = "Nueva contraseña",
                error = estado.errorClaveNueva,
                apoyo = "Mínimo 8 caracteres, con letras y números",
                modifier = Modifier.focusRequester(focos.getValue(CampoPerfil.CLAVE_NUEVA))
            )
            CampoClave(
                valor = estado.repetirClave,
                alCambiar = { onEvent(PerfilUiEvent.OnRepetirClaveChange(it)) },
                etiqueta = "Repetir nueva contraseña",
                error = estado.errorRepetir,
                modifier = Modifier.focusRequester(focos.getValue(CampoPerfil.REPETIR))
            )

            Spacer(modifier = Modifier.height(16.dp))
            //RN-18: siempre responde; atenuado mientras no haya cambios
            BotonEnviar(
                texto = "Guardar cambios",
                alPresionar = { onEvent(PerfilUiEvent.OnGuardar) },
                atenuado = !estado.hayCambios,
                cargando = estado.guardando,
                textoCargando = "Guardando..."
            )
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

//Iniciales, nombre, rol, desde cuándo y nivel (el administrador no tiene)
@Composable
private fun TarjetaCuenta(estado: PerfilUiState) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(estado.usernameGuardado.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(estado.usernameGuardado, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta(nombreRol(estado.rol), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    if (estado.creadoEn > 0) {
                        Text("Desde ${mesYAnio(estado.creadoEn)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                estado.nivel?.let {
                    Text("Nivel ${it.numero} · ${it.nombre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun nombreRol(rol: RolUsuario): String = when (rol) {
    RolUsuario.CLIENTE -> "Cliente"
    RolUsuario.CREADOR -> "Creador"
    RolUsuario.ADMINISTRADOR -> "Administrador"
}

@Composable
private fun CampoClave(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    apoyo: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        visualTransformation = PasswordVisualTransformation(),
        supportingText = if (apoyo != null && error == null) ({ Text(apoyo) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth()
    )
    MensajeError(mensaje = error)
}

@Preview(name = "Mi perfil", showBackground = true)
@Composable
private fun PerfilPreview() {
    TriskeleduTheme {
        Surface {
            PerfilContent(
                estado = PerfilUiState(
                    usernameGuardado = "camila_explora", username = "camila_explora", correo = "camila.explora@ejemplo.cl",
                    creadoEn = 1_790_866_800_000L, nivel = ReglaNivel.nivelDesde(160), claveActual = "x",
                    errorClaveActual = "La contraseña actual no es correcta", cargando = false
                ),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}
