package com.cookingplastic.triskeledu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cookingplastic.triskeledu.ui.theme.TriskeleduTheme

//Con Composable se podrá dibujar en pantalla y reacciona a los cambios
@Composable
//Aquí se dibujará texto como switch y se usará una vez por cada Preferencia
fun Interruptor(
    titulo: String,
    marcado: Boolean,
    //Unit es parecido a un void, no devuelve nada pero recibe
    alCambiar: (Boolean) -> Unit,
    //Modifier vacío, que sirve para dar instrucciones de dimensiones
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    //pone de izquierda a derecha, pero con fillMaxWidth ocupa todo el ancho
    Row(
        modifier = modifier
            .fillMaxWidth()
            //fillMaxWidthhace que la fila vaya de borde a borde y toggleable hace que responda al toque
            .toggleable(
                value = marcado,
                //role switch le dice a TalkBack que es un interruptor, así lo lee como activado o desactivado
                role = Role.Switch,
                //este cambia el valor
                onValueChange = alCambiar
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //Distinto a row, que va de lado a lado, este va de arriba a bajo
        //weight(1f) hace que se expanda para ocupar todo el espacio disponible que sobre
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (descripcion != null) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        //Spacer sirve para espaciar.. si, suena obvio
        Spacer(modifier = Modifier.width(16.dp))
        //Aquí val prendido y apagado. Checked lo muestra.
        Switch(
            checked = marcado,
            //Aunque onCheckChange avisa al código del cambio, está en null debido a que toggleable se encarga del toque y la accesibilidad.
            //¿Por qué dejarlo?
            //1. Porque es obligatorio
            //2. No se desaprovecha tampoco ya que null es una instrucción. Si le pasas una función, es tocable, pero si no, solo muestra el estado.
            // Así no maneja toques y no hay dos toques en la misma fila
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

//Vamos a hacer simulaciones.. preview permite esa "foto prueba"
@Preview(name = "Día", showBackground = true)
@Composable
private fun InterruptorDiaPreview() {
    TriskeleduTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                //Son datos de prueba, no funcionan. Vienen basados de Preferencia
                Interruptor(titulo = "Seguir mi ubicación", marcado = true, alCambiar = {})
                Interruptor(
                    titulo = "Tema noche",
                    //marcado false significa que está el tema apagado
                    marcado = false,
                    alCambiar = {},
                    descripcion = "Colores oscuros para usar de noche"
                )
            }
        }
    }
}

@Preview(name = "Noche", showBackground = true)
@Composable
private fun InterruptorNochePreview() {
    TriskeleduTheme(darkTheme = true) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                Interruptor(titulo = "Seguir mi ubicación", marcado = true, alCambiar = {})
                Interruptor(
                    titulo = "Tema noche",
                    //aquí está en true, o sea, prendido
                    marcado = true,
                    alCambiar = {},
                    descripcion = "Colores oscuros para usar de noche"
                )
            }
        }
    }
}
