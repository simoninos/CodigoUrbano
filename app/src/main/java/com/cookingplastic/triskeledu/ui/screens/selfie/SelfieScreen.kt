package com.cookingplastic.triskeledu.ui.screens.selfie

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.ui.components.MensajeError
import com.cookingplastic.triskeledu.ui.event.SelfieUiEvent
import com.cookingplastic.triskeledu.ui.state.SelfieUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.SelfieViewModel
import kotlinx.coroutines.delay
import java.io.File

//P13 · Selfie (ACT-03): cámara frontal con CameraX. La foto se guarda en el almacenamiento privado de la
//app (files/selfies), nunca en la galería. Sin permiso explica para qué es la cámara (EXC-02)
@Composable
fun SelfieScreen(
    alVolver: () -> Unit,
    viewModel: SelfieViewModel = viewModel(factory = SelfieViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val actividad = contexto as? Activity
    var preguntado by rememberSaveable { mutableStateOf(false) }

    //revisa el permiso; "bloqueado" = ya se preguntó y Android ya no muestra el diálogo
    fun informarPermiso() {
        val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val bloqueado = !concedido && preguntado && actividad != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.CAMERA)
        viewModel.onEvent(SelfieUiEvent.OnPermisoCamara(concedido, bloqueado))
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { informarPermiso() }

    //al volver desde Ajustes se revisa de nuevo
    val ciclo = LocalLifecycleOwner.current
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento -> if (evento == Lifecycle.Event.ON_RESUME) informarPermiso() }
        ciclo.lifecycle.addObserver(observador)
        onDispose { ciclo.lifecycle.removeObserver(observador) }
    }
    //la primera vez se pide solo
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && !preguntado) {
            preguntado = true
            pedirPermiso.launch(Manifest.permission.CAMERA)
        }
    }
    //con la selfie guardada se muestra el aviso un momento y se vuelve al detalle
    LaunchedEffect(estado.puntosGanados) {
        if (estado.puntosGanados != null) {
            delay(1500)
            alVolver()
        }
    }

    //la cámara siempre se ve oscura, como en el mockup
    TriskeleduTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            SelfieContent(
                estado = estado,
                onEvent = viewModel::onEvent,
                alVolver = alVolver,
                alPedirPermiso = {
                    preguntado = true
                    pedirPermiso.launch(Manifest.permission.CAMERA)
                },
                alAbrirAjustes = {
                    contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)))
                }
            )
        }
    }
}

@Composable
fun SelfieContent(
    estado: SelfieUiState,
    onEvent: (SelfieUiEvent) -> Unit,
    alVolver: () -> Unit,
    alPedirPermiso: () -> Unit,
    alAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarCamara: Boolean = true
) {
    val contexto = LocalContext.current
    //el caso de uso de CameraX que toma la foto; lo crea la vista previa
    val captura = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    Box(modifier = modifier.fillMaxSize()) {
        val puedeTomar = estado.permisoCamara == true && estado.desbloqueado && !estado.yaTomada
        if (puedeTomar && mostrarCamara) {
            VistaPreviaCamara(captura = captura, frontal = estado.camaraFrontal, alFallar = { onEvent(SelfieUiEvent.OnErrorCamara) }, modifier = Modifier.fillMaxSize())
            //guía ovalada para la cara; el lugar tiene que verse detrás
            val colorGuia = MaterialTheme.colorScheme.onBackground
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 170.dp)) {
                drawOval(color = colorGuia, style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }
        }

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (estado.nombrePunto.isBlank()) "Selfie" else "Selfie en ${estado.nombrePunto}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            if (puedeTomar) {
                Spacer(modifier = Modifier.padding(6.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Que se vea el lugar detrás de ti",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            when {
                estado.cargando -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                estado.puntosGanados != null -> Aviso(
                    icono = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) },
                    titulo = "¡Selfie guardada!",
                    texto = "Ganaste +${estado.puntosGanados} puntos. Queda solo en tu teléfono."
                )
                !estado.desbloqueado -> Aviso(titulo = SelfieViewModel.MENSAJE_BLOQUEADO, texto = null)
                estado.yaTomada -> Aviso(titulo = "Ya subiste tu selfie en este lugar", texto = "Los $PUNTOS_SELFIE puntos ya están sumados.")
                //EXC-02: sin permiso se explica para qué es la cámara y se ofrece darlo o abrir los ajustes
                estado.permisoCamara == false -> Aviso(
                    icono = { Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp)) },
                    titulo = "Necesitamos la cámara",
                    texto = "La usamos solo para tu selfie, la evidencia de tu visita. La foto queda en tu teléfono y no se publica.",
                    boton = if (estado.permisoBloqueado) "Abrir ajustes" else "Dar permiso",
                    alPresionar = if (estado.permisoBloqueado) alAbrirAjustes else alPedirPermiso
                )
                else -> PieCamara(
                    estado = estado,
                    alTomar = {
                        onEvent(SelfieUiEvent.OnTomandoFoto)
                        tomarFoto(contexto, captura, { onEvent(SelfieUiEvent.OnFotoGuardada(it)) }, { onEvent(SelfieUiEvent.OnErrorCamara) })
                    },
                    alCambiar = { onEvent(SelfieUiEvent.OnCambiarCamara) }
                )
            }
            MensajeError(mensaje = estado.mensajeError)
        }
    }
}

