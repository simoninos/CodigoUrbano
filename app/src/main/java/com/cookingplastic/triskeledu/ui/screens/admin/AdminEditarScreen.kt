package com.cookingplastic.triskeledu.ui.screens.admin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.ui.components.CodigoQr
import com.cookingplastic.triskeledu.ui.components.Interruptor
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.AdminEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminEditarUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.AdminEditarViewModel
import java.io.File

//se conecta al ViewModel, abre galería, cámara y permisos, y avisa cuando hay que volver
@Composable
fun AdminEditarScreen(
    alVolver: () -> Unit,
    viewModel: AdminEditarViewModel = viewModel(factory = AdminEditarViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    //cuando se guarda, vuelve a la pantalla anterior una sola vez
    LaunchedEffect(estado.guardado) {
        if (estado.guardado) alVolver()
    }

    val contexto = LocalContext.current

    //Photo Picker: abre la galería del sistema y devuelve la foto elegida, o null si cancela
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            //sin esto, el acceso a la foto se pierde al cerrar la app o reiniciar el teléfono
            runCatching {
                contexto.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.onEvent(AdminEditarUiEvent.OnImagenElegida(uri.toString()))
        }
    }

    //rememberSaveable: si Android cierra la pantalla mientras la cámara está abierta, no se pierde a dónde va la foto
    var fotoPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        val uri = fotoPendiente
        if (guardada && uri != null) viewModel.onEvent(AdminEditarUiEvent.OnImagenElegida(uri.toString()))
    }
    val abrirCamara: () -> Unit = {
        val uri = crearArchivoFoto(contexto)
        fotoPendiente = uri
        tomarFoto.launch(uri)
    }
    //como el manifest declara CAMERA, Android exige pedir el permiso antes de abrir la cámara
    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else viewModel.onEvent(AdminEditarUiEvent.OnPermisoCamaraDenegado)
    }

    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuestas ->
        if (respuestas.values.any { it }) viewModel.onEvent(AdminEditarUiEvent.OnUsarMiUbicacion)
        else viewModel.onEvent(AdminEditarUiEvent.OnPermisoUbicacionDenegado)
    }

    AdminEditarContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        //ImageOnly: solo muestra fotos, no videos
        alElegirFoto = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        alTomarFoto = {
            if (tienePermiso(contexto, Manifest.permission.CAMERA)) abrirCamara()
            else pedirCamara.launch(Manifest.permission.CAMERA)
        },
        alUsarUbicacion = {
            if (tienePermiso(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ||
                tienePermiso(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                viewModel.onEvent(AdminEditarUiEvent.OnUsarMiUbicacion)
            } else {
                pedirUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        alVolver = alVolver
    )
}

private fun tienePermiso(contexto: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

//la cámara necesita un archivo donde dejar la foto; FileProvider le da acceso solo a ese archivo
private fun crearArchivoFoto(contexto: Context): Uri {
    val carpeta = File(contexto.filesDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "punto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
}

//solo dibuja; galería, cámara y permisos quedan en la Screen porque necesitan la Activity
@Composable
fun AdminEditarContent(
    estado: AdminEditarUiState,
    onEvent: (AdminEditarUiEvent) -> Unit,
    alElegirFoto: () -> Unit,
    alTomarFoto: () -> Unit,
    alUsarUbicacion: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = if (estado.esEdicion) "Editar punto" else "Nuevo punto",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        CampoConError(
            valor = estado.nombre,
            alCambiar = { onEvent(AdminEditarUiEvent.OnNombreChange(it)) },
            etiqueta = "Nombre",
            error = estado.errorNombre
        )
        MenuDesplegable(
            etiqueta = "Tipo",
            valor = estado.tipo?.descripcion ?: "",
            opciones = TipoPuntoInteres.entries.map { it.descripcion },
            alElegir = { indice -> onEvent(AdminEditarUiEvent.OnTipoChange(TipoPuntoInteres.entries[indice])) },
            error = estado.errorTipo
        )
        CampoConError(
            valor = estado.descripcion,
            alCambiar = { onEvent(AdminEditarUiEvent.OnDescripcionChange(it)) },
            etiqueta = "Descripción (opcional)",
            error = estado.errorDescripcion,
            lineas = 3
        )

        TituloSeccion(texto = "Imagen")
        if (estado.imagenUri != null) {
            //Coil carga la foto desde su dirección y la recorta para llenar el espacio
            AsyncImage(
                model = estado.imagenUri,
                contentDescription = "Imagen del punto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = alElegirFoto, modifier = Modifier.weight(1f)) {
                Text(text = "Galería")
            }
            OutlinedButton(onClick = alTomarFoto, modifier = Modifier.weight(1f)) {
                Text(text = "Cámara")
            }
        }
        if (estado.imagenUri != null) {
            TextButton(onClick = { onEvent(AdminEditarUiEvent.OnQuitarImagen) }) {
                Text(text = "Quitar foto")
            }
        }
        MensajeError(mensaje = estado.avisoImagen)

        Spacer(modifier = Modifier.height(16.dp))
        TituloSeccion(texto = "Ubicación")
        OutlinedButton(
            onClick = alUsarUbicacion,
            enabled = !estado.buscandoUbicacion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (estado.buscandoUbicacion) "Buscando ubicación..." else "Usar mi ubicación actual")
        }
        MensajeError(mensaje = estado.avisoUbicacion)
        Spacer(modifier = Modifier.height(8.dp))
        //Text y no Decimal: algunos teclados numéricos no traen el signo menos, y en Santiago latitud y longitud son negativas
        CampoConError(
            valor = estado.latitud,
            alCambiar = { onEvent(AdminEditarUiEvent.OnLatitudChange(it)) },
            etiqueta = "Latitud",
            error = estado.errorLatitud
        )
        CampoConError(
            valor = estado.longitud,
            alCambiar = { onEvent(AdminEditarUiEvent.OnLongitudChange(it)) },
            etiqueta = "Longitud",
            error = estado.errorLongitud
        )
        CampoConError(
            valor = estado.altitud,
            alCambiar = { onEvent(AdminEditarUiEvent.OnAltitudChange(it)) },
            etiqueta = "Altitud (metros)",
            error = estado.errorAltitud,
            teclado = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(8.dp))
        TituloSeccion(texto = "Más información")
        CampoConError(
            valor = estado.descubreMas,
            alCambiar = { onEvent(AdminEditarUiEvent.OnDescubreMasChange(it)) },
            etiqueta = "Enlace \"Descubre más\" (opcional)",
            error = estado.errorDescubreMas,
            teclado = KeyboardType.Uri
        )
        //la primera opción es "Sin video"; por eso el índice de las cápsulas va corrido en 1
        MenuDesplegable(
            etiqueta = "Video asociado",
            valor = estado.capsulas.find { it.id == estado.capsulaId }?.titulo ?: "Sin video",
            opciones = listOf("Sin video") + estado.capsulas.map { it.titulo },
            alElegir = { indice ->
                onEvent(AdminEditarUiEvent.OnCapsulaChange(if (indice == 0) null else estado.capsulas[indice - 1].id))
            },
            error = null
        )

        TituloSeccion(texto = "Código QR")
        if (estado.codigoQr != null) {
            CodigoQr(
                contenido = estado.codigoQr,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            OutlinedButton(
                onClick = { onEvent(AdminEditarUiEvent.OnGenerarQr) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Generar QR")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        //aquí se usa el Interruptor de la meta 6, como pedía el plan
        Interruptor(
            titulo = "Activo",
            marcado = estado.activo,
            alCambiar = { onEvent(AdminEditarUiEvent.OnActivoChange(it)) },
            descripcion = "Si está activo, aparece en el mapa"
        )

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onEvent(AdminEditarUiEvent.OnGuardar) },
            enabled = estado.puedeGuardar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (estado.guardando) "Guardando..." else "Guardar")
        }
        TextButton(
            onClick = alVolver,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Cancelar")
        }
    }
}

//un campo con su error animado debajo; se repite 7 veces, por eso va en una función
@Composable
private fun CampoConError(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    MensajeError(mensaje = error)
    Spacer(modifier = Modifier.height(8.dp))
}

//@OptIn porque el menú desplegable de Material todavía está marcado como experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuDesplegable(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    alElegir: (Int) -> Unit,
    error: String?
) {
    //abierto o cerrado solo le importa a la pantalla, por eso es remember y no va al ViewModel
    var abierto by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it }
    ) {
        //readOnly: no se escribe, solo muestra lo elegido
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            opciones.forEachIndexed { indice, opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        alElegir(indice)
                        abierto = false
                    }
                )
            }
        }
    }
    MensajeError(mensaje = error)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun AdminEditarVacioPreview() {
    TriskeleduTheme {
        Surface {
            AdminEditarContent(
                estado = AdminEditarUiState(),
                onEvent = {},
                alElegirFoto = {},
                alTomarFoto = {},
                alUsarUbicacion = {},
                alVolver = {}
            )
        }
    }
}

