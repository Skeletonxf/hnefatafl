package io.github.skeletonxf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.skeletonxf.ui.App
import io.github.skeletonxf.ui.Environment

class MainActivity : ComponentActivity() {
    lateinit var appGlobalState: AppGlobalState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appState = AppGlobalState.getInstance(applicationContext)
        appGlobalState = appState
        setContent {
            App(environment = appState.environment)
        }
    }

    override fun onPause() {
        super.onPause()
        // Save any pending changes before our app might stop
        appGlobalState.environment.settings.save(immediate = true)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(environment = Environment.dummy())
}
