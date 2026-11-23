package com.cookingplastic.triskeledu.ui.screens.admin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.ui.components.BotonEnviar
import com.cookingplastic.triskeledu.ui.components.CodigoQr
import com.cookingplastic.triskeledu.ui.components.EnfocarPrimerError
import com.cookingplastic.triskeledu.ui.components.MapaSimple
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.components.SelectorUbicacionMapa
import com.cookingplastic.triskeledu.ui.components.guardarQrPng
import com.cookingplastic.triskeledu.ui.event.AdminEditarUiEvent
import com.cookingplastic.triskeledu.ui.state.AdminEditarUiState
import com.cookingplastic.triskeledu.ui.state.CampoPunto
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.AdminEditarViewModel
import java.io.File

//se conecta al ViewModel, abre galería, cámara, permisos y el selector de ubicación, y avisa cuando hay que volver
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
        //ADM-08: el PNG se guarda en el teléfono para imprimirlo
        alDescargarQr = { codigo -> viewModel.onEvent(AdminEditarUiEvent.OnQrDescargado(guardarQrPng(contexto, codigo))) },
        alVolver = alVolver
    )

    //P19: el selector de ubicación se abre encima del formulario
    estado.selectorUbicacion?.let { centro ->
        SelectorUbicacionMapa(
            latitudInicial = centro.latitud,
            longitudInicial = centro.longitud,
            alCancelar = { viewModel.onEvent(AdminEditarUiEvent.OnCerrarSelectorUbicacion) },
            alElegir = { latitud, longitud -> viewModel.onEvent(AdminEditarUiEvent.OnUbicacionElegida(latitud, longitud)) }
        )
    }
}

