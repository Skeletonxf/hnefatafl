package io.github.skeletonxf.data

sealed interface Piece {
    fun ownedBy(player: Player): Boolean
}

sealed interface Tile {
    companion object {
        fun from(tile: uniffi.hnefatafl.Tile): Tile = when (tile) {
            uniffi.hnefatafl.Tile.EMPTY -> Empty
            uniffi.hnefatafl.Tile.ATTACKER -> Attacker
            uniffi.hnefatafl.Tile.DEFENDER -> Defender
            uniffi.hnefatafl.Tile.KING -> King
        }

        fun from(tile: uniffi.hnefatafl.Dead): Piece = when (tile) {
            uniffi.hnefatafl.Dead.ATTACKER -> Attacker
            uniffi.hnefatafl.Dead.DEFENDER -> Defender
            uniffi.hnefatafl.Dead.KING -> King
        }
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