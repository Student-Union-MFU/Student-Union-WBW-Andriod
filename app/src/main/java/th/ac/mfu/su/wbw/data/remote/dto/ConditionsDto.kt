package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo responses.
 *
 * Only the `current` block is asked for and only the fields below are read — the API
 * returns exactly the variables named in the query string, so the request and these
 * classes have to be changed together. Everything is nullable: Open-Meteo omits a
 * variable rather than sending null when a model has no value for it at this hour, and a
 * missing humidity is not a reason to fail the whole card.
 */
@Serializable
data class WeatherResponse(
    val current: CurrentWeather? = null,
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    val precipitation: Double? = null,
    /** WMO 4677 present-weather code. See `WeatherKind` for the mapping. */
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
)

@Serializable
data class AirQualityResponse(
    val current: CurrentAirQuality? = null,
)

@Serializable
data class CurrentAirQuality(
    /**
     * The US EPA index, not the European one.
     *
     * Both are offered by the same call and they are not interchangeable — different
     * breakpoints, so the same air is "53" on one scale and "32" on the other. US AQI is
     * the scale `AqiBand` is written against.
     *
     * An earlier version reported raw PM2.5 in µg/m³ instead, on the argument that it is
     * the figure Chiang Rai actually talks about during burning season. True, but it did
     * not survive contact with the screen: "PM2.5 6 µg/m³ VERY GOOD" is a label ending in
     * a number, followed by another number, followed by a unit, followed by a verdict —
     * four things where the row has room for two. The index says the same thing in
     * "AQI 53 MODERATE", and a single number the reader can rank against 0–500 is what a
     * glance at Home can actually use.
     */
    @SerialName("us_aqi") val usAqi: Double? = null,
)
