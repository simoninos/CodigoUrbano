package com.cookingplastic.triskeledu.data.red

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Indica si el teléfono tiene internet. Es una interfaz para que el ViewModel
// no dependa del Context de Android y pueda usarse con la versión Fake.
interface RedRepository {
    fun hayConexion(): Boolean
}

// Versión real: consulta la red activa del sistema
class AndroidRedRepository(context: Context) : RedRepository {

    private val conectividad =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun hayConexion(): Boolean {
        val red = conectividad.activeNetwork ?: return false
        val capacidades = conectividad.getNetworkCapabilities(red) ?: return false
        // INTERNET: la red declara salida a internet. VALIDATED: Android comprobó que funciona.
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

// Versión de prueba: conectado se cambia a mano para simular el modo avión
class FakeRedRepository(var conectado: Boolean = true) : RedRepository {
    override fun hayConexion(): Boolean = conectado
}
