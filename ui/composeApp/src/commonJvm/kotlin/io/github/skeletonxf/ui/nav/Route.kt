package io.github.skeletonxf.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.AppContentScreen
import io.github.skeletonxf.ui.Environment
import io.github.skeletonxf.ui.MainMenuScreen
import io.github.skeletonxf.ui.RolePickerScreen
import io.github.skeletonxf.ui.credits.CreditsScreen
import io.github.skeletonxf.ui.credits.LicenseDetail
import io.github.skeletonxf.ui.credits.LicenseViewerScreen
import io.github.skeletonxf.ui.tutorial.TutorialScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object RolePicker : Route

    @Serializable
    data object Credits : Route

    @Serializable
    data class LicenseViewer(
        val license: LicenseDetail,
    ) : Route

    @Serializable
    data object Tutorial : Route

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
                    subclass(Credits::class, Credits.serializer())
                    subclass(LicenseViewer::class, LicenseViewer.serializer())
                    subclass(Tutorial::class, Tutorial.serializer())
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
    snackbar: @Composable () -> Unit,
) {
    val backStack = Route.rememberNavigationStack()
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        Surface {
            NavigationRoot(environment, backStack)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
        ) {
            snackbar()
        }
    }
}

@Composable
private fun NavigationRoot(
    environment: Environment,
    backStack: NavBackStack<NavKey> = Route.rememberNavigationStack(),
) {
    val handle = environment.gameStateHandle
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
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
                        MainMenuScreen(
                            onNewGame = { configuration ->
                                environment.startNewGame(configuration)
                                backStack.add(Route.Game(configuration))
                            },
                            onVersusComputer = {
                                backStack.add(Route.RolePicker)
                            },
                            onTutorial = {
                                backStack.add(Route.Tutorial)
                            },
                            onCredits = {
                                backStack.add(Route.Credits)
                            }
                        )
                    }

                    is Route.RolePicker -> NavEntry(key) {
                        RolePickerScreen(
                            onNewGame = { configuration ->
                                environment.startNewGame(configuration)
                                backStack[backStack.lastIndex] = Route.Game(configuration)
                            },
                            onCancel = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }

                    is Route.Credits -> NavEntry(key) {
                        CreditsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onView = { backStack.add(Route.LicenseViewer(it)) }
                        )
                    }

                    is Route.LicenseViewer -> NavEntry(key) {
                        LicenseViewerScreen(
                            onBack = { backStack.removeLastOrNull() },
                            license = key.license
                        )
                    }

                    is Route.Tutorial -> NavEntry(key) {
                        TutorialScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    is Route.Game -> NavEntry(key) {
                        if (handle != null) {
                            val state by handle.state
                            AppContentScreen(
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
