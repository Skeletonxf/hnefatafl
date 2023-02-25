package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.ffi.KEnum

sealed interface Piece

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
    object Attacker : Tile, Piece
    object Defender : Tile, Piece
    object King : Tile, Piece
}