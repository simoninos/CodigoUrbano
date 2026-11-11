package com.cookingplastic.triskeledu.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cookingplastic.triskeledu.TriskeleduApp
import com.cookingplastic.triskeledu.data.nivel.ControlNivel
import com.cookingplastic.triskeledu.data.nivel.SubidaNivel
import com.cookingplastic.triskeledu.domain.repository.PreferenciasRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

//P25: vive sobre toda la app (AppScaffold), así el diálogo aparece encima de la pantalla en que se sumaron los puntos
class SubidaNivelViewModel(
    private val controlNivel: ControlNivel,
    private val preferenciasRepository: PreferenciasRepository
) : ViewModel() {

    //null = no hay diálogo
    val subida: StateFlow<SubidaNivel?> = controlNivel.subida

    fun onEvent(evento: SubidaNivelUiEvent) {
        when (evento) {
            SubidaNivelUiEvent.OnUsarTema -> usarTema()
            SubidaNivelUiEvent.OnSeguirExplorando -> controlNivel.subidaVista()
        }
    }

    //LVL-02 y EXC-14: el tema se guarda y TemaDiaNoche lo aplica en toda la app al instante
    private fun usarTema() {
        val tema = subida.value?.tema
        controlNivel.subidaVista()
        if (tema == null) return
        viewModelScope.launch {
            try {
                //se leen las preferencias recién guardadas para no pisar nivelMaximoAlcanzado
                val actuales = preferenciasRepository.preferencias().first()
                preferenciasRepository.guardar(actuales.copy(tema = tema))
            } catch (e: Exception) {
                //si no se pudo guardar, el tema se puede elegir después en Configuración
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TriskeleduApp
                SubidaNivelViewModel(app.container.controlNivel, app.container.preferenciasRepository)
            }
        }
    }
}
