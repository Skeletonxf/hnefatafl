package io.github.skeletonxf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.skeletonxf.ui.App
import io.github.skeletonxf.ui.setup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appState = AppGlobalState.getInstance(applicationContext)
        setContent {
            App(environment = appState.environment)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(environment = setup())
}
