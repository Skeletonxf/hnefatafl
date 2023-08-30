package io.github.skeletonxf.ui

import io.github.skeletonxf.ffi.FFIThrowable
import androidx.compose.runtime.State as ComposeState
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner

interface GameState {
    fun debug()

    val state: ComposeState<State>

    fun makePlay(play: Play)
    fun makeBotPlay()

    sealed class State {
        abstract val opponent: Game.Opponent

        data class Game(
            val board: BoardData,
            val plays: List<Play>,
            val winner: Winner,
            val turn: Player,
            val dead: List<Piece>,
            val turnCount: UInt,
            override val opponent: Opponent,
        ) : State() {
            enum class Opponent {
                Human,
                ComputerAttackers,
                ComputerDefenders,
            }
        }

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
            override val opponent: Game.Opponent,
        ) : State()
    }
}
