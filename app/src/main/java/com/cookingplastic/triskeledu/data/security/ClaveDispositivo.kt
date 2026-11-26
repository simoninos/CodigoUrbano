package com.cookingplastic.triskeledu.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.cookingplastic.triskeledu.data.model.ProgresoPunto
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

//SEG-05: la clave del teléfono para Key Attestation. Se crea al iniciar sesión, atada al desafío del servidor, y con
//ella se firma cada escaneo al sincronizarlo. Es una interfaz para probar sin Android (en las pruebas, una clave común)
interface ClaveDispositivo {
    //crea la clave y devuelve su cadena de certificados en Base64, o null si el teléfono no puede (sin chip, error)
    fun crear(desafio: String): List<String>?

    //firma en Base64 de ECDSA-SHA256, o null si no hay clave
    fun firmar(texto: String): String?
}

//El texto que se firma; ms-progreso arma el mismo (FirmaEscaneo.java). Double.toString es igual en Kotlin y Java
fun textoFirmado(p: ProgresoPunto): String = listOf(
    p.id, p.puntoId, p.escaneadoEn.toString(), p.escaneoUbicacion.latitud.toString(), p.escaneoUbicacion.longitud.toString(),
    p.codigoQrLeido ?: "", p.escaneoSimulado.toString()
).joinToString("|")

//La clave vive en AndroidKeyStore: no sale del chip de seguridad (TEE o StrongBox) y no se puede copiar a otro teléfono.
//Android firma su certificado con la cadena de Google, y ese certificado dice qué app la pidió y si el teléfono tiene
//el arranque verificado. Eso lo revisa ms-usuarios
class ClaveAndroidKeyStore : ClaveDispositivo {

    override fun crear(desafio: String): List<String>? = try {
        val generador = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ALMACEN)
        generador.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(desafio.toByteArray(Charsets.UTF_8))
                .build()
        )
        //reemplaza la del inicio de sesión anterior: el token nuevo trae la clave pública nueva
        generador.generateKeyPair()
        almacen().getCertificateChain(ALIAS)?.map { Base64.getEncoder().encodeToString(it.encoded) }
    } catch (e: Exception) {
        null
    }

    override fun firmar(texto: String): String? = try {
        val clave = almacen().getKey(ALIAS, null) as? PrivateKey
        clave?.let {
            val firma = Signature.getInstance("SHA256withECDSA")
            firma.initSign(it)
            firma.update(texto.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(firma.sign())
        }
    } catch (e: Exception) {
        null
    }

    private fun almacen(): KeyStore = KeyStore.getInstance(ALMACEN).apply { load(null) }

    companion object {
        private const val ALMACEN = "AndroidKeyStore"
        private const val ALIAS = "triskeledu-escaneos"
    }
}
