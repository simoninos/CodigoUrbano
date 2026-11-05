package com.cookingplastic.triskeledu.data.local

import com.cookingplastic.triskeledu.data.model.Capsula
import com.cookingplastic.triskeledu.data.model.CategoriaCapsula
import com.cookingplastic.triskeledu.data.model.Comentario
import com.cookingplastic.triskeledu.data.model.EstadoCapsula
import com.cookingplastic.triskeledu.data.model.EstadoSolicitud
import com.cookingplastic.triskeledu.data.model.PUNTOS_CAPSULA
import com.cookingplastic.triskeledu.data.model.PUNTOS_SELFIE
import com.cookingplastic.triskeledu.data.model.Preferencias
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import com.cookingplastic.triskeledu.data.model.PuntoInteres
import com.cookingplastic.triskeledu.data.model.RolUsuario
import com.cookingplastic.triskeledu.data.model.TipoPuntoInteres
import com.cookingplastic.triskeledu.data.model.Ubicacion
import com.cookingplastic.triskeledu.data.model.Usuario
import java.util.UUID

/**
 * Datos de prueba de la app.
 *
 * - Puntos de interés: 16 lugares reales de Santiago Centro. Las coordenadas vienen de Mapcarta
 *   (datos de OpenStreetMap); cada punto indica su fuente. Las altitudes marcadas "aprox."
 *   son estimaciones. El tipo de cada punto es de prueba y no corresponde a su declaratoria oficial.
 * - Usuarios, cápsulas, preferencias, progreso y comentarios: ficticios.
 * - Contraseña de todos los usuarios: "Demo1234". Hash PBKDF2WithHmacSHA256 con
 *   120000 iteraciones, clave de 32 bytes y sal de 16 bytes, en Base64 sin saltos de línea.
 * - Los identificadores son UUID fijos, para poder referirse a cada registro desde el código.
 */
object DatosPrueba {

    /** Fecha de referencia de los datos: 2026-10-01 12:00 (Chile). */
    const val FECHA_BASE: Long = 1790866800000L
    const val CLAVE_DE_PRUEBA = "Demo1234"
    const val IMAGEN_PRUEBA = "android.resource://com.cookingplastic.triskeledu/drawable/punto_placeholder"
    //clip vertical de unos 20 s incluido en la app (res/raw), para probar el reproductor sin internet (RN-36)
    const val CLIP_PRUEBA = "android.resource://com.cookingplastic.triskeledu/raw/capsula_prueba"

    // ---------- ENLACES_CAPSULAS ----------
    // Cuando existan los videos (V1 a V6), pega aquí el enlace de compartir de Google Drive de cada punto,
    // entre comillas y en vez de null. Por ejemplo: val LA_MONEDA: String? = "https://drive.google.com/file/d/ID/view"
    // Mientras quede en null, la cápsula funciona sin enlace (RN-35); La Moneda usa el clip de prueba (RN-36).
    // Para que se vean, hay que desinstalar la app o borrar sus datos: la base se llena una sola vez.
    object ENLACES_CAPSULAS {
        val LA_MONEDA: String? = null
        val PRECOLOMBINO: String? = null
        val BELLAS_ARTES: String? = null
        val CATEDRAL: String? = null
        val PLAZA_ARMAS: String? = null
        val MERCADO_CENTRAL: String? = null
        val SANTA_LUCIA: String? = null
        val TEATRO_MUNICIPAL: String? = null
        val SACRAMENTINOS: String? = null
        val ESTACION_MAPOCHO: String? = null
        val PALACIO_COUSINO: String? = null
        val CORAZON_MARIA: String? = null
        val MUSEO_MERCED: String? = null
        val PADRE_ALONSO_OVALLE: String? = null
        val PARQUE_ALMAGRO: String? = null
    }

