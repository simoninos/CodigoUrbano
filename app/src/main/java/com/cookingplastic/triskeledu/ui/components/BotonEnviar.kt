package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//RN-18: el botón de enviar de todos los formularios. Siempre responde al toque; con errores solo se ve
//atenuado y el ViewModel muestra los errores. EXC-09: mientras envía muestra que carga y no acepta otro toque
@Composable
fun BotonEnviar(
    texto: String,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    atenuado: Boolean = false,
    cargando: Boolean = false,
    textoCargando: String = texto
) {
    Button(
        onClick = { if (!cargando) alPresionar() },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (atenuado && !cargando) 0.5f else 1f)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = if (cargando) textoCargando else texto)
    }
}

//Lleva el foco al campo con error cada vez que se intenta enviar (intento cambia en cada toque).
//focos tiene el FocusRequester de cada campo que se puede enfocar
@Composable
fun <C> EnfocarPrimerError(intento: Int, campo: C?, focos: Map<C, FocusRequester>) {
    LaunchedEffect(intento) {
        if (intento > 0) campo?.let { focos[it]?.requestFocus() }
    }
}

@Preview(showBackground = true)
@Composable
private fun BotonEnviarPreview() {
    TriskeleduTheme {
        androidx.compose.foundation.layout.Column {
            BotonEnviar(texto = "Crear cuenta", alPresionar = {})
            BotonEnviar(texto = "Crear cuenta", alPresionar = {}, atenuado = true)
            BotonEnviar(texto = "Crear cuenta", alPresionar = {}, cargando = true, textoCargando = "Creando cuenta...")
        }
    }
}
