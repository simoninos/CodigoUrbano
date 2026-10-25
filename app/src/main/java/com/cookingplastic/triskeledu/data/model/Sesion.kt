package com.cookingplastic.triskeledu.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

//La llave foránea impide guardar un usuarioId que no exista. SET_NULL: si se borra el usuario,
//la sesión queda en null ("nadie conectado") y la fila 1 sigue existiendo.
//El índice lo pide Room para no recorrer la tabla entera cuando cambian los usuarios
@Entity(
    tableName = "sesiones",
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index(value = ["usuarioId"])]
)
data class Sesion(
        //Valor fijo y escritura por defecto, así cuando Room busque la reemplaza
    @PrimaryKey
    val id: Int = 1,
        //Así, cuando recién tengan la app o cierren sesión, así el sistema detecta que no hay nadie conectado
    val usuarioId: String? = null
)