    // ---------- Usuarios ----------
    // Uno por nivel (1 a 8), el administrador y uno con solicitud de creador pendiente. Contraseña de todos: "Demo1234".
    // Van antes que las cápsulas porque cada cápsula guarda el id de su creador (contenido_ia).
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
        rol = RolUsuario.CREADOR,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_EXPLORADOR = Usuario(
        id = "3396dbb4-e1ca-5d63-94e3-24960c2388f0",
        username = "explorador_demo",
        correo = "explorador.demo@ejemplo.cl",
        claveHash = "CInhmeeffv3jAYsQ4IaoVTHAEpNHqf02b6RN8aWQfyI=",
        claveSal = "miVdZRmtfGja7XUpPLZNgw==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_GUARDIAN = Usuario(
        id = "6ff3eafc-05b8-58da-8c31-2670817e76ba",
        username = "guardian_demo",
        correo = "guardian.demo@ejemplo.cl",
        claveHash = "4uURltgOyDUeR+FbEdikCdBa+VPPj2OD9RjV4ioAc5c=",
        claveSal = "sUTmopOAq7StRD1UpxjNEg==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_SABIO = Usuario(
        id = "5759134f-d1d2-50d7-94d9-4356358c7b78",
        username = "sabio_demo",
        correo = "sabio.demo@ejemplo.cl",
        claveHash = "jpa8OAINtfolMjW11+xU126d6sfd6i992CaaXy6opp0=",
        claveSal = "jyvy1HNIWdf9Hr+3gNYGhA==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    val USUARIO_MAESTRO = Usuario(
        id = "278657b1-458e-516b-9a00-3aaf31038f8a",
        username = "maestro_demo",
        correo = "maestro.demo@ejemplo.cl",
        claveHash = "pXZM7EOTlQh4PlKXUG9AzgkuROwtSH8ajoikdD4VT2w=",
        claveSal = "NigOUV8+AsYoViNJfTsRBw==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
    )
    // Pidió ser creador al registrarse: no puede entrar hasta que el administrador responda (RN-29)
    val USUARIO_PENDIENTE = Usuario(
        id = "a359dd22-2a9c-5d55-b44f-6636972a25ca",
        username = "pendiente_demo",
        correo = "pendiente.demo@ejemplo.cl",
        claveHash = "fsqgFdix6zso2bBUA/+KTFxi5JoGkGQi7N7tDlgNRvg=",
        claveSal = "J2fcgvp1chfV4unmI+Z1Vw==",
        rol = RolUsuario.CLIENTE,
        terminosVersion = "1.0",
        terminosAceptadosEn = FECHA_BASE,
        creadoEn = FECHA_BASE,
        solicitudCreador = EstadoSolicitud.PENDIENTE,
        solicitadoEn = FECHA_BASE,
    )
    // En el orden del acceso rápido: un usuario por nivel, después el administrador y el pendiente
    val usuarios: List<Usuario> = listOf(
        USUARIO_NUEVA, USUARIO_EXPLORADOR, USUARIO_CAMILA, USUARIO_TOMAS, USUARIO_CONTENIDO,
        USUARIO_GUARDIAN, USUARIO_SABIO, USUARIO_MAESTRO, USUARIO_ADMIN, USUARIO_PENDIENTE,
    )