private fun tienePermiso(contexto: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

//la cámara necesita un archivo donde dejar la foto; FileProvider le da acceso solo a ese archivo
private fun crearArchivoFoto(contexto: Context): Uri {
    val carpeta = File(contexto.filesDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "punto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
}

//P10 · solo dibuja; galería, cámara, permisos y archivos quedan en la Screen porque necesitan la Activity
@Composable
fun AdminEditarContent(
    estado: AdminEditarUiState,
    onEvent: (AdminEditarUiEvent) -> Unit,
    alElegirFoto: () -> Unit,
    alTomarFoto: () -> Unit,
    alUsarUbicacion: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
    alDescargarQr: (String) -> Unit = {},
    mostrarMapa: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        //RN-18: un FocusRequester por campo, para llevar el foco al primero con error
        val focos = remember { CampoPunto.entries.associateWith { FocusRequester() } }
        EnfocarPrimerError(intento = estado.intentoEnvio, campo = estado.campoConError, focos = focos)

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(
                text = if (estado.esEdicion) "Editar punto" else "Crear punto",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
        }
        //después de tocar el botón con errores, un aviso arriba dice cuántos campos revisar
        if (estado.intentoEnvio > 0 && estado.camposConError > 0) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (estado.camposConError == 1) "Revisa el campo marcado" else "Revisa los ${estado.camposConError} campos marcados",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        CampoConError(
            valor = estado.nombre,
            alCambiar = { onEvent(AdminEditarUiEvent.OnNombreChange(it)) },
            etiqueta = "Nombre",
            error = estado.errorNombre,
            modifier = Modifier.focusRequester(focos.getValue(CampoPunto.NOMBRE))
        )
        MenuDesplegable(
            etiqueta = "Tipo",
            valor = estado.tipo?.descripcion ?: "",
            opciones = TipoPuntoInteres.entries.map { it.descripcion },
            alElegir = { indice -> onEvent(AdminEditarUiEvent.OnTipoChange(TipoPuntoInteres.entries[indice])) },
            error = estado.errorTipo,
            modifier = Modifier.focusRequester(focos.getValue(CampoPunto.TIPO))
        )
        CampoConError(
            valor = estado.descripcion,
            alCambiar = { onEvent(AdminEditarUiEvent.OnDescripcionChange(it)) },
            etiqueta = "Descripción (opcional)",
            error = estado.errorDescripcion,
            lineas = 3,
            modifier = Modifier.focusRequester(focos.getValue(CampoPunto.DESCRIPCION))
        )

        //ADM-02: imagen obligatoria, a ancho completo y de 112 dp de alto, como se verá en el detalle
        TituloSeccion(texto = "Imagen del punto")
        val bordeImagen = if (estado.errorImagen != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(2.dp, bordeImagen), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (estado.imagenUri != null) {
                //Coil carga la foto desde su dirección y la recorta para llenar el espacio
                AsyncImage(
                    model = estado.imagenUri,
                    contentDescription = "Imagen del punto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Sin imagen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        MensajeError(mensaje = estado.errorImagen ?: estado.avisoImagen)
        Text(
            "Así se verá en el detalle del punto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = alTomarFoto,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).focusRequester(focos.getValue(CampoPunto.IMAGEN))
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Cámara")
            }
            OutlinedButton(
                onClick = alElegirFoto,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Galería")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        //ADM-03: mini mapa con "Elegir en el mapa" (P19), coordenadas a mano y "Usar mi ubicación actual"
        TituloSeccion(texto = "Ubicación")
        MiniMapaUbicacion(estado = estado, mostrarMapa = mostrarMapa, alAbrirSelector = { onEvent(AdminEditarUiEvent.OnAbrirSelectorUbicacion) })
        Spacer(modifier = Modifier.height(8.dp))
        //Text y no Decimal: algunos teclados numéricos no traen el signo menos, y en Santiago latitud y longitud son negativas
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoCoordenada(
                valor = estado.latitud,
                alCambiar = { onEvent(AdminEditarUiEvent.OnLatitudChange(it)) },
                etiqueta = "Lat",
                hayError = estado.errorLatitud != null || estado.errorUbicacion != null,
                modifier = Modifier.weight(1f).focusRequester(focos.getValue(CampoPunto.LATITUD))
            )
            CampoCoordenada(
                valor = estado.longitud,
                alCambiar = { onEvent(AdminEditarUiEvent.OnLongitudChange(it)) },
                etiqueta = "Lon",
                hayError = estado.errorLongitud != null || estado.errorUbicacion != null,
                modifier = Modifier.weight(1f).focusRequester(focos.getValue(CampoPunto.LONGITUD))
            )
            CampoCoordenada(
                valor = estado.altitud,
                alCambiar = { onEvent(AdminEditarUiEvent.OnAltitudChange(it)) },
                etiqueta = "Alt (m)",
                hayError = estado.errorAltitud != null,
                teclado = KeyboardType.Decimal,
                modifier = Modifier.weight(0.8f).focusRequester(focos.getValue(CampoPunto.ALTITUD))
            )
        }
        MensajeError(mensaje = estado.errorUbicacion ?: estado.errorLatitud ?: estado.errorLongitud ?: estado.errorAltitud)
        TextButton(onClick = alUsarUbicacion, enabled = !estado.buscandoUbicacion, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (estado.buscandoUbicacion) "Buscando ubicación..." else "Usar mi ubicación actual · escribir a mano")
        }
        MensajeError(mensaje = estado.avisoUbicacion)

        Spacer(modifier = Modifier.height(8.dp))
        CampoConError(
            valor = estado.descubreMas,
            alCambiar = { onEvent(AdminEditarUiEvent.OnDescubreMasChange(it)) },
            etiqueta = "Enlace \"Descubre más\" (opcional)",
            error = estado.errorDescubreMas,
            teclado = KeyboardType.Uri,
            modifier = Modifier.focusRequester(focos.getValue(CampoPunto.DESCUBRE_MAS))
        )

        //ADM-08: el QR se genera al guardar; al editar se muestra con "Descargar QR (PNG)"
        TituloSeccion(texto = "Código QR")
        SeccionQr(codigo = estado.codigoQr, aviso = estado.avisoQr, alDescargar = alDescargarQr)

        Spacer(modifier = Modifier.height(16.dp))
        //ADM-07: solo cápsulas publicadas y libres; la primera opción es "Sin cápsula", por eso el índice va corrido en 1
        MenuDesplegable(
            etiqueta = "Cápsula (opcional)",
            valor = estado.capsulas.find { it.id == estado.capsulaId }?.titulo ?: "Sin cápsula",
            opciones = listOf("Sin cápsula") + estado.capsulas.map { it.titulo },
            alElegir = { indice ->
                onEvent(AdminEditarUiEvent.OnCapsulaChange(if (indice == 0) null else estado.capsulas[indice - 1].id))
            },
            error = null,
            apoyo = "Solo aparecen cápsulas publicadas y libres"
        )

        TarjetaActivo(estado = estado, alCambiar = { onEvent(AdminEditarUiEvent.OnActivoChange(it)) })

        MensajeError(mensaje = estado.mensajeError)

        Spacer(modifier = Modifier.height(16.dp))
        //RN-18: siempre responde; con errores se ve atenuado y al tocarlo los muestra todos
        BotonEnviar(
            texto = if (estado.esEdicion) "Guardar punto" else "Crear punto",
            alPresionar = { onEvent(AdminEditarUiEvent.OnGuardar) },
            atenuado = !estado.puedeGuardar,
            cargando = estado.guardando,
            textoCargando = "Guardando..."
        )
        TextButton(
            onClick = alVolver,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)
        ) {
            Text(text = "Cancelar")
        }
    }
}

//Mini mapa del lugar elegido. Es solo para mirar: un toque (o el botón) abre P19
@Composable
private fun MiniMapaUbicacion(estado: AdminEditarUiState, mostrarMapa: Boolean, alAbrirSelector: () -> Unit) {
    val latitud = estado.latitud.toDoubleOrNull()?.takeIf { estado.errorLatitud == null }
    val longitud = estado.longitud.toDoubleOrNull()?.takeIf { estado.errorLongitud == null }
    val borde = if (estado.errorUbicacion != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(2.dp, borde), RoundedCornerShape(16.dp))
    ) {
        if (latitud != null && longitud != null && mostrarMapa) {
            MapaSimple(latitud = latitud, longitud = longitud, modifier = Modifier.fillMaxSize())
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).size(36.dp).padding(bottom = 18.dp)
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {}
        }
        //capa encima del mapa: toma el toque para abrir P19 y deja que el formulario se desplace
        Box(modifier = Modifier.fillMaxSize().clickable(onClickLabel = "Elegir en el mapa", onClick = alAbrirSelector))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elegir en el mapa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

//ADM-08: antes de guardar solo explica; después muestra el QR y permite descargarlo
@Composable
private fun SeccionQr(codigo: String?, aviso: String?, alDescargar: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        if (codigo == null) {
            Text(
                text = "Se genera solo al guardar el punto. Después podrás descargarlo desde “Editar”.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CodigoQr(contenido = codigo, modifier = Modifier.size(96.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(codigo, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Se generó al guardar el punto. Imprímelo y ponlo en el lugar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = { alDescargar(codigo) },
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar QR (PNG)")
                }
                aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

//RN-14 y RN-22: sin cápsula el punto queda Sin cápsula y "Activo" no se puede encender
@Composable
private fun TarjetaActivo(estado: AdminEditarUiState, alCambiar: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (estado.puedeActivar) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Activo", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = when {
                        !estado.puedeActivar -> "Quedará en Sin cápsula. Se puede activar al asociar una cápsula publicada"
                        estado.activo -> "Visible en el mapa para todos"
                        else -> "Completo, oculto del mapa hasta activarlo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = estado.activo, onCheckedChange = alCambiar, enabled = estado.puedeActivar)
        }
    }
}

//un campo con su error animado debajo; se repite varias veces, por eso va en una función
@Composable
private fun CampoConError(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth()
    )
    MensajeError(mensaje = error)
    Spacer(modifier = Modifier.height(8.dp))
}

//latitud, longitud y altitud van juntas en una fila; el error se muestra una sola vez debajo
@Composable
private fun CampoCoordenada(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    hayError: Boolean,
    modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = hayError,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
        modifier = modifier
    )
}

