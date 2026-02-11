package io.github.skeletonxf.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.AppContent
import io.github.skeletonxf.ui.Environment
import io.github.skeletonxf.ui.MainMenuContent
import io.github.skeletonxf.ui.RolePickerContent
import io.github.skeletonxf.ui.nav.Navigation.Main.rememberNavigationStack

sealed interface Navigation {
    data object Main : Navigation

    data object RolePicker : Navigation

    // We must use class here not data class so that a new instance causes recomposition
    // when restarting the game.
    class Game(
        val configuration: Configuration,
    ) : Navigation

    @Composable
    fun rememberNavigationStack(): SnapshotStateList<Navigation> =
        remember { mutableStateListOf(Main) }
}

@Composable
fun NavigationRoot(
    environment: Environment,
) {
    val backStack = rememberNavigationStack()
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
            when (key) {
                is Navigation.Main -> NavEntry(key) {
                    MainMenuContent(
                        onNewGame = { configuration ->
                            environment.startNewGame(configuration)
                            backStack.add(Navigation.Game(configuration))
                        },
                        onVersusComputer = {
                            backStack.add(Navigation.RolePicker)
                        }
                    )
                }

                is Navigation.RolePicker -> NavEntry(key) {
                    RolePickerContent(
                        onNewGame = { configuration ->
                            environment.startNewGame(configuration)
                            backStack[backStack.lastIndex] = Navigation.Game(configuration)
                        },
                        onCancel = {
                            backStack.removeLastOrNull()
                        }
                    )
                }

                is Navigation.Game -> NavEntry(key) {
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
                                backStack[backStack.lastIndex] = Navigation.Game(handle.configuration)
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
        }
    )
}