//Obturador, cambio de cámara y la nota de privacidad
@Composable
private fun PieCamara(estado: SelfieUiState, alTomar: () -> Unit, alCambiar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                onClick = alTomar,
                enabled = !estado.guardando,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(5.dp, MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (estado.guardando) CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    else Icon(Icons.Outlined.CameraAlt, contentDescription = "Tomar selfie")
                }
            }
            Surface(
                onClick = alCambiar,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = if (estado.camaraFrontal) "Usar la cámara trasera" else "Usar la cámara frontal")
                }
            }
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tu selfie es privada y suma +$PUNTOS_SELFIE puntos como evidencia de tu visita.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Aviso(
    titulo: String,
    texto: String?,
    icono: (@Composable () -> Unit)? = null,
    boton: String? = null,
    alPresionar: () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            icono?.invoke()
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            texto?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (boton != null) {
                Button(onClick = alPresionar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(boton) }
            }
        }
    }
}

//La vista previa de CameraX con la cámara frontal (o la trasera si se cambia o no hay frontal)
@Composable
private fun VistaPreviaCamara(captura: ImageCapture, frontal: Boolean, alFallar: () -> Unit, modifier: Modifier = Modifier) {
    val ciclo = LocalLifecycleOwner.current
    var proveedor by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    DisposableEffect(Unit) { onDispose { proveedor?.unbindAll() } }
    val vistaPrevia = remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }.also { vistaPrevia.value = it }
        }
    )
    //se vuelve a conectar al cambiar de cámara
    val contexto = LocalContext.current
    LaunchedEffect(frontal, vistaPrevia.value) {
        val vista = vistaPrevia.value ?: return@LaunchedEffect
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener({
            try {
                val camaras = futuro.get()
                proveedor = camaras
                val preview = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
                val selector = if (frontal && camaras.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
                camaras.unbindAll()
                camaras.bindToLifecycle(ciclo, selector, preview, captura)
            } catch (e: Exception) {
                alFallar()
            }
        }, ContextCompat.getMainExecutor(contexto))
    }
}

//ACT-03: la foto queda en files/selfies, el almacenamiento privado de la app (no aparece en la galería)
private fun tomarFoto(contexto: Context, captura: ImageCapture, alGuardar: (String) -> Unit, alFallar: () -> Unit) {
    val carpeta = File(contexto.filesDir, "selfies").apply { mkdirs() }
    val archivo = File(carpeta, "selfie_${System.currentTimeMillis()}.jpg")
    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
    captura.takePicture(opciones, ContextCompat.getMainExecutor(contexto), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(resultado: ImageCapture.OutputFileResults) = alGuardar(Uri.fromFile(archivo).toString())
        override fun onError(error: ImageCaptureException) = alFallar()
    })
}
