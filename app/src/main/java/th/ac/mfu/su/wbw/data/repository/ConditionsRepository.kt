package th.ac.mfu.su.wbw.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.OpenMeteoApi
import th.ac.mfu.su.wbw.data.remote.dto.AirQualityResponse
import th.ac.mfu.su.wbw.data.remote.dto.WeatherResponse
import kotlin.math.roundToInt

/**
 * What it is like on the trail right now.
 *
 * Both halves are optional and independent. Open-Meteo serves them from two different
 * hosts, either can fail on its own, and neither is worth losing the other over — so a
 * response with weather and no air reading is a normal outcome, not an error.
 */
@Serializable
data class TrailConditions(
    val weather: Weather? = null,
    val air: AirReading? = null,
) {
    val isEmpty: Boolean get() = weather == null && air == null
}

@Serializable
data class Weather(
    val temperatureC: Int,
    val feelsLikeC: Int?,
    val humidityPercent: Int?,
    /** WMO 4677 present-weather code, unmapped — the UI owns the icon and the wording. */
    val code: Int,
)

/** US EPA AQI. See [th.ac.mfu.su.wbw.data.remote.dto.CurrentAirQuality.usAqi]. */
@Serializable
data class AirReading(val usAqi: Int)

/**
 * Trail weather and air quality, for a fixed point on the route.
 *
 * **The location is the trail, not the phone.** No location permission is asked for and
 * none is used, for two reasons: the participant wants to know what the walk will be like,
 * which is a fact about the hill rather than about wherever they are reading this — the
 * night before, that is a different province — and Home is the first screen after login,
 * which is the worst possible moment to throw a permission dialog for a decoration.
 *
 * Failures are swallowed into `null` rather than surfaced as an [ApiResult] error. This is
 * the one thing on Home that is not the app's own data, and a third-party outage must not
 * put an error message on the participant's home screen. The card is simply absent.
 */
class ConditionsRepository(
    private val api: OpenMeteoApi,
    private val cache: ResponseCache,
) {

    /**
     * The last reading, however old.
     *
     * Handed straight to the UI so the weather line is on screen in the first frame after a
     * cold start rather than a second or two later. Deliberately not age-checked: an hour-old
     * temperature is a far better answer than a gap where the line should be, and [trailConditions]
     * is already on its way to replacing it.
     */
    fun cached(): TrailConditions? = cache.read(ResponseCache.KeyConditions, TrailConditions.serializer())

    suspend fun trailConditions(): TrailConditions? {
        // On disk rather than in a field, so the ten minutes survive the process being
        // killed in a pocket — which, on a walk, is most of how this app is used.
        val age = cache.ageMillis(ResponseCache.KeyConditions)
        if (age != null && age < CacheTtlMillis) cached()?.let { return it }

        // Concurrently: two hosts, no ordering between them, and the card wants both
        // before it draws. Sequentially this would be two round trips deep instead of one.
        val fresh = coroutineScope {
            val weather = async { runCatching { api.weather(TrailLatitude, TrailLongitude) }.getOrNull() }
            val air = async { runCatching { api.airQuality(TrailLatitude, TrailLongitude) }.getOrNull() }
            TrailConditions(
                weather = weather.await().toWeather(),
                air = air.await().toAirReading(),
            )
        }

        // A round trip that came back with nothing is not worth caching — it is usually a
        // dropped connection, and storing it would both hide the line and restart the ten
        // minutes, keeping it hidden long after the signal came back.
        if (fresh.isEmpty) return cached()
        cache.write(ResponseCache.KeyConditions, TrailConditions.serializer(), fresh)
        return fresh
    }

    private fun WeatherResponse?.toWeather(): Weather? {
        val c = this?.current ?: return null
        // Temperature is the one field the card cannot be drawn without.
        val temperature = c.temperature ?: return null
        return Weather(
            temperatureC = temperature.roundToInt(),
            feelsLikeC = c.apparentTemperature?.roundToInt(),
            humidityPercent = c.humidity,
            code = c.weatherCode ?: 0,
        )
    }

    private fun AirQualityResponse?.toAirReading(): AirReading? =
        this?.current?.usAqi?.let { AirReading(it.roundToInt()) }

    companion object {
        /**
         * The centre of the baked trail (`res/raw/route_wbw.json`), which is MFU, Chiang
         * Rai. Written out rather than derived from `TrailRoute` at runtime so that this
         * layer does not need a `Context` — and the route is 8.3km end to end, well inside
         * one weather model cell, so any point on it would give the same answer.
         */
        private const val TrailLatitude = 20.0466
        private const val TrailLongitude = 99.9014

        /**
         * Open-Meteo advances its `current` block every 15 minutes and asks non-commercial
         * users to stay under 10,000 calls a day. Ten minutes keeps a participant tabbing
         * between Home and the map from spending a request each time, while still being
         * shorter than the interval the data actually changes on.
         */
        private const val CacheTtlMillis = 10 * 60 * 1000L
    }
}
