package io.github.skeletonxf.ui.menu

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.ui.Res
import io.github.skeletonxf.ui.RoleType
import io.github.skeletonxf.ui.icon
import io.github.skeletonxf.ui.strings.LocalChangeStrings
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.locales
import org.jetbrains.compose.resources.painterResource

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
fun MainMenu(
    onNewGame: (Configuration) -> Unit,
    onVersusComputer: () -> Unit,
) = Column {
    val strings = LocalStrings.current.mainMenu
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
                .fillMaxWidth()
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
                painter = painterResource(Res.drawable.icon),
                contentDescription = strings.appIcon,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onNewGame(Configuration(attackers = RoleType.Human, defenders = RoleType.Human)) },
            ) {
                Text(text = strings.twoPlayerGame)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onVersusComputer,
            ) {
                Text(text = strings.versusComputer)
            }
        }
    }
}