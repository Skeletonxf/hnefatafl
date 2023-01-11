package io.github.skeletonxf

import io.github.skeletonxf.board.Tile

interface GameState {
    fun debug()

    fun board(): List<Tile>
}