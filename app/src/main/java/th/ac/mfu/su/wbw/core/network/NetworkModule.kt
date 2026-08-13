package th.ac.mfu.su.wbw.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import th.ac.mfu.su.wbw.BuildConfig
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.WbwApi
import java.util.concurrent.TimeUnit

/** Builds the Retrofit-backed [WbwApi] pointed at BuildConfig.API_BASE_URL. */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true // backend may add fields; don't crash on them
        coerceInputValues = true // null -> default for non-null Kotlin fields
        explicitNulls = false
    }

    fun createApi(sessions: SessionStore): WbwApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessions))
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL) // ends with /wbw/
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WbwApi::class.java)
    }
}
