package io.github.skeletonxf.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Position
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.TileColor
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface

val emptyBoard = BoardData(
    List(11 * 11) { Tile.Empty }, 11
)

@Composable
fun Board(
    board: BoardData,
    plays: List<Play>,
    makePlay: (Play) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<Position?>(null) }
    Board(
        board,
        moves = plays.filter { play -> play.from == selected }.map { it.to },
        onSelect = {
            selected = if (selected == it) {
                null
            } else {
                it
            }
        },
        selected = selected,
        makePlay = makePlay,
    )
}

@Composable
fun Board(
    board: BoardData,
    moves: List<Position>,
    onSelect: (Position) -> Unit,
    selected: Position?,
    makePlay: (Play) -> Unit,
) {
    BoxWithConstraints {
        val width = max(minWidth, maxWidth)
        val height = max(minHeight, maxHeight)
        if (!width.isFinite || !height.isFinite) {
            throw UnsupportedOperationException("Unsupported constraints: $constraints")
        }
        val square = min(width, height)
        val margin = 1.dp
        val margins = margin.value * (board.length + 1)
        val availableTileSize = min(((square.value - margins) / board.length.toFloat()).dp, 64.dp)
        val roundedDownTileSize = (availableTileSize.value.toInt() / 4) * 4
        val tileSize = roundedDownTileSize.dp - margin
        val boardSize = (tileSize * board.length) + (margin * (board.length + 1))
        val selectedPiece = selected?.let { board[it.x, it.y] }
        Box(modifier = Modifier.background(HnefataflColors.night).size(boardSize)) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until board.length) {
                    Spacer(Modifier.height(margin))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (column in 0 until board.length) {
                            Spacer(Modifier.width(margin))
                            val position = Position(row, column)
                            val isMoveForSelected = moves.contains(position)
                            Tile(
                                tile = board[row, column],
                                color = when ((row + column) % 2 == 0) {
                                    true -> TileColor.Blank
                                    false -> TileColor.Filled
                                },
                                tileSize = tileSize,
                                onClick = {
                                    if (isMoveForSelected && selected != null) {
                                        makePlay(Play(from = selected, to = position))
                                    } else {
                                        onSelect(position)
                                    }
                                },
                                isSelected = position == selected,
                                isMoveFor = when {
                                    isMoveForSelected && selectedPiece is Piece -> selectedPiece
                                    else -> null
                                },
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

@Composable
@Preview
private fun EmptyBoardPreview() = PreviewSurface {
    Board(board = emptyBoard, moves = listOf(), onSelect = {}, selected = null, makePlay = {})
}

@Composable
private fun Tile(
    tile: Tile,
    color: TileColor,
    tileSize: Dp,
    onClick: () -> Unit,
    isSelected: Boolean,
    isMoveFor: Piece?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val resizeAnimation by animateFloatAsState(
        when (isSelected) {
            true -> when (isPressed) {
                true -> 8.0F
                false -> 4.0F
            }

            false -> when (isPressed) {
                true -> 4.0F
                false -> 8.0F
            }
        }
    )
    val inset = resizeAnimation.dp
    Box(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                indication = LocalIndication.current,
                interactionSource = interactionSource
            )
            .background(color.adjust(isLegalMove = isMoveFor != null))
            .size(tileSize)
    ) {
        val modifier = Modifier.size(tileSize.minus(inset)).align(Alignment.Center)
        when (tile) {
            Tile.Empty -> Unit
            is Piece -> tile.Icon(modifier)
        }
        isMoveFor?.Icon(modifier.alpha(0.20F))
    }
}

@Composable
private fun Piece.Icon(
    modifier: Modifier
) = when (this) {
    Tile.Attacker -> Icon(
        painter = painterResource("images/piece.svg"),
        contentDescription = "Attacker",
        modifier = modifier,
        tint = HnefataflColors.brown,
    )

    Tile.Defender -> Icon(
        painter = painterResource("images/piece.svg"),
        contentDescription = "Defender",
        modifier = modifier,
        tint = HnefataflColors.night,
    )

    Tile.King -> Icon(
        painter = painterResource("images/king.svg"),
        contentDescription = "King",
        modifier = modifier,
        tint = HnefataflColors.night,
    )
}