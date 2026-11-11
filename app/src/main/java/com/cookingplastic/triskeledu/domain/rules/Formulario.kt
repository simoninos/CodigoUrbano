package com.cookingplastic.triskeledu.domain.rules

//RN-18 (ACC-03): al tocar enviar, el foco va al primer campo con error, en el orden de la pantalla.
//Recibe pares (campo, error) y devuelve el primer campo cuyo error no es null; null si todo está bien.
//La usan todos los formularios: login, registro, punto, comentario y los que vienen
fun <C> primerCampoConError(vararg campos: Pair<C, String?>): C? = campos.firstOrNull { it.second != null }?.first
