package com.cookingplastic.triskeledu.data.local

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario

/**
 * Datos de prueba de la app.
 *
 * - Puntos de interés: 15 lugares reales de Santiago Centro. Las coordenadas vienen de Mapcarta
 *   (datos de OpenStreetMap); cada punto indica su fuente. Las altitudes marcadas "aprox."
 *   son estimaciones. El tipo de cada punto es de prueba y no corresponde a su declaratoria oficial.
 * - Usuarios y preferencias: ficticios.
 * - Contraseña de todos los usuarios: "Demo1234". Hash PBKDF2WithHmacSHA256 con
 *   120000 iteraciones, clave de 32 bytes y sal de 16 bytes, en Base64 sin saltos de línea.
 * - Los identificadores son UUID fijos, para poder referirse a cada registro desde el código.
 */
object DatosPrueba {

    /** Fecha de referencia de los datos: 2026-10-01 12:00 (Chile). */
    const val FECHA_BASE: Long = 1790866800000L
    const val CLAVE_DE_PRUEBA = "Demo1234"
    const val IMAGEN_PRUEBA = "android.resource://com.cookingplastic.triskeledu/drawable/punto_placeholder"

    // ---------- Cápsulas ----------
    // URL de ejemplo en el dominio reservado example.com.
    val CAPSULA_IA_01 = Capsula(
        id = "26ad7dd3-f096-5aa5-9af8-bd960eb2b377",
        titulo = "¿Qué es la inteligencia artificial?",
        urlStream = "https://example.com/capsulas/ia-01.mp4",
        categoria = "Fundamentos",
        duracionSegundos = 58,
    )
    val CAPSULA_IA_02 = Capsula(
        id = "40b5663b-077d-5d87-bad8-efd95d1c4142",
        titulo = "Cómo aprende una máquina",
        urlStream = "https://example.com/capsulas/ia-02.mp4",
        categoria = "Aprendizaje automático",
        duracionSegundos = 60,
    )
    val CAPSULA_IA_03 = Capsula(
        id = "ca694e3c-4d6d-593b-8f1d-9f9affe21e64",
        titulo = "Usar la IA de forma responsable",
        urlStream = "https://example.com/capsulas/ia-03.mp4",
        categoria = "Ética",
        duracionSegundos = 55,
    )
    val capsulas: List<Capsula> = listOf(CAPSULA_IA_01, CAPSULA_IA_02, CAPSULA_IA_03)

