package io.github.skeletonxf.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.ui.strings.LocalChangeStrings
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.locales
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.PreviewSurface
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun MenuContent(
    onNewGame: (GameState.State.Game.Opponent) -> Unit,
) = Surface {
    val strings = LocalStrings.current.menu
    Column(
        modifier = Modifier.fillMaxSize().shaderGradient(color1 = HnefataflColors.grey, color2 = Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            LanguagePicker()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = strings.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = 0.sp
                )
                Image(
                    painter = painterResource("images/icon.svg"),
                    contentDescription = strings.appIcon,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onNewGame(GameState.State.Game.Opponent.Human) },
                ) {
                    Text(text = strings.twoPlayerGame)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onNewGame(GameState.State.Game.Opponent.ComputerAttackers) },
                ) {
                    Text(text = strings.versusComputer)
                }
            }
        }
    }
}

@Composable
fun LanguagePicker() {
    var dropdown by remember { mutableStateOf(false) }
    LanguagePicker(
        dropdown = dropdown,
        onSetDropdown = { dropdown = it },
    )
}

@Composable
fun LanguagePicker(
    dropdown: Boolean,
    onSetDropdown: (Boolean) -> Unit,
) {
    val strings = LocalStrings.current
    Column {
        val changeStrings = LocalChangeStrings.current
        TextButton(
            onClick = { onSetDropdown(true) },
            colors = ButtonDefaults.textButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(text = strings.name)
        }
        DropdownMenu(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceVariant),
            expanded = dropdown,
            onDismissRequest = { onSetDropdown(false) }
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                locales.forEach { (locale, strings) ->
                    DropdownMenuItem(
                        text = { Text(text = strings.name) },
                        onClick = { changeStrings(locale) }.then { onSetDropdown(false) }
                    )
                }
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