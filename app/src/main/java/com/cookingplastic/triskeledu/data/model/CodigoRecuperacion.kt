package com.cookingplastic.triskeledu.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

//Código de 6 dígitos para recuperar la contraseña, simulado en el teléfono (SIM-01).
//Una fila por usuario: un código nuevo reemplaza al anterior. Se guarda con hash y sal, como la contraseña
@Entity(
    tableName = "codigos_recuperacion",
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)]
)
data class CodigoRecuperacion(
    @PrimaryKey
    val usuarioId: String,
    val codigoHash: String,
    val codigoSal: String,
    val creadoEn: Long = System.currentTimeMillis(),
    //vence a los 15 minutos y sirve una sola vez
    val venceEn: Long,
    val usado: Boolean = false
)
