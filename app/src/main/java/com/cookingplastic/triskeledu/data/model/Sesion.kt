package com.cookingplastic.triskeledu.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sesiones")
data class Sesion(
        //Valor fijo y escritura por defecto, así cuando Room busque la reemplaza
    @PrimaryKey
    val id: Int = 1,
        //Así, cuando recién tengan la app o cierren sesión, así el sistema detecta que no hay nadie conectado
    val usuarioId: String? = null
)
