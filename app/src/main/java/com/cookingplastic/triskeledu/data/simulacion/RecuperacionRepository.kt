package com.cookingplastic.triskeledu.data.simulacion

import com.cookingplastic.triskeledu.data.local.dao.CodigoRecuperacionDao
import com.cookingplastic.triskeledu.data.local.dao.NotificacionDao
import com.cookingplastic.triskeledu.data.local.dao.UsuarioDao
import com.cookingplastic.triskeledu.data.model.CodigoRecuperacion
import com.cookingplastic.triskeledu.data.model.Notificacion
import com.cookingplastic.triskeledu.data.model.TipoNotificacion
import com.cookingplastic.triskeledu.data.notificaciones.AvisoLocal
import com.cookingplastic.triskeledu.data.security.HashContrasena
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//ACC-05: recuperar la contraseña con un código de 6 dígitos.
//Cuando exista el backend, se cambia la versión simulada por una que llame al servidor, sin tocar el ViewModel
interface RecuperacionRepository {
    //genera y "envía" el código. No avisa si el correo no existe, para no revelar qué cuentas hay (como RN-19)
    suspend fun enviarCodigo(correo: String)

    //true si cambió la contraseña; false si el código no es válido, ya venció o ya se usó
    suspend fun cambiarClave(correo: String, codigo: String, claveNueva: String): Boolean
}

//SIM-01: el "servidor" es el mismo teléfono. El código llega como notificación local (tipo SISTEMA)
//y queda en el historial; en la base solo se guarda su hash, como la contraseña
class RecuperacionSimulada(
    private val usuarioDao: UsuarioDao,
    private val codigoDao: CodigoRecuperacionDao,
    private val notificacionDao: NotificacionDao,
    private val aviso: AvisoLocal,
    //se pueden cambiar en las pruebas para simular el paso del tiempo y un código conocido
    private val ahora: () -> Long = { System.currentTimeMillis() },
    private val generarCodigo: () -> String = { (0..999_999).random().toString().padStart(6, '0') },
    //el hash tarda: se calcula fuera del hilo principal (en las pruebas, en el mismo hilo)
    private val calculo: CoroutineDispatcher = Dispatchers.Default
) : RecuperacionRepository {

    override suspend fun enviarCodigo(correo: String) {
        val usuario = usuarioDao.buscarPorCorreo(correo.trim().lowercase()) ?: return
        val codigo = generarCodigo()
        val hash = withContext(calculo) { HashContrasena.hashear(codigo) }
        val creado = ahora()
        //un código nuevo reemplaza al anterior del mismo usuario
        codigoDao.guardar(
            CodigoRecuperacion(
                usuarioId = usuario.id,
                codigoHash = hash.hash,
                codigoSal = hash.sal,
                creadoEn = creado,
                venceEn = creado + VIGENCIA_MS
            )
        )
        val titulo = "Código para recuperar tu contraseña"
        val texto = "Tu código es $codigo. Vence en $MINUTOS_VIGENCIA minutos y sirve una sola vez."
        notificacionDao.insertar(Notificacion(usuarioId = usuario.id, tipo = TipoNotificacion.SISTEMA, titulo = titulo, texto = texto))
        aviso.mostrar(TipoNotificacion.SISTEMA, titulo, texto)
    }

    override suspend fun cambiarClave(correo: String, codigo: String, claveNueva: String): Boolean {
        val usuario = usuarioDao.buscarPorCorreo(correo.trim().lowercase()) ?: return false
        val guardado = codigoDao.buscar(usuario.id) ?: return false
        //sirve una vez y vence a los 15 minutos
        if (guardado.usado || ahora() > guardado.venceEn) return false
        val correcto = withContext(calculo) {
            HashContrasena.verificar(codigo, guardado.codigoHash, guardado.codigoSal)
        }
        if (!correcto) return false
        val nueva = withContext(calculo) { HashContrasena.hashear(claveNueva) }
        usuarioDao.cambiarClave(usuario.id, nueva.hash, nueva.sal)
        codigoDao.marcarUsado(usuario.id)
        return true
    }

    companion object {
        const val MINUTOS_VIGENCIA = 15
        const val VIGENCIA_MS = MINUTOS_VIGENCIA * 60_000L
    }
}
