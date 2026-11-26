package com.cookingplastic.triskeledu.data.remoto

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import com.cookingplastic.triskeledu.data.sincronizacion.MotivoRechazo
import com.cookingplastic.triskeledu.data.sincronizacion.RespuestaRegistro

//DTO: lo que viaja como JSON entre la app y el backend. Tienen los mismos nombres de campo que los records
//de Java del backend (Gson los asocia por nombre). Se separan de las entidades de Room para que un cambio
//en la base del teléfono no cambie el contrato con el servidor, y al revés

// ---- ms-usuarios ----
data class RegistroDto(
    val id: String,
    val username: String,
    val correo: String,
    val clave: String,
    val aceptaTerminos: Boolean,
    val terminosVersion: String,
    val quiereSerCreador: Boolean
)

//SEG-05: desafio y atestacion son para Key Attestation; null si el teléfono no pudo crear la clave
data class LoginDto(
    val correo: String,
    val clave: String,
    val desafio: String? = null,
    val atestacion: List<String>? = null
)

data class DesafioDto(val desafio: String, val expiraEn: Long)

//token es null si la cuenta está en revisión (RN-29)
data class SesionDto(
    val token: String?,
    val expiraEn: Long?,
    val cuentaEnRevision: Boolean,
    val usuario: UsuarioDto
)

data class UsuarioDto(
    val id: String,
    val username: String,
    val correo: String,
    val rol: RolUsuario,
    val terminosVersion: String?,
    val terminosAceptadosEn: Long,
    val creadoEn: Long,
    val solicitudCreador: EstadoSolicitud?,
    val solicitadoEn: Long?,
    val solicitudResueltaEn: Long?
) {
    //el servidor nunca manda la contraseña: el hash local lo pone quien llama (UsuariosConBackend)
    fun aUsuario(claveHash: String, claveSal: String) = Usuario(
        id = id,
        username = username,
        correo = correo,
        claveHash = claveHash,
        claveSal = claveSal,
        rol = rol,
        terminosVersion = terminosVersion ?: "",
        terminosAceptadosEn = terminosAceptadosEn,
        creadoEn = creadoEn,
        solicitudCreador = solicitudCreador,
        solicitadoEn = solicitadoEn,
        solicitudResueltaEn = solicitudResueltaEn
    )
}

// ---- ms-contenido ----
data class PuntoDto(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val tipo: TipoPuntoInteres,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double,
    val imagenUri: String,
    val descubreMasUrl: String?,
    val codigoQr: String,
    val capsulaId: String?,
    val activo: Boolean,
    val actualizadoEn: Long
) {
    fun aPunto() = PuntoInteres(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        tipo = tipo,
        ubicacion = Ubicacion(latitud = latitud, longitud = longitud, altitud = altitud),
        imagenUri = imagenUri,
        descubreMasUrl = descubreMasUrl,
        codigoQr = codigoQr,
        capsulaId = capsulaId,
        activo = activo,
        actualizadoEn = actualizadoEn
    )
}

//P10: lo que envía el administrador. El servidor no recibe "activo": se cambia con su propio endpoint (ADM-09)
data class PuntoEnvioDto(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val tipo: TipoPuntoInteres,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double,
    val imagenUri: String,
    val descubreMasUrl: String?,
    val codigoQr: String?,
    val capsulaId: String?
) {
    companion object {
        fun desde(p: PuntoInteres) = PuntoEnvioDto(
            id = p.id,
            nombre = p.nombre,
            descripcion = p.descripcion,
            tipo = p.tipo,
            latitud = p.ubicacion.latitud,
            longitud = p.ubicacion.longitud,
            altitud = p.ubicacion.altitud,
            imagenUri = p.imagenUri,
            descubreMasUrl = p.descubreMasUrl,
            codigoQr = p.codigoQr.ifBlank { null },
            capsulaId = p.capsulaId
        )
    }
}

data class ActivoDto(val activo: Boolean)

data class AsociarDto(val capsulaId: String)

data class CapsulaDto(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    val urlStream: String?,
    val categoria: CategoriaCapsula,
    val duracionSegundos: Int,
    val estado: EstadoCapsula,
    val creadorId: String,
    val creadaEn: Long,
    val publicadaEn: Long?
) {
    fun aCapsula() = Capsula(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        urlStream = urlStream,
        categoria = categoria,
        duracionSegundos = duracionSegundos,
        estado = estado,
        creadorId = creadorId,
        creadaEn = creadaEn,
        publicadaEn = publicadaEn
    )
}

data class CapsulaEnvioDto(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    val urlStream: String?,
    val categoria: CategoriaCapsula,
    val duracionSegundos: Int
) {
    companion object {
        fun desde(c: Capsula) = CapsulaEnvioDto(c.id, c.titulo, c.descripcion, c.urlStream, c.categoria, c.duracionSegundos)
    }
}

// ---- ms-progreso ----
//ESC-02: una fila de progreso_puntos. Los puntos no viajan: los calcula el servidor (BKD-05)
data class EscaneoDto(
    val id: String,
    val puntoId: String,
    val escaneadoEn: Long,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double,
    val capsulaVistaEn: Long?,
    val selfieEn: Long?,
    val selfieUri: String?,
    //SEG-01, SEG-03 y SEG-05: lo revisa ms-progreso
    val simulada: Boolean = false,
    val precision: Double? = null,
    val codigoQr: String? = null,
    val firma: String? = null
) {
    companion object {
        //firma: la de la clave del teléfono sobre textoFirmado(p), o null si no hay clave
        fun desde(p: ProgresoPunto, firma: String? = null) = EscaneoDto(
            id = p.id,
            puntoId = p.puntoId,
            escaneadoEn = p.escaneadoEn,
            latitud = p.escaneoUbicacion.latitud,
            longitud = p.escaneoUbicacion.longitud,
            altitud = p.escaneoUbicacion.altitud,
            capsulaVistaEn = p.capsulaVistaEn,
            selfieEn = p.selfieEn,
            selfieUri = p.selfieUri,
            simulada = p.escaneoSimulado,
            precision = p.escaneoPrecision,
            codigoQr = p.codigoQrLeido,
            firma = firma
        )
    }
}

data class ComentarioDto(val id: String, val puntoId: String, val texto: String, val creadoEn: Long) {
    companion object {
        fun desde(c: Comentario) = ComentarioDto(c.id, c.puntoId, c.texto, c.creadoEn)
    }
}

//EXC-08: el servidor responde una fila por cada registro enviado
data class RespuestaRegistroDto(val id: String, val aceptado: Boolean, val motivo: String?) {
    //un motivo que la app no conoce se trata como duplicado: igual se quita del teléfono con su aviso
    fun aRespuesta() = RespuestaRegistro(
        id = id,
        motivo = if (aceptado) null else MotivoRechazo.entries.find { it.name == motivo } ?: MotivoRechazo.DUPLICADO
    )
}

data class FilaRankingDto(val posicion: Int, val usuarioId: String, val username: String?, val puntos: Int)

data class RankingDto(val periodo: String, val filas: List<FilaRankingDto>, val miPosicion: FilaRankingDto?)
