package com.cookingplastic.triskeledu.ui.screens.escanear

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookingplastic.triskeledu.ui.event.EscanearUiEvent
import com.cookingplastic.triskeledu.ui.state.EscanearUiState
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme
import com.cookingplastic.triskeledu.ui.viewmodels.EscanearViewModel
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
//Preview de CameraX se llama igual que la anotación @Preview de Compose: le damos otro nombre
import androidx.camera.core.Preview as CamaraPreview

//Pantalla 5: se conecta al ViewModel y pide el permiso de cámara
@Composable
fun EscanearScreen(
    viewModel: EscanearViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current

    //abre el diálogo del sistema "¿Permitir que la app tome fotos?" y entrega la respuesta
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        viewModel.onEvent(EscanearUiEvent.OnPermisoCamara(concedido))
    }

    //al entrar: si ya hay permiso se avisa; si no, se pide
    LaunchedEffect(Unit) {
        val yaConcedido = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (yaConcedido) viewModel.onEvent(EscanearUiEvent.OnPermisoCamara(true))
        else pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    EscanearContent(
        estado = estado,
        onEvent = viewModel::onEvent,
        onPedirPermiso = { pedirPermiso.launch(Manifest.permission.CAMERA) }
    )
}

//Solo dibuja: así el Preview funciona sin ViewModel ni cámara
@Composable
fun EscanearContent(
    estado: EscanearUiState,
    onEvent: (EscanearUiEvent) -> Unit,
    onPedirPermiso: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
    ) {
        //capa 1: la cámara (en el Preview de Android Studio no hay cámara, así que se salta)
        if (estado.permisoCamara == true && !LocalInspectionMode.current) {
            VistaCamara(
                onCodigoLeido = { onEvent(EscanearUiEvent.OnCodigoLeido(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }

        //capa 2: el marco del visor, al centro
        MarcoVisor(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
        )

        //capa 3: el título, arriba
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Text(
                text = "Escanea el QR del lugar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        //capa 4: la tarjeta de abajo cambia según el estado
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    estado.permisoCamara == false -> {
                        Text(
                            text = "Necesitamos la cámara para leer el código QR del lugar.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = onPedirPermiso, modifier = Modifier.fillMaxWidth()) {
                            Text("Dar permiso")
                        }
                    }

                    estado.codigoLeido != null -> {
                        Text(
                            text = "QR leído",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = estado.codigoLeido, style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { onEvent(EscanearUiEvent.OnEscanearOtro) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Escanear otro")
                        }
                    }

                    else -> AvisoDistancia()
                }
            }
        }
    }
}

//"Debes estar a 60 m o menos...", como en el mockup
@Composable
private fun AvisoDistancia() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            text = "Debes estar a 60 m o menos del lugar para desbloquearlo. Funciona sin internet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

//Las cuatro esquinas del visor (Agua) y la línea de lectura al medio (Puntos)
@Composable
private fun MarcoVisor(modifier: Modifier = Modifier) {
    val colorEsquinas = MaterialTheme.colorScheme.tertiary
    val colorLinea = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val largo = size.minDimension * 0.22f
        val grosor = 6.dp.toPx()

        //cada esquina son dos líneas que salen del mismo punto; dx y dy dicen hacia dónde
        fun esquina(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(colorEsquinas, Offset(x, y), Offset(x + dx * largo, y), grosor, StrokeCap.Round)
            drawLine(colorEsquinas, Offset(x, y), Offset(x, y + dy * largo), grosor, StrokeCap.Round)
        }
        esquina(0f, 0f, 1f, 1f)                    //arriba a la izquierda
        esquina(size.width, 0f, -1f, 1f)           //arriba a la derecha
        esquina(0f, size.height, 1f, -1f)          //abajo a la izquierda
        esquina(size.width, size.height, -1f, -1f) //abajo a la derecha

        drawLine(
            colorLinea,
            Offset(size.width * 0.15f, size.height / 2),
            Offset(size.width * 0.85f, size.height / 2),
            4.dp.toPx(),
            StrokeCap.Round
        )
    }
}

//La cámara de verdad: CameraX muestra la imagen y ML Kit busca códigos QR en cada cuadro
@Composable
private fun VistaCamara(
    onCodigoLeido: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    //siempre la versión más nueva de la función, aunque la pantalla se redibuje
    val alLeer by rememberUpdatedState(onCodigoLeido)

    //el lector solo busca QR. El modelo viene dentro de la app: funciona sin internet
    val lector = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    //un hilo aparte para analizar imágenes sin trabar la pantalla
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    var proveedorCamara by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    //al salir de la pantalla se apaga la cámara y se liberan el lector y el hilo
    DisposableEffect(Unit) {
        onDispose {
            proveedorCamara?.unbindAll()
            lector.close()
            ejecutor.shutdown()
        }
    }

    //AndroidView permite usar una vista clásica de Android (PreviewView) dentro de Compose
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val vistaPrevia = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val futuro = ProcessCameraProvider.getInstance(ctx)
            futuro.addListener({
                val proveedor = futuro.get()
                proveedorCamara = proveedor

                //caso de uso 1: mostrar la imagen en pantalla
                val preview = CamaraPreview.Builder().build().also {
                    it.setSurfaceProvider(vistaPrevia.surfaceProvider)
                }
                //caso de uso 2: analizar cada cuadro. Si ML Kit va lento, se salta cuadros viejos
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analisis.setAnalyzer(ejecutor) { imagen ->
                    analizarCuadro(imagen, lector) { codigo -> alLeer(codigo) }
                }

                //la cámara trasera se prende y se apaga sola junto con la pantalla
                proveedor.unbindAll()
                proveedor.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analisis
                )
            }, ContextCompat.getMainExecutor(ctx))
            vistaPrevia
        }
    )
}

//Pasa un cuadro de la cámara a ML Kit y avisa si encontró un QR
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analizarCuadro(
    imagen: ImageProxy,
    lector: BarcodeScanner,
    alEncontrar: (String) -> Unit
) {
    val mediaImage = imagen.image
    if (mediaImage == null) {
        imagen.close()
        return
    }
    val entrada = InputImage.fromMediaImage(mediaImage, imagen.imageInfo.rotationDegrees)
    lector.process(entrada)
        .addOnSuccessListener { codigos ->
            codigos.firstOrNull()?.rawValue?.let(alEncontrar)
        }
        //siempre hay que cerrar el cuadro; si no, la cámara deja de mandar cuadros nuevos
        .addOnCompleteListener { imagen.close() }
}

@Preview(showBackground = true)
@Composable
fun EscanearBuscandoPreview() {
    TriskeleduTheme {
        EscanearContent(estado = EscanearUiState(permisoCamara = true), onEvent = {}, onPedirPermiso = {})
    }
}

@Preview(showBackground = true)
@Composable
fun EscanearLeidoPreview() {
    TriskeleduTheme {
        EscanearContent(
            estado = EscanearUiState(permisoCamara = true, codigoLeido = "TRISKELEDU-LA-MONEDA"),
            onEvent = {},
            onPedirPermiso = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EscanearSinPermisoPreview() {
    TriskeleduTheme {
        EscanearContent(estado = EscanearUiState(permisoCamara = false), onEvent = {}, onPedirPermiso = {})
    }
}