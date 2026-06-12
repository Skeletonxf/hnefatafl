package io.github.skeletonxf.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {
    // No op
    Box(modifier)
}

@Composable
actual fun VerticalScrollbar(
    scrollState: LazyListState,
    modifier: Modifier
) {
    // No op
    Box(modifier)
}