package com.cookingplastic.triskeledu

import android.app.Application

//Aplplication es un objeto que se crea una vez y vive mientras esté la app abierta, es como un contenedor.
class TriskeleduApp : Application() {

    //Como se requiere el context, solo se prepara
    lateinit var container: AppContainer
        private set

    //Y aquí arranca la app, da el contexto y arranca
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
