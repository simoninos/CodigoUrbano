package com.cookingplastic.triskeledu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//Antes era privado dentro de LoginScreen. Ahora lo comparten Login, Registro y después Admin
@Composable
fun MensajeError(
    mensaje: String?,
    modifier: Modifier = Modifier
) {
    //aparece creciendo hacia abajo y se va encogiendo, como pide el plan
    AnimatedVisibility(
        visible = mensaje != null,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Text(
            //si es null usa "": en la animación de salida el mensaje ya no existe
            text = mensaje ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp)
        )
    }
}
