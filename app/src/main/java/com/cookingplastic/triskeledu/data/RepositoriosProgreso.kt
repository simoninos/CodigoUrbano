package com.cookingplastic.triskeledu.data

import android.content.Context
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository

//Entrega el progreso y los comentarios guardados en Room, que crea AppContainer.
//Es una sola instancia para toda la app: lo que se escanea en Escanear se ve al tiro en
//Mis lugares, el Mapa y el Detalle, y sigue ahí al cerrar y abrir la app
object RepositoriosProgreso {

    private fun contenedor(context: Context) = (context.applicationContext as TriskeleduApp).container

    fun progreso(context: Context): ProgresoRepository = contenedor(context).progresoRepository

    fun comentarios(context: Context): ComentarioRepository = contenedor(context).comentarioRepository
}
