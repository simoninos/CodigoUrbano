package com.cookingplastic.triskeledu.ui.screens.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//P01: "Servidor: http://…" bajo el acceso rápido. Al tocarlo se escribe la IP del computador con el backend
@Composable
fun AjusteServidor(viewModel: ServidorViewModel = viewModel(factory = ServidorViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AjusteServidorContent(estado = estado, onEvent = viewModel::onEvent)
}

@Composable
fun AjusteServidorContent(estado: ServidorUiState, onEvent: (ServidorUiEvent) -> Unit) {
    TextButton(onClick = { onEvent(ServidorUiEvent.OnAbrir) }) {
        Text(
            text = "Servidor: ${estado.actual}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (!estado.dialogoAbierto) return

    AlertDialog(
        onDismissRequest = { onEvent(ServidorUiEvent.OnCerrar) },
        title = { Text("Servidor") },
        text = {
            Column {
                Text(
                    text = "La dirección del API Gateway. Con el celular como punto de acceso, escribe la IP del " +
                        "computador en esa red (en el computador: ipconfig). Sin puerto se usa el 8080.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.texto,
                    onValueChange = { onEvent(ServidorUiEvent.OnTextoChange(it)) },
                    label = { Text("Dirección") },
                    placeholder = { Text("192.168.43.10") },
                    singleLine = true,
                    isError = estado.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onEvent(ServidorUiEvent.OnGuardar) })
                )
                MensajeError(mensaje = estado.error)
                when {
                    estado.probando -> Text("Probando la conexión…", style = MaterialTheme.typography.bodyMedium)
                    estado.responde == true -> Text(
                        "Guardado. El servidor responde",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    //se guardó igual, por si el backend se está encendiendo
                    estado.responde == false -> MensajeError(
                        mensaje = "Guardado, pero no responde. Revisa que el computador esté en la red del celular, " +
                            "que el backend esté encendido y que el firewall deje pasar el puerto 8080"
                    )
                }
                TextButton(onClick = { onEvent(ServidorUiEvent.OnRestablecer) }) {
                    Text("Usar la del emulador (${estado.deFabrica})")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(ServidorUiEvent.OnGuardar) }, enabled = !estado.probando) {
                Text("Guardar y probar")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ServidorUiEvent.OnCerrar) }) { Text("Cerrar") }
        }
    )
}

@Preview(name = "Diálogo del servidor", showBackground = true)
@Composable
private fun AjusteServidorPreview() {
    TriskeleduTheme {
        Surface {
            AjusteServidorContent(
                estado = ServidorUiState(
                    actual = "http://10.0.2.2:8080", deFabrica = "http://10.0.2.2:8080",
                    dialogoAbierto = true, texto = "192.168.43.10", responde = true
                ),
                onEvent = {}
            )
        }
    }
}
