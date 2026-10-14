package com.cookingplastic.triskeledu.data.model

import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "capsulas")
data class Capsula(
@PrimaryKey
val id: String = UUID.randomUUID().toString(),
val titulo: String,
val urlStream: String,
val categoria: String? = null,
val duracionSegundos: Int? = null
)
