package io.github.skeletonxf.ui

import io.github.skeletonxf.ffi.FFIThrowable
import androidx.compose.runtime.State as ComposeState
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Play

interface GameState {
    fun debug()

    val state: ComposeState<State>

    sealed class State {
        data class Game(
            val board: BoardData,
            val plays: List<Play>,
        ) : State()

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
        ) : State()
    }
}
