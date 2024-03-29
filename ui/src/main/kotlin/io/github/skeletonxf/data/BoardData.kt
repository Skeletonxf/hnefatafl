package io.github.skeletonxf.data

data class BoardData(private val tiles: List<Tile>, val length: Int) {
    operator fun get(x: Int, y: Int) = tiles[y + (x * length)]
}