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
) {
    data class Menu(
        val twoPlayerGame: String = "Two player game",
        val versusComputer: String = "Versus computer (coming soon)",
    ) {
        val title: String = "Hnefatafl"
    }
}

val locales = mapOf(
    "en-GB" to Strings(
        name = "🇬🇧"
    ),
    "en-US" to Strings(
        name = "🇺🇸"
    ),
    "es" to Strings(
        name = "🇪🇸",
        menu = Strings.Menu(
            // this is kinda a different copy but versus computer seems to translate
            // better as "Jugar contra" so keeping this form for the 2 player option
            // works nicely. "juego de dos jugadores" would be more literal, but I don't
            // like the repetition
            twoPlayerGame = "Jugar contra su amigo",
            versusComputer = "Jugar contra el ordenador (próximamente)",
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