    // ---------- Puntos de interés ----------
    // 13 activos y completos, 1 completo pero desactivado (Parque Almagro)
    // y 1 incompleto que no se puede activar (Palacio Matte: sin imagen, video ni QR).
    // Fuente: https://mapcarta.com/37333438
    val PUNTO_LA_MONEDA = PuntoInteres(
        id = "4e2a401d-572c-52e2-a9d5-bb847e2fedf5",
        nombre = "Palacio de La Moneda",
        descripcion = "Sede de la Presidencia de la República de Chile.",
        tipo = TipoPuntoInteres.AIPC,
        ubicacion = Ubicacion(latitud = -33.44294, longitud = -70.65388, altitud = 537.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Palacio_de_La_Moneda",
        codigoQr = "TRISKEL-PI-001",
        capsulaId = CAPSULA_IA_01.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/N1297763053  · altitud aprox.
    val PUNTO_PRECOLOMBINO = PuntoInteres(
        id = "83d6034b-21d0-5ec9-a45a-54d2da0df299",
        nombre = "Museo Chileno de Arte Precolombino",
        descripcion = "Museo dedicado al arte de los pueblos originarios de América.",
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.439025, longitud = -70.652195, altitud = 545.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Museo_Chileno_de_Arte_Precolombino",
        codigoQr = "TRISKEL-PI-002",
        capsulaId = CAPSULA_IA_02.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/26953352
    val PUNTO_BELLAS_ARTES = PuntoInteres(
        id = "379bbd72-0e1c-5c6f-9430-69a82c77ab95",
        nombre = "Museo Nacional de Bellas Artes",
        descripcion = "Museo de arte ubicado en el Parque Forestal.",
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.43534, longitud = -70.64358, altitud = 584.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Museo_Nacional_de_Bellas_Artes_(Chile)",
        codigoQr = "TRISKEL-PI-003",
        capsulaId = CAPSULA_IA_03.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/37333118
    val PUNTO_CATEDRAL = PuntoInteres(
        id = "8dc9be32-5a90-56a6-9961-b59ecc59cb7e",
        nombre = "Catedral Metropolitana de Santiago",
        descripcion = "Catedral de Santiago, frente a la Plaza de Armas.",
        tipo = TipoPuntoInteres.AR,
        ubicacion = Ubicacion(latitud = -33.43761, longitud = -70.65141, altitud = 544.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Catedral_Metropolitana_de_Santiago",
        codigoQr = "TRISKEL-PI-004",
        capsulaId = CAPSULA_IA_01.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/28447634
    val PUNTO_PLAZA_ARMAS = PuntoInteres(
        id = "8d67461c-2118-56f3-8469-7296bd95fe73",
        nombre = "Plaza de Armas",
        descripcion = "Plaza fundacional de la ciudad de Santiago.",
        tipo = TipoPuntoInteres.ZCH,
        ubicacion = Ubicacion(latitud = -33.43728, longitud = -70.65029, altitud = 574.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Plaza_de_Armas_de_Santiago",
        codigoQr = "TRISKEL-PI-005",
        capsulaId = CAPSULA_IA_02.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/es/W24964188  · altitud aprox.
    val PUNTO_MERCADO_CENTRAL = PuntoInteres(
        id = "003b6f58-c9b2-52dd-b71c-8b5de5f14395",
        nombre = "Mercado Central de Santiago",
        descripcion = "Mercado con estructura de fierro, junto al río Mapocho.",
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.433777, longitud = -70.651039, altitud = 535.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Mercado_Central_de_Santiago",
        codigoQr = "TRISKEL-PI-006",
        capsulaId = CAPSULA_IA_03.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W23321628
    val PUNTO_SANTA_LUCIA = PuntoInteres(
        id = "73b78b30-310d-597d-8e75-281819f6d7d7",
        nombre = "Cerro Santa Lucía",
        descripcion = null,
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.44001, longitud = -70.64332, altitud = 629.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Cerro_Santa_Lucía",
        codigoQr = "TRISKEL-PI-007",
        capsulaId = CAPSULA_IA_01.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W112317702  · altitud aprox.
    val PUNTO_TEATRO_MUNICIPAL = PuntoInteres(
        id = "159b6b8d-0d92-58bf-a67f-dd52a16657c8",
        nombre = "Teatro Municipal de Santiago",
        descripcion = "Teatro de ópera, ballet y conciertos en el centro de Santiago.",
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.4411, longitud = -70.64756, altitud = 560.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Teatro_Municipal_de_Santiago",
        codigoQr = "TRISKEL-PI-008",
        capsulaId = CAPSULA_IA_02.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W41691117  · altitud aprox.
    val PUNTO_SACRAMENTINOS = PuntoInteres(
        id = "d1d0bc0a-7b9b-55f0-80a6-bd11aa9d6fb9",
        nombre = "Basílica de los Sacramentinos",
        descripcion = null,
        tipo = TipoPuntoInteres.AR,
        ubicacion = Ubicacion(latitud = -33.451344, longitud = -70.649306, altitud = 555.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-009",
        capsulaId = CAPSULA_IA_03.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W24964200  · altitud aprox.
    val PUNTO_ESTACION_MAPOCHO = PuntoInteres(
        id = "d3463398-0481-567d-ae94-3e7697a5320a",
        nombre = "Estación Mapocho",
        descripcion = "Antigua estación de trenes, hoy centro cultural.",
        tipo = TipoPuntoInteres.AIME,
        ubicacion = Ubicacion(latitud = -33.43223, longitud = -70.65458, altitud = 530.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-010",
        capsulaId = CAPSULA_IA_01.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W180191376  · altitud aprox.
    val PUNTO_PALACIO_COUSINO = PuntoInteres(
        id = "f8c23cf9-a61c-52b0-a7b6-b1b58b4d7ebb",
        nombre = "Palacio Cousiño",
        descripcion = "Palacio residencial en calle Dieciocho.",
        tipo = TipoPuntoInteres.ARYP,
        ubicacion = Ubicacion(latitud = -33.45217, longitud = -70.65663, altitud = 545.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = "https://es.wikipedia.org/wiki/Palacio_Cousiño",
        codigoQr = "TRISKEL-PI-011",
        capsulaId = CAPSULA_IA_02.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W208401630  · altitud aprox.
    val PUNTO_CORAZON_MARIA = PuntoInteres(
        id = "c42b2d96-e2d0-59b6-92a4-5bd389908afc",
        nombre = "Basílica del Corazón de María",
        descripcion = null,
        tipo = TipoPuntoInteres.AR,
        ubicacion = Ubicacion(latitud = -33.455394, longitud = -70.650908, altitud = 545.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-012",
        capsulaId = CAPSULA_IA_03.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/W895887128  · altitud aprox.
    val PUNTO_MUSEO_MERCED = PuntoInteres(
        id = "59ea0d4f-e82f-52ce-8d5f-6b01c2440683",
        nombre = "Museo de la Merced",
        descripcion = null,
        tipo = TipoPuntoInteres.MONUMENTO,
        ubicacion = Ubicacion(latitud = -33.43854, longitud = -70.64682, altitud = 548.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-013",
        capsulaId = CAPSULA_IA_01.id,
        activo = true,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/37333138
    val PUNTO_PARQUE_ALMAGRO = PuntoInteres(
        id = "0a356dea-e933-5967-8aee-4a4c2b7789b8",
        nombre = "Parque Almagro",
        descripcion = null,
        tipo = TipoPuntoInteres.ZCH,
        ubicacion = Ubicacion(latitud = -33.45202, longitud = -70.65363, altitud = 526.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-014",
        capsulaId = CAPSULA_IA_02.id,
        activo = false,
        actualizadoEn = FECHA_BASE,
    )
    // Fuente: https://mapcarta.com/es/W448519539  · altitud aprox.
    val PUNTO_PALACIO_MATTE = PuntoInteres(
        id = "f7be64ee-14f8-5e9a-b3e9-3ba3f8426d41",
        nombre = "Palacio Matte",
        descripcion = null,
        tipo = TipoPuntoInteres.ARYP,
        ubicacion = Ubicacion(latitud = -33.43878, longitud = -70.65701, altitud = 544.0),
        imagenUri = null,
        descubreMasUrl = null,
        codigoQr = null,
        capsulaId = null,
        activo = false,
        actualizadoEn = FECHA_BASE,
    )
    val puntos: List<PuntoInteres> = listOf(
        PUNTO_LA_MONEDA,
        PUNTO_PRECOLOMBINO,
        PUNTO_BELLAS_ARTES,
        PUNTO_CATEDRAL,
        PUNTO_PLAZA_ARMAS,
        PUNTO_MERCADO_CENTRAL,
        PUNTO_SANTA_LUCIA,
        PUNTO_TEATRO_MUNICIPAL,
        PUNTO_SACRAMENTINOS,
        PUNTO_ESTACION_MAPOCHO,
        PUNTO_PALACIO_COUSINO,
        PUNTO_CORAZON_MARIA,
        PUNTO_MUSEO_MERCED,
        PUNTO_PARQUE_ALMAGRO,
        PUNTO_PALACIO_MATTE,
    )

    // ---------- Usuarios ----------
    // Contraseña de todos: "Demo1234".
    val USUARIO_CAMILA = Usuario(
        id = "0b649aaf-8a45-5034-88ad-d6afaa6a408f",
        username = "camila_explora",
        correo = "camila.explora@ejemplo.cl",
        claveHash = "/IS44nDdmqtpFrZvxYfIdFEpfYOKPOGI3B8BYCPUyZY=",
        claveSal = "LS6fBUpJ12bZK0zVAbBs+A==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_TOMAS = Usuario(
        id = "b7dc5930-5783-5b18-bc33-0522a4babdae",
        username = "tomas_ruta",
        correo = "tomas.ruta@ejemplo.cl",
        claveHash = "raiMl1hr2uWhNykkJtKAAlfqSHV1T57Go1FaNZEQqHM=",
        claveSal = "r56gZjF9+tc5ZIXOKtf8Cg==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_NUEVA = Usuario(
        id = "161ab0d1-c8a3-5449-9ac4-843a6b73d9bc",
        username = "usuaria_nueva",
        correo = "usuaria.nueva@ejemplo.cl",
        claveHash = "u4iQ1vH4MtGWZ5fuChSL895p4a6PWv5WgnzBuflM4Xc=",
        claveSal = "PINOWCztWoNKd2ScqD2h9A==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_ADMIN = Usuario(
        id = "2d45eab4-290c-50cf-8f1c-d0c92ceced6d",
        username = "admin_triskel",
        correo = "admin@ejemplo.cl",
        claveHash = "o/CQp8qZzdObHB2J3KCW2Bu+XNF/TzMe4Oqn70Ny2JY=",
        claveSal = "5fQwkjPE1PiG7Q4r3kmDHw==",
        rol = RolUsuario.ADMINISTRADOR,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_CONTENIDO = Usuario(
        id = "3da6e8de-43cb-54c7-bf54-934bf4e5bc2d",
        username = "contenido_ia",
        correo = "contenido@ejemplo.cl",
        claveHash = "JaCF7Bqk1Zwb2dxy8BlO/oE7enLUpOSfD6TO0jNBi5o=",
        claveSal = "/xBmiH9HPIyRv25T5aQEeg==",
        rol = RolUsuario.GENERADOR,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val usuarios: List<Usuario> = listOf(USUARIO_CAMILA, USUARIO_TOMAS, USUARIO_NUEVA, USUARIO_ADMIN, USUARIO_CONTENIDO)

    // ---------- Preferencias ----------
    val preferencias: List<Preferencias> = listOf(
        Preferencias(usuarioId = USUARIO_CAMILA.id, temaNoche = false, notifLugaresCercanos = true, notifNuevosVideos = true, seguirUbicacion = true, mostrarBloqueados = true),
        Preferencias(usuarioId = USUARIO_TOMAS.id, temaNoche = true, notifLugaresCercanos = true, notifNuevosVideos = false, seguirUbicacion = true, mostrarBloqueados = true),
        Preferencias(usuarioId = USUARIO_NUEVA.id, temaNoche = false, notifLugaresCercanos = true, notifNuevosVideos = false, seguirUbicacion = true, mostrarBloqueados = true),
        Preferencias(usuarioId = USUARIO_ADMIN.id, temaNoche = false, notifLugaresCercanos = true, notifNuevosVideos = false, seguirUbicacion = true, mostrarBloqueados = true),
        Preferencias(usuarioId = USUARIO_CONTENIDO.id, temaNoche = false, notifLugaresCercanos = true, notifNuevosVideos = false, seguirUbicacion = true, mostrarBloqueados = true),
    )

    // ---------- Ubicaciones fijas para la versión Fake del GPS ----------
    /** A 35.0 m de La Moneda: el escaneo se acepta. */
    val UBICACION_CERCA_LA_MONEDA = Ubicacion(latitud = -33.442626, longitud = -70.65388, altitud = 537.0)
    /** A 58.9 m de La Moneda: justo dentro del radio. */
    val UBICACION_BORDE_DENTRO = Ubicacion(latitud = -33.44241, longitud = -70.65388, altitud = 537.0)
    /** A 60.9 m de La Moneda: justo fuera del radio, el escaneo se rechaza. */
    val UBICACION_BORDE_FUERA = Ubicacion(latitud = -33.442392, longitud = -70.65388, altitud = 537.0)
    /** En la Plaza de Armas, a 712 m de La Moneda: lejos para escanear La Moneda. */
    val UBICACION_PLAZA_DE_ARMAS = Ubicacion(latitud = -33.43728, longitud = -70.65029, altitud = 574.0)
}
