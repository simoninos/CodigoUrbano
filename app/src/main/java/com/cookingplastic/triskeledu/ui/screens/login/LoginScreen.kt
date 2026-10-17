package com.cookingplastic.triskeledu.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.event.SesionUiEvent
import com.cookingplastic.triskeledu.ui.state.SesionUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.SesionViewModel

@Composable
//Screen se conecta a ViewModel
fun LoginScreen(
    alIniciarSesion: () -> Unit,
    alOlvidarClave: () -> Unit,
    alCrearCuenta: () -> Unit,
    viewModel: SesionViewModel = viewModel(factory = SesionViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

      //como un Composable se lee muchas veces, no queremos que se llame alIniciarSesion todo el rato. Por eso se usa LaunchedEffect, haciendo que se ejecute solo cuando cambia la clave
    LaunchedEffect(estado.sesionIniciada) {
        if (estado.sesionIniciada) alIniciarSesion()
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
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        )
        MensajeError(mensaje = estado.errorClave)

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onEvent(SesionUiEvent.OnIniciarSesion) },
            enabled = estado.puedeIniciar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (estado.cargando) "Ingresando..." else "Iniciar sesión")
        }

        TextButton(onClick = alOlvidarClave) {
            Text(text = "Olvidé mi contraseña")
        }
        TextButton(onClick = alCrearCuenta) {
            Text(text = "Crear cuenta")
        }
    }
}

@Composable
private fun MensajeError(mensaje: String?) {
    AnimatedVisibility(
        visible = mensaje != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Text(
            text = mensaje ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp)
        )
    }
}

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun LoginVacioPreview() {
    TriskeleduTheme {
        Surface {
            LoginContent(
                estado = SesionUiState(),
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
                    errorClave = "Ingresa tu contraseña"
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
                    mensajeError = "Correo o contraseña incorrectos"
                ),
                onEvent = {},
                alOlvidarClave = {},
                alCrearCuenta = {}
            )
        }
    }
}
