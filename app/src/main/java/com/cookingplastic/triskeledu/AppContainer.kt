package com.cookingplastic.triskeledu

import android.content.Context
import androidx.room.Room
import com.cookingplastic.triskeledu.data.local.AppDatabase
import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.local.CargaInicial
import com.cookingplastic.triskeledu.data.nivel.ControlNivel
import com.cookingplastic.triskeledu.data.nivel.ProgresoConNivel
import com.cookingplastic.triskeledu.data.notificaciones.AndroidAvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.notificaciones.PuntosConAvisos
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import kotlinx.coroutines.flow.first
import com.cookingplastic.triskeledu.data.repository.CapsulaRepository
import com.cookingplastic.triskeledu.data.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.data.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.ProgresoRepository
import com.cookingplastic.triskeledu.data.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.RoomCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.RoomComentarioRepository
import com.cookingplastic.triskeledu.data.repository.RoomNotificacionRepository
import com.cookingplastic.triskeledu.data.repository.RoomPreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.RoomProgresoRepository
import com.cookingplastic.triskeledu.data.repository.RoomPuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.RoomUsuarioRepository
import com.cookingplastic.triskeledu.data.repository.UsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionRepository
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionSimulada
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.data.simulacion.ServidorSimulado
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesSimuladas
import com.cookingplastic.triskeledu.data.sincronizacion.ComentariosConSincronizacion
import com.cookingplastic.triskeledu.data.sincronizacion.ProgramadorSincronizacion
import com.cookingplastic.triskeledu.data.sincronizacion.ProgresoConSincronizacion
import com.cookingplastic.triskeledu.data.sincronizacion.ServidorApi
import com.cookingplastic.triskeledu.data.sincronizacion.Sincronizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

//Context para que así room sepa dónde guardar
class AppContainer(context: Context) {

    //Se crea base de datos y con lazy se crea la priimera vez que alguien lo pida, no antes
    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "triskeledu.db")
            //la primera vez que se crea la base, se llena con DatosPrueba
            .addCallback(CargaInicial)
            //mientras desarrollamos: si sube la versión, la base se borra y se crea de nuevo en vez de
            //cerrar la app. CargaInicial vuelve a llenarla con los datos de prueba
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    //Ahora los repositorios guardan en Room; los Fake quedan para las pruebas.
    //Siguen siendo una sola instancia: todas las pantallas ven la misma sesión
    val usuarioRepository: UsuarioRepository by lazy { RoomUsuarioRepository(database.usuarioDao()) }
    val preferenciasRepository: PreferenciasRepository by lazy {
        RoomPreferenciasRepository(database.preferenciasDao(), database.usuarioDao())
    }
    val capsulaRepository: CapsulaRepository by lazy { RoomCapsulaRepository(database.capsulaDao()) }
    //RN-24: al activarse un punto se avisa "Nueva cápsula"; PuntosConAvisos envuelve al repositorio de Room
    val puntoInteresRepository: PuntoInteresRepository by lazy {
        PuntosConAvisos(RoomPuntoInteresRepository(database.puntoInteresDao(), database.capsulaDao()), generadorNotificaciones)
    }
    //LVL-02: el progreso de Room envuelto para revisar el nivel después de cada suma de puntos
    private val progresoRoom: ProgresoRepository by lazy { RoomProgresoRepository(database.progresoDao()) }
    val controlNivel: ControlNivel by lazy {
        ControlNivel(progresoRoom, preferenciasRepository, usuarioRepository, database.notificacionDao(), avisoLocal)
    }
    val progresoRepository: ProgresoRepository by lazy {
        //PER-04: al completar un lugar (80 de 80) llega el aviso "Logro"
        val conNivel = ProgresoConNivel(progresoRoom, controlNivel) { usuarioId, puntoId ->
            puntoInteresRepository.obtenerPorId(puntoId).first()?.let { generadorNotificaciones.lugarCompletado(usuarioId, it) }
        }
        //ESC-02: después de cada suma se pide sincronizar con el servidor
        ProgresoConSincronizacion(conNivel, programadorSincronizacion::programar)
    }
    val comentarioRepository: ComentarioRepository by lazy {
        ComentariosConSincronizacion(RoomComentarioRepository(database.comentarioDao()), programadorSincronizacion::programar)
    }

    //notificaciones del sistema; el applicationContext vive mientras la app esté abierta
    val avisoLocal: AvisoLocal by lazy { AndroidAvisoLocal(context.applicationContext) }

    //SIM-01: recuperación de contraseña simulada en el teléfono, sin backend
    val recuperacionRepository: RecuperacionRepository by lazy {
        RecuperacionSimulada(database.usuarioDao(), database.codigoRecuperacionDao(), database.notificacionDao(), avisoLocal)
    }

    //LUG-03: "Por descubrir" pide abrir el mapa en un lugar; Mis descubrimientos y el Mapa comparten este pedido
    val pedidoMapa: PedidoMapa by lazy { PedidoMapa() }

    //CRE-02 a CRE-08: publicar, retirar, eliminar y asociar cápsulas con sus reglas
    val gestorCapsulas: GestorCapsulas by lazy {
        GestorCapsulas(capsulaRepository, puntoInteresRepository, avisos = generadorNotificaciones)
    }

    //SIM-02: solicitudes de creador resueltas en el mismo teléfono
    val solicitudesRepository: SolicitudesRepository by lazy {
        SolicitudesSimuladas(usuarioRepository, database.notificacionDao(), avisoLocal)
    }

    //PER-03: el historial de notificaciones (P16)
    val notificacionRepository: NotificacionRepository by lazy { RoomNotificacionRepository(database.notificacionDao()) }

    //PER-04 y RN-24: arma cada notificación local, la guarda en el historial y respeta los interruptores
    val generadorNotificaciones: GeneradorNotificaciones by lazy {
        GeneradorNotificaciones(notificacionRepository, avisoLocal, usuarioRepository, preferenciasRepository)
    }

    //SIM-03: el servidor es simulado. Con backend, aquí va la versión Retrofit de ServidorApi y nada más cambia
    val servidorApi: ServidorApi by lazy { ServidorSimulado(progresoRoom, puntoInteresRepository) }
    val sincronizador: Sincronizador by lazy {
        Sincronizador(servidorApi, progresoRepository, comentarioRepository, puntoInteresRepository, usuarioRepository, generadorNotificaciones)
    }
    val programadorSincronizacion: ProgramadorSincronizacion by lazy { ProgramadorSincronizacion(context.applicationContext) }

    //SIM-03: cada vez que alguien inicia sesión se envía lo que su cuenta dejó pendiente
    fun sincronizarAlEntrar(alcance: CoroutineScope) {
        alcance.launch {
            usuarioRepository.usuarioActual().map { it?.id }.distinctUntilChanged().filterNotNull()
                //RNF-11: si la base falla, no se sincroniza al entrar, pero la app sigue
                .catch { }
                .collect { programadorSincronizacion.programar() }
        }
    }
}
