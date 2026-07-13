package io.github.skeletonxf.ui.tutorial

import BackButton
import TitleHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Position
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.ui.game.Board
import io.github.skeletonxf.ui.game.GameState
import io.github.skeletonxf.ui.game.Role
import io.github.skeletonxf.ui.game.RoleType
import io.github.skeletonxf.ui.game.FatalError
import io.github.skeletonxf.ui.localBackgroundScope
import io.github.skeletonxf.ui.game.rememberBoardState
import io.github.skeletonxf.ui.shaderGradient
import io.github.skeletonxf.ui.game.startingBoard
import io.github.skeletonxf.ui.state.StateHolder
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import io.github.skeletonxf.ui.tutorial.TutorialState.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

data class TutorialState(
    val initialState: State,
    private val scope: CoroutineScope,
) : StateHolder {

    val state = mutableStateOf(initialState)

    fun makePlay(play: Play) {
        state.value.handle.makePlay(play)
    }

    fun makeBotPlay() {
        state.value.handle.makeBotPlay()
    }

    fun changeState(to: State) {
        state.value = to
    }

    data class State(
        val handle: GameStateHandle,
    )
}

@Stable
class TutorialViewModel : ViewModel() {
    val state = TutorialState(
        scope = viewModelScope,
        initialState = pieceMovingInitialState(),
    )

    fun makePlay(play: Play) = state.makePlay(play)
    fun makeBotPlay() = state.makeBotPlay()
    fun showCapturing() = state.changeState(capturingInitialState())
    fun showSpecialTiles() = state.changeState(specialTilesInitialState())

    companion object {
        private fun pieceMovingInitialState() = State(
            GameStateHandle(
                localBackgroundScope,
                Configuration(attackers = RoleType.Human, defenders = RoleType.Human)
            )
        )

        private fun capturingInitialState() = State(
            GameStateHandle.fromStartingConfiguration(
                coroutineScope = localBackgroundScope,
                configuration = Configuration(
                    attackers = RoleType.Computer,
                    defenders = RoleType.Human
                ),
                strategy = Role.Computer.Strategy.Random,
                tiles = List(11 * 11) { i ->
                    val x = i / 11
                    val y = i % 11
                    if (x == 8 && y == 8) {
                        Tile.King
                    } else if (
                        (x == 3 && y == 3) ||
                        (x == 4 && y == 1) ||
                        (x == 3 && y == 0) ||
                        (x == 0 && y == 6) ||
                        (x == 6 && y == 6) ||
                        (x == 6 && y == 0) ||
                        (x == 6 && y == 5) ||
                        (x == 5 && y == 6) ||
                        // We pad the 7th row and column with defender tiles to ensure the attackers
                        // can't run out of the visible board shown to the user. This has the
                        // unfortunate side effect that the right and bottom of the 'board' can be
                        // used for captures but the chance of the user triggering this seems fairly
                        // low?
                        (x == 7) ||
                        (y == 7)
                    ) {
                        Tile.Defender
                    } else if ((x == 3 && y == 1) || (x == 0 && y == 5)) {
                        Tile.Attacker
                    } else {
                        Tile.Empty
                    }
                },
                turn = Player.Defender,
                dead = listOf(),
            )
        )

        private fun specialTilesInitialState() = State(
            GameStateHandle.fromStartingConfiguration(
                coroutineScope = localBackgroundScope,
                configuration = Configuration(
                    attackers = RoleType.Computer,
                    defenders = RoleType.Human
                ),
                strategy = Role.Computer.Strategy.Random,
                tiles = List(11 * 11) { i ->
                    val x = i / 11
                    val y = i % 11
                    if (
                        (x == 0 && y == 2) ||
                        // We trap a lone attacker in a bunch of defenders so it can't do anything
                        // but always has a single valid turn.
                        (x == 6 && y == 5) ||
                        (x == 7 && y == 4) ||
                        (x == 8 && y == 4) ||
                        (x == 7 && y == 6) ||
                        (x == 8 && y == 6) ||
                        (x == 9 && y == 5)
                    ) {
                        Tile.Defender
                    } else if (x == 2 && y == 0) {
                        Tile.King
                    } else if (x == 7 && y == 5) {
                        Tile.Attacker
                    } else {
                        Tile.Empty
                    }
                },
                turn = Player.Defender,
                dead = listOf(),
            )
        )
    }
}

