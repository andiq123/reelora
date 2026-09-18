package tv.reelora.app

internal enum class WeatherKind(val icon: Int, val label: String, val color: Long) {
    Sun(R.drawable.weather_sun, "Clear sky", 0xFFFFD68A),
    Moon(R.drawable.weather_moon, "Clear night", 0xFFD6D5FF),
    PartlyCloudy(R.drawable.weather_partly, "Partly cloudy", 0xFFFFDFA5),
    PartlyCloudyNight(R.drawable.weather_partly_night, "Partly cloudy", 0xFFD6D5FF),
    Cloud(R.drawable.weather_cloud, "Cloudy", 0xFFDDE7F2),
    Fog(R.drawable.weather_fog, "Fog", 0xFFDDE7F2),
    Rain(R.drawable.weather_rain, "Rain", 0xFFB4D8FF),
    Snow(R.drawable.weather_snow, "Snow", 0xFFD8F5FF),
    Storm(R.drawable.weather_storm, "Thunderstorm", 0xFFE0CDFF),
    Unknown(R.drawable.weather_unknown, "Weather unavailable", 0xFFDDE7F2),
}

internal fun weatherKind(code: Int, isDay: Boolean = true): WeatherKind = when (code) {
    0, 1 -> if (isDay) WeatherKind.Sun else WeatherKind.Moon
    2 -> if (isDay) WeatherKind.PartlyCloudy else WeatherKind.PartlyCloudyNight
    3 -> WeatherKind.Cloud
    45, 48 -> WeatherKind.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.Rain
    71, 73, 75, 77, 85, 86 -> WeatherKind.Snow
    95, 96, 99 -> WeatherKind.Storm
    else -> WeatherKind.Unknown
}
