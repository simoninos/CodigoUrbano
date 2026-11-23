package com.cookingplastic.triskeledu.ui.screens.login

import android.Manifest
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//P23 (ACC-07, RN-29): la cuenta pidió ser creador y espera la respuesta del administrador.
//No tiene UiState: solo pide el permiso de notificaciones, para avisar apenas respondan
@Composable
fun SolicitudRevisionScreen(alVolverAlInicio: () -> Unit) {
    val contexto = LocalContext.current
    var activadas by remember { mutableStateOf(NotificationManagerCompat.from(contexto).areNotificationsEnabled()) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        activadas = concedido
    }
    SolicitudRevisionContent(
        notificacionesActivadas = activadas,
        //Android 13 o superior pide el permiso; antes vienen activadas por defecto
        alActivarNotificaciones = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        alVolverAlInicio = alVolverAlInicio
    )
}

@Composable
fun SolicitudRevisionContent(
    notificacionesActivadas: Boolean,
    alActivarNotificaciones: () -> Unit,
    alVolverAlInicio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                IconButton(onClick = alVolverAlInicio) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inicio")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu solicitud está en revisión",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                append("Pediste ser ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("creador de contenido") }
                append(". Un administrador la revisará y mientras tanto tu cuenta queda en espera.")
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        //sugiere activar las notificaciones (ACC-07)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text("Activa las notificaciones", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Así te avisamos apenas respondan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                if (notificacionesActivadas) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Notificaciones activadas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Button(onClick = alActivarNotificaciones, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Activar notificaciones")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Punto("Si la aprueban, entras como creador.")
            Punto("Si no, tu cuenta pasa a cliente y puedes jugar.")
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = alVolverAlInicio, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Volver al inicio")
        }
    }
}

@Composable
private fun Punto(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SolicitudRevisionPreview() {
    TriskeleduTheme {
        Surface { SolicitudRevisionContent(notificacionesActivadas = false, alActivarNotificaciones = {}, alVolverAlInicio = {}) }
    }
}
