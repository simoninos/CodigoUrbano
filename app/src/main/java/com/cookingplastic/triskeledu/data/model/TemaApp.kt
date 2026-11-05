package com.cookingplastic.triskeledu.data.model

//Los 8 temas de la app (RN-32). Cada nivel del 1 al 8 desbloquea el suyo; se guarda en Preferencias.tema
enum class TemaApp(val nombre: String, val nivelRequerido: Int) {
    CLASICO("Clásico", 1),
    CERRO_SANTA_LUCIA("Cerro Santa Lucía", 2),
    ATARDECER_MAPOCHO("Atardecer en el Mapocho", 3),
    NOCHE_MUSEO("Noche de museo", 4),
    DESIERTO_FLORIDO("Desierto florido", 5),
    BOSQUE_NATIVO("Bosque nativo", 6),
    GLACIAR("Glaciar", 7),
    IA_NEON("IA neón", 8),
}
