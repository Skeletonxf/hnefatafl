import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.board.BoardData
import io.github.skeletonxf.GameStateHandle
import io.github.skeletonxf.HnefataflMaterialTheme
import io.github.skeletonxf.PreviewSurface
import io.github.skeletonxf.board.Board
import io.github.skeletonxf.board.Tile

@Composable
@Preview
fun App(board: BoardData) {
    HnefataflMaterialTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Content(board)
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
    val state = GameStateHandle()
    val board = state.board()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Hnefatafl",
        ) {
            App(board)
        }
    }
}
