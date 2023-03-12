package io.github.skeletonxf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Position

class BoardState(initialSate: Position? = null) {
    var selected by mutableStateOf(initialSate)
        private set

    fun filterPlaysToSelected(plays: List<Play>): List<Position> = plays
        .filter { play -> play.from == selected }.map { it.to }

    fun select(position: Position) {
        selected = if (selected == position) {
            null
        } else {
            position
        }
    }

    fun deselect() { selected = null }
}

@Composable
fun rememberBoardState(): BoardState = rememberSaveable(saver = BoardStateSaver) {
    BoardState()
}

// There must be some official way of making Bundle-storable types for desktop too?
private val BoardStateSaver = Saver<BoardState, String>(
    save = { boardState -> boardState.selected.toSerializedString() },
    restore = { string -> BoardState(string.fromSerializedString()) },
)

private fun Position?.toSerializedString(): String = when (this) {
    null -> "null"
    else -> "${this.x}-${this.y}"
}

private fun String.fromSerializedString(): Position? = when (this) {
    "null" -> null
    else -> split("-").map { it.toInt() }.let { positions ->
        Position(x = positions[0], y = positions[1])
    }
}

