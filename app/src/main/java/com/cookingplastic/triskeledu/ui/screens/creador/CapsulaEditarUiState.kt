package com.cookingplastic.triskeledu.ui.screens.creador

import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.domain.rules.ValidacionCapsula

//Campos de P18 en el orden de la pantalla (RN-18: el foco va al primero con error)
enum class CampoCapsula { TITULO, DESCRIPCION, CATEGORIA, ENLACE, DURACION }

//CRE-06: guardar cambios de una cápsula que ya existía y eliminar un borrador piden confirmación
enum class ConfirmacionCapsula { GUARDAR, ELIMINAR }

//P18 · Crear o editar cápsula (CRE-02, CRE-06)
data class CapsulaEditarUiState(
    //null mientras es nueva; se llena al guardarla por primera vez
    val capsulaId: String? = null,
    val estadoCapsula: EstadoCapsula = EstadoCapsula.BORRADOR,
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: CategoriaCapsula? = null,
    val enlace: String = "",
    //en segundos, como texto para poder escribirlo
    val duracion: String = "",
    val errorTitulo: String? = null,
    val errorDescripcion: String? = null,
    val errorCategoria: String? = null,
    val errorEnlace: String? = null,
    val errorDuracion: String? = null,
    val intentoEnvio: Int = 0,
    val campoConError: CampoCapsula? = null,
    val confirmacion: ConfirmacionCapsula? = null,
    val guardando: Boolean = false,
    //guardada o eliminada: la pantalla vuelve atrás
    val terminada: Boolean = false,
    //"Probar video": la cápsula ya se guardó y se abre P18-B con este id
    val abrirVistaPrevia: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false
) {
    val esNueva: Boolean
        get() = capsulaId == null

    //CRE-06: solo un borrador que ya existe se puede eliminar
    val puedeEliminar: Boolean
        get() = !esNueva && estadoCapsula == EstadoCapsula.BORRADOR

    //RN-18: el botón se ve atenuado mientras falte algo
    val completa: Boolean
        get() = ValidacionCapsula.validarTitulo(titulo) == null &&
            ValidacionCapsula.validarDescripcion(descripcion) == null &&
            ValidacionCapsula.validarCategoria(categoria) == null &&
            ValidacionCapsula.validarEnlace(enlace) == null &&
            ValidacionCapsula.validarDuracion(duracion) == null
}
