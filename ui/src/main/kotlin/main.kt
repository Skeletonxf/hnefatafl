import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.ffi.Configuration
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.logging.ForestLogger
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.logging.LogLevel
import io.github.skeletonxf.logging.PrintLogger
import io.github.skeletonxf.logging.Tree
import io.github.skeletonxf.logging.TreeIdentifier
import io.github.skeletonxf.settings.Settings
import io.github.skeletonxf.ui.App
import io.github.skeletonxf.ui.GameState
import io.github.skeletonxf.ui.MenuContent
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.ProvideStrings
import io.github.skeletonxf.ui.strings.Strings
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Dimension

val localBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun main() {
    Log.add(PrintLogger())
    val forest = ForestLogger()
    Log.add(forest)

    application {
        Window(
            onCloseRequest = {
                Settings.instance?.save(immediate = true)
                exitApplication()
            },
            title = "Hnefatafl",
        ) {
            SideEffect {
                window.minimumSize = Dimension(450, 450)
            }
            IconSideEffect(window)

            var handle: GameStateHandle? by remember { mutableStateOf(null) }
            var lastUsedConfig: Configuration? by remember { mutableStateOf(null) }
            Root(
                handle = handle,
                timber = forest.timber.collectAsState().value,
                onDismiss = forest::dismiss,
                onNewGame = { config ->
                    lastUsedConfig = config
                    handle = GameStateHandle(localBackgroundScope, config)
                },
                onRestart = {
                    lastUsedConfig
                        ?.let { config -> handle = GameStateHandle(localBackgroundScope, config) }
                        ?: Log.error("Unable to find configuration used for previous game")
                },
                onQuit = { handle = null },
            )
        }
    }
}

@Composable
fun Root(
    handle: GameStateHandle?,
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
    onNewGame: (Configuration) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = HnefataflMaterialTheme {
    ProvideStrings {
        Box {
            Surface {
                when (handle) {
                    null -> MenuContent(onNewGame = onNewGame)
                    else -> {
                        val state by handle.state
                        App(
                            state = state,
                            makePlay = handle::makePlay,
                            makeBotPlay = handle::makeBotPlay,
                            onQuit = onQuit,
                            onRestart = onRestart,
                        )
                    }
                }
            }
            SnackbarHost(timber, onDismiss, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun IconSideEffect(composeWindow: ComposeWindow) {
    // Workaround for https://github.com/JetBrains/compose-jb/issues/1838
    val icon = painterResource("images/icon.svg")
    val density = LocalDensity.current
    SideEffect {
        composeWindow.iconImage = icon.toAwtImage(density, LayoutDirection.Ltr, Size(128F, 128F))
    }
}

@Composable
private fun SnackbarHost(
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.component
    if (timber.isNotEmpty()) {
        val tree = timber.first()
        Snackbar(
            modifier = modifier.padding(16.dp),
            dismissAction = {
                TextButton(
                    onClick = { onDismiss(tree.id) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Text(text = strings.ok)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Error messages aren't localised at the moment so add a flag prefix for non-English locales to make
                // it a little more clear that the content isn't localised.
                val prefix = when (LocalStrings.current.type) {
                    Strings.Type.British, Strings.Type.American -> ""
                    Strings.Type.CastilianSpanish, Strings.Type.LatinAmericanSpanish -> "🇬🇧 "
                }
                val level = when (tree.level) {
                    LogLevel.Debug -> "D: "
                    LogLevel.Warn -> "W: "
                    LogLevel.Error -> "E: "
                }
                SelectionContainer {
                    Text(text = "$prefix$level${tree.message}", textAlign = TextAlign.Center)
                }
            }
        }
    }
}