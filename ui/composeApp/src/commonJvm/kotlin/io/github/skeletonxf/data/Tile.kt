package io.github.skeletonxf.data

import uniffi.hnefatafl.Dead

sealed interface Piece {
    fun ownedBy(player: Player): Boolean

    fun toDead(): Dead
}

sealed interface Tile {
    companion object {
        fun from(tile: uniffi.hnefatafl.Tile): Tile = when (tile) {
            uniffi.hnefatafl.Tile.EMPTY -> Empty
            uniffi.hnefatafl.Tile.ATTACKER -> Attacker
            uniffi.hnefatafl.Tile.DEFENDER -> Defender
            uniffi.hnefatafl.Tile.KING -> King
        }

        fun from(tile: Dead): Piece = when (tile) {
            Dead.ATTACKER -> Attacker
            Dead.DEFENDER -> Defender
            Dead.KING -> King
        }
    }

    fun toTile(): uniffi.hnefatafl.Tile = when (this) {
        Attacker -> uniffi.hnefatafl.Tile.ATTACKER
        Defender -> uniffi.hnefatafl.Tile.DEFENDER
        Empty -> uniffi.hnefatafl.Tile.EMPTY
        King -> uniffi.hnefatafl.Tile.KING
    }

    object Empty : Tile

    object Attacker : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Attacker
        override fun toDead() = Dead.ATTACKER
    }

    object Defender : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Defender
        override fun toDead() = Dead.DEFENDER
    }

    object King : Tile, Piece {
        override fun ownedBy(player: Player) = player == Player.Defender
        override fun toDead() = Dead.KING
    }
}