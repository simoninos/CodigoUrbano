package com.cookingplastic.triskeledu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cookingplastic.triskeledu.data.location.ServicioLugaresCercanos
import com.cookingplastic.triskeledu.data.notificaciones.ReglaAvisos
import com.cookingplastic.triskeledu.navigation.AppScaffold
import com.cookingplastic.triskeledu.ui.theme.TemaDiaNoche
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()

        setContent {
            TemaDiaNoche {
                AppScaffold()
            }
        }

        //RNF-03: mientras la app está a la vista se decide si corre la búsqueda en segundo plano. Se vuelve a
        //revisar al volver a la app (después de dar el permiso en el diálogo o en Ajustes), al iniciar o cerrar sesión
        //y al cambiar los interruptores. El servicio sigue corriendo al minimizar la app
        val container = (application as TriskeleduApp).container
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                combine(container.usuarioRepository.usuarioActual(), container.preferenciasRepository.preferencias()) { usuario, preferencias ->
                    ReglaAvisos.vigilarEnSegundoPlano(usuario, preferencias, ServicioLugaresCercanos.permisoSegundoPlano(this@MainActivity))
                }
                    //RNF-11: si no se pudo leer, "Lugar cercano" sigue funcionando con la app abierta
                    .catch { }
                    .collect { debeCorrer -> ServicioLugaresCercanos.actualizar(this@MainActivity, debeCorrer) }
            }
        }
    }
}
