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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.ui.Board
import io.github.skeletonxf.ui.GameState
import io.github.skeletonxf.ui.RoleType
import io.github.skeletonxf.ui.game.FatalError
import io.github.skeletonxf.ui.localBackgroundScope
import io.github.skeletonxf.ui.rememberBoardState
import io.github.skeletonxf.ui.shaderGradient
import io.github.skeletonxf.ui.state.StateHolder
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import kotlinx.coroutines.CoroutineScope

data class PieceMovingTutorialState(
    private val scope: CoroutineScope,
) : StateHolder {

    val state = mutableStateOf(
        State(
            GameStateHandle(
                localBackgroundScope,
                Configuration(attackers = RoleType.Human, defenders = RoleType.Human)
            )
        )
    )

    fun makePlay(play: Play) {
        state.value.handle.makePlay(play)
    }

    data class State(
        val handle: GameStateHandle,
    )
}

@Stable
class PieceMovingTutorialViewModel : ViewModel() {
    val state = PieceMovingTutorialState(
        scope = viewModelScope,
    )

    fun makePlay(play: Play) = state.makePlay(play)
}

enum class Step {
    Moving,
}

@Composable
fun TutorialScreen(
    onBack: () -> Unit,
) {
    TutorialContent(
        onBack = onBack,
        step = Step.Moving,
    )
}

@Composable
fun TutorialContent(
    onBack: () -> Unit,
    step: Step,
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
            when (step) {
                Step.Moving -> PieceMovingTutorialContent()
            }
        }
    }
}

@Composable
fun PieceMovingTutorialContent() {
    val strings = LocalStrings.current.tutorial
    val viewModel = viewModel { PieceMovingTutorialViewModel() }
    val viewModelState = viewModel.state.state.value.handle
    val state by viewModelState.state
    val boardState = rememberBoardState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val readingWidth = 500.dp
        Text(
            text = strings.movement,
            modifier = Modifier.widthIn(max = readingWidth).padding(horizontal = 16.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.movementDescription,
            modifier = Modifier.widthIn(max = readingWidth).padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is GameState.State.Game -> {
                    Board(
                        board = s.board.let { boardData ->
                            // Take the top left of the board to display only
                            BoardData(
                                tiles = List(7 * 7) { i ->
                                    val row = i / 7
                                    val column = i % 7
                                    boardData.get(x = row, y = column)
                                },
                                length = 7,
                            )
                        },
                        boardState = boardState,
                        plays = s.plays.filter { play ->
                            play.from.x < 7 && play.from.y < 7
                        },
                        // Ignore dead for now
                        dead = listOf(),
                        makePlay = viewModel::makePlay,
                        previousPlay = s.previousPlay,
                        isLoading = s.turnPlayerRole().isLoading
                    )
                }
                is GameState.State.FatalError -> FatalError(state = s, actions = null)
            }
        }
    }
}
