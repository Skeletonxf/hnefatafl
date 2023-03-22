package io.github.skeletonxf.ui.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

data class Strings(
    val name: String,
    val menu: Menu = Menu(),
    val game: Game = Game(),
) {
    data class Menu(
        val twoPlayerGame: String = "Two player game",
        val versusComputer: String = "Versus computer (coming soon)",
        val appIcon: String = "App icon",
    ) {
        val title: String = "Hnefatafl"
    }

    data class Game(
        val quit: String = "Quit",
        val restart: String = "Restart",
        val defendersTurn: (UInt) -> String = { turnsTaken -> "Defender's turn (${turnsTaken + 1u})" },
        val attackersTurn: (UInt) -> String = { turnsTaken -> "Attacker's turn (${turnsTaken + 1u})" },
        val defendersVictory: (UInt) -> String = { turnsTaken -> "Defender's victory ($turnsTaken)" },
        val attackersVictory: (UInt) -> String = { turnsTaken -> "Attacker's victory ($turnsTaken)" },
    )
}

private val britishEnglish = Strings(
    name = "🇬🇧"
)

private val castilianSpanish = Strings(
    name = "🇪🇸",
    menu = Strings.Menu(
        // this is kinda a different copy but versus computer seems to translate
        // better as "Jugar contra" so keeping this form for the 2 player option
        // works nicely. "juego de dos jugadores" would be more literal, but I don't
        // like the repetition
        twoPlayerGame = "Jugar contra su amigo",
        versusComputer = "Jugar contra el ordenador (próximamente)",
        appIcon = "Icono de la aplicación",
    ),
    game = Strings.Game(
        quit = "Abandonar",
        restart = "Reiniciar",
        defendersTurn = { turnsTaken -> "El turno de los defensores (${turnsTaken + 1u})" },
        attackersTurn = { turnsTaken -> "El turno de los atacantes (${turnsTaken + 1u})" },
        defendersVictory = { turnsTaken -> "La victoria de los defensores ($turnsTaken)" },
        attackersVictory = { turnsTaken -> "La victoria de los atacantes ($turnsTaken)" }
    )
)

val locales = mapOf(
    "en-GB" to britishEnglish,
    "en-US" to britishEnglish.copy(name = "🇺🇸"),
    "es-ES" to castilianSpanish,
    "es-419" to castilianSpanish.copy(
        name = "Latinoamérica",
        menu = castilianSpanish.menu.copy(
            versusComputer = "Jugar contra el computadora (próximamente)",
        )
    )
)

val LocalStrings = compositionLocalOf { locales["en-GB"]!! }

val LocalChangeStrings = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun ProvideStrings(content: @Composable () -> Unit) {
    var strings by remember { mutableStateOf(locales["en-GB"]!!) }
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalChangeStrings provides { locale -> locales[locale]?.let { strings = it } }
    ) {
        content()
    }
}