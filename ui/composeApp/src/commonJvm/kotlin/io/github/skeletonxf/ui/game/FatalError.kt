package io.github.skeletonxf.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.ui.GameState
import io.github.skeletonxf.ui.strings.LocalStrings

data class Actions(
    val onRestart: () -> Unit,
    val onQuit: () -> Unit,
)

@Composable
fun FatalError(
    state: GameState.State.FatalError,
    actions: Actions?,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current.game
    Column(
        modifier = modifier,
    ) {
        Text(text = strings.failure)
        Spacer(Modifier.height(16.dp))
        Text(state.message)
        Spacer(Modifier.height(8.dp))
        Text(state.cause.message ?: "")
        actions?.let { actions ->
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = actions.onRestart,
            ) {
                Text(text = strings.restart)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = actions.onQuit,
            ) {
                Text(text = strings.mainMenu)
            }
        }
    }
}