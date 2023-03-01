package io.github.skeletonxf.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import io.github.skeletonxf.ui.theme.PreviewSurface
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.ffi.FFIThrowable
import io.github.skeletonxf.ui.theme.HnefataflColors
import java.lang.Integer.min

@Composable
fun App(state: GameState.State, makePlay: (Play) -> Unit) {
    HnefataflMaterialTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    is GameState.State.Game -> Content(
                        board = state.board,
                        plays = state.plays,
                        turn = state.turn,
                        makePlay = makePlay,
                    )

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
fun Content(board: BoardData, plays: List<Play>, turn: Player, makePlay: (Play) -> Unit) {
    val title: @Composable () -> Unit = {
        Crossfade(targetState = turn, animationSpec = tween(durationMillis = 500)) { turn ->
            Text(
                text = when (turn) {
                    Player.Defender -> "Defender's turn"
                    Player.Attacker -> "Attacker's turn"
                },
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 8.dp),
                color = HnefataflColors.night,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontSize = 48.sp,
                    letterSpacing = 0.sp
                ),
            )
        }
    }
    val mainContent: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Board(board, plays, makePlay)
        }
    }
    Layout(
        content = {
            title()
            mainContent()
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        if (
            !constraints.hasBoundedHeight || !constraints.hasBoundedWidth ||
            constraints.minHeight > constraints.maxHeight || constraints.minWidth > constraints.maxWidth
        ) {
            throw IllegalArgumentException("Constraints unsupported: $constraints")
        }
        val (titleMeasurable, mainContentMeasurable) = measurables[0] to measurables[1]

        val titlePlaceable = titleMeasurable.measure(constraints.copy(minHeight = 0))
        val centerAlign = constraints.maxHeight > titlePlaceable.height * 8
        val restrictedHeight = if (centerAlign) {
            // Title can comfortably push main content below it and still center the main content
            constraints.maxHeight - (titlePlaceable.height * 2)
        } else {
            // Push main content down, don't try to vertically center it because space is tight
            constraints.maxHeight - (titlePlaceable.height)
        }
        val mainContentPlaceable = mainContentMeasurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = min(constraints.minHeight, restrictedHeight),
                maxHeight = restrictedHeight,
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            // Title is top aligned
            titlePlaceable.placeRelative(
                x = 0,
                y = 0
            )
            // Main content varies based on available space for vertical alignment, but always starts just below
            // title
            mainContentPlaceable.placeRelative(
                x = 0,
                y = titlePlaceable.height
            )
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content(BoardData(listOf(Tile.Empty), 1), plays = listOf(), turn = Player.Defender, makePlay = {})
}

@Composable
@Preview
private fun FatalErrorPreview() = PreviewSurface {
    App(
        state = GameState.State.FatalError(
            message = "Contextual message here",
            cause = FFIThrowable("Problem here", null, Void::class)
        ),
        makePlay = {},
    )
}