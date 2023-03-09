package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.ffi.KEnum

sealed interface Piece {
    fun ownedBy(player: Player): Boolean

    companion object {
        fun valueOf(tile: Tile): Piece? = when (tile) {
            is Piece -> tile
            else -> null
        }
    }
}

sealed interface Tile : KEnum {
    override fun value(): Byte = when (this) {
        Empty -> bindings_h.Empty().toByte()
        Attacker -> bindings_h.Attacker().toByte()
        Defender -> bindings_h.Defender().toByte()
        King -> bindings_h.King().toByte()
    }

    companion object {
        private val variants = listOf(Empty, Attacker, Defender, King)
        fun valueOf(tile: Byte) = KEnum.valueOf(tile, variants, Empty)
    }

    object Empty : Tile

    object Attacker : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Attacker
    }

    object Defender : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Defender
    }

    object King : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Defender
    }
}