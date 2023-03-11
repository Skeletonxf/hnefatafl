package io.github.skeletonxf.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.ui.theme.PreviewSurface

@Composable
fun MenuContent(
    onNewGame: () -> Unit,
) = Surface {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Hnefatafl",
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                letterSpacing = 0.sp
            )
            Image(
                painter = painterResource("images/icon.svg"),
                contentDescription = "App icon",
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNewGame,
            ) {
                Text(text = "Two player game")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {},
                enabled = false,
            ) {
                Text(text = "Versus computer (coming soon)")
            }
        }
    }
}

@Composable
@Preview
private fun MenuContentPreview() = PreviewSurface {
    MenuContent(onNewGame = {})
}