    // ---------- Cápsulas ----------
    // Una cápsula propia por punto (RN-20), todas PUBLICADAS por contenido_ia, más una en borrador y una
    // publicada, las dos sin punto.
    // Sin enlace (null) salvo La Moneda, que usa el clip de prueba hasta tener su video (RN-35, RN-36).
    private fun capsulaPublicada(id: String, titulo: String, descripcion: String, categoria: CategoriaCapsula, duracion: Int, enlace: String?) =
        Capsula(
            id = id,
            titulo = titulo,
            descripcion = descripcion,
            urlStream = enlace,
            categoria = categoria,
            duracionSegundos = duracion,
            estado = EstadoCapsula.PUBLICADA,
            creadorId = USUARIO_CONTENIDO.id,
            creadaEn = FECHA_BASE,
            publicadaEn = FECHA_BASE,
        )
    val CAPSULA_LA_MONEDA = capsulaPublicada(
        id = "26ad7dd3-f096-5aa5-9af8-bd960eb2b377",
        titulo = "¿Qué es la inteligencia artificial?",
        descripcion = "Una introducción corta a qué hace una IA y qué no.",
        categoria = CategoriaCapsula.QUE_ES_IA,
        duracion = 20,
        enlace = ENLACES_CAPSULAS.LA_MONEDA ?: CLIP_PRUEBA,
    )
    val CAPSULA_PRECOLOMBINO = capsulaPublicada(
        id = "40b5663b-077d-5d87-bad8-efd95d1c4142",
        titulo = "Cómo aprende una máquina",
        descripcion = "Ejemplos y patrones: la base del aprendizaje automático.",
        categoria = CategoriaCapsula.QUE_ES_IA,
        duracion = 60,
        enlace = ENLACES_CAPSULAS.PRECOLOMBINO,
    )
    val CAPSULA_BELLAS_ARTES = capsulaPublicada(
        id = "ca694e3c-4d6d-593b-8f1d-9f9affe21e64",
        titulo = "IA que crea imágenes: ¿quién es el artista?",
        descripcion = "Qué pasa con la autoría cuando una IA pinta.",
        categoria = CategoriaCapsula.IA_RESPONSABLE,
        duracion = 90,
        enlace = ENLACES_CAPSULAS.BELLAS_ARTES,
    )
    val CAPSULA_CATEDRAL = capsulaPublicada(
        id = "b65f07e5-4591-58ab-9c0e-d0806c0d2042",
        titulo = "Restaurar el patrimonio con visión por computador",
        descripcion = "Cómo una cámara y una IA ayudan a cuidar edificios antiguos.",
        categoria = CategoriaCapsula.HISTORIA_PATRIMONIO,
        duracion = 120,
        enlace = ENLACES_CAPSULAS.CATEDRAL,
    )
    val CAPSULA_PLAZA_ARMAS = capsulaPublicada(
        id = "9fa4bdeb-8dca-563b-a114-11d0672287ac",
        titulo = "La ciudad que aprende: sensores y datos",
        descripcion = "Datos del tránsito y del aire que una ciudad puede usar mejor.",
        categoria = CategoriaCapsula.IA_CIUDAD,
        duracion = 100,
        enlace = ENLACES_CAPSULAS.PLAZA_ARMAS,
    )
    val CAPSULA_MERCADO_CENTRAL = capsulaPublicada(
        id = "dfc2dfaf-63a1-51f0-90ac-f8de83a22f98",
        titulo = "Recomendaciones: cómo una app adivina lo que te gusta",
        descripcion = "Los sistemas que sugieren música, videos y compras.",
        categoria = CategoriaCapsula.IA_VIDA_DIARIA,
        duracion = 80,
        enlace = ENLACES_CAPSULAS.MERCADO_CENTRAL,
    )
    val CAPSULA_SANTA_LUCIA = capsulaPublicada(
        id = "2869a974-6053-5600-b71d-574229d91702",
        titulo = "Mapas y rutas: la IA que te guía",
        descripcion = "Cómo una app calcula el mejor camino.",
        categoria = CategoriaCapsula.IA_CIUDAD,
        duracion = 75,
        enlace = ENLACES_CAPSULAS.SANTA_LUCIA,
    )
    val CAPSULA_TEATRO_MUNICIPAL = capsulaPublicada(
        id = "1d0455b2-3901-5775-af62-be53f4fcdbe7",
        titulo = "Música compuesta con IA",
        descripcion = "Qué puede crear una IA y qué sigue siendo humano.",
        categoria = CategoriaCapsula.IA_VIDA_DIARIA,
        duracion = 110,
        enlace = ENLACES_CAPSULAS.TEATRO_MUNICIPAL,
    )
    val CAPSULA_SACRAMENTINOS = capsulaPublicada(
        id = "9a9eab82-3aea-55f3-99dc-ac127a8760f2",
        titulo = "Reconocer estilos arquitectónicos con IA",
        descripcion = "Una IA que distingue estilos a partir de fotos.",
        categoria = CategoriaCapsula.HISTORIA_PATRIMONIO,
        duracion = 95,
        enlace = ENLACES_CAPSULAS.SACRAMENTINOS,
    )
    val CAPSULA_ESTACION_MAPOCHO = capsulaPublicada(
        id = "099f50aa-4c1b-58f2-ba63-de19da751217",
        titulo = "Transporte inteligente en Santiago",
        descripcion = "Frecuencias y horarios que se ajustan con datos.",
        categoria = CategoriaCapsula.IA_CIUDAD,
        duracion = 130,
        enlace = ENLACES_CAPSULAS.ESTACION_MAPOCHO,
    )
    val CAPSULA_PALACIO_COUSINO = capsulaPublicada(
        id = "73c6310f-8e66-58b3-8486-1adabd98d20d",
        titulo = "Digitalizar archivos históricos con IA",
        descripcion = "Leer documentos antiguos sin dañarlos.",
        categoria = CategoriaCapsula.HISTORIA_PATRIMONIO,
        duracion = 105,
        enlace = ENLACES_CAPSULAS.PALACIO_COUSINO,
    )
    val CAPSULA_CORAZON_MARIA = capsulaPublicada(
        id = "c1e91a22-ffcf-5366-9d12-668bd798a86c",
        titulo = "Sesgos: cuando la IA se equivoca",
        descripcion = "Por qué una IA puede ser injusta y cómo evitarlo.",
        categoria = CategoriaCapsula.IA_RESPONSABLE,
        duracion = 140,
        enlace = ENLACES_CAPSULAS.CORAZON_MARIA,
    )
    val CAPSULA_MUSEO_MERCED = capsulaPublicada(
        id = "ee7f8867-9edb-5f80-8e88-e6fbfac48cd6",
        titulo = "Traductores automáticos en el museo",
        descripcion = "Cómo una IA traduce textos y audios para los visitantes.",
        categoria = CategoriaCapsula.IA_VIDA_DIARIA,
        duracion = 85,
        enlace = ENLACES_CAPSULAS.MUSEO_MERCED,
    )
    val CAPSULA_PADRE_ALONSO_OVALLE = capsulaPublicada(
        id = "bd5c9225-a0a9-504e-af83-56bfe0af2957",
        titulo = "Aprender con IA: tutores y estudiantes",
        descripcion = "Usar la IA para estudiar sin dejar de pensar por uno mismo.",
        categoria = CategoriaCapsula.IA_VIDA_DIARIA,
        duracion = 150,
        enlace = ENLACES_CAPSULAS.PADRE_ALONSO_OVALLE,
    )
    val CAPSULA_PARQUE_ALMAGRO = capsulaPublicada(
        id = "a0c0b266-5567-5048-940d-789289e86996",
        titulo = "Privacidad: tus datos y la IA",
        descripcion = "Qué datos entregas al usar una app y cómo cuidarlos.",
        categoria = CategoriaCapsula.IA_RESPONSABLE,
        duracion = 115,
        enlace = ENLACES_CAPSULAS.PARQUE_ALMAGRO,
    )
    // En borrador y sin punto: para ver las pantallas del creador (P17 y P18)
    val CAPSULA_BORRADOR = Capsula(
        id = "3ea41da3-6967-53c8-8e2b-ce39fc142834",
        titulo = "Chatbots: conversar con una máquina",
        descripcion = "Cómo responde un asistente de chat y cuándo desconfiar.",
        urlStream = null,
        categoria = CategoriaCapsula.QUE_ES_IA,
        duracionSegundos = 70,
        estado = EstadoCapsula.BORRADOR,
        creadorId = USUARIO_CONTENIDO.id,
        creadaEn = FECHA_BASE,
    )
    // Publicada y sin punto (CRE-08): el creador la puede asociar a Palacio Matte, que está Sin cápsula
    val CAPSULA_LIBRE = capsulaPublicada(
        id = "7c2f4e1a-9b3d-5e6f-8a1b-2c3d4e5f6a7b",
        titulo = "La IA que ordena el tránsito",
        descripcion = "Cómo los semáforos inteligentes aprenden del flujo de autos.",
        categoria = CategoriaCapsula.IA_CIUDAD,
        duracion = 64,
        enlace = null,
    )
    val capsulas: List<Capsula> = listOf(
        CAPSULA_LA_MONEDA,
        CAPSULA_PRECOLOMBINO,
        CAPSULA_BELLAS_ARTES,
        CAPSULA_CATEDRAL,
        CAPSULA_PLAZA_ARMAS,
        CAPSULA_MERCADO_CENTRAL,
        CAPSULA_SANTA_LUCIA,
        CAPSULA_TEATRO_MUNICIPAL,
        CAPSULA_SACRAMENTINOS,
        CAPSULA_ESTACION_MAPOCHO,
        CAPSULA_PALACIO_COUSINO,
        CAPSULA_CORAZON_MARIA,
        CAPSULA_MUSEO_MERCED,
        CAPSULA_PADRE_ALONSO_OVALLE,
        CAPSULA_PARQUE_ALMAGRO,
        CAPSULA_BORRADOR,
        CAPSULA_LIBRE,
    )

