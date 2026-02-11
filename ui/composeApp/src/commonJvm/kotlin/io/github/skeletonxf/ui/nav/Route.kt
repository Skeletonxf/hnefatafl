package io.github.skeletonxf.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.AppContent
import io.github.skeletonxf.ui.Environment
import io.github.skeletonxf.ui.MainMenuContent
import io.github.skeletonxf.ui.RolePickerContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object RolePicker : Route

    // We must use class here not data class so that a new instance causes recomposition
    // when restarting the game.
    @Serializable
    class Game(
        val configuration: Configuration,
    ) : Route

    companion object {
        private val configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                   subclass(Main::class, Main.serializer())
                   subclass(RolePicker::class, RolePicker.serializer())
                   subclass(Game::class, Game.serializer())
                }
            }
        }

        @Composable
        fun rememberNavigationStack(): NavBackStack<NavKey> = rememberNavBackStack(
            configuration = configuration,
            Main,
        )
    }
}

@Composable
fun NavigationRoot(
    environment: Environment,
) {
    val backStack = Route.rememberNavigationStack()
    val handle = environment.gameStateHandle
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut(),
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut(),
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(),
                initialContentExit = fadeOut(),
            )
        },
        entryProvider = { key ->
            if (key is Route) {
                when (key) {
                    is Route.Main -> NavEntry(key) {
                        MainMenuContent(
                            onNewGame = { configuration ->
                                environment.startNewGame(configuration)
                                backStack.add(Route.Game(configuration))
                            },
                            onVersusComputer = {
                                backStack.add(Route.RolePicker)
                            }
                        )
                    }

                    is Route.RolePicker -> NavEntry(key) {
                        RolePickerContent(
                            onNewGame = { configuration ->
                                environment.startNewGame(configuration)
                                backStack[backStack.lastIndex] = Route.Game(configuration)
                            },
                            onCancel = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }

                    is Route.Game -> NavEntry(key) {
                        if (handle != null) {
                            val state by handle.state
                            AppContent(
                                state = state,
                                makePlay = handle::makePlay,
                                makeBotPlay = handle::makeBotPlay,
                                onRestart = {
                                    val success = environment.restartGame()
                                    if (!success) {
                                        Log.error("Unable to find configuration used for previous game")
                                    }
                                    backStack[backStack.lastIndex] = Route.Game(handle.configuration)
                                },
                                onQuit = {
                                    environment.stopGame()
                                    backStack.removeLastOrNull()
                                },
                            )
                        } else {
                            // We lost our handle somehow, so kick the
                            // user back
                            backStack.removeLastOrNull()
                        }
                    }
                }
            } else {
                NavEntry(key) {
                    // should be impossible
                }
            }
        }
    )
}
