package io.github.skeletonxf.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import io.github.skeletonxf.ffi.ConfigHandle
import io.github.skeletonxf.ffi.GameStateHandle
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
    val strings = LocalStrings.current.menu
    Column {
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
                    onClick = onNewGame,
                ) {
                    Text(text = strings.twoPlayerGame)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {},
                    enabled = false,
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
                backgroundColor = MaterialTheme.colors.background,
            ),
        ) {
            Text(text = strings.name)
        }
        DropdownMenu(expanded = dropdown, onDismissRequest = { onSetDropdown(false) }) {
            locales.forEach { (locale, strings) ->
                DropdownMenuItem(onClick = { changeStrings(locale) }.then { onSetDropdown(false) }) {
                    Text(text = strings.name)
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
