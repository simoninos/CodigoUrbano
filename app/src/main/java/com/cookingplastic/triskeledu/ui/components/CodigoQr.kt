package com.cookingplastic.triskeledu.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

//Dibuja un código QR (con ZXing) a partir de un texto
@Composable
fun CodigoQr(
    contenido: String,
    modifier: Modifier = Modifier,
    tamanoPx: Int = 512
) {
    //remember: el QR se calcula solo cuando cambia el contenido, no en cada redibujo
    val imagen = remember(contenido, tamanoPx) { generarQr(contenido, tamanoPx) }
    Image(
        bitmap = imagen,
        contentDescription = "Código QR del punto",
        modifier = modifier
    )
}

private fun generarQr(contenido: String, tamanoPx: Int): ImageBitmap = crearBitmapQr(contenido, tamanoPx).asImageBitmap()

//ZXing entrega una matriz de sí/no; cada "sí" es un cuadradito negro. También lo usa "Descargar QR"
fun crearBitmapQr(contenido: String, tamanoPx: Int): Bitmap {
    val matriz = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx)
    val pixeles = IntArray(tamanoPx * tamanoPx) { i ->
        val x = i % tamanoPx
        val y = i / tamanoPx
        //excepción a "colores solo del tema": un QR necesita negro sobre blanco para que las cámaras lo lean
        if (matriz[x, y]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixeles, tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888)
}

//ADM-08: "Descargar QR (PNG)". Desde Android 10 queda en Descargas/Triskeledu sin pedir permisos;
//en Android 8 y 9, en la carpeta de descargas de la app. Devuelve dónde quedó, o null si falló
fun guardarQrPng(contexto: Context, contenido: String): String? {
    val nombre = "$contenido.png"
    val bitmap = crearBitmapQr(contenido, 1024)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val datos = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Triskeledu")
            }
            val uri = contexto.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, datos) ?: return null
            contexto.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return null
            "Descargas/Triskeledu/$nombre"
        } else {
            val carpeta = contexto.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
            val archivo = File(carpeta, nombre)
            FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            archivo.absolutePath
        }
    } catch (e: Exception) {
        null
    }
}

@Preview
@Composable
private fun CodigoQrPreview() {
    CodigoQr(contenido = "punto-de-prueba", modifier = Modifier.size(160.dp))
}
