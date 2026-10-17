package com.rafael.bovedabms

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rafael.bovedabms.crypto.CryptoManager
import com.rafael.bovedabms.network.Sesion
import com.rafael.bovedabms.ui.BovedaScreen
import com.rafael.bovedabms.ui.LoginScreen
import com.rafael.bovedabms.ui.theme.BovedaBMSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope

class SessionViewModel : ViewModel() {
    // La app AHORA SIEMPRE arranca en el login
    var pantallaActual by mutableStateOf("login")
    var llaveRAM by mutableStateOf<SecretKeySpec?>(null)
    var llavePersonalRAM by mutableStateOf<SecretKeySpec?>(null)
    var tiempoMinimizado = 0L

    fun cerrarSesion() {
        pantallaActual = "login"
        llaveRAM = null
        llavePersonalRAM = null
    }

    // AQUI ARREGLAMOS EL PARPADEO: Calculamos las llaves en un proceso en segundo plano
    fun derivarLlavesAsync(
        passMaes: String,
        passPerso: String,
        onListo: (SecretKeySpec, SecretKeySpec) -> Unit = { _, _ -> }
    ) {
        // Ponemos la pantalla bonita de carga para tapar el login
        pantallaActual = "calculando_llaves"

        viewModelScope.launch(Dispatchers.Default) {
            // Esto es lo que demora los 2 segundos, pero ahora se hace sin congelar la pantalla
            val llaveM = CryptoManager.derivarLlave(passMaes, "BovedaInfraestructuraSalt2026")
            val llaveP = CryptoManager.derivarLlave(passPerso, "MisClavesPersonales_E2EE")

            // Cuando termina, abrimos la bóveda
            launch(Dispatchers.Main) {
                llaveRAM = llaveM
                llavePersonalRAM = llaveP
                pantallaActual = "boveda"
                onListo(llaveM, llaveP)
            }
        }
    }

    // Login manual: la llave maestra ya viene calculada; la personal se calcula en segundo plano
    // (antes se calculaba en el hilo principal y congelaba la pantalla)
    fun abrirConLlaveMaestra(
        llaveM: SecretKeySpec,
        passPerso: String,
        onListo: (SecretKeySpec, SecretKeySpec) -> Unit = { _, _ -> }
    ) {
        pantallaActual = "calculando_llaves"
        viewModelScope.launch(Dispatchers.Default) {
            val llaveP = CryptoManager.derivarLlave(passPerso, "MisClavesPersonales_E2EE")
            launch(Dispatchers.Main) {
                llaveRAM = llaveM
                llavePersonalRAM = llaveP
                pantallaActual = "boveda"
                onListo(llaveM, llaveP)
            }
        }
    }
}

