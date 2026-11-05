package com.cookingplastic.triskeledu.data.notificaciones

import com.cookingplastic.triskeledu.data.location.distanciaEnMetros
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import java.time.Instant
import java.time.ZoneId

//Reglas de las notificaciones locales (RN-24). Solo lógica: se prueban sin Android
object ReglaAvisos {

    //"Lugar cercano" a 200 m o menos de un punto activo que todavía no escanea
    const val RADIO_CERCANO_METROS = 200.0

    //como máximo una vez al día por punto: true si ya hay un aviso de ese punto con la fecha de hoy
    fun yaAvisadoHoy(historial: List<Notificacion>, puntoId: String, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean {
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        return historial.any {
            it.tipo == TipoNotificacion.LUGAR_CERCANO && it.puntoId == puntoId &&
                Instant.ofEpochMilli(it.creadaEn).atZone(zona).toLocalDate() == hoy
        }
    }

    //se respetan los interruptores de Configuración; Logro y Sistema no tienen interruptor
    fun permitido(tipo: TipoNotificacion, preferencias: Preferencias): Boolean = when (tipo) {
        TipoNotificacion.LUGAR_CERCANO -> preferencias.notifLugaresCercanos
        TipoNotificacion.NUEVA_CAPSULA -> preferencias.notifNuevasCapsulas
        TipoNotificacion.SOLICITUD -> preferencias.notifSolicitudes
        TipoNotificacion.PUNTO_LISTO -> preferencias.notifPuntosListos
        TipoNotificacion.LOGRO, TipoNotificacion.SISTEMA -> true
    }

    //"Lugar cercano": los puntos activos que el usuario todavía no escanea, a 200 m o menos, con su distancia
    fun cercanosSinEscanear(activos: List<PuntoInteres>, escaneados: Set<String>, ubicacion: Ubicacion): List<Pair<PuntoInteres, Double>> =
        activos.filter { it.activo && it.id !in escaneados }
            .map { it to distanciaEnMetros(ubicacion, it.ubicacion) }
            .filter { (_, distancia) -> distancia <= RADIO_CERCANO_METROS }

    //RNF-03 y EXC-13: la búsqueda con la app minimizada corre solo para quien juega (no el administrador ni una
    //cuenta en revisión), con "Lugares cercanos" y "Seguir mi ubicación" encendidos y el permiso de segundo plano
    fun vigilarEnSegundoPlano(usuario: Usuario?, preferencias: Preferencias?, permisoSegundoPlano: Boolean): Boolean =
        usuario != null && usuario.rol != RolUsuario.ADMINISTRADOR && usuario.solicitudCreador != EstadoSolicitud.PENDIENTE &&
            preferencias != null && preferencias.usuarioId == usuario.id &&
            preferencias.notifLugaresCercanos && preferencias.seguirUbicacion && permisoSegundoPlano
}
