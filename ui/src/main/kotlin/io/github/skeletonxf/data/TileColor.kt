package io.github.skeletonxf.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.github.skeletonxf.ui.theme.HnefataflColors

enum class TileColor(val color: Color) {
    Blank(HnefataflColors.grey),
    Filled(HnefataflColors.light);

    fun adjust(isLegalMove: Boolean): Color = when (isLegalMove) {
        false -> color
        true -> HnefataflColors.dark.copy(alpha = 0.15F).compositeOver(color)
    }
}