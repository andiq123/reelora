package tv.reelora.app

import java.util.Locale

// Android's bundled emoji font supplies the artwork; no image requests or bitmap cache.
private val countryCodes by lazy {
    Locale.getISOCountries().associateBy { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.ENGLISH).lowercase(Locale.ROOT) }
}

internal fun footballCountryFlag(country: String): String? {
    val name = country.trim().lowercase(Locale.ROOT)
    val subdivision = when (name) {
        "england" -> "gbeng"
        "scotland" -> "gbsct"
        "wales" -> "gbwls"
        else -> null
    }
    if (subdivision != null) return String(Character.toChars(0x1F3F4)) +
        subdivision.map { String(Character.toChars(0xE0000 + it.code)) }.joinToString("") + String(Character.toChars(0xE007F))
    val code = when (name) {
        "usa", "united states of america" -> "US"
        "south korea", "korea republic" -> "KR"
        "ivory coast" -> "CI"
        "czech republic" -> "CZ"
        "turkey" -> "TR"
        "kosovo" -> "XK"
        else -> countryCodes[name] ?: country.uppercase(Locale.ROOT).takeIf { it in countryCodes.values }
    } ?: return null
    return code.map { String(Character.toChars(0x1F1E6 + it.code - 'A'.code)) }.joinToString("")
}
