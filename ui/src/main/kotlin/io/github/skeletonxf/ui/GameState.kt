package io.github.skeletonxf.ui

import io.github.skeletonxf.ffi.FFIThrowable
import androidx.compose.runtime.State as ComposeState
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner

interface GameState {
    fun debug()

    val state: ComposeState<State>

    fun makePlay(play: Play)

    sealed class State {
        data class Game(
            val board: BoardData,
            val plays: List<Play>,
            val winner: Winner,
            val turn: Player,
        ) : State()

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
        ) : State()
    }
}
