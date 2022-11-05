package io.github.skeletonxf.board

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import io.github.skeletonxf.HnefataflColors
import io.github.skeletonxf.PreviewSurface

enum class Tile {
    Empty, Attacker, Defender, King
}

enum class TileColor(val color: Color) {
    Blank(HnefataflColors.grey), Filled(HnefataflColors.brown)
}

@Stable
interface BoardData {
    fun get(x: Int, y: Int): Tile
    val length: Int
}

object EmptyBoard : BoardData {
    override val length = 11
    override fun get(x: Int, y: Int) = Tile.Empty
}

@Composable
fun Board(
    board: BoardData,
) {
    BoxWithConstraints {
        val width = max(minWidth, maxWidth)
        val height = max(minHeight, maxHeight)
        if (!width.isFinite || !height.isFinite) {
            throw UnsupportedOperationException()
        }
        val square = min(width, height)
        val margin = 1.dp
        val margins = margin.value * (board.length + 1)
        val availableTileSize = min(((square.value - margins) / board.length.toFloat()).dp, 64.dp)
        val roundedDownTileSize = (availableTileSize.value.toInt() / 4) * 4
        val tileSize = roundedDownTileSize.dp - margin
        val boardSize = (tileSize * board.length) + (margin * (board.length + 1))
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.background(HnefataflColors.night).size(boardSize)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until board.length) {
                        Spacer(Modifier.height(margin))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (column in 0 until board.length) {
                                Spacer(Modifier.width(margin))
                                val color = when ((row + column) % 2 == 0) {
                                    true -> TileColor.Blank
                                    false -> TileColor.Filled
                                }
                                val tile = board.get(row, column)
                                Box(
                                    modifier = Modifier
                                        .background(color.color)
                                        .size(tileSize)
                                )
                            }
                            Spacer(Modifier.height(margin))
                        }
                    }
                    Spacer(Modifier.height(margin))
                }
            }
        }
    }
}

@Composable
@Preview
private fun EmptyBoardPreview() = PreviewSurface {
    Board(EmptyBoard)
}