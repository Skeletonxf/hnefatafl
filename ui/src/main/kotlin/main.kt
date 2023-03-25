import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.ffi.ConfigHandle
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.settings.Config
import io.github.skeletonxf.ui.App
import io.github.skeletonxf.ui.MenuContent
import io.github.skeletonxf.ui.strings.ProvideStrings
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import java.awt.Dimension

fun main() {
   application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Hnefatafl",
        ) {
            SideEffect {
                window.minimumSize = Dimension(450, 450)
            }
            IconSideEffect(window)

            var handle: GameStateHandle? by remember { mutableStateOf(null) }
            // TODO: File IO and proper config handle creation, maybe wrap this in a settings handle?
            // Should have graceful way to fall back to sensible defaults if file IO fails
            val config: ConfigHandle? by remember { mutableStateOf(ConfigHandle.new("""locale = 'en-GB'""").okOrNull()) }
            Root(
                handle = handle,
                config = config,
                onNewGame = { handle = GameStateHandle() },
                onQuit = { handle = null },
            )
        }
    }
}

@Composable
fun Root(
    handle: GameStateHandle?,
    config: Config?,
    onNewGame: () -> Unit,
    onQuit: () -> Unit,
) = HnefataflMaterialTheme {
    ProvideStrings(config = config) {
        Surface {
            when (handle) {
                null -> MenuContent(onNewGame = onNewGame)
                else -> {
                    val state by handle.state
                    App(
                        state = state,
                        makePlay = handle::makePlay,
                        onQuit = onQuit,
                        onRestart = onNewGame,
                    )
                }
            }
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
