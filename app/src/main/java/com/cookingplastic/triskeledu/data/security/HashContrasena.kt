package com.cookingplastic.triskeledu.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

//Convierte la contraseña en un hash con PBKDF2 y una sal aleatoria. La clave en texto nunca se guarda
object HashContrasena {

    //lo que se guarda en Usuario: claveHash y claveSal
    data class Resultado(val hash: String, val sal: String)

    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    //más vueltas = más lento de adivinar por fuerza bruta, pero también más lento al iniciar sesión
    private const val ITERACIONES = 120_000
    private const val LARGO_HASH_BITS = 256
    private const val LARGO_SAL_BYTES = 16

    //la sal es distinta para cada usuario: dos personas con la misma clave quedan con hashes distintos
    fun hashear(clave: String): Resultado {
        val sal = ByteArray(LARGO_SAL_BYTES)
        //SecureRandom y no Random: los números tienen que ser imposibles de predecir
        SecureRandom().nextBytes(sal)
        val hash = calcular(clave, sal)
        return Resultado(hash = aTexto(hash), sal = aTexto(sal))
    }

    //no se "deshace" el hash: se vuelve a calcular con la misma sal y se compara
    fun verificar(clave: String, hashGuardado: String, salGuardada: String): Boolean {
        val calculado = calcular(clave, aBytes(salGuardada))
        //isEqual compara todo sin cortar en el primer byte distinto, así el tiempo no da pistas
        return MessageDigest.isEqual(aBytes(hashGuardado), calculado)
    }

    private fun calcular(clave: String, sal: ByteArray): ByteArray {
        val especificacion = PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, LARGO_HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).encoded
        } finally {
            //borra la clave de la memoria apenas se usa
            especificacion.clearPassword()
        }
    }

    //Base64 pasa los bytes a texto para poder guardarlos en Room como String
    private fun aTexto(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun aBytes(texto: String): ByteArray = Base64.getDecoder().decode(texto)
}
