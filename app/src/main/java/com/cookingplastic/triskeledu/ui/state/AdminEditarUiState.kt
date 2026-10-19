package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import java.util.UUID

//Lo que muestra el formulario del administrador
data class AdminEditarUiState(
    //el id se crea al abrir el formulario y el QR lo usa como contenido
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    //null hasta que se elige uno del menú
    val tipo: TipoPuntoInteres? = null,
    val descripcion: String = "",
    //las coordenadas se escriben como texto y pasan a Double al guardar
    val latitud: String = "",
    val longitud: String = "",
    val altitud: String = "",
    val descubreMas: String = "",
    //dirección de la foto elegida (content://...), null si no hay
    val imagenUri: String? = null,
    //null significa sin video asociado
    val capsulaId: String? = null,
    //las cápsulas para el menú, vienen del repositorio
    val capsulas: List<Capsula> = emptyList(),
    //null hasta que se aprieta "Generar QR"
    val codigoQr: String? = null,
    val activo: Boolean = false,
    //un error por cada campo que tiene regla en Validaciones
    val errorNombre: String? = null,
    val errorTipo: String? = null,
    val errorDescripcion: String? = null,
    val errorLatitud: String? = null,
    val errorLongitud: String? = null,
    val errorAltitud: String? = null,
    val errorDescubreMas: String? = null,
    val mensajeError: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
) {
    //obligatorios: nombre, tipo y las 3 coordenadas. Lo opcional, si se llena, también tiene que estar bien
    val puedeGuardar: Boolean
        get() = nombre.isNotBlank() && tipo != null &&
                latitud.isNotBlank() && longitud.isNotBlank() && altitud.isNotBlank() &&
                errorNombre == null && errorTipo == null && errorDescripcion == null &&
                errorLatitud == null && errorLongitud == null && errorAltitud == null &&
                errorDescubreMas == null && !guardando
}
