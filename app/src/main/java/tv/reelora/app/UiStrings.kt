package tv.reelora.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalRomanian = staticCompositionLocalOf { false }

private val RomanianUi = mapOf(
    "Loading discovery…" to "Se încarcă recomandările…", "Discovery unavailable · your apps are ready" to "Recomandări indisponibile · aplicațiile sunt gata",
    "Not rated" to "Fără evaluare", "Retry" to "Reîncearcă", "All your apps are on Home" to "Toate aplicațiile sunt pe Acasă", "Clear" to "Șterge",
    "Ambient trailers after a quiet moment" to "Trailere după o perioadă de inactivitate",
    "Hold an app to move, rename or hide it" to "Ține apăsat pe o aplicație pentru a o muta, redenumi sau ascunde",
    "Find your next movie" to "Găsește următorul film", "Search unavailable · try again" to "Căutare indisponibilă · încearcă din nou",
    "Check your connection and search again" to "Verifică conexiunea și caută din nou", "Try a different title" to "Încearcă alt titlu",
    "Search movies, series and animation" to "Caută filme, seriale și animație",
    "Check for updates" to "Caută actualizări", "Checking…" to "Se verifică…",
    "Downloading…" to "Se descarcă…", "Install update" to "Instalează",
    "You're up to date" to "Ai ultima versiune", "Allow installation to continue" to "Permite instalarea pentru a continua",
    "Update check failed · try again" to "Verificarea a eșuat · încearcă din nou",
    "Update could not be verified · try again" to "Actualizarea nu poate fi instalată · verifică conexiunea și versiunea",
    "Discover" to "Descoperă", "Explore" to "Explorează", "YOUR APPS" to "APLICAȚIILE TALE",
    "Your apps, ready" to "Aplicațiile tale sunt gata",
    "Football" to "Fotbal", "Upcoming" to "Urmează", "Finished" to "Încheiat", "Previous" to "Anterior", "Live" to "În direct",
    "Settings" to "Setări", "Waiting for internet" to "Așteptăm conexiunea", "Updating weather" to "Actualizăm vremea", "Loading weather" to "Se încarcă vremea",
    "Clear sky" to "Cer senin", "Clear night" to "Noapte senină", "Partly cloudy" to "Parțial noros", "Cloudy" to "Noros", "Fog" to "Ceață", "Rain" to "Ploaie", "Snow" to "Ninsoare", "Thunderstorm" to "Furtună", "Weather unavailable" to "Vreme indisponibilă", "A quiet home for apps and discovery" to "Un spațiu calm pentru aplicații și descoperire",
    "APP SHELF" to "APLICAȚII", "Find and manage Home apps" to "Găsește și organizează aplicațiile",
    "HOME" to "ACASĂ", "Featured movies or a calm wallpaper" to "Filme recomandate sau un fundal calm",
    "WEATHER & TIME" to "VREME ȘI ORĂ", "Location, temperature and clock" to "Locație, temperatură și ceas",
    "SYSTEM" to "SISTEM", "Home and Android controls" to "Comenzi pentru ecranul principal și Android",
    "Done" to "Gata", "Search" to "Căutare", "Hidden" to "Ascunse", "Labels on" to "Etichete pornite",
    "Labels off" to "Etichete oprite", "Lifted focus" to "Focus ridicat", "Outline focus" to "Contur focus",
    "Movies on" to "Filme pornite", "Apps only" to "Doar aplicații", "New wallpaper" to "Fundal nou",
    "Football on" to "Fotbal pornit", "Football off" to "Fotbal oprit", "Theater on" to "Cinema pornit",
    "Theater off" to "Cinema oprit", "Daily wallpaper · Picsum" to "Fundal zilnic · Picsum",
    "Default home" to "Launcher implicit", "Device settings" to "Setările dispozitivului", "Network, display, sound and Android system" to "Rețea, imagine, sunet și sistem Android", "English" to "English", "Română" to "Română",
    "App options" to "Opțiuni aplicație", "Move" to "Mută", "Reorder on Home" to "Reordonează pe Acasă",
    "Rename" to "Redenumește", "Shelf label" to "Nume pe raft", "App info" to "Informații",
    "Manage or uninstall" to "Gestionează aplicația", "Hide" to "Ascunde", "Remove from Home" to "Elimină de pe Acasă",
    "Close" to "Închide", "Rename app" to "Redenumește aplicația", "Change the name shown on Home" to "Schimbă numele afișat pe Acasă",
    "App name" to "Numele aplicației", "Reset" to "Resetează", "Cancel" to "Anulează", "Save" to "Salvează",
    "Hidden apps" to "Aplicații ascunse", "Open an app or return it to Home" to "Deschide sau readaugă o aplicație pe Acasă",
    "Hidden from Home" to "Ascunsă de pe Acasă", "Open" to "Deschide", "Show on Home" to "Arată pe Acasă",
    "Restore anytime from Hidden apps" to "Restabilește oricând din Aplicații ascunse",
    "No hidden apps" to "Nu există aplicații ascunse", "Weather location" to "Locația meteo",
    "Search, choose, then confirm" to "Caută, alege, apoi confirmă", "City" to "Oraș", "Searching…" to "Se caută…",
    "Choose the correct location" to "Alege locația corectă", "No locations found" to "Nu s-au găsit locații",
    "Type at least two letters" to "Scrie cel puțin două litere", "Use location" to "Folosește locația",
    "Search, choose, then confirm" to "Caută, alege, apoi confirmă", "Movies, series and animation" to "Filme, seriale și animație",
    "Type a title…" to "Scrie un titlu…", "Voice" to "Voce", "Popular now" to "Populare acum",
    "Finding suggestions…" to "Se caută sugestii…", "No matches" to "Niciun rezultat", "Suggestions" to "Sugestii",
    "View" to "Vezi", "Back" to "Înapoi", "Play trailer" to "Redă trailerul", "Cast" to "Distribuție",
    "More like this" to "Titluri similare", "No other titles found" to "Nu s-au găsit alte titluri",
    "Cast information unavailable" to "Informațiile despre distribuție nu sunt disponibile",
    "TOP FOOTBALL" to "FOTBAL IMPORTANT", "LIVE" to "LIVE", "NEXT" to "URMĂTORUL", "LAST" to "ULTIMUL",
    "Loading fixtures…" to "Se încarcă meciurile…", "Fixtures unavailable · retrying" to "Meciurile nu sunt disponibile · reîncercăm",
    "No fixture" to "Niciun meci", "STREAMING" to "STREAMING", "RENT / BUY" to "ÎNCHIRIAZĂ / CUMPĂRĂ",
    "Search movies & TV" to "Caută filme și seriale", "Manage Home apps" to "Gestionează aplicațiile",
    "Discover movies" to "Descoperă filme", "Popular movies" to "Filme populare",
    "Home" to "Acasă", "Apps" to "Aplicații", "Weather & time" to "Vreme și oră", "System" to "Sistem",
    "Action" to "Acțiune", "Adventure" to "Aventură", "Animation" to "Animație", "Comedy" to "Comedie",
    "Crime" to "Crimă", "Documentary" to "Documentar", "Drama" to "Dramă", "Family" to "Familie",
    "Fantasy" to "Fantastic", "History" to "Istoric", "Horror" to "Horror", "Music" to "Muzică",
    "Mystery" to "Mister", "Romance" to "Romantic", "Sci-Fi" to "SF", "TV Movie" to "Film TV",
    "Thriller" to "Thriller", "War" to "Război", "Western" to "Western", "Action & Adventure" to "Acțiune și aventură",
    "Kids" to "Copii", "News" to "Știri", "Reality" to "Reality", "Sci-Fi & Fantasy" to "SF și fantastic",
    "Soap" to "Telenovelă", "Talk" to "Talk-show", "War & Politics" to "Război și politică",
    "NO STREAMING LISTED" to "FĂRĂ STREAMING LISTAT", "Coming soon" to "În curând", "Now in cinemas" to "Acum în cinematografe",
    "Trending this week" to "În tendințe săptămâna aceasta", "Top rated movies" to "Filme apreciate",
    "Popular series" to "Seriale populare", "Popular animation" to "Animații populare",
    "Movies by TMDB · Availability by JustWatch · Weather by Open-Meteo" to "Filme prin TMDB · Disponibilitate prin JustWatch · Vreme prin Open-Meteo",
)

