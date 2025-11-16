package io.github.skeletonxf.ui

import LoadingSpinner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Position
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.TileColor
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.max
import kotlin.math.roundToInt

val emptyBoard = BoardData(
    List(11 * 11) { Tile.Empty }, 11
)

@Composable
fun Board(
    board: BoardData,
    boardState: BoardState,
    plays: List<Play>,
    dead: List<Piece>,
    makePlay: (Play) -> Unit,
    isLoading: Boolean,
) {
    // Clear selection and don't let the user select when loading
    LaunchedEffect(isLoading) {
        if (isLoading) {
            boardState.deselect()
        }
    }
    Board(
        board,
        moves = boardState.filterPlaysToSelected(plays),
        dead = dead,
        onSelect = boardState::select.takeUnless { isLoading } ?: {},
        selected = boardState.selected.takeUnless { isLoading },
        makePlay = makePlay,
        isLoading = isLoading,
    )
}

@Composable
fun Board(
    board: BoardData,
    moves: List<Position>,
    dead: List<Piece>,
    onSelect: (Position) -> Unit,
    selected: Position?,
    makePlay: (Play) -> Unit,
    isLoading: Boolean,
) {
    BoxWithConstraints {
        val width = max(minWidth, maxWidth)
        val height = max(minHeight, maxHeight)
        if (!width.isFinite || !height.isFinite) {
            throw UnsupportedOperationException("Unsupported constraints: $constraints")
        }
        val longerLength = max(width, height)
        val shorterLength = min(width, height)
        val isHorizontal = width > height
        // The board length gives the number of tiles for our shorter length
        // We need to reserve at least 32 dp on the longer length on both sides
        // for the graveyards. This could make the longer length become shorter
        // after factoring in the graveyards.
        val longerBoardLength = longerLength - 64.dp
        val shorterBoardLength = shorterLength
        val square = min(shorterBoardLength, longerBoardLength)
        val margin = 1.dp
        val margins = margin.value * (board.length + 1)
        val availableTileSize = min(((square.value - margins) / board.length.toFloat()).dp, 64.dp)
        val roundedDownTileSize = (availableTileSize.value.roundToInt() / 4) * 4
        val tileSize = roundedDownTileSize.dp - margin
        val boardSize = (tileSize * board.length) + (margin * (board.length + 1))
        val selectedPiece = selected?.let { board[it.x, it.y] }

        val availableSideWidth = (longerLength - boardSize) / 2
        val sideMargin = if (availableSideWidth < 100.dp) {
            2.dp
        } else {
            4.dp
        }
        val sideSpace = min(availableSideWidth - sideMargin, tileSize)
        val graveyardSize = if (isHorizontal) {
            Modifier
                .width(sideSpace)
                .height(boardSize)
        } else {
            Modifier
                .height(sideSpace)
                .width(boardSize)
        }
        val defenderGraveyard: @Composable () -> Unit = {
            PieceGraveyard(
                dead = dead.filter { it.ownedBy(Player.Defender) },
                tileSize = sideSpace,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .border(width = margin, color = HnefataflColors.night)
                    .then(graveyardSize)
            )
        }
        val boardContents: @Composable () -> Unit = {
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
                                    isSpecial = position.let { (x, y) ->
                                        (x == board.length / 2 && y == board.length / 2) ||
                                                (x == 0 && y == 0) ||
                                                (x == board.length - 1 && y == 0) ||
                                                (x == 0 && y == board.length - 1) ||
                                                (x == board.length - 1 && y == board.length - 1)

                                    }
                                )
                            }
                            Spacer(Modifier.height(margin))
                        }
                    }
                    Spacer(Modifier.height(margin))
                }
                if (isLoading) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45F)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingSpinner(size = tileSize * 3, strokeWidth = max(2.dp, tileSize / 3))
                        }
                    }
                }
            }
        }
        val attackerGraveyard: @Composable () -> Unit = {
            PieceGraveyard(
                dead = dead.filter { it.ownedBy(Player.Attacker) },
                tileSize = sideSpace,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .border(width = margin, color = HnefataflColors.night)
                    .then(graveyardSize)
            )
        }
        if (isHorizontal) {
            Row {
                defenderGraveyard()
                Spacer(Modifier.width(sideMargin))
                boardContents()
                Spacer(Modifier.width(sideMargin))
                attackerGraveyard()
            }
        } else {
            Column {
                defenderGraveyard()
                Spacer(Modifier.height(sideMargin))
                boardContents()
                Spacer(Modifier.height(sideMargin))
                attackerGraveyard()
            }
        }
    }
}

