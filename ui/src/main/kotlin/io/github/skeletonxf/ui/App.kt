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
import androidx.compose.material.Button
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
import io.github.skeletonxf.ui.theme.PreviewSurface
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.ffi.FFIThrowable
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import java.lang.Integer.max
import java.lang.Integer.min

@Composable
fun App(
    state: GameState.State,
    makePlay: (Play) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    val strings = LocalStrings.current.game
    when (state) {
        is GameState.State.Game -> Content(
            state = state,
            makePlay = makePlay,
            onRestart = onRestart,
            onQuit = onQuit,
        )

        is GameState.State.FatalError -> Column {
            Text("Something went horribly wrong 😭")
            Spacer(Modifier.height(16.dp))
            Text(state.message)
            Spacer(Modifier.height(8.dp))
            Text(state.cause.message)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
            ) {
                Text(text = strings.restart)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onQuit,
            ) {
                Text(text = "Main menu")
            }
        }
    }
}

@Composable
private fun QuitButton(onQuit: () -> Unit) {
    Button(onClick = onQuit, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(text = LocalStrings.current.game.quit)
    }
}

@Composable
private fun RestartButton(onRestart: () -> Unit) {
    Button(onClick = onRestart, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(text = LocalStrings.current.game.restart)
    }
}

@Composable
private fun Title(
    turn: Player,
    winner: Winner,
    turnCount: UInt,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.game
    val turnsTaken = when (turn) {
        Player.Defender -> turnCount / 2u
        Player.Attacker -> (turnCount - 1u) / 2u
    }
    Text(
        text = when (winner) {
            Winner.None -> when (turn) {
                Player.Defender -> strings.defendersTurn(turnsTaken)
                Player.Attacker -> strings.attackersTurn(turnsTaken)
            }

            Winner.Defenders -> strings.defendersVictory(turnsTaken)
            Winner.Attackers -> strings.attackersVictory(turnsTaken)
        },
        modifier = modifier,
        color = HnefataflColors.night,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.sp
        ),
    )
}

@Suppress("NAME_SHADOWING")
@Composable
fun Content(
    state: GameState.State.Game,
    makePlay: (Play) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) {
    val boardState = rememberBoardState()
    val onRestart = boardState::deselect.then(onRestart)
    val onQuit = boardState::deselect.then(onQuit)
    val title: @Composable () -> Unit = {
        Crossfade(targetState = state.turn, animationSpec = tween(durationMillis = 500)) { turn ->
            Title(
                turn = turn,
                winner = state.winner,
                turnCount = state.turnCount,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
    val mainContent: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Board(
                board = state.board,
                boardState = boardState,
                plays = state.plays,
                dead = state.dead,
                makePlay = makePlay,
            )
        }
    }
    val header: @Composable () -> Unit = {
        Box(Modifier.background(HnefataflColors.light))
    }
    Layout(
        content = {
            QuitButton(onQuit = onQuit)
            RestartButton(onRestart = onRestart)
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

        val quitButton = measurables[0]
        val restartButton = measurables[1]
        val titleMeasurable = measurables[2]
        val mainContentMeasurable = measurables[3]
        val headerMeasurable = measurables[4]

        val quitPlaceable = quitButton.measure(constraints.copy(minHeight = 0, minWidth = 0))
        val restartPlaceable = restartButton.measure(constraints.copy(minHeight = 0, minWidth = 0))
        val titleMargin = max(quitPlaceable.width, restartPlaceable.width)

        // TODO: If we have more height than width available we should consider placing the title under
        // the quit and restart buttons instead of centered between them, which will free up a bunch of space
        // for the title text without shrinking the board
        val titlePlaceable = titleMeasurable.measure(
            constraints.copy(
                minHeight = 0,
                minWidth = constraints.maxWidth - (2 * titleMargin),
                maxWidth = constraints.maxWidth - (2 * titleMargin),
            )
        )

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
            // Title sits above board, centered between the two buttons
            titlePlaceable.placeRelative(
                x = (constraints.maxWidth - titlePlaceable.width) / 2,
                y = verticalPadding
            )
            // Main content varies based on available space for vertical alignment, but always starts just below
            // title
            mainContentPlaceable.placeRelative(
                x = 0,
                y = verticalPadding + titlePlaceable.height
            )
            // Quit and restart buttons are pinned to the top corners
            quitPlaceable.placeRelative(x = 0, y = 0)
            restartPlaceable.placeRelative(x = constraints.maxWidth - restartPlaceable.width, y = 0)
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.None,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
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
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun GameOverPreview() = PreviewSurface {
    Content(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.Attackers,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}