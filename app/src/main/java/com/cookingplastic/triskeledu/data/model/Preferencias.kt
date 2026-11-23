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
    val notifNuevasCapsulas: Boolean = false,
    val seguirUbicacion: Boolean = true,
    val mostrarBloqueados: Boolean = true,
    //estas tres solo las usa el administrador (P08-Admin)
    val notifSolicitudes: Boolean = true,
    val notifPuntosListos: Boolean = true,
    val mostrarInactivos: Boolean = true,
    val tema: TemaApp = TemaApp.CLASICO,
    //RN-33: el nivel más alto que alcanzó; los temas desbloqueados no se pierden aunque el nivel baje
    val nivelMaximoAlcanzado: Int = 1
)
