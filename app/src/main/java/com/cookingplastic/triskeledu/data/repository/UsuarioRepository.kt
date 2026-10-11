package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Usuario

class UsuarioRepository{
    private var usuario = Usuario("Simoninos", 154, 0)
    suspend fun getUsuario(): Usuario {
        return usuario
    }

    suspend fun guardarUsuario(usuarioActualizado: Usuario){
        usuario = usuarioActualizado
    }
}
