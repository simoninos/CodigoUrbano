package com.cookingplastic.triskeledu.data.model

import androidx.room.ForeignKey
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferencias", foreignKeys= [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)])
data class Preferencias(
    @PrimaryKey
    val usuarioId: String,
    val temaNoche: Boolean = false,
    val notifLugaresCercanos: Boolean = true,
    val notifNuevosVideos: Boolean = false,
    val seguirUbicacion: Boolean = true,
    val mostrarBloqueados: Boolean = true
)
