package io.github.skeletonxf.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MenuContent(
    onNewGame: () -> Unit,
) = Surface {
    val strings = LocalStrings.current
    Column {
        Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column {
                val changeStrings = LocalChangeStrings.current
                var dropdown by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { dropdown = true },
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = MaterialTheme.colors.background,
                    ),
                ) {
                    Text(text = strings.name)
                }
                DropdownMenu(expanded = dropdown, onDismissRequest = { dropdown = false }) {
                    locales.forEach { (locale, strings) ->
                        DropdownMenuItem(onClick = { changeStrings(locale) }.then { dropdown = false }) {
                            Text(text = strings.name)
                        }
                    }
                }
            }
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
                    text = strings.menu.title,
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
                    Text(text = strings.menu.twoPlayerGame)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {},
                    enabled = false,
                ) {
                    Text(text = strings.menu.versusComputer)
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