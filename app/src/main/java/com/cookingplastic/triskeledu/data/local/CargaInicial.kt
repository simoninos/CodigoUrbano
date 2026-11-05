package com.cookingplastic.triskeledu.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

//Llena la base con los datos de prueba del equipo (DatosPrueba) la primera vez que se crea.
//onCreate corre una sola vez, al crear el archivo, y antes de que cualquier pantalla lea: no hay carreras.
//Aquí no se pueden usar los DAO (la base se está creando), por eso va SQL directo
object CargaInicial : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) = cargar(db)

    //Cuando sube la versión, fallbackToDestructiveMigration borra y recrea las tablas sin llamar a
    //onCreate, así que quedan vacías. Si al abrir no hay usuarios, se vuelven a cargar los datos de prueba
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.query("SELECT COUNT(*) FROM usuarios").use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) cargar(db)
        }
    }

    //El orden importa por las claves foráneas: usuarios antes que preferencias y cápsulas (su creador),
    //cápsulas antes que puntos, y puntos antes que el progreso
    private fun cargar(db: SupportSQLiteDatabase) {
        //las claves ya vienen hasheadas con el mismo PBKDF2 de HashContrasena (clave de todos: Demo1234)
        DatosPrueba.usuarios.forEach { u ->
            db.execSQL(
                "INSERT INTO usuarios (id, username, correo, claveHash, claveSal, rol, terminosVersion, terminosAceptadosEn, creadoEn, " +
                        "solicitudCreador, solicitadoEn, solicitudResueltaEn, solicitudResueltaPor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                //los enums se guardan como texto con .name, igual que en Converters
                arrayOf<Any?>(u.id, u.username, u.correo, u.claveHash, u.claveSal, u.rol.name, u.terminosVersion, u.terminosAceptadosEn,
                    u.creadoEn, u.solicitudCreador?.name, u.solicitadoEn, u.solicitudResueltaEn, u.solicitudResueltaPor)
            )
        }
        //SQLite no tiene Boolean: Room los guarda como 1 o 0
        DatosPrueba.preferencias.forEach { p ->
            db.execSQL(
                "INSERT INTO preferencias (usuarioId, temaNoche, notifLugaresCercanos, notifNuevasCapsulas, seguirUbicacion, mostrarBloqueados, " +
                        "notifSolicitudes, notifPuntosListos, mostrarInactivos, tema, nivelMaximoAlcanzado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(p.usuarioId, p.temaNoche.aNumero(), p.notifLugaresCercanos.aNumero(), p.notifNuevasCapsulas.aNumero(),
                    p.seguirUbicacion.aNumero(), p.mostrarBloqueados.aNumero(), p.notifSolicitudes.aNumero(), p.notifPuntosListos.aNumero(),
                    p.mostrarInactivos.aNumero(), p.tema.name, p.nivelMaximoAlcanzado)
            )
        }
        DatosPrueba.capsulas.forEach { c ->
            db.execSQL(
                "INSERT INTO capsulas (id, titulo, descripcion, urlStream, categoria, duracionSegundos, estado, creadorId, creadaEn, publicadaEn) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(c.id, c.titulo, c.descripcion, c.urlStream, c.categoria.name, c.duracionSegundos, c.estado.name,
                    c.creadorId, c.creadaEn, c.publicadaEn)
            )
        }
        //la ubicación del punto es @Embedded sin prefijo: columnas latitud, longitud y altitud
        DatosPrueba.puntos.forEach { p ->
            db.execSQL(
                "INSERT INTO puntos_interes (id, nombre, descripcion, tipo, latitud, longitud, altitud, imagenUri, " +
                        "descubreMasUrl, codigoQr, capsulaId, activo, actualizadoEn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(p.id, p.nombre, p.descripcion, p.tipo.name, p.ubicacion.latitud, p.ubicacion.longitud,
                    p.ubicacion.altitud, p.imagenUri, p.descubreMasUrl, p.codigoQr, p.capsulaId, p.activo.aNumero(), p.actualizadoEn)
            )
        }
        //la ubicación del escaneo usa @Embedded(prefix = "escaneo_")
        DatosPrueba.progreso.forEach { g ->
            db.execSQL(
                "INSERT INTO progreso_puntos (id, usuarioId, puntoId, escaneadoEn, escaneo_latitud, escaneo_longitud, escaneo_altitud, " +
                        "puntosEscaneo, capsulaVistaEn, puntosCapsula, selfieUri, selfieEn, puntosSelfie, sincronizado) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(g.id, g.usuarioId, g.puntoId, g.escaneadoEn, g.escaneoUbicacion.latitud, g.escaneoUbicacion.longitud,
                    g.escaneoUbicacion.altitud, g.puntosEscaneo, g.capsulaVistaEn, g.puntosCapsula, g.selfieUri, g.selfieEn,
                    g.puntosSelfie, g.sincronizado.aNumero())
            )
        }
        DatosPrueba.comentarios.forEach { k ->
            db.execSQL(
                "INSERT INTO comentarios (id, usuarioId, puntoId, texto, creadoEn, sincronizado) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(k.id, k.usuarioId, k.puntoId, k.texto, k.creadoEn, k.sincronizado.aNumero())
            )
        }
    }

    private fun Boolean.aNumero(): Int = if (this) 1 else 0
}
