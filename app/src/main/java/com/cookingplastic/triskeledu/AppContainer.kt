package com.cookingplastic.triskeledu

import android.content.Context
import androidx.room.Room
import com.cookingplastic.triskeledu.data.local.AppDatabase
import com.cookingplastic.triskeledu.data.creador.GestorCapsulas
import com.cookingplastic.triskeledu.data.externo.RetrofitWikipediaRepository
import com.cookingplastic.triskeledu.data.externo.WikipediaRepository
import com.cookingplastic.triskeledu.data.local.CargaInicial
import com.cookingplastic.triskeledu.data.nivel.ControlNivel
import com.cookingplastic.triskeledu.data.nivel.ProgresoConNivel
import com.cookingplastic.triskeledu.data.notificaciones.AndroidAvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.notificaciones.GeneradorNotificaciones
import com.cookingplastic.triskeledu.data.notificaciones.PuntosConAvisos
import com.cookingplastic.triskeledu.navigation.PedidoMapa
import kotlinx.coroutines.flow.first
import com.cookingplastic.triskeledu.domain.repository.CapsulaRepository
import com.cookingplastic.triskeledu.domain.repository.ComentarioRepository
import com.cookingplastic.triskeledu.data.repository.NotificacionRepository
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import com.cookingplastic.triskeledu.domain.repository.ProgresoRepository
import com.cookingplastic.triskeledu.domain.repository.PuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.RoomCapsulaRepository
import com.cookingplastic.triskeledu.data.repository.RoomComentarioRepository
import com.cookingplastic.triskeledu.data.repository.RoomNotificacionRepository
import com.cookingplastic.triskeledu.data.repository.RoomPreferenciasRepository
import com.cookingplastic.triskeledu.data.repository.RoomProgresoRepository
import com.cookingplastic.triskeledu.data.repository.RoomPuntoInteresRepository
import com.cookingplastic.triskeledu.data.repository.RoomUsuarioRepository
import com.cookingplastic.triskeledu.domain.repository.UsuarioRepository
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionRepository
import com.cookingplastic.triskeledu.data.simulacion.RecuperacionSimulada
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesRepository
import com.cookingplastic.triskeledu.data.simulacion.SolicitudesSimuladas
import com.cookingplastic.triskeledu.data.remoto.ActualizadorContenido
import com.cookingplastic.triskeledu.data.remoto.BackendApi
import com.cookingplastic.triskeledu.data.remoto.CapsulasConBackend
import com.cookingplastic.triskeledu.data.remoto.ClienteBackend
import com.cookingplastic.triskeledu.data.remoto.DireccionServidor
import com.cookingplastic.triskeledu.data.remoto.PuntosConBackend
import com.cookingplastic.triskeledu.data.remoto.RankingServidor
import com.cookingplastic.triskeledu.data.remoto.RetrofitServidorApi
import com.cookingplastic.triskeledu.data.remoto.SolicitudesConBackend
import com.cookingplastic.triskeledu.data.remoto.UsuariosConBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
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

    //BKD-01: backend propio a través del API Gateway. La dirección de fábrica sale de BuildConfig
    //(10.0.2.2 es el computador visto desde el emulador); en un teléfono se escribe en P01, "Servidor",
    //con la IP que el punto de acceso del celular le dio al computador, y queda guardada
    private val preferenciasRed = context.getSharedPreferences("servidor", Context.MODE_PRIVATE)
    val direccionServidor: DireccionServidor by lazy {
        DireccionServidor(
            porDefecto = BuildConfig.URL_BACKEND,
            leer = { preferenciasRed.getString("url", null) },
            guardar = { url -> preferenciasRed.edit().apply { if (url == null) remove("url") else putString("url", url) }.apply() }
        )
    }

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val backendApi: BackendApi by lazy {
        ClienteBackend.crear(
            urlBase = BuildConfig.URL_BACKEND,
            servidor = { direccionServidor.actual.value },
            //el interceptor corre en un hilo de OkHttp, nunca en el principal: puede esperar a Room
            token = { runBlocking { database.usuarioDao().token() } },
            //EXC-07: token vencido o inválido; se cierra la sesión y la app vuelve al Login
            alVencer = { alcance.launch { usuarioRepository.cerrarSesion() } }
        )
    }

    //Room sigue siendo la base del teléfono (offline-first); los repositorios ConBackend escriben primero en el
    //servidor y después en Room. Los Fake quedan para las pruebas. Una sola instancia: todas las pantallas ven lo mismo
    private val usuariosRoom: UsuarioRepository by lazy { RoomUsuarioRepository(database.usuarioDao()) }
    val usuarioRepository: UsuarioRepository by lazy { UsuariosConBackend(usuariosRoom, database.usuarioDao(), backendApi) }
    val preferenciasRepository: PreferenciasRepository by lazy {
        RoomPreferenciasRepository(database.preferenciasDao(), database.usuarioDao())
    }
    private val capsulasRoom: CapsulaRepository by lazy { RoomCapsulaRepository(database.capsulaDao()) }
    val capsulaRepository: CapsulaRepository by lazy { CapsulasConBackend(capsulasRoom, backendApi) }
    private val puntosRoom: PuntoInteresRepository by lazy {
        RoomPuntoInteresRepository(database.puntoInteresDao(), database.capsulaDao())
    }
    //RN-24: al activarse un punto se avisa "Nueva cápsula"; PuntosConAvisos envuelve a PuntosConBackend
    val puntoInteresRepository: PuntoInteresRepository by lazy {
        PuntosConAvisos(PuntosConBackend(puntosRoom, backendApi), generadorNotificaciones)
    }
    //trae del servidor las cápsulas y los puntos que el usuario puede ver; escribe directo en Room
    val actualizadorContenido: ActualizadorContenido by lazy { ActualizadorContenido(backendApi, capsulasRoom, puntosRoom) }
    //LUG-06: el ranking de todos los usuarios lo calcula ms-progreso
    val rankingServidor: RankingServidor by lazy { RankingServidor(backendApi) }
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

    //ADM-10: el servidor responde la solicitud; la versión local deja la notificación al usuario
    val solicitudesRepository: SolicitudesRepository by lazy {
        SolicitudesConBackend(SolicitudesSimuladas(usuarioRepository, database.notificacionDao(), avisoLocal), database.usuarioDao(), backendApi)
    }

    //PER-03: el historial de notificaciones (P16)
    val notificacionRepository: NotificacionRepository by lazy { RoomNotificacionRepository(database.notificacionDao()) }

    //PER-04 y RN-24: arma cada notificación local, la guarda en el historial y respeta los interruptores
    val generadorNotificaciones: GeneradorNotificaciones by lazy {
        GeneradorNotificaciones(notificacionRepository, avisoLocal, usuarioRepository, preferenciasRepository)
    }

    //API externa: resumen de Wikipedia del lugar en el Detalle (Retrofit)
    val wikipediaRepository: WikipediaRepository by lazy { RetrofitWikipediaRepository.crear() }

    //ESC-02: ms-progreso revalida los escaneos y comentarios. Es la misma interfaz que implementaba ServidorSimulado
    //(sigue en data/simulacion para las pruebas): solo cambió esta línea, ni el Sincronizador ni los ViewModels
    val servidorApi: ServidorApi by lazy { RetrofitServidorApi(backendApi) }
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
                .collect {
                    programadorSincronizacion.programar()
                    //BKD-03: los puntos y cápsulas que ve este rol; sin servidor quedan los de Room (EXC-06)
                    runCatching { actualizadorContenido.actualizar() }
                }
        }
    }
}
