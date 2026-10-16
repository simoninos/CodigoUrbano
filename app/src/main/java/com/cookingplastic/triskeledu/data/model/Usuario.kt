package com.cookingplastic.triskeledu.data.model

import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "usuarios", indices = [Index(value = ["username"], unique = true), Index(value= ["correo"], unique = true)])
data class Usuario(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val correo: String,
        //Se utiliza una clave hash + un valor clave sal que es aleatorio, para evitar que una misma contraseña tenga el mismo hash.
    val claveHash: String,
    val claveSal: String,
        //El rol queda predeterminado a cliente, evitando así la creación accidental de un admin
    val rol: RolUsuario = RolUsuario.CLIENTE,
    val terminosVersion: String,
        //Terminos aceptados queda sin valor predeterminado evitando así un falso positivo
    val terminosAceptadosEn: Long,
    val creadoEn: Long = System.currentTimeMillis()
)
