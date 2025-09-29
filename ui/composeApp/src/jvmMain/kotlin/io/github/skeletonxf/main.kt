package io.github.skeletonxf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.settings.Settings
import io.github.skeletonxf.ui.App
import io.github.skeletonxf.ui.Res
import io.github.skeletonxf.ui.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = {
            Settings.instance?.save(immediate = true)
            exitApplication()
        },
        title = "Hnefatafl",
    ) {
        SideEffect {
            window.minimumSize = Dimension(450, 450)
        }
        IconSideEffect(window)
        App()
    }
}

@Composable
private fun IconSideEffect(composeWindow: ComposeWindow) {
    // Workaround for https://github.com/JetBrains/compose-jb/issues/1838
    val icon = painterResource(Res.drawable.icon)
    val density = LocalDensity.current
    SideEffect {
        composeWindow.iconImage = icon.toAwtImage(
            density, LayoutDirection.Ltr, Size(128F, 128F)
        )
    }
}