enum class Step {
    Moving,
    Capture,
    SpecialTiles,
}

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
) {
    val viewModel = viewModel { TutorialViewModel() }
    val viewModelState = viewModel.state.state.value.handle
    val state by viewModelState.state
    var step by rememberSaveable { mutableStateOf(Step.Moving) }
    val turn = (state as? GameState.State.Game)?.turn
    LaunchedEffect(turn) {
        val game = (state as? GameState.State.Game)
        if (turn == Player.Attacker && step == Step.Moving) {
            viewModel.showCapturing()
            step = Step.Capture
        }
        if (game?.turnPlayerRole() is Role.Computer && game.winner == Winner.None) {
            viewModel.makeBotPlay()
        }
        // The user should have no difficulty winning the capture step but
        // it is possible to lose all the pieces visible on screen at which
        // point the game would be considered still running due to the invisible
        // pieces off screen, so detect if these are the only defender pieces left
        if (
            step == Step.Capture &&
            (game?.winner != Winner.None || game.board.all { position, tile ->
                tile != Tile.Defender || position.x >= 7 && position.y >= 7
            })
        ) {
            viewModel.showSpecialTiles()
            step = Step.SpecialTiles
        }
    }
    TutorialContent(
        onBack = onBack,
        step = step,
        state = state,
        makePlay = viewModel::makePlay,
    )
}

@Composable
fun TutorialContent(
    onBack: () -> Unit,
    step: Step,
    state: GameState.State,
    makePlay: (Play) -> Unit,
) {
    val strings = LocalStrings.current.tutorial
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
            .safeDrawingPadding()
    ) {
        TitleHeader(
            start = {
                BackButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp))
            },
            title = {
                Text(text = strings.title, fontSize = 24.sp)
            },
            modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
        )
        Box(modifier = Modifier.fillMaxSize()) {
            PartialBoardTutorialContent(
                step = step,
                state = state,
                makePlay = makePlay,
            )
        }
    }
}

@Composable
fun PartialBoardTutorialContent(
    step: Step,
    state: GameState.State,
    makePlay: (Play) -> Unit,
) {
    val strings = LocalStrings.current.tutorial
    val boardState = rememberBoardState(
        initialSelection = if (step == Step.Moving) { Position(x = 3, y = 5) } else { null }
    )
    LaunchedEffect(step) {
        // Remove selection state after updating step
        if (step != Step.Moving) {
            boardState.deselect()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val readingWidth = 500.dp
        Text(
            text = when (step) {
                Step.Moving -> strings.movement
                Step.Capture -> strings.capture
                Step.SpecialTiles -> strings.specialTiles
            },
            modifier = Modifier.widthIn(max = readingWidth).padding(horizontal = 16.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (step) {
                Step.Moving -> strings.movementDescription
                Step.Capture -> strings.captureDescription
                Step.SpecialTiles -> strings.specialTilesDescription
            },
            modifier = Modifier.widthIn(max = readingWidth).padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is GameState.State.Game -> {
                    val visibleLength = when (step) {
                        Step.Moving,
                        Step.Capture -> 7
                        Step.SpecialTiles -> 4
                    }
                    Board(
                        board = s.board.let { boardData ->
                            // Take the top left of the board to display only
                            BoardData(
                                tiles = List(visibleLength * visibleLength) { i ->
                                    val row = i / visibleLength
                                    val column = i % visibleLength
                                    boardData.get(x = row, y = column)
                                },
                                length = visibleLength,
                                specialTiles = s.board.specialTiles,
                            )
                        },
                        boardState = boardState,
                        plays = s.plays.filter { play ->
                            play.from.x < visibleLength && play.from.y < visibleLength
                        },
                        // Ignore dead for tutorial
                        dead = listOf(),
                        makePlay = makePlay,
                        previousPlay = s.previousPlay,
                        isLoading = s.turnPlayerRole().isLoading
                    )
                }
                is GameState.State.FatalError -> FatalError(state = s, actions = null)
            }
        }
    }
}

@Composable
@Preview
fun PieceMovingTutorialPreview() = PreviewSurface {
    TutorialContent(
        state = GameState.State.Game(
            board = startingBoard,
            plays = listOf(
                Position(x = 2, y = 5),
                Position(x = 3, y = 1),
                Position(x = 3, y = 2),
                Position(x = 3, y = 3),
                Position(x = 3, y = 4),
                Position(x = 3, y = 6),
            ).map { Play(from = Position(x = 3, y = 5), to = it) },
            winner = Winner.None,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0.toUInt(),
            attackers = Role.Human(),
            defenders = Role.Human(),
            previousPlay = null,
        ),
        makePlay = {},
        onBack = {},
        step = Step.Moving,
    )
}