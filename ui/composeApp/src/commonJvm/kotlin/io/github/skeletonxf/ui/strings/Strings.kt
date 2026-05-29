package io.github.skeletonxf.ui.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.settings.Settings
import java.util.Locale

data class Strings(
    val name: String,
    val type: Type,
    val mainMenu: MainMenu = MainMenu(),
    val rolePicker: RolePicker = RolePicker(),
    val credits: Credits = Credits(),
    val game: Game = Game(),
    val component: Component = Component(),
) {
    data class MainMenu(
        val twoPlayerGame: String = "Two player game",
        val versusComputer: String = "Versus computer",
        val appIcon: String = "App icon",
        val credits: String = "Credits",
    ) {
        val title: String = "Hnefatafl"
    }

    data class RolePicker(
        val attackers: String = "Play Attackers",
        val defenders: String = "Play Defenders",
    )

    data class Credits(
        val title: String = "Credits",
        val homepage: String = "Homepage",
        val viewText: String = "View licence",
    )

    data class Game(
        val quit: String = "Quit",
        val restart: String = "Restart",
        val defendersTurn: (UInt) -> String = { turnsTaken -> "Defender's turn (${turnsTaken + 1u})" },
        val attackersTurn: (UInt) -> String = { turnsTaken -> "Attacker's turn (${turnsTaken + 1u})" },
        val defendersVictory: (UInt) -> String = { turnsTaken -> "Defender's victory ($turnsTaken)" },
        val attackersVictory: (UInt) -> String = { turnsTaken -> "Attacker's victory ($turnsTaken)" },
        val mainMenu: String = "Main menu",
        val failure: String = "Something went horribly wrong 😭",
        val piece: Piece = Piece(),
    ) {
        data class Piece(
            val attacker: String = "Attacker",
            val defender: String = "Defender",
            val king: String = "King",
        )
    }

    data class Component(
        val ok: String = "OK",
        val back: String = "Back",
        val cancel: String = "Cancel",
    )

    enum class Type {
        British, American, CastilianSpanish, LatinAmericanSpanish,
    }
}

private val britishEnglish = Strings(
    name = "English 🇬🇧",
    type = Strings.Type.British,
)

private val castilianSpanish = Strings(
    name = "Español 🇪🇸",
    type = Strings.Type.CastilianSpanish,
    mainMenu = Strings.MainMenu(
        // this is kinda a different copy but versus computer seems to translate
        // better as "Jugar contra" so keeping this form for the 2 player option
        // works nicely. "juego de dos jugadores" would be more literal, but I don't
        // like the repetition
        twoPlayerGame = "Jugar contra su amigo",
        versusComputer = "Jugar contra el ordenador",
        appIcon = "Icono de la aplicación",
        credits = "Ver los créditos",
    ),
    rolePicker = Strings.RolePicker(
        attackers = "Jugar los atacantes",
        defenders = "Jugar los defensores",
    ),
    credits = Strings.Credits(
        title = "Los créditos",
        homepage = "Página",
        viewText = "Ver la licencia",
    ),
    game = Strings.Game(
        quit = "Abandonar",
        restart = "Reiniciar",
        defendersTurn = { turnsTaken -> "El turno de los defensores (${turnsTaken + 1u})" },
        attackersTurn = { turnsTaken -> "El turno de los atacantes (${turnsTaken + 1u})" },
        defendersVictory = { turnsTaken -> "La victoria de los defensores ($turnsTaken)" },
        attackersVictory = { turnsTaken -> "La victoria de los atacantes ($turnsTaken)" },
        mainMenu = "Menú principal",
        failure = "Algo terriblemente malo pasó 😭",
        piece = Strings.Game.Piece(
            attacker = "Atacante",
            defender = "Defensor",
            king = "Rey",
        )
    ),
    component = Strings.Component(
        // pronunciation is different but this word seems regularly used in spanish for computing contexts?
        ok = "OK",
        back = "Volver",
        cancel = "Cancelar",
    )
)

val locales = mapOf(
    "en-GB" to britishEnglish,
    "en-US" to britishEnglish.copy(
        name = "English 🇺🇸",
        type = Strings.Type.American,
        credits = britishEnglish.credits.copy(
            viewText = "View license"
        )
    ),
    "es-ES" to castilianSpanish,
    "es-419" to castilianSpanish.copy(
        name = "Español latinoaméricano",
        type = Strings.Type.LatinAmericanSpanish,
        mainMenu = castilianSpanish.mainMenu.copy(
            versusComputer = "Jugar contra el computadora",
        )
    )
)

fun getDefaultLocale(): String {
    val locale = Locale.getDefault()
    val language = locale.language
    val region = locale.country
    val english = "en"
    val spanish = "es"
    val britain = "GB"
    val america = "US"
    val spain = "ES"
    return when {
        language == english && region == britain -> "en-GB"
        language == english && region == america -> "en-US"
        language == spanish && region == spain -> "es-ES"
        language == english -> "en-GB"
        language == spanish -> "es-419"
        else -> "en-GB"
    }.also { Log.debug("Picking $it for user's localisation: $language-$region") }
}

val LocalStrings = compositionLocalOf { britishEnglish }
val LocalChangeStrings = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun ProvideStrings(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    val strings by remember {
        val locale by settings.locale.value
        derivedStateOf {
            locales[locale]
                ?: britishEnglish.also { Log.warn("Unable to match $locale to a supported set of strings") }
        }
    }
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalChangeStrings provides { value ->
            settings.locale.set(value)
            settings.save()
        }
    ) {
        content()
    }
}