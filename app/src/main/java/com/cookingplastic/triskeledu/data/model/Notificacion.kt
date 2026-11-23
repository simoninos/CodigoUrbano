package com.cookingplastic.triskeledu.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

//Historial de notificaciones locales (PER-03). Queda guardada aunque se descarte la del sistema.
//CASCADE: si se borra el usuario, se borran sus notificaciones. El índice ordena el historial rápido
@Entity(
    tableName = "notificaciones",
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["usuarioId", "creadaEn"])]
)
data class Notificacion(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val usuarioId: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val texto: String,
    //lo que se abre al tocarla; null si no abre nada
    val puntoId: String? = null,
    val capsulaId: String? = null,
    val creadaEn: Long = System.currentTimeMillis(),
    val leida: Boolean = false
)
