package com.cookingplastic.triskeledu.ui.components

import android.graphics.Bitmap
import android.graphics.Color
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

//ZXing entrega una matriz de sí/no; cada "sí" es un cuadradito negro
private fun generarQr(contenido: String, tamanoPx: Int): ImageBitmap {
    val matriz = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx)
    val pixeles = IntArray(tamanoPx * tamanoPx) { i ->
        val x = i % tamanoPx
        val y = i / tamanoPx
        //excepción a "colores solo del tema": un QR necesita negro sobre blanco para que las cámaras lo lean
        if (matriz[x, y]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixeles, tamanoPx, tamanoPx, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Preview
@Composable
private fun CodigoQrPreview() {
    CodigoQr(contenido = "punto-de-prueba", modifier = Modifier.size(160.dp))
}
