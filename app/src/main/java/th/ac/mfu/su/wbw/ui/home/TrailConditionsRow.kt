package th.ac.mfu.su.wbw.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.repository.TrailConditions
import th.ac.mfu.su.wbw.ui.theme.AirGoodTint
import th.ac.mfu.su.wbw.ui.theme.AirModerateTint
import th.ac.mfu.su.wbw.ui.theme.AirSensitiveTint
import th.ac.mfu.su.wbw.ui.theme.AirUnhealthyTint
import th.ac.mfu.su.wbw.ui.theme.SkyCloudTint
import th.ac.mfu.su.wbw.ui.theme.SkyFogTint
import th.ac.mfu.su.wbw.ui.theme.SkyRainTint
import th.ac.mfu.su.wbw.ui.theme.SkySnowTint
import th.ac.mfu.su.wbw.ui.theme.SkyStormTint
import th.ac.mfu.su.wbw.ui.theme.SkySunTint
import th.ac.mfu.su.wbw.ui.theme.WbwColors
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import kotlin.math.roundToInt

/**
 * What it is like on the hill: temperature on the left, air on the right.
 *
 * Drawn straight onto the backdrop — no pane, no edge, no fill of its own. These are the
 * quietest facts on Home and they were the loudest-looking thing on it: two glass pills
 * under the greeting made the top of the screen read as a control panel, and gave a
 * number nobody came here for the same surface treatment as the participant pass. Type on
 * the ground says the same thing and stops asking for a turn.
 *
 * Which means the separation between the two readings has to be spacing rather than
 * anything drawn. There is no divider mark between them for the same reason there is no
 * border around them.
 *
 * A [FlowRow], so a reading that will not fit drops to a second line instead of being
 * truncated. The widest the air side ever gets — "AQI 173 SENSITIVE GROUPS", "AQI 173
 * มีผลต่อสุขภาพ" — is exactly when the air is at its worst, which is the worst possible
 * moment to be eliding the word that says so. On a normal day both readings fit on one
 * line and the wrap never happens.
 *
 * Air quality is here rather than being a nice extra: this event is walked in northern
 * Thailand, where the burning-season haze is the reason somebody would decide to carry a
 * mask or skip the hill. It is the one number on Home that can change what a participant
 * does today.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrailConditionsRow(conditions: TrailConditions, modifier: Modifier = Modifier) {
    val colors = wbwColors
    val weather = conditions.weather
    val air = conditions.air

    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (weather != null) {
            val sky = Sky.of(weather.code)
            Reading(
                icon = sky.icon,
                // The sky's description rides on the icon rather than taking a third slot
                // of width. Nothing is lost: it is the icon's job to say this, and saying
                // it here means a screen reader gets "Partly cloudy, 29 degrees" instead
                // of a bare number.
                iconDescription = stringResource(sky.labelRes),
                iconTint = sky.tint,
                lead = "${weather.temperatureC}°",
                trail = weather.feelsLikeC?.let { stringResource(R.string.home_weather_feels, it) },
                trailTint = colors.onBackdropMuted,
            )
        }
        if (air != null) {
            val band = AqiBand.of(air.usAqi)
            Reading(
                icon = Icons.Outlined.Air,
                iconDescription = stringResource(R.string.home_air_quality),
                // The glyph carries the band's colour at every level, because that is what
                // the ramp is for — you should be able to tell green from amber without
                // reading. The *word* stays restrained and only reddens once the air is
                // actually bad, the same way the announcements list treats `emergency`: a
                // full-colour "GOOD" in type would be the loudest thing on Home for the
                // least urgent fact on it.
                iconTint = band.iconTint,
                lead = stringResource(R.string.home_air_aqi, air.usAqi),
                trail = stringResource(band.labelRes),
                trailTint = band.tint(colors),
            )
        }
    }
}

/**
 * One reading: a mark, a number, and a word about the number.
 *
 * Written once for both so the two cannot drift apart in tracking or type size — the row
 * only reads as one line of thought while they match, and with no pane around them there
 * is nothing else holding them together.
 */
