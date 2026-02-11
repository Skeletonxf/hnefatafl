package io.github.skeletonxf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.ui.menu.MainMenu
import io.github.skeletonxf.ui.menu.RolePickerMenu
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MainMenuContent(
    onNewGame: (Configuration) -> Unit,
    onVersusComputer: () -> Unit,
) = Surface {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
    ) {
        MainMenu(
            onNewGame = onNewGame,
            onVersusComputer = onVersusComputer,
        )
    }
}

@Composable
fun RolePickerContent(
    onNewGame: (Configuration) -> Unit,
    onCancel: () -> Unit,
) = Surface {
    Column(
        modifier = Modifier.fillMaxSize().shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
    ) {
        RolePickerMenu(
            onNewGame = onNewGame,
            onCancel = onCancel,
        )
    }
}

@Composable
@Preview
private fun MainMenuContentPreview() = PreviewSurface {
    MainMenuContent(onNewGame = {}, onVersusComputer = {})
}

@Composable
@Preview
private fun RolePickerContentPreview() = PreviewSurface {
    RolePickerContent(onNewGame = {}, onCancel = {})
}