@Preview(name = "Con errores", showBackground = true)
@Composable
private fun AdminEditarErroresPreview() {
    TriskeleduTheme {
        Surface {
            AdminEditarContent(
                estado = AdminEditarUiState(
                    nombre = "La",
                    latitud = "-91",
                    altitud = "alto",
                    descubreMas = "http://ejemplo.cl",
                    errorNombre = "El nombre es obligatorio",
                    errorTipo = "Elige un tipo",
                    errorLatitud = "Latitud fuera de rango",
                    errorAltitud = "La altitud debe ser un número",
                    errorDescubreMas = "Ingresa un enlace válido"
                ),
                onEvent = {},
                alElegirFoto = {},
                alTomarFoto = {},
                alUsarUbicacion = {},
                alVolver = {}
            )
        }
    }
}

@Preview(name = "Completo (noche)", showBackground = true)
@Composable
private fun AdminEditarCompletoNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            AdminEditarContent(
                estado = AdminEditarUiState(
                    id = "punto-la-moneda",
                    esEdicion = true,
                    nombre = "La Moneda",
                    tipo = TipoPuntoInteres.MONUMENTO,
                    latitud = "-33.4429",
                    longitud = "-70.6540",
                    altitud = "567",
                    capsulas = listOf(Capsula(id = "capsula-1", titulo = "Historia de La Moneda", urlStream = "")),
                    capsulaId = "capsula-1",
                    codigoQr = "punto-la-moneda",
                    activo = true
                ),
                onEvent = {},
                alElegirFoto = {},
                alTomarFoto = {},
                alUsarUbicacion = {},
                alVolver = {}
            )
        }
    }
}