@Composable
private fun Reading(
    icon: ImageVector,
    iconDescription: String,
    iconTint: Color,
    lead: String,
    trail: String?,
    trailTint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = wbwColors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // The mark is the only coloured thing in the row — see the conditions block in
        // Color.kt. It is also the only piece that can say *what kind* of day it is, since
        // everything beside it is a number, which is what earns it the exception.
        Icon(icon, iconDescription, tint = iconTint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            lead,
            color = colors.onBackdrop,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        if (trail != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                trail.uppercase(),
                color = trailTint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * WMO 4677 present-weather codes, collapsed to the states worth drawing differently.
 *
 * The standard has dozens of codes and Open-Meteo returns a couple of dozen of them;
 * distinguishing "light drizzle" from "moderate drizzle" with a 14dp glyph is a
 * distinction nobody can see, so intensity is dropped and only the kind survives. The
 * frozen states are kept even though this event is walked at 500m in Chiang Rai and will
 * never see one, because dropping them would mean silently drawing snow as rain if the
 * app is ever pointed at another trail.
 */
private enum class Sky(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
) {
    Clear(R.string.weather_clear, Icons.Outlined.WbSunny, SkySunTint),
    // Still a sun, so still the sun's colour — the distinction between "clear" and "mainly
    // clear" is not one a tint at this size could carry, and trying would only make two
    // near-identical yellows.
    MainlyClear(R.string.weather_mainly_clear, Icons.Outlined.WbSunny, SkySunTint),
    PartlyCloudy(R.string.weather_partly_cloudy, Icons.Outlined.FilterDrama, SkySunTint),
    Overcast(R.string.weather_overcast, Icons.Outlined.Cloud, SkyCloudTint),
    Fog(R.string.weather_fog, Icons.Outlined.BlurOn, SkyFogTint),
    Drizzle(R.string.weather_drizzle, Icons.Outlined.Grain, SkyRainTint),
    Rain(R.string.weather_rain, Icons.Outlined.WaterDrop, SkyRainTint),
    Showers(R.string.weather_showers, Icons.Outlined.Grain, SkyRainTint),
    Snow(R.string.weather_snow, Icons.Outlined.AcUnit, SkySnowTint),
    Thunderstorm(R.string.weather_thunderstorm, Icons.Outlined.Bolt, SkyStormTint),
    ;

    companion object {
        fun of(code: Int): Sky = when (code) {
            0 -> Clear
            1 -> MainlyClear
            2 -> PartlyCloudy
            3 -> Overcast
            45, 48 -> Fog
            51, 53, 55, 56, 57 -> Drizzle
            61, 63, 65, 66, 67 -> Rain
            80, 81, 82 -> Showers
            71, 73, 75, 77, 85, 86 -> Snow
            95, 96, 99 -> Thunderstorm
            // An unmapped code is still weather; the pill keeps its temperature and shows
            // a neutral sky rather than disappearing over a number it did not recognise.
            else -> Overcast
        }
    }
}

/**
 * US EPA AQI bands, at the official breakpoints.
 *
 * The names are the EPA's, with one shortened: "Unhealthy for Sensitive Groups" will not
 * fit half a phone's width in either language, so it is carried as "Sensitive groups" /
 * "กลุ่มเสี่ยง" — which keeps the part that tells you whether it means you.
 */
private enum class AqiBand(@param:StringRes val labelRes: Int, val iconTint: Color) {
    Good(R.string.aqi_good, AirGoodTint),
    Moderate(R.string.aqi_moderate, AirModerateTint),
    SensitiveGroups(R.string.aqi_sensitive, AirSensitiveTint),
    Unhealthy(R.string.aqi_unhealthy, AirUnhealthyTint),
    VeryUnhealthy(R.string.aqi_very_unhealthy, AirUnhealthyTint),
    Hazardous(R.string.aqi_hazardous, AirUnhealthyTint),
    ;

    /**
     * The *word's* colour, which escalates far later than the glyph's — the glyph is a
     * mark you glance at, the word is type on a photograph and has to stay legible first.
     *
     * [AirUnhealthyTint] rather than `colors.danger` because this line sits on the
     * backdrop: `danger` flips to a dark red in light theme, and the ground under it is
     * the same dark photograph either way.
     */
    fun tint(colors: WbwColors): Color = when (this) {
        Good, Moderate -> colors.onBackdropMuted
        SensitiveGroups -> colors.onBackdrop
        Unhealthy, VeryUnhealthy, Hazardous -> AirUnhealthyTint
    }

    companion object {
        fun of(aqi: Int): AqiBand = when {
            aqi <= 50 -> Good
            aqi <= 100 -> Moderate
            aqi <= 150 -> SensitiveGroups
            aqi <= 200 -> Unhealthy
            aqi <= 300 -> VeryUnhealthy
            else -> Hazardous
        }
    }
}
