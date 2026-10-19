package com.cookingplastic.triskeledu.data.local

import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import androidx.room.TypeConverter

class Converters{
    //Se usa TypeConverter para que la base de datos pueda entender el tipoPuntoInteresATexto
    //Se usa 2 funciones: entrada y salida.
    @TypeConverter
    fun rolATexto(roluser: RolUsuario): String = roluser.name

    @TypeConverter
    fun textoARol(text: String): RolUsuario = RolUsuario.valueOf(text)

    @TypeConverter
    fun tipoPuntoInteresATexto(tpi: TipoPuntoInteres): String = tpi.name

    @TypeConverter
    fun textoATipoPuntoInteres(text: String): TipoPuntoInteres = TipoPuntoInteres.valueOf(text)
}
