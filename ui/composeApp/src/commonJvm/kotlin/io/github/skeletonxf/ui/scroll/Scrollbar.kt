package io.github.skeletonxf.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollbar(scrollState: LazyListState, modifier: Modifier = Modifier)