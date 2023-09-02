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

    sealed interface State {
        data class Game(
            val board: BoardData,
            val plays: List<Play>,
            val winner: Winner,
            val turn: Player,
            val dead: List<Piece>,
            val turnCount: UInt,
            val attackers: Role,
            val defenders: Role,
        ) : State {
            fun turnPlayerRole() = when (turn) {
                Player.Attacker -> attackers
                Player.Defender -> defenders
            }
        }

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
            val attackers: RoleType,
            val defenders: RoleType,
        ) : State
    }
}

sealed interface Role {
    val isLoading: Boolean

    /** If the state can be in a loading state, exists it */
    fun exitLoading(): Role
    /** If the state can be in a loading state, enters it */
    fun enterLoading(): Role

    fun type(): RoleType

    class Human : Role {
        override val isLoading = false
        override fun enterLoading() = this
        override fun exitLoading() = this
        override fun type() = RoleType.Human
    }

    data class Computer(
        override val isLoading: Boolean,
    ) : Role {
        override fun enterLoading() = copy(isLoading = true)
        override fun exitLoading() = copy(isLoading = false)
        override fun type() = RoleType.Computer
    }
}

enum class RoleType {
    Human, Computer
}

