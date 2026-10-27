package com.cookingplastic.triskeledu.data

import android.content.Context
import com.cookingplastic.triskeledu.data.local.DatosPrueba
import com.cookingplastic.triskeledu.data.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.FakeComentarioRepository
import com.cookingplastic.triskeledu.data.repository.FakeProgresoRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository

//Una sola instancia del progreso y de los comentarios para toda la app: lo que se escanea en
//Escanear se ve al tiro en Mis lugares, el Mapa y el Detalle.
//Mismo patrón que RepositoriosCompartidos de Simon, mientras AppContainer no los registre.
//Reciben context porque la versión Room lo va a necesitar; así las pantallas no cambian después
object RepositoriosProgreso {

    private val progresoFake: ProgresoRepository by lazy { FakeProgresoRepository(DatosPrueba.progreso) }
    private val comentariosFake: ComentarioRepository by lazy { FakeComentarioRepository(DatosPrueba.comentarios) }

    fun progreso(context: Context): ProgresoRepository = progresoFake

    fun comentarios(context: Context): ComentarioRepository = comentariosFake
}