@Composable
@Preview
private fun EmptyBoardPreview() = PreviewSurface {
    Board(
        board = emptyBoard,
        moves = listOf(),
        dead = listOf(),
        onSelect = {},
        selected = null,
        makePlay = {},
        isLoading = false,
    )
}

@Composable
@Preview
private fun LoadingBoardPreview() = PreviewSurface {
    Board(
        board = emptyBoard,
        moves = listOf(),
        dead = listOf(),
        onSelect = {},
        selected = null,
        makePlay = {},
        isLoading = true,
    )
}

@Composable
private fun Tile(
    tile: Tile,
    color: TileColor,
    tileSize: Dp,
    onClick: () -> Unit,
    isSelected: Boolean,
    isMoveFor: Piece?,
    isSpecial: Boolean,
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
        if (isSpecial) {
            Box(
                modifier = Modifier
                    .size(tileSize - 2.dp)
                    .align(Alignment.Center)
                    .border(width = Dp.Hairline, color = HnefataflColors.night)
            )
        }
        val modifier = Modifier.size(tileSize.minus(inset)).align(Alignment.Center)
        when (tile) {
            Tile.Empty -> Unit
            is Piece -> tile.Icon(modifier)
        }
        isMoveFor?.Icon(modifier.alpha(0.20F))
    }
}

@Composable
fun Piece.Icon(
    modifier: Modifier
) {
    val strings = LocalStrings.current.game.piece
    when (this) {
        Tile.Attacker -> Icon(
            painter = painterResource(Res.drawable.piece),
            contentDescription = strings.attacker,
            modifier = modifier,
            tint = HnefataflColors.brown,
        )

        Tile.Defender -> Icon(
            painter = painterResource(Res.drawable.piece),
            contentDescription = strings.defender,
            modifier = modifier,
            tint = HnefataflColors.night,
        )

        Tile.King -> Icon(
            painter = painterResource(Res.drawable.king),
            contentDescription = strings.king,
            modifier = modifier,
            tint = HnefataflColors.night,
        )
    }
}

@Composable
private fun PieceGraveyard(
    dead: List<Piece>,
    tileSize: Dp,
    modifier: Modifier = Modifier,
) {
    val inset = 8.dp
    val pieceSize = tileSize - inset
    Layout(
        content = {
            dead.forEach { piece ->
                Box(modifier = Modifier.size(tileSize)) {
                    piece.Icon(modifier = Modifier.size(pieceSize).align(Alignment.Center))
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
            throw IllegalArgumentException("Constraints unsupported: $constraints")
        }
        val width = max(constraints.minWidth, constraints.maxWidth)
        val height = max(constraints.minHeight, constraints.maxHeight)
        val longerLength = max(width, height)
        val isHorizontal = width > height
        val pieces = measurables.size
        val spacePerPiece = (longerLength - (tileSize.toPx() / 2)) / pieces
        val spaceUsed = spacePerPiece
        val placeables = measurables.map { measurable ->
            measurable.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = tileSize.roundToPx(),
                    minHeight = 0,
                    maxHeight = tileSize.roundToPx()
                )
            )
        }
        layout(width, height) {
            var i = if (isHorizontal) {
                width - (tileSize.toPx() / 2)
            } else {
                height - (tileSize.toPx() / 2)
            }
            placeables.forEach { placeable ->
                if (isHorizontal) {
                    placeable.placeRelative(
                        x = (i - (placeable.width) / 2).roundToInt(),
                        y = (height - placeable.height) / 2,
                    )
                } else {
                    placeable.placeRelative(
                        x = (width - placeable.width) / 2,
                        y = (i - (placeable.height / 2)).roundToInt(),
                    )
                }
                i -= spaceUsed
            }
        }
    }
}

@Composable
@Preview
private fun PieceGraveyardRowsPreview() = PreviewSurface {
    Row {
        listOf(1, 3, 4, 6, 10, 12, 16, 18).forEach { number ->
            PieceGraveyard(
                dead = List(number) { Tile.Defender },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .background(HnefataflColors.light)
                    .width(50.dp)
                    .height(300.dp),
                tileSize = 32.dp
            )
        }
    }
}

@Composable
@Preview
private fun PieceGraveyardColumnsPreview() = PreviewSurface {
    Column {
        listOf(1, 3, 4, 6, 10, 12, 16, 18).forEach { number ->
            PieceGraveyard(
                dead = List(number) { Tile.Defender },
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .background(HnefataflColors.light)
                    .width(300.dp)
                    .height(50.dp),
                tileSize = 32.dp
            )
        }
    }
}