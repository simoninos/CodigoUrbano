package com.cookingplastic.triskeledu.data.repository

import com.cookingplastic.triskeledu.data.model.Preferencias
import kotlinx.coroutines.flow.Flow

interface PreferenciasRepository {
    fun preferencias(): Flow<Preferencias> //no olvidar que Flow permite que vayan los datos de forma asincrona! Parecido a kafka, pero en mini escala
    suspend fun guardar(preferencias: Preferencias)
}
