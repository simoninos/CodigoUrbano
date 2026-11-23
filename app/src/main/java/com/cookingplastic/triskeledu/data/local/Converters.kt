package com.cookingplastic.triskeledu.data.local

import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
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

    //v3: un par por cada enum nuevo que se guarda
    @TypeConverter
    fun categoriaATexto(categoria: CategoriaCapsula): String = categoria.name

    @TypeConverter
    fun textoACategoria(text: String): CategoriaCapsula = CategoriaCapsula.valueOf(text)

    @TypeConverter
    fun estadoCapsulaATexto(estado: EstadoCapsula): String = estado.name

    @TypeConverter
    fun textoAEstadoCapsula(text: String): EstadoCapsula = EstadoCapsula.valueOf(text)

    //la solicitud de creador puede no existir: null se guarda como null
    @TypeConverter
    fun estadoSolicitudATexto(estado: EstadoSolicitud?): String? = estado?.name

    @TypeConverter
    fun textoAEstadoSolicitud(text: String?): EstadoSolicitud? = text?.let { EstadoSolicitud.valueOf(it) }

    @TypeConverter
    fun tipoNotificacionATexto(tipo: TipoNotificacion): String = tipo.name

    @TypeConverter
    fun textoATipoNotificacion(text: String): TipoNotificacion = TipoNotificacion.valueOf(text)

    @TypeConverter
    fun temaATexto(tema: TemaApp): String = tema.name

    @TypeConverter
    fun textoATema(text: String): TemaApp = TemaApp.valueOf(text)
}
