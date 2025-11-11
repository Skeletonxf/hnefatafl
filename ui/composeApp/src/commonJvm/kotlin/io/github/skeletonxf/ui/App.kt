package io.github.skeletonxf.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.skeletonxf.ui.theme.PreviewSurface
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.logging.ForestLogger
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.logging.LogLevel
import io.github.skeletonxf.logging.PrintLogger
import io.github.skeletonxf.logging.Tree
import io.github.skeletonxf.logging.TreeIdentifier
import io.github.skeletonxf.settings.FilePaths
import io.github.skeletonxf.settings.Setting
import io.github.skeletonxf.settings.Settings
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.ProvideStrings
import io.github.skeletonxf.ui.strings.Strings
import io.github.skeletonxf.ui.strings.locales
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.file.Path
import java.nio.file.Paths

val localBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

data class Environment(
    val forestLogger: ForestLogger,
    val filePaths: FilePaths,
    val settings: Settings,
) {
    companion object {
        fun dummy(): Environment = Environment(
            forestLogger = ForestLogger(),
            filePaths = object : FilePaths {
                override fun settingsPath(): Path = Paths.get("./settings.toml")
            },
            settings = object : Settings {
                override val locale: Setting<String> = object : Setting<String> {
                    override val value = mutableStateOf("en-GB")
                    override fun set(value: String) = Unit
                }
                override fun save(immediate: Boolean) = Unit
            }
        )
    }
}

fun setup(filePaths: FilePaths): Environment {
    Log.add(PrintLogger())
    val forest = ForestLogger()
    Log.add(forest)
    return Environment(
        forestLogger = forest,
        filePaths = filePaths,
        settings = Settings.new(ioScope = localBackgroundScope, filePaths = filePaths)
    )
}

@Composable
fun App(environment: Environment) {
    val forest = environment.forestLogger
    // FIXME: Need to hoist this higher so configuration changes on
    // Android don't reset it
    var handle: GameStateHandle? by remember { mutableStateOf(null) }
    var lastUsedConfig: Configuration? by remember { mutableStateOf(null) }
    Root(
        environment = environment,
        handle = handle,
        timber = forest.timber.collectAsState().value,
        onDismiss = forest::dismiss,
        onNewGame = { config ->
            lastUsedConfig = config
            handle = GameStateHandle(localBackgroundScope, config)
        },
        onRestart = {
            lastUsedConfig
                ?.let { config -> handle = GameStateHandle(localBackgroundScope, config) }
                ?: Log.error("Unable to find configuration used for previous game")
        },
        onQuit = { handle = null },
    )
}

