package io.github.skeletonxf.data

data class BoardData(
    private val tiles: List<Tile>,
    val length: Int,
    val specialTiles: List<Position> = run {
        listOf(
            Position(x = 0, y = 0),
            Position(x = 0, y = length - 1),
            Position(x = length - 1, y = 0),
            Position(x = length - 1, y = length - 1),
            Position(x = length / 2, y = length / 2),
        )
    }
) {
    operator fun get(x: Int, y: Int) = tiles[y + (x * length)]

    fun isSpecial(x: Int, y: Int) = specialTiles.any { position ->
        x == position.x && y == position.y
    }
}