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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import io.github.skeletonxf.ui.strings.locales
import io.github.skeletonxf.ui.theme.HnefataflColors
import java.lang.Integer.max
import java.lang.Integer.min

@Composable
fun App(
    state: GameState.State,
    makePlay: (Play) -> Unit,
    makeBotPlay: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    val strings = LocalStrings.current.game
    when (state) {
        is GameState.State.Game -> {
            Content(
                state = state,
                makePlay = makePlay,
                onRestart = onRestart,
                onQuit = onQuit,
            )
            LaunchedEffect(state.turn) {
                // TODO: Loading state and prevent player making moves while it's the computer's turn
                val computerTurn = when (state.opponent) {
                    GameState.State.Game.Opponent.Human -> return@LaunchedEffect
                    GameState.State.Game.Opponent.ComputerAttackers -> Player.Attacker
                    GameState.State.Game.Opponent.ComputerDefenders -> Player.Defender
                }
                if (state.turn == computerTurn) {
                    makeBotPlay()
                }
            }
        }

        is GameState.State.FatalError -> Column {
            Text(text = strings.failure)
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
                Text(text = strings.mainMenu)
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
        Player.Attacker -> (if (turnCount > 0u) turnCount - 1u else turnCount) / 2u
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

        // Ideally each button goes in the top left and top right, but when horizontal space is
        // tight switch to both buttons on the left to give the main text more room
        val centerAlignTitle = quitPlaceable.width + restartPlaceable.width < constraints.maxWidth * 0.25

        val titleMargins = when (centerAlignTitle) {
            true -> 2 * titleMargin
            false -> titleMargin
        }
        val titlePlaceable = titleMeasurable.measure(
            constraints.copy(
                minHeight = 0,
                minWidth = constraints.maxWidth - titleMargins,
                maxWidth = constraints.maxWidth - titleMargins,
            )
        )
        val titleHeaderHeight = when (centerAlignTitle) {
            true -> max(titlePlaceable.height, max(quitPlaceable.height, restartPlaceable.height))
            false -> max(titlePlaceable.height, quitPlaceable.height + restartPlaceable.height)
        }

        val mainContentPlaceable = mainContentMeasurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight - (titleHeaderHeight),
            )
        )

        val desiredVerticalPadding = constraints.maxHeight - mainContentPlaceable.height
        val usedHeight = mainContentPlaceable.height + titleHeaderHeight
        val emptyHeight = constraints.maxHeight - usedHeight
        val verticalPadding = min(max((desiredVerticalPadding / 2) - titleHeaderHeight, 0), emptyHeight)

        val headerHeight = verticalPadding + titleHeaderHeight
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
            titlePlaceable.placeRelative(
                x = when (centerAlignTitle) {
                    // Title sits above board, centered between the two buttons
                    true -> (constraints.maxWidth - titlePlaceable.width) / 2
                    // Title header is left aligned, placed after the two buttons
                    false -> titleMargin
                },
                // On very portrait layouts, top align the title instead of sitting above
                // the board, to keep it aligned with the buttons
                y = when (constraints.maxHeight > constraints.maxWidth * 1.1) {
                    true -> 0
                    false -> verticalPadding
                }
            )
            // Main content varies based on available space for vertical alignment, but always starts just below
            // title
            mainContentPlaceable.placeRelative(
                x = 0,
                y = verticalPadding + titleHeaderHeight
            )
            when (centerAlignTitle) {
                true -> {
                    // Quit and restart buttons are pinned to the top corners
                    quitPlaceable.placeRelative(x = 0, y = 0)
                    restartPlaceable.placeRelative(x = constraints.maxWidth - restartPlaceable.width, y = 0)
                }
                false -> {
                    // Quit and restart buttons are left aligned in a column
                    quitPlaceable.placeRelative(x = 0, y = 0)
                    restartPlaceable.placeRelative(x = 0, y = quitPlaceable.height)
                }
            }
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
            opponent = GameState.State.Game.Opponent.Human,
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
            cause = FFIThrowable("Problem here", null, Void::class),
            opponent = GameState.State.Game.Opponent.Human,
        ),
        makePlay = {},
        makeBotPlay = {},
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun FatalErrorSpanishPreview() = PreviewSurface {
    CompositionLocalProvider(LocalStrings provides locales["es-ES"]!!) {
        App(
            state = GameState.State.FatalError(
                message = "Contextual message here",
                cause = FFIThrowable("Problem here", null, Void::class),
                opponent = GameState.State.Game.Opponent.Human,
            ),
            makePlay = {},
            makeBotPlay = {},
            onRestart = {},
            onQuit = {},
        )
    }
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
            opponent = GameState.State.Game.Opponent.Human,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}