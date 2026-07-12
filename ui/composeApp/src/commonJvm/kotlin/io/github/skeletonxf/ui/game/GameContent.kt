package io.github.skeletonxf.ui.game

import TooltipIconButton
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.skeletonxf.HeaderAlignment
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.getPlatform
import io.github.skeletonxf.ui.Res
import io.github.skeletonxf.ui.close
import io.github.skeletonxf.ui.localBackgroundScope
import io.github.skeletonxf.ui.restart
import io.github.skeletonxf.ui.state.StateHolder
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.locales
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.painterResource

data class GameContentState(
    val configuration: Configuration,
    private val scope: CoroutineScope,
) : StateHolder {

    val state = mutableStateOf(startGame())

    fun makePlay(play: Play) {
        state.value.handle.makePlay(play)
    }

    fun makeBotPlay() {
        state.value.handle.makeBotPlay()
    }

    fun restart() {
        state.value = startGame()
    }

    data class State(
        val handle: GameStateHandle,
    )

    private fun startGame() = State(
        GameStateHandle(localBackgroundScope, configuration)
    )
}

class GameContentViewModel(private val configuration: Configuration) : ViewModel() {
    val state = mutableStateOf(
        GameContentState(
            scope = viewModelScope,
            configuration = configuration
        )
    )

    fun makePlay(play: Play) = state.value.makePlay(play)
    fun makeBotPlay() = state.value.makeBotPlay()
    fun restart() = state.value.restart()
}

@Composable
fun GameContentScreen(
    configuration: Configuration,
    onBack: () -> Unit,
) {
    val viewModel = viewModel { GameContentViewModel(configuration = configuration) }
    val state = viewModel.state.value.state.value.handle.state.value

    GameContent(
        state = state,
        makePlay = viewModel::makePlay,
        makeBotPlay = viewModel::makeBotPlay,
        onRestart = viewModel::restart,
        onQuit = onBack,
    )
}

@Composable
fun GameContent(
    state: GameState.State,
    makePlay: (Play) -> Unit,
    makeBotPlay: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    when (state) {
        is GameState.State.Game -> {
            GameContent(
                state = state,
                makePlay = makePlay,
                onRestart = onRestart,
                onQuit = onQuit,
            )
            LaunchedEffect(state.turnCount) {
                if (state.turnPlayerRole() is Role.Computer && state.winner == Winner.None) {
                    makeBotPlay()
                }
            }
            // Disable back presses while playing to prevent accidentally
            // quitting the game. TODO: Turn into confirmation dialog?
            // FIXME: This seems to have no effect on Esc in desktop?
            val navigationState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                isBackEnabled = state.winner != Winner.None,
                onBackCancelled = {},
                onBackCompleted = {},
            )
        }

        is GameState.State.FatalError -> FatalError(
            state = state,
            actions = Actions(
                onRestart = onRestart,
                onQuit = onQuit,
            ),
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameContent(
    state: GameState.State.Game,
    makePlay: (Play) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) {
    val boardState = rememberBoardState()
    val onRestart = boardState::deselect.then(onRestart)
    val onQuit = boardState::deselect.then(onQuit)
    val safeDrawing = WindowInsets.safeDrawing

    Column {
        Box(
            modifier = Modifier
                .background(HnefataflColors.light)
                .then(
                    safeDrawing.let { insets ->
                        val padding = insets.asPaddingValues()
                        val layoutDirection = LocalLayoutDirection.current
                        val excludingBottom = PaddingValues(
                            top = padding.calculateTopPadding(),
                            start = padding.calculateStartPadding(layoutDirection),
                            end = padding.calculateEndPadding(layoutDirection),
                        )
                        Modifier
                            .padding(excludingBottom)
                            .consumeWindowInsets(excludingBottom)
                    }
                ),
        ) {
            Header(
                state = state,
                onRestart = onRestart,
                onQuit = onQuit,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(safeDrawing.let {
                        insets ->
                    val padding = insets.asPaddingValues()
                    val layoutDirection = LocalLayoutDirection.current
                    val excludingBottom = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    )
                    Modifier
                        .padding(excludingBottom)
                        .consumeWindowInsets(excludingBottom)
                }),
            contentAlignment = Alignment.Center,
        ) {
            Board(
                board = state.board,
                boardState = boardState,
                plays = state.plays,
                dead = state.dead,
                makePlay = makePlay,
                previousPlay = state.previousPlay,
                isLoading = state.turnPlayerRole().isLoading
            )
        }
    }
}

data class Actions(
    val onRestart: () -> Unit,
    val onQuit: () -> Unit,
)

@Composable
fun FatalError(
    state: GameState.State.FatalError,
    actions: Actions?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current.game
    Column(
        modifier = modifier,
    ) {
        Text(text = strings.failure)
        Spacer(Modifier.height(16.dp))
        Text(state.message)
        Spacer(Modifier.height(8.dp))
        Text(state.cause.message ?: "")
        actions?.let { actions ->
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = actions.onRestart,
            ) {
                Text(text = strings.restart)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = actions.onQuit,
            ) {
                Text(text = strings.mainMenu)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    state: GameState.State.Game,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.game
    Surface(
        color = HnefataflColors.light,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipIconButton(
                onClick = onQuit,
                painter = painterResource(Res.drawable.close),
                text = strings.quit,
            )
            val headerAlignment = getPlatform().headerAlignment
            Box(modifier = Modifier.weight(1F)) {
                Crossfade(
                    targetState = state.turn,
                    animationSpec = tween(durationMillis = 500)
                ) { turn ->
                    Title(
                        turn = turn,
                        winner = state.winner,
                        turnCount = state.turnCount,
                        textAlign = when (headerAlignment) {
                            HeaderAlignment.Left -> TextAlign.Left
                            HeaderAlignment.Center -> TextAlign.Center
                        },
                        modifier = Modifier.align(
                            when (headerAlignment) {
                                HeaderAlignment.Left -> Alignment.CenterStart
                                HeaderAlignment.Center -> Alignment.Center
                            }
                        )
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
            TooltipIconButton(
                onClick = onRestart,
                painter = painterResource(Res.drawable.restart),
                text = strings.restart,
            )
        }
    }
}

@Composable
private fun Title(
    turn: Player,
    winner: Winner,
    turnCount: UInt,
    textAlign: TextAlign,
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
        textAlign = textAlign,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.sp
        ),
    )
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    GameContent(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.None,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
            attackers = Role.Human(),
            defenders = Role.Human(),
            previousPlay = null,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun FatalErrorPreview() = PreviewSurface {
    GameContent(
        state = GameState.State.FatalError(
            message = "Contextual message here",
            cause = Throwable("Problem here"),
            attackers = RoleType.Human,
            defenders = RoleType.Human,
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
        GameContent(
            state = GameState.State.FatalError(
                message = "Contextual message here",
                cause = Throwable("Problem here"),
                attackers = RoleType.Human,
                defenders = RoleType.Human,
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
    GameContent(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.Attackers,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
            attackers = Role.Human(),
            defenders = Role.Human(),
            previousPlay = null,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}
