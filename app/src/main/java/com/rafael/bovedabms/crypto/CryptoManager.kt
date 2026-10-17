package com.rafael.bovedabms.crypto

import android.util.Base64
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    // Ahora recibe el saltText dinámicamente como parámetro
    fun derivarLlave(password: String, saltText: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val saltBytes = saltText.toByteArray(Charsets.UTF_8)

        // 100,000 iteraciones y 256 bits
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), saltBytes, 100000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun descifrarTexto(textoCifradoB64: String, ivB64: String, llave: SecretKeySpec): String {
        return try {
            val textoCifradoBytes = Base64.decode(textoCifradoB64, Base64.DEFAULT)
            val ivBytes = Base64.decode(ivB64, Base64.DEFAULT)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, llave, spec)

            val textoDescifradoBytes = cipher.doFinal(textoCifradoBytes)
            String(textoDescifradoBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Bloqueado"
        }
    }

    fun cifrarTexto(textoPlano: String, llave: SecretKeySpec): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, llave)

        val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val cifradoBytes = cipher.doFinal(textoPlano.toByteArray(Charsets.UTF_8))
        val cifradoB64 = Base64.encodeToString(cifradoBytes, Base64.NO_WRAP)

        return Pair(cifradoB64, ivB64)
    }
}