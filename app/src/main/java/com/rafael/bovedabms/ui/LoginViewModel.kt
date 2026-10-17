package com.rafael.bovedabms.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafael.bovedabms.crypto.CryptoManager
import com.rafael.bovedabms.network.Sesion
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

class LoginViewModel : ViewModel() {
    var email = mutableStateOf("")
    var passwordPersonal = mutableStateOf("")
    var passwordMaestra = mutableStateOf("")

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf("")

    fun login(onSuccess: (SecretKeySpec, String, String, String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = ""

            try {
                // Generar la llave maestra aquí con su Sal respectiva
                val llaveDerivada = CryptoManager.derivarLlave(passwordMaestra.value, "BovedaInfraestructuraSalt2026")

                // Inicia sesión en la API y guarda el token de sesión en memoria
                Sesion.iniciar(email.value, passwordPersonal.value)

                // Si llegamos aquí, enviamos todo al MainActivity
                onSuccess(llaveDerivada, email.value, passwordPersonal.value, passwordMaestra.value)

            } catch (e: Exception) {
                if (e is retrofit2.HttpException) {
                    errorMessage.value = when (e.code()) {
                        429 -> "Demasiados intentos fallidos. Espera 15 minutos e inténtalo de nuevo."
                        else -> "Credenciales incorrectas."
                    }
                } else {
                    errorMessage.value = "Fallo de conexión: ${e.message}"
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}