//@OptIn porque el menú desplegable de Material todavía está marcado como experimental
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuDesplegable(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    alElegir: (Int) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    apoyo: String? = null
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
            modifier = modifier
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
    apoyo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
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

@Preview(name = "Crear punto con errores", showBackground = true)
@Composable
private fun AdminEditarErroresPreview() {
    TriskeleduTheme {
        Surface {
            AdminEditarContent(
                estado = AdminEditarUiState(
                    intentoEnvio = 1,
                    errorNombre = "El nombre es obligatorio",
                    errorTipo = "Elige un tipo",
                    errorImagen = "Agrega una imagen del punto",
                    errorUbicacion = "Elige la ubicación del punto"
                ),
                onEvent = {},
                alElegirFoto = {},
                alTomarFoto = {},
                alUsarUbicacion = {},
                alVolver = {},
                mostrarMapa = false
            )
        }
    }
}

@Preview(name = "Editar punto (noche)", showBackground = true)
@Composable
private fun AdminEditarCompletoNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            AdminEditarContent(
                estado = AdminEditarUiState(
                    id = DatosPrueba.PUNTO_LA_MONEDA.id,
                    esEdicion = true,
                    nombre = "La Moneda",
                    tipo = TipoPuntoInteres.MONUMENTO,
                    latitud = "-33.4429",
                    longitud = "-70.6540",
                    altitud = "567",
                    capsulas = listOf(DatosPrueba.CAPSULA_LA_MONEDA),
                    capsulaId = DatosPrueba.CAPSULA_LA_MONEDA.id,
                    codigoQr = "TRISKEL-PI-001",
                    activo = true
                ),
                onEvent = {},
                alElegirFoto = {},
                alTomarFoto = {},
                alUsarUbicacion = {},
                alVolver = {},
                mostrarMapa = false
            )
        }
    }
}
