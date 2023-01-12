package io.github.skeletonxf

import io.github.skeletonxf.board.BoardData

interface GameState {
    fun debug()

    fun board(): BoardData
}
