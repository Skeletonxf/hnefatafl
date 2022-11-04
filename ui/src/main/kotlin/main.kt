import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.skeletonxf.Bridge
import io.github.skeletonxf.HnefataflMaterialTheme
import io.github.skeletonxf.PreviewSurface

@Composable
@Preview
fun App() {
    HnefataflMaterialTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Content()
            }
        }
    }
}

@Composable
fun Content() {
    Column {
        Text(text = "Hello world")
        Button(onClick = {}) {
            Text(text = "Click me")
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content()
}

fun main() = application {
    Bridge().helloWorld()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hnefatafl",
    ) {
        App()
    }
}
