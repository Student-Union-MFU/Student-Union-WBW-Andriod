package th.ac.mfu.su.wbw.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import th.ac.mfu.su.wbw.data.remote.dto.AirQualityResponse
import th.ac.mfu.su.wbw.data.remote.dto.WeatherResponse

/**
 * Open-Meteo — trail weather and air quality.
 *
 * Chosen because it needs **no API key and no account**: there is nothing to put in
 * `local.properties`, nothing to rotate, and a fresh checkout builds and runs this
 * feature with no setup. (OpenWeatherMap, the obvious alternative, requires a key per
 * developer and would have joined the Maps key in the secrets plugin.) It is free for
 * non-commercial use under CC-BY 4.0.
 *
 * The two products live on different hosts, so the paths here are absolute URLs —
 * Retrofit replaces the base URL entirely when a `@GET` value is a full URL — and the
 * base the client is built with is never actually used for these calls.
 *
 * The `current` query parameter names the variables to return, and it has to stay in step
 * with the fields on [WeatherResponse] / [AirQualityResponse]: anything not named here
 * simply will not be in the response.
 *
 * These calls go out on a client with **no** [th.ac.mfu.su.wbw.core.network.AuthInterceptor]
 * on it — see `NetworkModule.createOpenMeteoApi`.
 */
interface OpenMeteoApi {

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun weather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = WeatherVariables,
        @Query("timezone") timezone: String = Timezone,
    ): WeatherResponse

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = AirVariables,
        @Query("timezone") timezone: String = Timezone,
    ): AirQualityResponse

    companion object {
        private const val WeatherVariables =
            "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m"
        private const val AirVariables = "us_aqi"

        /**
         * The event's zone, fixed rather than the device's.
         *
         * The measurements themselves are absolute, so this only sets which local clock
         * the response's `time` field is expressed in. Sent anyway so that a response
         * read in a log or a crash report says the hour the walkers were actually
         * standing in, not the hour on a phone somebody forgot to change.
         */
        private const val Timezone = "Asia/Bangkok"
    }
}
