package io.github.skeletonxf.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import io.github.skeletonxf.ui.theme.PreviewSurface
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.ffi.FFIThrowable

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