package io.github.skeletonxf.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.skeletonxf.ffi.Configuration
import io.github.skeletonxf.ui.menu.MainMenu
import io.github.skeletonxf.ui.menu.MenuState
import io.github.skeletonxf.ui.menu.RolePickerMenu
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MenuContent(
    onNewGame: (Configuration) -> Unit,
) = Surface {
    var state by remember { mutableStateOf(MenuState.MainMenu) }
    Column(
        modifier = Modifier.fillMaxSize().shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
    ) {
        Crossfade(targetState = state) { s ->
            when (s) {
                MenuState.MainMenu -> MainMenu(
                    onNewGame = onNewGame,
                    onVersusComputer = { state = MenuState.RolePicker }
                )

                MenuState.RolePicker -> RolePickerMenu(
                    onNewGame = onNewGame,
                    onCancel = { state = MenuState.MainMenu }
                )
            }
        }
    }
}

@Composable
@Preview
private fun MenuContentPreview() = PreviewSurface {
    MenuContent(onNewGame = {})
}
