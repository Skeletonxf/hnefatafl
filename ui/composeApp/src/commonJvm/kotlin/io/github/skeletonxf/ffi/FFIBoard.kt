package io.github.skeletonxf.ffi

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Tile
import java.lang.foreign.MemorySegment

@JvmInline
value class TileArrayAddress(override val address: MemorySegment): TypedMemorySegment

/**
 * Note: gridSize^2 is assumed to match the length of the tile array, this is not validated
 */
fun tileArrayToBoard(tileArrayAddress: TileArrayAddress, gridSize: Int): BoardData =
    tileArrayAddress.address.use(bindings_h::tile_array_destroy) { tiles ->
        val length = bindings_h.tile_array_length(tiles).toInt()
        BoardData(
            tiles = List(length) { i -> Tile.valueOf(bindings_h.tile_array_get(tiles, i.toLong())) },
            length = gridSize,
        )
    }