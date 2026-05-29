package io.github.skeletonxf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
fun MainMenuScreen(
    onNewGame: (Configuration) -> Unit,
    onVersusComputer: () -> Unit,
    onCredits: () -> Unit,
) = Surface {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
            .safeDrawingPadding()
    ) {
        MainMenu(
            onNewGame = onNewGame,
            onVersusComputer = onVersusComputer,
            onCredits = onCredits,
        )
    }
}

@Composable
fun RolePickerScreen(
    onNewGame: (Configuration) -> Unit,
    onCancel: () -> Unit,
) = Surface {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
            .safeDrawingPadding()
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
    MainMenuScreen(onNewGame = {}, onVersusComputer = {}, onCredits = {})
}

@Composable
@Preview
private fun RolePickerContentPreview() = PreviewSurface {
    RolePickerScreen(onNewGame = {}, onCancel = {})
}
