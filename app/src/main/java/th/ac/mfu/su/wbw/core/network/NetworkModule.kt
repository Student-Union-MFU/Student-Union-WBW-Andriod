package th.ac.mfu.su.wbw.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import th.ac.mfu.su.wbw.BuildConfig
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.OpenMeteoApi
import th.ac.mfu.su.wbw.data.remote.WbwApi
import java.util.concurrent.TimeUnit

/** Builds the Retrofit-backed [WbwApi] pointed at BuildConfig.API_BASE_URL. */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true // backend may add fields; don't crash on them
        coerceInputValues = true // null -> default for non-null Kotlin fields
        explicitNulls = false
    }

    private fun logging() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    fun createApi(sessions: SessionStore): WbwApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessions))
            .addInterceptor(logging())
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

    /**
     * Open-Meteo, on its own client.
     *
     * The separate client is the point of this function, not an accident of tidiness:
     * [AuthInterceptor] attaches the participant's bearer token to **every** request that
     * goes through the client it is installed on, and Open-Meteo is a third party. Sharing
     * the WBW client here would hand a stranger's server a 30-day JWT for a student's
     * account on every weather refresh. It must never gain that interceptor.
     *
     * Timeouts are shorter than the WBW client's for the opposite reason to usual: this
     * is decoration. If the weather is not there in a few seconds the card simply does not
     * appear, and waiting 30 seconds to find that out only holds a coroutine open on a
     * phone that is probably on a bad connection halfway up a hill.
     *
     * The base URL is a formality — every call in [OpenMeteoApi] carries an absolute URL,
     * because the forecast and air-quality products are on different hosts — but Retrofit
     * requires one, so it is set to the host most of them use.
     */
    fun createOpenMeteoApi(): OpenMeteoApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(logging())
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApi::class.java)
    }
}
