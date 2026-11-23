package com.cookingplastic.triskeledu.ui.state

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import java.util.UUID

//P10 · Crear o editar punto (ADM-02 a ADM-08)
data class AdminEditarUiState(
    //el id se crea al abrir el formulario
    val id: String = UUID.randomUUID().toString(),
    //true cuando se abrió un punto que ya existe (la ruta trae su id)
    val esEdicion: Boolean = false,
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
    //ADM-07: las cápsulas del menú: PUBLICADAS y libres, de cualquier creador, más la que ya tiene el punto
    val capsulas: List<Capsula> = emptyList(),
    //ADM-08: null en un punto nuevo; el QR se genera solo al guardarlo
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
    //v3: la imagen es obligatoria (ADM-02)
    val errorImagen: String? = null,
    //ADM-03: sin latitud ni longitud se pide elegir la ubicación
    val errorUbicacion: String? = null,
    //P19 abierto, centrado aquí; null = cerrado
    val selectorUbicacion: Ubicacion? = null,
    //resultado de "Descargar QR (PNG)"
    val avisoQr: String? = null,
    val mensajeError: String? = null,
    //avisos bajo la imagen y bajo "Usar mi ubicación" (permiso negado o GPS sin respuesta)
    val avisoImagen: String? = null,
    val avisoUbicacion: String? = null,
    val buscandoUbicacion: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    //RN-18: cambia en cada toque con errores, para llevar el foco al primero
    val intentoEnvio: Int = 0,
    val campoConError: CampoPunto? = null
) {
    //cuántos campos marcados hay después de tocar el botón: "Revisa los N campos marcados"
    val camposConError: Int
        get() = listOf(errorNombre, errorTipo, errorDescripcion, errorImagen, errorUbicacion, errorLatitud,
            errorLongitud, errorAltitud, errorDescubreMas).count { it != null }

    //RN-14: solo se puede encender "Activo" con una cápsula elegida
    val puedeActivar: Boolean
        get() = capsulaId != null

    //obligatorios: nombre, tipo, imagen y las 3 coordenadas. Lo opcional, si se llena, también tiene que estar bien.
    //Ya no deshabilita el botón: solo decide si se ve atenuado (RN-18)
    val puedeGuardar: Boolean
        get() = nombre.isNotBlank() && tipo != null && imagenUri != null &&
                latitud.isNotBlank() && longitud.isNotBlank() && altitud.isNotBlank() &&
                errorNombre == null && errorTipo == null && errorDescripcion == null &&
                errorLatitud == null && errorLongitud == null && errorAltitud == null &&
                errorDescubreMas == null && !guardando
}

//en el orden en que aparecen en la pantalla
enum class CampoPunto { NOMBRE, TIPO, DESCRIPCION, IMAGEN, LATITUD, LONGITUD, ALTITUD, DESCUBRE_MAS }
