package com.cookingplastic.triskeledu

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

//Aplplication es un objeto que se crea una vez y vive mientras esté la app abierta, es como un contenedor.
class TriskeleduApp : Application() {

    //Como se requiere el context, solo se prepara
    lateinit var container: AppContainer
        private set

    //trabajos que duran lo mismo que la app (no una pantalla); SupervisorJob: si uno falla, los demás siguen
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    //Y aquí arranca la app, da el contexto y arranca
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        //SIM-03: al abrir la app (o al iniciar sesión) se sincroniza lo pendiente apenas haya red
        container.sincronizarAlEntrar(alcance)
    }
}
