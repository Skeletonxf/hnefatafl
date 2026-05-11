import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.ui.theme.PreviewSurface
import androidx.compose.ui.tooling.preview.Preview
import io.github.skeletonxf.ui.Res
import io.github.skeletonxf.ui.restart
import org.jetbrains.compose.resources.painterResource

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
            progress = { 0.5F },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    painter: Painter,
    text: String,
    modifier: Modifier = Modifier,
    tooltipAnchorPosition: TooltipAnchorPosition = TooltipAnchorPosition.Below,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = tooltipAnchorPosition
        ),
        tooltip = {
            PlainTooltip {
                Text(text = text)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
        ) {
            Icon(
                painter = painter,
                contentDescription = text,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TooltipIconButtonPreview() = PreviewSurface {
    TooltipIconButton(
        onClick = {},
        painter = painterResource(Res.drawable.restart),
        text = "Restart",
    )
}