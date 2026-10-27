package com.cookingplastic.triskeledu.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

//Llena la base con los datos de prueba del equipo (DatosPrueba) la primera vez que se crea.
//onCreate corre una sola vez, al crear el archivo, y antes de que cualquier pantalla lea: no hay carreras.
//Aquí no se pueden usar los DAO (la base se está creando), por eso va SQL directo
object CargaInicial : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        DatosPrueba.capsulas.forEach { c ->
            db.execSQL(
                "INSERT INTO capsulas (id, titulo, urlStream, categoria, duracionSegundos) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(c.id, c.titulo, c.urlStream, c.categoria, c.duracionSegundos)
            )
        }
        //las claves ya vienen hasheadas con el mismo PBKDF2 de HashContrasena (clave de todos: Demo1234)
        DatosPrueba.usuarios.forEach { u ->
            db.execSQL(
                "INSERT INTO usuarios (id, username, correo, claveHash, claveSal, rol, terminosVersion, terminosAceptadosEn, creadoEn) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                //rol.name: el enum se guarda como texto, igual que en Converters
                arrayOf<Any?>(u.id, u.username, u.correo, u.claveHash, u.claveSal, u.rol.name, u.terminosVersion, u.terminosAceptadosEn, u.creadoEn)
            )
        }
        //SQLite no tiene Boolean: Room los guarda como 1 o 0
        DatosPrueba.preferencias.forEach { p ->
            db.execSQL(
                "INSERT INTO preferencias (usuarioId, temaNoche, notifLugaresCercanos, notifNuevosVideos, seguirUbicacion, mostrarBloqueados) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(p.usuarioId, p.temaNoche.aNumero(), p.notifLugaresCercanos.aNumero(), p.notifNuevosVideos.aNumero(),
                    p.seguirUbicacion.aNumero(), p.mostrarBloqueados.aNumero())
            )
        }
    }

    private fun Boolean.aNumero(): Int = if (this) 1 else 0
}
