package com.cookingplastic.triskeledu.data.nivel

import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.model.NivelUsuario
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TemaApp
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.domain.rules.ReglaNivel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

//Lo que muestra "¡Subiste de nivel!" (P25). tema es null del nivel 9 en adelante (RN-32)
data class SubidaNivel(
    val nivel: NivelUsuario,
    val tema: TemaApp?,
    val nombreUsuario: String
)

//LVL-02 y RN-34: después de sumar puntos (escaneo, cápsula o selfie) revisa si el nivel superó el más alto
//alcanzado. Si es así, lo guarda, deja una notificación LOGRO y avisa a P25. Es una sola instancia (AppContainer)
class ControlNivel(
    private val progresoRepository: ProgresoRepository,
    private val preferenciasRepository: PreferenciasRepository,
    private val usuarioRepository: UsuarioRepository,
    private val notificacionDao: NotificacionDao,
    //PER-04: "Logro" también aparece en el sistema (sin permiso no se muestra, pero queda en el historial)
    private val avisoLocal: AvisoLocal? = null
) {
    private val _subida = MutableStateFlow<SubidaNivel?>(null)
    //P25 se muestra mientras esto no sea null
    val subida: StateFlow<SubidaNivel?> = _subida.asStateFlow()

    //si llegan dos sumas casi juntas, se revisan de a una: así no se guardan dos notificaciones
    private val turno = Mutex()

    suspend fun revisar(usuarioId: String) = turno.withLock {
        val usuario = usuarioRepository.usuarioActual().first() ?: return@withLock
        //el administrador no tiene nivel (RN-33)
        if (usuario.id != usuarioId || usuario.rol == RolUsuario.ADMINISTRADOR) return@withLock

        val preferencias = preferenciasRepository.preferencias().first()
        if (preferencias.usuarioId != usuarioId) return@withLock

        val nivel = ReglaNivel.nivelDesde(progresoRepository.totalPuntos(usuarioId).first())
        //RN-34: si bajó y vuelve a subir a un nivel que ya tuvo, no se repite
        if (nivel.numero <= preferencias.nivelMaximoAlcanzado) return@withLock

        //RN-33: el nivel más alto queda guardado y con él los temas desbloqueados
        preferenciasRepository.guardar(preferencias.copy(nivelMaximoAlcanzado = nivel.numero))
        //si subió dos niveles de una vez, se muestra el más alto con su tema
        val tema = ReglaNivel.temaDelNivel(nivel.numero)
        val logro = Notificacion(
            usuarioId = usuarioId,
            tipo = TipoNotificacion.LOGRO,
            titulo = "¡Subiste al nivel ${nivel.numero}!",
            texto = if (tema != null) "Ahora eres ${nivel.nombre}. Desbloqueaste el tema ${tema.nombre}."
            else "Ahora eres ${nivel.nombre} con ${nivel.puntosTotales} puntos."
        )
        notificacionDao.insertar(logro)
        avisoLocal?.mostrar(logro.tipo, logro.titulo, logro.texto)
        _subida.value = SubidaNivel(nivel = nivel, tema = tema, nombreUsuario = usuario.username)
    }

    //P25 se cerró ("Usar este tema" o "Seguir explorando")
    fun subidaVista() {
        _subida.value = null
    }
}
