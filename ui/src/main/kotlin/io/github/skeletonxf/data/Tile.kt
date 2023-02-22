package io.github.skeletonxf.data

sealed interface Piece

sealed interface Tile {
    fun value(): Byte = when (this) {
        Empty -> 0
        Attacker -> 1
        Defender -> 2
        King -> 3
    }

    companion object {
        fun valueOf(tile: Byte) = when (tile) {
            Attacker.value() -> Attacker
            Defender.value() -> Defender
            King.value() -> King
            else -> Empty
        }
    }

    object Empty : Tile
    object Attacker : Tile, Piece
    object Defender : Tile, Piece
    object King : Tile, Piece
}