internal fun localizeUi(text: String, romanian: Boolean): String {
    if (!romanian) return text
    RomanianUi[text]?.let { return it }
    return when {
        text.contains("Weather") -> text.replace("Weather", "Vreme")
        text.startsWith("Hidden · ") -> text.replaceFirst("Hidden", "Ascunse")
        text.startsWith("After ") -> text.replaceFirst("After ", "După ").replace(" min", " min")
        text.startsWith("Selected · ") -> text.replaceFirst("Selected", "Selectat")
        text.startsWith("Availability by JustWatch") -> text.replaceFirst("Availability by", "Disponibilitate prin")
        text.contains(" · Movies & TV") -> text.replace(" · Movies & TV", " · Filme și TV").replace(" · Updating…", " · Se actualizează…")
        text.startsWith("LIVE · ") -> text.split(" · ").joinToString(" · ") { RomanianUi[it] ?: it }
        text.startsWith("NEXT · ") -> text.replaceFirst("NEXT", "URMĂTORUL").split(" · ").joinToString(" · ") { RomanianUi[it] ?: it }
        text.startsWith("LAST · ") -> text.replaceFirst("LAST", "ULTIMUL").split(" · ").joinToString(" · ") { RomanianUi[it] ?: it }
        text.contains("COMING") -> text.replace("COMING", "ÎN CURÂND")
        text.contains("RELEASES TODAY") -> text.replace("RELEASES TODAY", "APARE AZI")
        text.contains("RELEASED") -> text.replace("RELEASED", "LANSAT")
        else -> text
    }
}

@Composable internal fun tr(text: String) = localizeUi(text, LocalRomanian.current)
