package com.cookingplastic.triskeledu.data.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cookingplastic.triskeledu.R
import com.cookingplastic.triskeledu.data.model.TipoNotificacion

//Muestra una notificación del sistema. Es una interfaz para que la lógica se pruebe sin Android
interface AvisoLocal {
    fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String)
}

//Versión real con NotificationManager: un canal por tipo, así el usuario puede silenciar cada uno
class AndroidAvisoLocal(private val context: Context) : AvisoLocal {

    override fun mostrar(tipo: TipoNotificacion, titulo: String, texto: String) {
        //EXC-03: sin permiso no se muestra, pero la notificación igual queda en el historial (tabla notificaciones)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        crearCanal(tipo)
        //tocarla abre la app
        val abrirApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notificacion = NotificationCompat.Builder(context, tipo.name)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notificacion)
    }

    //crear un canal que ya existe no hace nada, por eso se puede llamar cada vez
    private fun crearCanal(tipo: TipoNotificacion) {
        val canal = NotificationChannel(tipo.name, nombreCanal(tipo), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun nombreCanal(tipo: TipoNotificacion): String = when (tipo) {
        TipoNotificacion.LUGAR_CERCANO -> "Lugares cercanos"
        TipoNotificacion.NUEVA_CAPSULA -> "Nuevas cápsulas"
        TipoNotificacion.LOGRO -> "Logros"
        TipoNotificacion.SOLICITUD -> "Solicitudes de creador"
        TipoNotificacion.PUNTO_LISTO -> "Puntos listos para activar"
        TipoNotificacion.SISTEMA -> "Avisos de la cuenta"
    }
}
