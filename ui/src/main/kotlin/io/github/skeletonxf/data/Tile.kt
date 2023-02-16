package io.github.skeletonxf.data

enum class Tile {
    Empty, Attacker, Defender, King;

    private fun value(): Byte = when (this) {
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
}