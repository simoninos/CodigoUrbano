package com.cookingplastic.triskeledu.ui.screens.creador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.ui.components.BotonEnviar
import com.cookingplastic.triskeledu.ui.components.EnfocarPrimerError
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.CapsulaEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.CampoCapsula
import com.cookingplastic.triskeledu.ui.state.CapsulaEditarUiState
import com.cookingplastic.triskeledu.ui.state.ConfirmacionCapsula
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.validation.ValidacionCapsula
import com.cookingplastic.triskeledu.ui.viewmodels.CapsulaEditarViewModel

//P18 · Crear o editar cápsula (CRE-02, CRE-06)
@Composable
fun CapsulaEditarScreen(
    alVolver: () -> Unit,
    alProbarVideo: (String) -> Unit,
    viewModel: CapsulaEditarViewModel = viewModel(factory = CapsulaEditarViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.terminada) { if (estado.terminada) alVolver() }
    //"Probar video" ya guardó el borrador: se abre P18-B una sola vez
    LaunchedEffect(estado.abrirVistaPrevia) {
        estado.abrirVistaPrevia?.let {
            viewModel.onEvent(CapsulaEditarUiEvent.OnVistaPreviaAbierta)
            alProbarVideo(it)
        }
    }
    CapsulaEditarContent(estado = estado, onEvent = viewModel::onEvent, alVolver = alVolver)
}

@Composable
fun CapsulaEditarContent(
    estado: CapsulaEditarUiState,
    onEvent: (CapsulaEditarUiEvent) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    //RN-18: el foco va al primer campo con error cada vez que se toca "Guardar" o "Probar video"
    val focos = remember { CampoCapsula.entries.associateWith { FocusRequester() } }
    EnfocarPrimerError(intento = estado.intentoEnvio, campo = estado.campoConError, focos = focos)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CabeceraCreador(titulo = if (estado.esNueva) "Nueva cápsula" else "Editar cápsula", alVolver = alVolver) {
            EtiquetaEstadoCapsula(estado.estadoCapsula)
        }
        if (estado.cargando) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            return@Column
        }
        Spacer(modifier = Modifier.padding(4.dp))

        OutlinedTextField(
            value = estado.titulo,
            onValueChange = { onEvent(CapsulaEditarUiEvent.OnTituloChange(it)) },
            label = { Text("Título") },
            singleLine = true,
            isError = estado.errorTitulo != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoCapsula.TITULO))
        )
        MensajeError(mensaje = estado.errorTitulo)

        OutlinedTextField(
            value = estado.descripcion,
            onValueChange = { onEvent(CapsulaEditarUiEvent.OnDescripcionChange(it)) },
            label = { Text("Descripción breve (opcional)") },
            minLines = 3,
            isError = estado.errorDescripcion != null,
            supportingText = { Text("${estado.descripcion.trim().length}/${ValidacionCapsula.DESCRIPCION_MAXIMA}") },
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoCapsula.DESCRIPCION))
        )
        MensajeError(mensaje = estado.errorDescripcion)

        SelectorCategoria(
            categoria = estado.categoria,
            hayError = estado.errorCategoria != null,
            alElegir = { onEvent(CapsulaEditarUiEvent.OnCategoriaChange(it)) },
            modifier = Modifier.focusRequester(focos.getValue(CampoCapsula.CATEGORIA))
        )
        MensajeError(mensaje = estado.errorCategoria)

        OutlinedTextField(
            value = estado.enlace,
            onValueChange = { onEvent(CapsulaEditarUiEvent.OnEnlaceChange(it)) },
            label = { Text("Enlace de Google Drive") },
            placeholder = { Text("drive.google.com/file/d/…") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            singleLine = true,
            isError = estado.errorEnlace != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoCapsula.ENLACE))
        )
        //la ayuda queda siempre del mismo color; el error va aparte, debajo
        Text(
            text = "Compártelo como “Cualquier persona con el enlace”. Video vertical de 3 minutos como máximo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        MensajeError(mensaje = estado.errorEnlace)

        OutlinedTextField(
            value = estado.duracion,
            onValueChange = { onEvent(CapsulaEditarUiEvent.OnDuracionChange(it)) },
            label = { Text("Duración en segundos (1 a 180)") },
            singleLine = true,
            isError = estado.errorDuracion != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().focusRequester(focos.getValue(CampoCapsula.DURACION))
        )
        MensajeError(mensaje = estado.errorDuracion)

        Spacer(modifier = Modifier.padding(4.dp))
        //CRE-03: guarda el borrador y abre la vista previa, desde donde se publica
        OutlinedButton(
            onClick = { onEvent(CapsulaEditarUiEvent.OnProbarVideo) },
            enabled = !estado.guardando,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Probar video")
        }
        Spacer(modifier = Modifier.padding(2.dp))
        BotonEnviar(
            texto = "Guardar",
            alPresionar = { onEvent(CapsulaEditarUiEvent.OnGuardar) },
            atenuado = !estado.completa,
            cargando = estado.guardando,
            textoCargando = "Guardando..."
        )
        MensajeError(mensaje = estado.mensajeError)
        if (estado.puedeEliminar) {
            TextButton(onClick = { onEvent(CapsulaEditarUiEvent.OnEliminar) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Eliminar borrador", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    when (estado.confirmacion) {
        ConfirmacionCapsula.GUARDAR -> Confirmar(
            titulo = "¿Guardar los cambios?",
            texto = "Se actualizará “${estado.titulo.trim()}”.",
            accion = "Guardar",
            peligrosa = false,
            onEvent = onEvent
        )
        ConfirmacionCapsula.ELIMINAR -> Confirmar(
            titulo = "¿Eliminar este borrador?",
            texto = "“${estado.titulo.trim()}” se borrará y no se puede deshacer.",
            accion = "Eliminar",
            peligrosa = true,
            onEvent = onEvent
        )
        null -> Unit
    }
}

//RN-25: la lista cerrada de categorías
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoria(
    categoria: CategoriaCapsula?,
    hayError: Boolean,
    alElegir: (CategoriaCapsula) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = categoria?.etiqueta ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            placeholder = { Text("Elige una categoría") },
            isError = hayError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            CategoriaCapsula.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    onClick = {
                        alElegir(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Confirmar(titulo: String, texto: String, accion: String, peligrosa: Boolean, onEvent: (CapsulaEditarUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(CapsulaEditarUiEvent.OnCancelarConfirmacion) },
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = { onEvent(CapsulaEditarUiEvent.OnConfirmar) }) {
                Text(accion, color = if (peligrosa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = { onEvent(CapsulaEditarUiEvent.OnCancelarConfirmacion) }) { Text("Cancelar") } }
    )
}

@Preview(name = "Nueva cápsula con errores", showBackground = true)
@Composable
private fun CapsulaEditarPreview() {
    TriskeleduTheme {
        Surface {
            CapsulaEditarContent(
                estado = CapsulaEditarUiState(
                    titulo = "Algoritmos en el Mercado",
                    descripcion = "Cómo una app recomienda qué comprar.",
                    categoria = CategoriaCapsula.IA_VIDA_DIARIA,
                    enlace = "drive.google.com/folders/1aB",
                    errorEnlace = "Pega un enlace de Google Drive",
                    errorDuracion = "Ingresa la duración en segundos"
                ),
                onEvent = {},
                alVolver = {}
            )
        }
    }
}
