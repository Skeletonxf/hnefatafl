package io.github.skeletonxf

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

object HnefataflColors {
    val brown = Color(red = 0x90, green = 0x52, blue = 0x45)
    val dark = Color(red = 0x6A, green = 0x34, blue = 0x41)
    val middle = brown.copy(alpha = 0.5F).compositeOver(dark)
    val night = dark.copy(alpha = 0.6F).compositeOver(Color.Black)
    val grey = brown.copy(alpha = 0.1F).compositeOver(Color.White)
    val light = brown.copy(alpha = 0.3F).compositeOver(Color.White)
}

@Composable
private fun ColorBackground(
    backgroundColor: Color,
    textColor: Color,
    text: String,
) = Box(
    modifier = Modifier.background(backgroundColor).fillMaxWidth()
) {
    Text(text = text, color = textColor)
}

@Composable
@Preview
private fun ColorPreview() = PreviewSurface {
    Column {
        ColorBackground(
            backgroundColor = MaterialTheme.colors.background,
            textColor = MaterialTheme.colors.onBackground,
            text = "Background (Grey)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colors.surface,
            textColor = MaterialTheme.colors.onSurface,
            text = "Surface (White)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colors.primary,
            textColor = MaterialTheme.colors.onPrimary,
            text = "Primary (Brown)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colors.primaryVariant,
            textColor = MaterialTheme.colors.onPrimary,
            text = "Primary variant (Middle)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colors.secondary,
            textColor = MaterialTheme.colors.onSecondary,
            text = "Secondary (Dark)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colors.secondaryVariant,
            textColor = MaterialTheme.colors.onSecondary,
            text = "Secondary variant (Night)",
        )
        ColorBackground(
            backgroundColor = HnefataflColors.light,
            textColor = MaterialTheme.colors.onBackground,
            text = "Light",
        )
    }
}

@Composable
fun PreviewSurface(content: @Composable () -> Unit) = HnefataflMaterialTheme {
    Surface(content = content)
}

@Composable
fun HnefataflMaterialTheme(
    content: @Composable () -> Unit
) {
    val colors = Colors(
        primary = HnefataflColors.brown,
        primaryVariant = HnefataflColors.middle,
        secondary = HnefataflColors.dark,
        secondaryVariant = HnefataflColors.night,
        background = HnefataflColors.grey,
        surface = Color.White,
        error = Color.Red,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = HnefataflColors.dark,
        onSurface = HnefataflColors.dark,
        onError = Color.Black,
        isLight = true,
    )
    MaterialTheme(
        colors = colors,
        content = content,
    )
}