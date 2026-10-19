package com.cookingplastic.triskeledu.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.RegistroUiEvent
import com.cookingplastic.triskeledu.ui.state.RegistroUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.RegistroViewModel

//igual que LoginScreen: se conecta al ViewModel y avisa cuando hay que navegar
@Composable
fun RegistroScreen(
    alRegistrarse: () -> Unit,
    alVolver: () -> Unit,
    viewModel: RegistroViewModel = viewModel(factory = RegistroViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    //cuando se crea la cuenta, navega una sola vez
    LaunchedEffect(estado.registrado) {
        if (estado.registrado) alRegistrarse()
    }

    RegistroContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        alVolver = alVolver
    )
}

//solo dibuja, por eso se puede ver en las previews sin ViewModel
@Composable
fun RegistroContent(
    estado: RegistroUiState,
    onEvent: (RegistroUiEvent) -> Unit,
    alVolver: () -> Unit,
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
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = estado.username,
            onValueChange = { onEvent(RegistroUiEvent.OnUsernameChange(it)) },
            label = { Text("Nombre de usuario") },
            singleLine = true,
            isError = estado.errorUsername != null,
            //teclado normal; "Siguiente" salta al correo
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        MensajeError(mensaje = estado.errorUsername)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = estado.correo,
            onValueChange = { onEvent(RegistroUiEvent.OnCorreoChange(it)) },
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
            onValueChange = { onEvent(RegistroUiEvent.OnClaveChange(it)) },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = estado.errorClave != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                //Next y no Done: todavía falta repetirla
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        MensajeError(mensaje = estado.errorClave)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = estado.repetirClave,
            onValueChange = { onEvent(RegistroUiEvent.OnRepetirClaveChange(it)) },
            label = { Text("Repetir contraseña") },
            singleLine = true,
            isError = estado.errorRepetirClave != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            //"Listo" intenta crear la cuenta; el ViewModel decide si se puede
            keyboardActions = KeyboardActions(onDone = { onEvent(RegistroUiEvent.OnRegistrar) }),
            modifier = Modifier.fillMaxWidth()
        )
        MensajeError(mensaje = estado.errorRepetirClave)

        Spacer(modifier = Modifier.height(8.dp))

        //mismo truco del Interruptor: toda la fila marca la casilla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = estado.aceptaTerminos,
                    //TalkBack lo lee como casilla, marcada o no
                    role = Role.Checkbox,
                    onValueChange = { onEvent(RegistroUiEvent.OnTerminosChange(it)) }
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = estado.aceptaTerminos,
                //null porque el toque lo maneja la fila
                onCheckedChange = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Acepto los términos y condiciones",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        MensajeError(mensaje = estado.errorTerminos)

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onEvent(RegistroUiEvent.OnRegistrar) },
            enabled = estado.puedeRegistrar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (estado.cargando) "Creando cuenta..." else "Crear cuenta")
        }

        TextButton(onClick = alVolver) {
            Text(text = "Ya tengo cuenta")
        }
    }
}

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun RegistroVacioPreview() {
    TriskeleduTheme {
        Surface {
            RegistroContent(
                estado = RegistroUiState(),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}

@Preview(name = "Con errores", showBackground = true)
@Composable
private fun RegistroErroresPreview() {
    TriskeleduTheme {
        Surface {
            RegistroContent(
                estado = RegistroUiState(
                    //"cliente_prueba" ya existe en el Fake, por eso el error de nombre
                    username = "cliente_prueba",
                    correo = "nuevo@triskeledu.cl",
                    clave = "Clave1234",
                    repetirClave = "Clave123",
                    errorUsername = "Ese nombre ya está en uso",
                    errorRepetirClave = "Las contraseñas no coinciden",
                    errorTerminos = "Debes aceptar los términos"
                ),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}

@Preview(name = "Listo para crear (noche)", showBackground = true)
@Composable
private fun RegistroListoNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            RegistroContent(
                estado = RegistroUiState(
                    username = "pingüino_23",
                    correo = "pinguino@triskeledu.cl",
                    clave = "Clave1234",
                    repetirClave = "Clave1234",
                    aceptaTerminos = true
                ),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}
