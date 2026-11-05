package com.cookingplastic.triskeledu.data.sincronizacion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cookingplastic.triskeledu.TriskeleduApp
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

//SIM-03: WorkManager corre este trabajo cuando hay red. En modo avión espera; al volver la conexión,
//Android lo lanza solo aunque la app esté cerrada
class SincronizacionWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TriskeleduApp).container
        return try {
            container.sincronizador.sincronizar()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            //EXC-06: si algo falla se reintenta más tarde; lo pendiente sigue guardado en el teléfono
            if (runAttemptCount < MAXIMO_INTENTOS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAXIMO_INTENTOS = 3
    }
}

//Deja pedida una sincronización que corre apenas haya red
class ProgramadorSincronizacion(private val context: Context) {

    fun programar() {
        val pedido = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        //un solo trabajo con este nombre: si ya hay uno esperando la red, el nuevo va después y lee lo que falte
        try {
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
        } catch (e: Exception) {
            //lo guardado ya está en Room con sincronizado = false: se enviará la próxima vez que se pida
        }
    }

    companion object {
        private const val NOMBRE = "sincronizacion"
    }
}
