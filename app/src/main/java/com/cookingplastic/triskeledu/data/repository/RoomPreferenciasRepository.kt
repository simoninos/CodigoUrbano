package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.local.dao.PreferenciasDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.Preferencias
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

//Las preferencias son del usuario conectado; por eso también recibe el DAO de usuarios
class RoomPreferenciasRepository(
    private val preferenciasDao: PreferenciasDao,
    private val usuarioDao: UsuarioDao
) : PreferenciasRepository {

    //flatMapLatest: cada vez que cambia el usuario conectado, deja de escuchar las preferencias
    //del anterior y empieza a escuchar las del nuevo
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun preferencias(): Flow<Preferencias> =
        usuarioDao.usuarioActual().flatMapLatest { usuario ->
            if (usuario == null) {
                //sin sesión: valores por defecto (por eso el login se ve de día)
                flowOf(Preferencias(usuarioId = ""))
            } else {
                //si todavía no guardó nada, también valen los valores por defecto, pero con su id
                preferenciasDao.preferenciasDe(usuario.id).map { it ?: Preferencias(usuarioId = usuario.id) }
            }
        }

    override suspend fun guardar(preferencias: Preferencias) {
        //sin usuario no hay a quién guardarle; la llave foránea además lo rechazaría
        if (preferencias.usuarioId.isBlank()) return
        preferenciasDao.guardar(preferencias)
    }
}
