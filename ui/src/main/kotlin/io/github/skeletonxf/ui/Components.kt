import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout

@Composable
private fun ContentSpacer(
    content: @Composable () -> Unit,
) {
    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            // Just pad the space for the content
            // *runs away*
        }
    }
}