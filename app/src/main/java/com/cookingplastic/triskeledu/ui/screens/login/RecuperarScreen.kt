package com.cookingplastic.triskeledu.ui.screens.login

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionSimulada
import com.cookingplastic.triskeledu.ui.components.BotonEnviar
import com.cookingplastic.triskeledu.ui.components.EnfocarPrimerError
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//P02b (ACC-05, SIM-01): el código llega como notificación local. La pantalla pide el permiso antes de enviarlo
@Composable
fun RecuperarScreen(
    alVolver: () -> Unit,
    alIrAlLogin: () -> Unit,
    viewModel: RecuperarViewModel = viewModel(factory = RecuperarViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        viewModel.onEvent(RecuperarUiEvent.OnPermisoRespondido(concedido))
    }

    //el correo ya está bien: se pide el permiso (si falta) y después se envía el código
    LaunchedEffect(estado.pedirPermiso) {
        if (!estado.pedirPermiso) return@LaunchedEffect
        val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (faltaPermiso) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.onEvent(RecuperarUiEvent.OnPermisoRespondido(NotificationManagerCompat.from(contexto).areNotificationsEnabled()))
    }

    RecuperarContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver, alIrAlLogin = alIrAlLogin)
}

@Composable
fun RecuperarContent(
    estado: RecuperarUiState,
    onEvent: (RecuperarUiEvent) -> Unit,
    alVolver: () -> Unit,
    alIrAlLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focos = remember { CampoRecuperar.entries.associateWith { FocusRequester() } }
    EnfocarPrimerError(intento = estado.intentoEnvio, campo = estado.campoConError, focos = focos)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
            Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            estado.listo -> ClaveCambiada(alIrAlLogin = alIrAlLogin)
            !estado.codigoEnviado -> {
                Text(
                    text = "Escribe el correo de tu cuenta y te enviaremos un código de 6 dígitos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = estado.correo,
                    onValueChange = { onEvent(RecuperarUiEvent.OnCorreoChange(it)) },
                    label = { Text("Correo") },
                    singleLine = true,
                    isError = estado.errorCorreo != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoRecuperar.CORREO))
                )
                MensajeError(mensaje = estado.errorCorreo)
                MensajeError(mensaje = estado.mensaje)
                Spacer(modifier = Modifier.height(24.dp))
                BotonEnviar(
                    texto = "Enviar código",
                    alPresionar = { onEvent(RecuperarUiEvent.OnEnviarCodigo) },
                    atenuado = !estado.puedeEnviarCodigo,
                    cargando = estado.cargando || estado.pedirPermiso,
                    textoCargando = "Enviando..."
                )
            }
            else -> {
                Text(
                    text = buildAnnotatedString {
                        append("Si el correo está registrado, te enviamos un código de 6 dígitos a ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(estado.correo.trim().lowercase()) }
                        append(". Vence en ${RecuperacionSimulada.MINUTOS_VIGENCIA} minutos.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Código", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                CampoCodigo(
                    valor = estado.codigo,
                    alCambiar = { onEvent(RecuperarUiEvent.OnCodigoChange(it)) },
                    conError = estado.errorCodigo != null,
                    modifier = Modifier.focusRequester(focos.getValue(CampoRecuperar.CODIGO))
                )
                MensajeError(mensaje = estado.errorCodigo)
                TextButton(onClick = { onEvent(RecuperarUiEvent.OnReenviarCodigo) }, enabled = !estado.cargando) {
                    Text("Reenviar código")
                }
                MensajeError(mensaje = estado.mensaje)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = estado.claveNueva,
                    onValueChange = { onEvent(RecuperarUiEvent.OnClaveNuevaChange(it)) },
                    label = { Text("Nueva contraseña") },
                    supportingText = { if (estado.errorClave == null) Text("Mínimo 8 caracteres, con letras y números") },
                    singleLine = true,
                    isError = estado.errorClave != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoRecuperar.CLAVE_NUEVA))
                )
                MensajeError(mensaje = estado.errorClave)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = estado.repetirClave,
                    onValueChange = { onEvent(RecuperarUiEvent.OnRepetirClaveChange(it)) },
                    label = { Text("Repetir nueva contraseña") },
                    singleLine = true,
                    isError = estado.errorRepetirClave != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoRecuperar.REPETIR_CLAVE))
                )
                MensajeError(mensaje = estado.errorRepetirClave)
                Spacer(modifier = Modifier.height(24.dp))
                BotonEnviar(
                    texto = "Cambiar contraseña",
                    alPresionar = { onEvent(RecuperarUiEvent.OnCambiarClave) },
                    atenuado = !estado.puedeCambiarClave,
                    cargando = estado.cargando,
                    textoCargando = "Cambiando..."
                )
            }
        }
    }
}

//Seis casillas para el código, como en el mockup. Por dentro es un solo campo de texto
@Composable
private fun CampoCodigo(valor: String, alCambiar: (String) -> Unit, conError: Boolean, modifier: Modifier = Modifier) {
    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Código de 6 dígitos" },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(6) { i ->
                    val digito = valor.getOrNull(i)
                    val borde = when {
                        conError -> MaterialTheme.colorScheme.error
                        digito != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, borde),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = digito?.toString() ?: "", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ClaveCambiada(alIrAlLogin: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(64.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Tu contraseña se cambió", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Ya puedes iniciar sesión con la contraseña nueva.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonEnviar(texto = "Ir a iniciar sesión", alPresionar = alIrAlLogin)
    }
}

@Preview(name = "Paso 1", showBackground = true)
@Composable
private fun RecuperarCorreoPreview() {
    TriskeleduTheme { Surface { RecuperarContent(estado = RecuperarUiState(), onEvent = {}, alVolver = {}, alIrAlLogin = {}) } }
}

@Preview(name = "Paso 2", showBackground = true, heightDp = 800)
@Composable
private fun RecuperarCodigoPreview() {
    TriskeleduTheme {
        Surface {
            RecuperarContent(
                estado = RecuperarUiState(correo = "camila.explora@ejemplo.cl", codigoEnviado = true, codigo = "4817"),
                onEvent = {},
                alVolver = {},
                alIrAlLogin = {}
            )
        }
    }
}
