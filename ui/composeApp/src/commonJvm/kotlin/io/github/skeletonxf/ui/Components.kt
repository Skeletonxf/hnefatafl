import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.ui.theme.PreviewSurface
import org.jetbrains.compose.ui.tooling.preview.Preview

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

@Composable
fun LoadingSpinner(size: Dp, strokeWidth: Dp) {
    val preview = LocalInspectionMode.current
    if (preview) {
        CircularProgressIndicator(
            progress = 0.5F,
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth
        )
    } else {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
@Preview
fun LoadingSpinnerPreview() = PreviewSurface {
    LoadingSpinner(size = 32.dp, strokeWidth = 4.dp)
}