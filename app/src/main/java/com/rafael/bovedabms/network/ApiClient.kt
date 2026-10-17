package com.rafael.bovedabms.network

import com.rafael.bovedabms.models.CredencialPersonal
import com.rafael.bovedabms.models.CredencialPersonalCreate
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.rafael.bovedabms.models.LoginRequest
import com.rafael.bovedabms.models.Usuario
import com.rafael.bovedabms.models.Equipo
import com.rafael.bovedabms.models.EquipoUpdateModel
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Usuario

    // La descarga funcionaba porque tenía la barra al final
    @GET("equipos/")
    suspend fun getEquipos(): List<Equipo>

    // Agregamos la barra al final para la creación
    @POST("equipos/")
    suspend fun crearEquipo(@Body equipo: Equipo): Equipo

    // Ruta exacta que configuraste en tu FastAPI de Render
    @PUT("equipos/{id}/password")
    suspend fun actualizarPassword(
        @Path("id") id: Int,
        @Body updateData: EquipoUpdateModel
    ): Map<String, String>
    // NUEVO ENDPOINT PARA TUS CLAVES PERSONALES
    @GET("personales/{usuario_id}")
    suspend fun getCredencialesPersonales(
        @Path("usuario_id") usuarioId: Int
    ): List<CredencialPersonal>
    @POST("personales/")
    suspend fun crearCredencialPersonal(@Body credencial: CredencialPersonalCreate): Map<String, String>
}

// Token de sesión: vive SOLO en memoria (no se guarda en disco). Al cerrar la app se pierde.
object Sesion {
    @Volatile
    var token: String? = null

    @Volatile
    var usuarioActual: Usuario? = null

    fun cerrar() {
        token = null
        usuarioActual = null
    }

    // Inicia sesión en la API y guarda el token y el usuario logueado.
    suspend fun iniciar(email: String, passwordPersonal: String): Usuario {
        cerrar()
        val respuesta = ApiClient.retrofitService.login(
            LoginRequest(email = email, password_personal = passwordPersonal)
        )
        token = respuesta.token
        usuarioActual = respuesta
        return respuesta
    }
}

// Agrega "Authorization: Bearer <token>" a todas las peticiones cuando hay sesión
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val tokenActual = Sesion.token
        val peticion = if (tokenActual != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $tokenActual")
                .build()
        } else {
            original
        }
        return chain.proceed(peticion)
    }
}

object ApiClient {
    // Tu URL real de Render
    private const val BASE_URL = "https://api-boveda-bms.onrender.com/"

    // Render (plan gratis) tarda en "despertar": con 60 s evitamos falsos fallos de conexión
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val retrofitService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}