    // RN-14 (excepción de demo): estas cápsulas activan su punto aunque no tengan enlace.
    // by lazy: se arma la primera vez que se pide, cuando la lista de arriba ya existe
    val idsCapsulasPrueba: Set<String> by lazy { capsulas.map { it.id }.toSet() }

    // ---------- Puntos de interés ----------
    // 16 puntos: 14 activos con su cápsula, Parque Almagro inactivo (con cápsula publicada)
    // y Palacio Matte sin cápsula (no se puede activar). Así se ven los tres estados del administrador (RN-27).
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
        capsulaId = CAPSULA_LA_MONEDA.id,
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
        capsulaId = CAPSULA_PRECOLOMBINO.id,
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
        capsulaId = CAPSULA_BELLAS_ARTES.id,
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
        capsulaId = CAPSULA_CATEDRAL.id,
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
        capsulaId = CAPSULA_PLAZA_ARMAS.id,
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
        capsulaId = CAPSULA_MERCADO_CENTRAL.id,
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
        capsulaId = CAPSULA_SANTA_LUCIA.id,
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
        capsulaId = CAPSULA_TEATRO_MUNICIPAL.id,
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
        capsulaId = CAPSULA_SACRAMENTINOS.id,
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
        capsulaId = CAPSULA_ESTACION_MAPOCHO.id,
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
        capsulaId = CAPSULA_PALACIO_COUSINO.id,
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
        capsulaId = CAPSULA_CORAZON_MARIA.id,
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
        capsulaId = CAPSULA_MUSEO_MERCED.id,
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
        capsulaId = CAPSULA_PARQUE_ALMAGRO.id,
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
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-015",
        capsulaId = null,
        activo = false,
        actualizadoEn = FECHA_BASE,
    )
    // Rescatado de la rama prueba (commit 89cdc2f) con un QR nuevo: el de esa rama chocaba con Parque Almagro
    val PUNTO_PADRE_ALONSO_OVALLE = PuntoInteres(
        id = "f7be64ee-14f8-5e9a-b1e9-3ba3f8426d41",
        nombre = "Padre Alonso de Ovalle",
        descripcion = null,
        tipo = TipoPuntoInteres.ARYP,
        ubicacion = Ubicacion(latitud = -33.44699124944042, longitud = -70.65788550858184, altitud = 544.0),
        imagenUri = IMAGEN_PRUEBA,
        descubreMasUrl = null,
        codigoQr = "TRISKEL-PI-016",
        capsulaId = CAPSULA_PADRE_ALONSO_OVALLE.id,
        activo = true,
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
        PUNTO_PADRE_ALONSO_OVALLE,
        PUNTO_PARQUE_ALMAGRO,
        PUNTO_PALACIO_MATTE,
    )

    // ---------- Preferencias ----------
    // Solo se escribe lo que no es el valor por defecto. nivelMaximoAlcanzado es el nivel de cada uno:
    // así no aparece "¡Subiste de nivel!" la primera vez que entran (RN-34)
    val preferencias: List<Preferencias> = listOf(
        Preferencias(usuarioId = USUARIO_NUEVA.id),
        Preferencias(usuarioId = USUARIO_EXPLORADOR.id, nivelMaximoAlcanzado = 2),
        Preferencias(usuarioId = USUARIO_CAMILA.id, notifNuevasCapsulas = true, nivelMaximoAlcanzado = 3),
        Preferencias(usuarioId = USUARIO_TOMAS.id, temaNoche = true, nivelMaximoAlcanzado = 4),
        Preferencias(usuarioId = USUARIO_CONTENIDO.id, nivelMaximoAlcanzado = 5),
        Preferencias(usuarioId = USUARIO_GUARDIAN.id, nivelMaximoAlcanzado = 6),
        Preferencias(usuarioId = USUARIO_SABIO.id, nivelMaximoAlcanzado = 7),
        Preferencias(usuarioId = USUARIO_MAESTRO.id, nivelMaximoAlcanzado = 8),
        Preferencias(usuarioId = USUARIO_ADMIN.id),
        Preferencias(usuarioId = USUARIO_PENDIENTE.id),
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
    /** A unos 300 m de la Catedral: el servidor simulado rechaza un escaneo hecho desde aquí (EXC-08). */
    val UBICACION_LEJOS_CATEDRAL = Ubicacion(latitud = -33.43491, longitud = -70.65141, altitud = 544.0)

    // ---------- Progreso y comentarios ----------
    // Van al final porque usan los puntos, los usuarios y las ubicaciones de arriba: en un object,
    // las propiedades se crean en orden, y una que aún no se crea vale null.
    // Las fechas son relativas al momento en que se abre la app, para que "recién descubierto"
    // (menos de un día) se vea siempre.
    private const val UNA_HORA_MS = 60L * 60L * 1000L
    private val AHORA = System.currentTimeMillis()

    // Una fila de progreso de prueba: el QR siempre está escaneado (10); cápsula (50) y selfie (20) según se pida.
    // El id sale del usuario y el punto, así queda fijo sin escribirlo a mano.
    // SIM-03: casi todas ya las confirmó el servidor (sincronizado = true); la que no, se revisa al entrar
    private fun progresoDePrueba(
        usuario: Usuario,
        punto: PuntoInteres,
        haceHoras: Long,
        capsula: Boolean = false,
        selfie: Boolean = false,
        dondeEscaneo: Ubicacion = punto.ubicacion,
        id: String = UUID.nameUUIDFromBytes("progreso:${usuario.id}:${punto.id}".toByteArray()).toString(),
        sincronizado: Boolean = true,
    ): ProgresoPunto {
        val escaneadoEn = AHORA - haceHoras * UNA_HORA_MS
        return ProgresoPunto(
            id = id,
            usuarioId = usuario.id,
            puntoId = punto.id,
            escaneadoEn = escaneadoEn,
            escaneoUbicacion = dondeEscaneo,
            capsulaVistaEn = if (capsula) escaneadoEn + UNA_HORA_MS / 2 else null,
            puntosCapsula = if (capsula) PUNTOS_CAPSULA else 0,
            selfieUri = if (selfie) IMAGEN_PRUEBA else null,
            selfieEn = if (selfie) escaneadoEn + UNA_HORA_MS else null,
            puntosSelfie = if (selfie) PUNTOS_SELFIE else 0,
            sincronizado = sincronizado,
        )
    }

    private fun completados(usuario: Usuario, vararg puntos: PuntoInteres): List<ProgresoPunto> =
        puntos.mapIndexed { i, punto -> progresoDePrueba(usuario, punto, haceHoras = 24L * (i + 2), capsula = true, selfie = true) }

    // Cada usuario queda dentro del rango de puntos de su nivel (RN-31): 0, 35, 105, 210, 350, 525, 735 y 980.
    // Padre Alonso de Ovalle no lo tiene nadie, para poder escanearlo en la demostración
    val progreso: List<ProgresoPunto> = buildList {
        // explorador_demo (nivel 2): 10 + 60 = 70
        add(progresoDePrueba(USUARIO_EXPLORADOR, PUNTO_PLAZA_ARMAS, haceHoras = 30))
        // EXC-08 y EXC-12: la Catedral se guardó sin conexión a 300 m y falta sincronizarla. Al entrar con
        // explorador_demo el servidor simulado la rechaza: quedan 10 puntos, baja al nivel 1 y conserva su tema
        add(progresoDePrueba(USUARIO_EXPLORADOR, PUNTO_CATEDRAL, haceHoras = 5, capsula = true,
            dondeEscaneo = UBICACION_LEJOS_CATEDRAL, sincronizado = false))
        // camila_explora (nivel 3): un lugar de cada estado de Mis lugares; 10 + 10 + 60 + 80 = 160
        // La Moneda: recién descubierto, con la cápsula sin ver (para probar el reproductor bloqueado)
        add(progresoDePrueba(USUARIO_CAMILA, PUNTO_LA_MONEDA, haceHoras = 2, dondeEscaneo = UBICACION_CERCA_LA_MONEDA, id = "13f69bdf-7d25-5822-8434-5546bde7784a"))
        // Catedral: escaneada hace 3 días, falta la cápsula
        add(progresoDePrueba(USUARIO_CAMILA, PUNTO_CATEDRAL, haceHoras = 72, id = "3a933908-1a05-5928-989a-b4d2d677070d"))
        // Bellas Artes: falta la selfie
        add(progresoDePrueba(USUARIO_CAMILA, PUNTO_BELLAS_ARTES, haceHoras = 50, capsula = true))
        // Precolombino: completado, 80 de 80
        add(progresoDePrueba(USUARIO_CAMILA, PUNTO_PRECOLOMBINO, haceHoras = 120, capsula = true, selfie = true, id = "d829bc4e-2691-5751-b95d-9531b295291d"))
        // tomas_ruta (nivel 4): 80 + 80 + 60 = 220
        add(progresoDePrueba(USUARIO_TOMAS, PUNTO_PLAZA_ARMAS, haceHoras = 48, capsula = true, selfie = true, dondeEscaneo = UBICACION_PLAZA_DE_ARMAS, id = "514157e8-5cf0-54fe-b846-3fdbd2976b4a"))
        add(progresoDePrueba(USUARIO_TOMAS, PUNTO_PRECOLOMBINO, haceHoras = 31, capsula = true, selfie = true))
        add(progresoDePrueba(USUARIO_TOMAS, PUNTO_LA_MONEDA, haceHoras = 51, capsula = true))
        // contenido_ia (nivel 5): 5 completados = 400
        addAll(completados(USUARIO_CONTENIDO, PUNTO_BELLAS_ARTES, PUNTO_SANTA_LUCIA, PUNTO_TEATRO_MUNICIPAL, PUNTO_MUSEO_MERCED, PUNTO_ESTACION_MAPOCHO))
        // guardian_demo (nivel 6): 7 completados = 560
        addAll(completados(USUARIO_GUARDIAN, PUNTO_LA_MONEDA, PUNTO_PRECOLOMBINO, PUNTO_BELLAS_ARTES, PUNTO_CATEDRAL,
            PUNTO_PLAZA_ARMAS, PUNTO_MERCADO_CENTRAL, PUNTO_SANTA_LUCIA))
        // sabio_demo (nivel 7): 10 completados = 800
        addAll(completados(USUARIO_SABIO, PUNTO_LA_MONEDA, PUNTO_PRECOLOMBINO, PUNTO_BELLAS_ARTES, PUNTO_CATEDRAL,
            PUNTO_PLAZA_ARMAS, PUNTO_MERCADO_CENTRAL, PUNTO_SANTA_LUCIA, PUNTO_TEATRO_MUNICIPAL, PUNTO_SACRAMENTINOS,
            PUNTO_ESTACION_MAPOCHO))
        // maestro_demo (nivel 8): 13 completados = 1040
        addAll(completados(USUARIO_MAESTRO, PUNTO_LA_MONEDA, PUNTO_PRECOLOMBINO, PUNTO_BELLAS_ARTES, PUNTO_CATEDRAL,
            PUNTO_PLAZA_ARMAS, PUNTO_MERCADO_CENTRAL, PUNTO_SANTA_LUCIA, PUNTO_TEATRO_MUNICIPAL, PUNTO_SACRAMENTINOS,
            PUNTO_ESTACION_MAPOCHO, PUNTO_PALACIO_COUSINO, PUNTO_CORAZON_MARIA, PUNTO_MUSEO_MERCED))
    }

    val comentarios: List<Comentario> = listOf(
        Comentario(
            id = "dfbc2f95-b730-5414-97a5-b986de8307c7",
            usuarioId = USUARIO_CAMILA.id,
            puntoId = PUNTO_PRECOLOMBINO.id,
            texto = "El video explica muy bien cómo aprende una máquina. Lo vería de nuevo.",
            creadoEn = AHORA - 118 * UNA_HORA_MS,
            sincronizado = true,
        ),
        Comentario(
            id = "f6fcc412-cc6c-56fd-ad0c-160c1bce4ff5",
            usuarioId = USUARIO_TOMAS.id,
            puntoId = PUNTO_PRECOLOMBINO.id,
            texto = "Vine con mi curso y la cerámica nos encantó.",
            creadoEn = AHORA - 30 * UNA_HORA_MS,
            sincronizado = true,
        ),
        Comentario(
            id = "fc7bcb4f-226f-5ff6-9a11-1d4cd134f4c5",
            usuarioId = USUARIO_TOMAS.id,
            puntoId = PUNTO_LA_MONEDA.id,
            texto = "Recomiendo llegar temprano para ver el cambio de guardia.",
            creadoEn = AHORA - 50 * UNA_HORA_MS,
            sincronizado = true,
        ),
    )
}
