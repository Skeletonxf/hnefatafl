package io.github.skeletonxf.ui.theme

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalInspectionMode

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
            backgroundColor = MaterialTheme.colorScheme.background,
            textColor = MaterialTheme.colorScheme.onBackground,
            text = "Background (Grey)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.onSurface,
            text = "Surface (White)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary,
            text = "Primary (Brown)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary,
            text = "Secondary (Dark)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
            text = "Tertiary (Night)",
        )
        ColorBackground(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = "Surface variant",
        )
        ColorBackground(
            backgroundColor = HnefataflColors.light,
            textColor = MaterialTheme.colorScheme.onBackground,
            text = "Light",
        )
    }
}

@Composable
fun PreviewSurface(content: @Composable () -> Unit) = HnefataflMaterialTheme {
    // Not sure why this needs setting to true, but after we can forget about the desktop preview weirdness
    CompositionLocalProvider(LocalInspectionMode provides true) {
        Surface(content = content)
    }
}

@Composable
fun HnefataflMaterialTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = HnefataflColors.brown,
        onPrimary = Color.White,
        secondary = HnefataflColors.dark,
        onSecondary = Color.White,
        tertiary = HnefataflColors.night,
        onTertiary = Color.White,
        surface = Color.White,
        onSurface = HnefataflColors.dark,
        surfaceVariant = Color(red = 0xF5, green = 0xDD, blue = 0xD9),
        onSurfaceVariant = Color(red = 0x53, green = 0x43, blue = 0x40),
        background = HnefataflColors.grey,
        onBackground = HnefataflColors.dark,
        error = Color.Red,
        onError = Color.Black,
        outline = Color(0x85736F),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}