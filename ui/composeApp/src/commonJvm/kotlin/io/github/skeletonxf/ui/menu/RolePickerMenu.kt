package io.github.skeletonxf.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.ui.Icon
import io.github.skeletonxf.ui.RoleType
import io.github.skeletonxf.ui.strings.LocalStrings

@Composable
fun RolePickerMenu(
    onNewGame: (Configuration) -> Unit,
    onCancel: () -> Unit,
) = Column {
    val strings = LocalStrings.current.rolePicker
    Box(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Button(onClick = onCancel, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = strings.cancel)
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
            Row {
                RolePicker(
                    onClick = {
                        onNewGame(Configuration(attackers = RoleType.Human, defenders = RoleType.Computer))
                    },
                    label = strings.attackers,
                    icon = Tile.Attacker
                )
                Spacer(Modifier.width(32.dp))
                RolePicker(
                    onClick = {
                        onNewGame(Configuration(attackers = RoleType.Computer, defenders = RoleType.Human))
                    },
                    label = strings.defenders,
                    icon = Tile.King
                )
            }
        }
    }
}

@Composable
private fun RolePicker(
    onClick: () -> Unit,
    label: String,
    icon: Piece,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    icon.Icon(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onClick,
    ) {
        Text(text = label)
    }
}