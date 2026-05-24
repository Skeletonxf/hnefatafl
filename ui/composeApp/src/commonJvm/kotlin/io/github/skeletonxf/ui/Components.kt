import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.skeletonxf.ui.strings.LocalStrings
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipTextButton(
    onClick: () -> Unit,
    text: String,
    tooltip: String,
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
                Text(text = tooltip)
            }
        },
        state = rememberTooltipState(),
    ) {
        TextButton(
            onClick = onClick,
        ) {
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun TooltipTextButtonPreview() = PreviewSurface {
    TooltipTextButton(
        onClick = {},
        text = "Apache License 2.0",
        tooltip = "https://www.apache.org/licenses/LICENSE-2.0",
    )
}

@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.component
    Button(onClick = onClick, modifier = modifier) {
        Text(text = strings.cancel)
    }
}

@Composable
@Preview
fun CancelButtonPreview() = PreviewSurface {
    CancelButton(onClick = {})
}

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.component
    Button(onClick = onClick, modifier = modifier) {
        Text(text = strings.back)
    }
}

@Composable
@Preview
fun BackButtonPreview() = PreviewSurface {
    BackButton(onClick = {})
}

@Composable
fun TitleHeader(
    start: @Composable () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box { start() }
            Box { title() }
        },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        // Remove minWidth constraint from fillMaxWidth for each component
        // so they don't expand to fill space
        val start = measurables[0].measure(constraints = constraints.copy(minWidth = 0))
        // Deduct the start content width twice over so we can center the title.
        val title = measurables[1].measure(
            constraints = constraints.copy(
                minWidth = 0,
                maxWidth = constraints.maxWidth - (start.width * 2)
            )
        )
        // The minWidth constraint always determines our width
        val height = max(start.height, title.height)
        layout(
            width = constraints.minWidth,
            height = height,
        ) {
            // Put the start content vertically centered at the start of the layout
            start.placeRelative(x = 0, y = (height - start.height) / 2)
            // Try to center the title content horizontally and vertically
            title.placeRelative(
                x = (constraints.minWidth - title.width) / 2,
                y = (height - title.height) / 2
            )
        }
    }
}

@Preview
@Composable
fun TitleHeaderPreview() = PreviewSurface {
    TitleHeader(
        start = { BackButton(onClick = {}) },
        title = { Text("Title header") },
        modifier = Modifier.width(300.dp)
    )
}