class MainActivity : FragmentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sessionViewModel: SessionViewModel

    companion object {
        private const val LIMITE_INACTIVIDAD_MS = 5 * 60 * 1000L // 5 minutos
        private const val KEY_SEGUNDO_PLANO_DESDE = "segundoPlanoDesde"
        private const val KEY_LLAVE_MAESTRA = "llaveMaestraB64"
        private const val KEY_LLAVE_PERSONAL = "llavePersonalB64"
        private const val KEY_LLAVE_ALGORITMO = "llaveAlgoritmo"
    }

    // Guardamos las llaves YA calculadas en el almacenamiento cifrado (igual que las contraseñas),
    // así desbloquear con PIN/huella es instantáneo y no hay que repetir el cálculo lento.
    private fun guardarLlavesDerivadas(maestra: SecretKeySpec, personal: SecretKeySpec) {
        sharedPreferences.edit()
            .putString(KEY_LLAVE_MAESTRA, Base64.encodeToString(maestra.encoded, Base64.NO_WRAP))
            .putString(KEY_LLAVE_PERSONAL, Base64.encodeToString(personal.encoded, Base64.NO_WRAP))
            .putString(KEY_LLAVE_ALGORITMO, maestra.algorithm)
            .apply()
    }

    // Abre la bóveda con las llaves guardadas. Devuelve false si no hay (primera vez o app actualizada).
    private fun abrirBovedaRapida(): Boolean {
        val m = sharedPreferences.getString(KEY_LLAVE_MAESTRA, null) ?: return false
        val p = sharedPreferences.getString(KEY_LLAVE_PERSONAL, null) ?: return false
        val alg = sharedPreferences.getString(KEY_LLAVE_ALGORITMO, null) ?: return false
        return try {
            sessionViewModel.llaveRAM = SecretKeySpec(Base64.decode(m, Base64.NO_WRAP), alg)
            sessionViewModel.llavePersonalRAM = SecretKeySpec(Base64.decode(p, Base64.NO_WRAP), alg)
            sessionViewModel.pantallaActual = "boveda"
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    // Guardamos la hora en que la app pasó a segundo plano, para que sobreviva si Android cierra el proceso
    private fun marcarSegundoPlano(ahora: Long) {
        sharedPreferences.edit().putLong(KEY_SEGUNDO_PLANO_DESDE, ahora).apply()
    }

    private fun limpiarMarcaSegundoPlano() {
        sharedPreferences.edit().remove(KEY_SEGUNDO_PLANO_DESDE).apply()
    }

    // true si todavía no pasaron los 5 minutos (y el reloj no se movió hacia atrás)
    private fun sesionVigente(desde: Long): Boolean {
        val transcurrido = System.currentTimeMillis() - desde
        return desde > 0L && transcurrido >= 0L && transcurrido <= LIMITE_INACTIVIDAD_MS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "boveda_segura_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Si Android cerró la app en segundo plano (el ViewModel empieza de cero) pero aún no pasaron
        // 5 minutos, renovamos el token JWT y reabrimos la bóveda sin pedir PIN.
        if (sessionViewModel.pantallaActual == "login" && sessionViewModel.llaveRAM == null) {
            val desde = sharedPreferences.getLong(KEY_SEGUNDO_PLANO_DESDE, 0L)
            val email = sharedPreferences.getString("email", "") ?: ""
            val passMaes = sharedPreferences.getString("passMaestra", "") ?: ""
            val passPerso = sharedPreferences.getString("passPersonal", "") ?: ""
            if (sesionVigente(desde) && email.isNotEmpty() && passPerso.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        Sesion.iniciar(email, passPerso)
                        if (!abrirBovedaRapida() && passMaes.isNotEmpty()) {
                            sessionViewModel.derivarLlavesAsync(passMaes, passPerso) { m, p -> guardarLlavesDerivadas(m, p) }
                        }
                    } catch (_: Exception) {
                        // Si falla la red al restaurar, permanece en login
                    }
                }
            }
            limpiarMarcaSegundoPlano()
        }

        setContent {
            BovedaBMSTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) {
                                val ahora = System.currentTimeMillis()
                                sessionViewModel.tiempoMinimizado = ahora
                                // Solo se recuerda si estamos dentro de la bóveda
                                if (sessionViewModel.pantallaActual == "boveda") marcarSegundoPlano(ahora)
                            } else if (event == Lifecycle.Event.ON_START) {
                                val desde = sessionViewModel.tiempoMinimizado
                                // Solo cierra sesión si pasaron más de 5 minutos Y estábamos dentro de la bóveda
                                if (desde > 0L && !sesionVigente(desde) && sessionViewModel.pantallaActual == "boveda") {
                                    sessionViewModel.cerrarSesion()
                                    Toast.makeText(this@MainActivity, "Sesión bloqueada tras 5 min de inactividad", Toast.LENGTH_SHORT).show()
                                }
                                // Ya volvimos: se reinicia el contador y se borra la marca guardada
                                sessionViewModel.tiempoMinimizado = 0L
                                limpiarMarcaSegundoPlano()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    when (sessionViewModel.pantallaActual) {
                        "login" -> {
                            LoginScreen(
                                tieneCredencialesGuardadas = sharedPreferences.contains("email"),
                                onPedirPinHuella = {
                                    mostrarPromptPinHuella()
                                },
                                onLoginSuccess = { llaveDerivada, email, passPersonal, passMaestra ->
                                    guardarCredencialesSeguras(email, passPersonal, passMaestra)
                                    sessionViewModel.abrirConLlaveMaestra(llaveDerivada, passPersonal) { m, p -> guardarLlavesDerivadas(m, p) }
                                }
                            )
                        }
                        "calculando_llaves" -> {
                            // Esta es la pantalla elegante que cubre los 2 segundos de demora
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Desencriptando Bóveda...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Calculando llaves criptográficas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        "boveda" -> {
                            if (sessionViewModel.llaveRAM != null && sessionViewModel.llavePersonalRAM != null) {
                                BovedaScreen(
                                    llaveMaestra = sessionViewModel.llaveRAM!!,
                                    llavePersonal = sessionViewModel.llavePersonalRAM!!
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun guardarCredencialesSeguras(email: String, passPersonal: String, passMaestra: String) {
        sharedPreferences.edit()
            .putString("email", email)
            .putString("passPersonal", passPersonal)
            .putString("passMaestra", passMaestra)
            .apply()
    }

    private fun mostrarPromptPinHuella() {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Bóveda BMS")
            .setSubtitle("Usa tu PIN o Huella para entrar")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val email = sharedPreferences.getString("email", "") ?: ""
                val passMaes = sharedPreferences.getString("passMaestra", "") ?: ""
                val passPerso = sharedPreferences.getString("passPersonal", "") ?: ""

                if (email.isNotEmpty() && passPerso.isNotEmpty() && passMaes.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            // 1. Iniciar sesión en el backend para obtener el token JWT en memoria
                            Sesion.iniciar(email, passPerso)

                            // 2. Abrir al instante si las llaves ya están guardadas o calcularlas
                            if (!abrirBovedaRapida()) {
                                sessionViewModel.derivarLlavesAsync(passMaes, passPerso) { m, p -> guardarLlavesDerivadas(m, p) }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error al conectar con el servidor: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No hay credenciales guardadas. Inicia sesión manualmente.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}