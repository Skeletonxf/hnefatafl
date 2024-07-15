package io.github.skeletonxf.ui

import androidx.compose.animation.Crossfade
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import io.github.skeletonxf.ffi.Configuration
import io.github.skeletonxf.ui.menu.MainMenu
import io.github.skeletonxf.ui.menu.MenuState
import io.github.skeletonxf.ui.menu.RolePickerMenu
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

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


private fun Modifier.shaderGradient(color1: Color, color2: Color) = drawWithCache {
    val effect = RuntimeEffect.makeForShader(
        """
        uniform float2 resolution;
        layout(color) uniform half4 color1;
        layout(color) uniform half4 color2;
    
        half4 main(in float2 coord) {
            float2 fraction = coord / resolution.xy;
    
            float mixValue = distance(fraction, vec2(0.2, 1.2));
            return mix(color1, color2, mixValue);
        }
        """.trimIndent()
    )
    val width = size.width
    val height = size.height
    val inputs = arrayOf(
        width,
        height,
        color1.red,
        color1.blue,
        color1.green,
        color1.alpha,
        color2.red,
        color2.blue,
        color2.green,
        color2.alpha
    )
    val bytes = ByteBuffer.allocate(inputs.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
        inputs.forEachIndexed { index, float ->
            putFloat(4 * index, float)
        }
    }.array()
    val shader = effect.makeShader(
        uniforms = Data.makeFromBytes(bytes),
        children = null,
        localMatrix = null,
    )
    val shaderBrush = ShaderBrush(shader)
    onDrawBehind {
        drawRect(
            shaderBrush,
        )
    }
}