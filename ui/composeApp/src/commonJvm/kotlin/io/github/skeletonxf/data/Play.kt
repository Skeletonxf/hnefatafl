package io.github.skeletonxf.data

import uniffi.hnefatafl.FlatPlay

data class Play(
    val from: Position,
    val to: Position
) {
    companion object {
        fun from(play: FlatPlay): Play = Play(
            from = Position(x = play.fromX.toInt(), y = play.fromY.toInt()),
            to = Position(x = play.toX.toInt(), y = play.toY.toInt())
        )
    }
}