@Composable
fun Root(
    environment: Environment,
    handle: GameStateHandle?,
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
    onNewGame: (Configuration) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = HnefataflMaterialTheme {
    ProvideStrings(settings = environment.settings) {
        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .safeDrawingPadding()
        ) {
            Surface {
                when (handle) {
                    null -> MenuContent(onNewGame = onNewGame)
                    else -> {
                        val state by handle.state
                        AppContent(
                            state = state,
                            makePlay = handle::makePlay,
                            makeBotPlay = handle::makeBotPlay,
                            onQuit = onQuit,
                            onRestart = onRestart,
                        )
                    }
                }
            }
            SnackbarHost(timber, onDismiss, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun AppContent(
    state: GameState.State,
    makePlay: (Play) -> Unit,
    makeBotPlay: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    val strings = LocalStrings.current.game
    when (state) {
        is GameState.State.Game -> {
            Content(
                state = state,
                makePlay = makePlay,
                onRestart = onRestart,
                onQuit = onQuit,
            )
            LaunchedEffect(state.turn) {
                // TODO: Prevent player attempting to make moves while it's the computer's turn
                if (state.turnPlayerRole() is Role.Computer) {
                    makeBotPlay()
                }
            }
        }

        is GameState.State.FatalError -> Column {
            Text(text = strings.failure)
            Spacer(Modifier.height(16.dp))
            Text(state.message)
            Spacer(Modifier.height(8.dp))
            Text(state.cause.message ?: "")
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
            ) {
                Text(text = strings.restart)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onQuit,
            ) {
                Text(text = strings.mainMenu)
            }
        }
    }
}

@Composable
private fun QuitButton(onQuit: () -> Unit) {
    Button(onClick = onQuit, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(text = LocalStrings.current.game.quit)
    }
}

@Composable
private fun RestartButton(onRestart: () -> Unit) {
    Button(onClick = onRestart, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(text = LocalStrings.current.game.restart)
    }
}

@Composable
private fun Title(
    turn: Player,
    winner: Winner,
    turnCount: UInt,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.game
    val turnsTaken = when (turn) {
        Player.Defender -> turnCount / 2u
        Player.Attacker -> (if (turnCount > 0u) turnCount - 1u else turnCount) / 2u
    }
    Text(
        text = when (winner) {
            Winner.None -> when (turn) {
                Player.Defender -> strings.defendersTurn(turnsTaken)
                Player.Attacker -> strings.attackersTurn(turnsTaken)
            }

            Winner.Defenders -> strings.defendersVictory(turnsTaken)
            Winner.Attackers -> strings.attackersVictory(turnsTaken)
        },
        modifier = modifier,
        color = HnefataflColors.night,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.sp
        ),
    )
}

@Suppress("NAME_SHADOWING")
@Composable
fun Content(
    state: GameState.State.Game,
    makePlay: (Play) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) {
    val boardState = rememberBoardState()
    val onRestart = boardState::deselect.then(onRestart)
    val onQuit = boardState::deselect.then(onQuit)
    val title: @Composable () -> Unit = {
        Crossfade(targetState = state.turn, animationSpec = tween(durationMillis = 500)) { turn ->
            Title(
                turn = turn,
                winner = state.winner,
                turnCount = state.turnCount,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 8.dp),
            )
        }
    }
    val mainContent: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Board(
                board = state.board,
                boardState = boardState,
                plays = state.plays,
                dead = state.dead,
                makePlay = makePlay,
                isLoading = state.turnPlayerRole().isLoading
            )
        }
    }
    val header: @Composable () -> Unit = {
        Box(Modifier.background(HnefataflColors.light))
    }
    Layout(
        content = {
            QuitButton(onQuit = onQuit)
            RestartButton(onRestart = onRestart)
            title()
            mainContent()
            header()
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        if (
            !constraints.hasBoundedHeight || !constraints.hasBoundedWidth ||
            constraints.minHeight > constraints.maxHeight || constraints.minWidth > constraints.maxWidth
        ) {
            throw IllegalArgumentException("Constraints unsupported: $constraints")
        }

        val quitButton = measurables[0]
        val restartButton = measurables[1]
        val titleMeasurable = measurables[2]
        val mainContentMeasurable = measurables[3]
        val headerMeasurable = measurables[4]

        val quitPlaceable = quitButton.measure(constraints.copy(minHeight = 0, minWidth = 0))
        val restartPlaceable = restartButton.measure(constraints.copy(minHeight = 0, minWidth = 0))

        val titleMargin = max(quitPlaceable.width, restartPlaceable.width)

        // Ideally each button goes in the top left and top right, but when horizontal space is
        // tight switch to both buttons on the left to give the main text more room
        val centerAlignTitle = quitPlaceable.width + restartPlaceable.width < constraints.maxWidth * 0.25

        val titleMargins = when (centerAlignTitle) {
            true -> 2 * titleMargin
            false -> titleMargin
        }
        val titlePlaceable = titleMeasurable.measure(
            constraints.copy(
                minHeight = 0,
                minWidth = constraints.maxWidth - titleMargins,
                maxWidth = constraints.maxWidth - titleMargins,
            )
        )
        val titleHeaderHeight = when (centerAlignTitle) {
            true -> max(titlePlaceable.height, max(quitPlaceable.height, restartPlaceable.height))
            false -> max(titlePlaceable.height, quitPlaceable.height + restartPlaceable.height)
        }

        val mainContentPlaceable = mainContentMeasurable.measure(
            Constraints(
                minWidth = constraints.minWidth,
                maxWidth = constraints.maxWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight - (titleHeaderHeight),
            )
        )

        val desiredVerticalPadding = constraints.maxHeight - mainContentPlaceable.height
        val usedHeight = mainContentPlaceable.height + titleHeaderHeight
        val emptyHeight = constraints.maxHeight - usedHeight
        val verticalPadding = min(max((desiredVerticalPadding / 2) - titleHeaderHeight, 0), emptyHeight)

        val headerHeight = verticalPadding + titleHeaderHeight
        val headerPlaceable = headerMeasurable.measure(
            Constraints(
                minWidth = constraints.maxWidth,
                maxWidth = constraints.maxWidth,
                minHeight = headerHeight,
                maxHeight = headerHeight,
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            // Header sits at the top behind everything else
            headerPlaceable.placeRelative(x = 0, y = 0)
            titlePlaceable.placeRelative(
                x = when (centerAlignTitle) {
                    // Title sits above board, centered between the two buttons
                    true -> (constraints.maxWidth - titlePlaceable.width) / 2
                    // Title header is left aligned, placed after the two buttons
                    false -> titleMargin
                },
                // On very portrait layouts, top align the title instead of sitting above
                // the board, to keep it aligned with the buttons
                y = when (constraints.maxHeight > constraints.maxWidth * 1.1) {
                    true -> 0
                    false -> verticalPadding
                }
            )
            // Main content varies based on available space for vertical alignment, but always starts just below
            // title
            mainContentPlaceable.placeRelative(
                x = 0,
                y = verticalPadding + titleHeaderHeight
            )
            when (centerAlignTitle) {
                true -> {
                    // Quit and restart buttons are pinned to the top corners
                    quitPlaceable.placeRelative(x = 0, y = 0)
                    restartPlaceable.placeRelative(x = constraints.maxWidth - restartPlaceable.width, y = 0)
                }
                false -> {
                    // Quit and restart buttons are left aligned in a column
                    quitPlaceable.placeRelative(x = 0, y = 0)
                    restartPlaceable.placeRelative(x = 0, y = quitPlaceable.height)
                }
            }
        }
    }
}

@Composable
private fun SnackbarHost(
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.component
    if (timber.isNotEmpty()) {
        val tree = timber.first()
        Snackbar(
            modifier = modifier.padding(16.dp),
            dismissAction = {
                TextButton(
                    onClick = { onDismiss(tree.id) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Text(text = strings.ok)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Error messages aren't localised at the moment so add a flag prefix for non-English locales to make
                // it a little more clear that the content isn't localised.
                val prefix = when (LocalStrings.current.type) {
                    Strings.Type.British, Strings.Type.American -> ""
                    Strings.Type.CastilianSpanish, Strings.Type.LatinAmericanSpanish -> "🇬🇧 "
                }
                val level = when (tree.level) {
                    LogLevel.Debug -> "D: "
                    LogLevel.Warn -> "W: "
                    LogLevel.Error -> "E: "
                }
                SelectionContainer {
                    Text(text = "$prefix$level${tree.message}", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
@Preview
private fun ContentPreview() = PreviewSurface {
    Content(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.None,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
            attackers = Role.Human(),
            defenders = Role.Human(),
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun FatalErrorPreview() = PreviewSurface {
    AppContent(
        state = GameState.State.FatalError(
            message = "Contextual message here",
            cause = Throwable("Problem here"),
            attackers = RoleType.Human,
            defenders = RoleType.Human,
        ),
        makePlay = {},
        makeBotPlay = {},
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun FatalErrorSpanishPreview() = PreviewSurface {
    CompositionLocalProvider(LocalStrings provides locales["es-ES"]!!) {
        AppContent(
            state = GameState.State.FatalError(
                message = "Contextual message here",
                cause = Throwable("Problem here"),
                attackers = RoleType.Human,
                defenders = RoleType.Human,
            ),
            makePlay = {},
            makeBotPlay = {},
            onRestart = {},
            onQuit = {},
        )
    }
}

@Composable
@Preview
private fun GameOverPreview() = PreviewSurface {
    Content(
        state = GameState.State.Game(
            board = emptyBoard,
            plays = listOf(),
            winner = Winner.Attackers,
            turn = Player.Defender,
            dead = listOf(),
            turnCount = 0u,
            attackers = Role.Human(),
            defenders = Role.Human(),
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}
