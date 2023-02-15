import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.*
import io.github.skeletonxf.board.BoardData
import io.github.skeletonxf.board.Board
import io.github.skeletonxf.board.Tile

@Composable
@Preview
fun App(state: GameState.State) {
    HnefataflMaterialTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    is GameState.State.Game ->  Content(state.board)
                    is GameState.State.FatalError -> Column {
                        Text("Something went horribly wrong 😭")
                        Spacer(Modifier.height(16.dp))
                        Text(state.message)
                        Spacer(Modifier.height(8.dp))
                        Text(state.cause.message)
                    }
                }
            }
        }
    }
}

@Composable
fun Content(board: BoardData) {
    Column {
        Box(
            Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Board(board)
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content(BoardData(listOf(Tile.Empty), 1))
}

fun main() {
    val handle = GameStateHandle()
    val state by handle.state

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Hnefatafl",
        ) {
            IconSideEffect(window)
            App(state)
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
@Preview
private fun FatalErrorPreview() = PreviewSurface {
    App(
        state = GameState.State.FatalError(
            message = "Contextual message here",
            cause = FFIThrowable("Problem here", null, Void::class)
        )
    )
}