package io.github.skeletonxf

import androidx.compose.runtime.State as ComposeState
import io.github.skeletonxf.board.BoardData

interface GameState {
    fun debug()

    val state: ComposeState<State>

    sealed class State {
        data class Game(
            val board: BoardData
        ) : State()

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
        ) : State()
    }
}
