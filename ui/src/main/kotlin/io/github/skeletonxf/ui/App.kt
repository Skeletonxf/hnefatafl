package io.github.skeletonxf.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
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
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.ffi.FFIThrowable
import io.github.skeletonxf.ui.theme.HnefataflColors
import java.lang.Integer.max
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
                        winner = state.winner,
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
private fun Title(
    turn: Player,
    winner: Winner,
    modifier: Modifier = Modifier,
) {
    Text(
        text = when (winner) {
            Winner.None -> when (turn) {
                Player.Defender -> "Defender's turn"
                Player.Attacker -> "Attacker's turn"
            }
            Winner.Defenders -> "Defender's victory"
            Winner.Attackers -> "Attacker's victory"
        },
        modifier = modifier,
        color = HnefataflColors.night,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            letterSpacing = 0.sp
        ),
    )
}

@Composable
fun Content(board: BoardData, plays: List<Play>, turn: Player, winner: Winner, makePlay: (Play) -> Unit) {
    val title: @Composable () -> Unit = {
        Crossfade(targetState = turn, animationSpec = tween(durationMillis = 500)) { turn ->
            Title(
                turn = turn,
                winner = winner,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
    val mainContent: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Board(board, plays, makePlay)
        }
    }
    val header: @Composable () -> Unit = {
        Box(Modifier.background(HnefataflColors.light))
    }
    Layout(
        content = {
            title()
            mainContent()
            header()
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        if (
            !constraints.hasBoundedHeight || !constraints.hasBoundedWidth ||
            constraints.minHeight > constraints.maxHeight || constraints.minWidth > constraints.maxWidth
        ) {
            throw IllegalArgumentException("Constraints unsupported: $constraints")
        }

        val titleMeasurable = measurables[0]
        val mainContentMeasurable = measurables[1]
        val headerMeasurable = measurables[2]

        val titlePlaceable = titleMeasurable.measure(constraints.copy(minHeight = 0))
        val mainContentPlaceable = mainContentMeasurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight - (titlePlaceable.height),
            )
        )

        val desiredVerticalPadding = constraints.maxHeight - mainContentPlaceable.height
        val usedHeight = mainContentPlaceable.height + titlePlaceable.height
        val emptyHeight = constraints.maxHeight - usedHeight
        val verticalPadding = min(max((desiredVerticalPadding / 2) - titlePlaceable.height, 0), emptyHeight)

        val headerHeight = verticalPadding + titlePlaceable.height
        val headerPlaceable = headerMeasurable.measure(
            Constraints(
                minWidth = constraints.maxWidth,
                maxWidth = constraints.maxWidth,
                minHeight = headerHeight,
                maxHeight = headerHeight,
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            // Header sits at the top behind everything else
            headerPlaceable.placeRelative(x = 0, y = 0)
            // Title sits above board
            titlePlaceable.placeRelative(
                x = 0,
                y = verticalPadding
            )
            // Main content varies based on available space for vertical alignment, but always starts just below
            // title
            mainContentPlaceable.placeRelative(
                x = 0,
                y = verticalPadding + titlePlaceable.height
            )
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content(
        board = emptyBoard,
        plays = listOf(),
        turn = Player.Defender,
        winner = Winner.None,
        makePlay = {}
    )
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