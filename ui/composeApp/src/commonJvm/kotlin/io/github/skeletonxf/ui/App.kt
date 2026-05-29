package io.github.skeletonxf.ui

import TooltipIconButton
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.skeletonxf.HeaderAlignment
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.ffi.GameStateHandle
import io.github.skeletonxf.functions.then
import io.github.skeletonxf.getPlatform
import io.github.skeletonxf.logging.ForestLogger
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.logging.LogLevel
import io.github.skeletonxf.logging.PrintLogger
import io.github.skeletonxf.logging.Tree
import io.github.skeletonxf.logging.TreeIdentifier
import io.github.skeletonxf.settings.FilePaths
import io.github.skeletonxf.settings.Setting
import io.github.skeletonxf.settings.Settings
import io.github.skeletonxf.ui.nav.NavigationRoot
import io.github.skeletonxf.ui.strings.LocalStrings
import io.github.skeletonxf.ui.strings.ProvideStrings
import io.github.skeletonxf.ui.strings.Strings
import io.github.skeletonxf.ui.strings.locales
import io.github.skeletonxf.ui.theme.HnefataflColors
import io.github.skeletonxf.ui.theme.HnefataflMaterialTheme
import io.github.skeletonxf.ui.theme.PreviewSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.painterResource
import java.nio.file.Path
import java.nio.file.Paths

val localBackgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

data class Environment(
    val forestLogger: ForestLogger,
    val filePaths: FilePaths,
    val settings: Settings,
) {
    private val handle = mutableStateOf<GameStateHandle?>(null)

    val gameStateHandle: GameStateHandle?
        get() = handle.value

    fun startNewGame(config: Configuration) {
        handle.value = GameStateHandle(localBackgroundScope, config)
    }

    fun restartGame(): Boolean {
        val config = gameStateHandle?.configuration ?: return false
        startNewGame(config)
        return true
    }

    fun stopGame() {
        handle.value = null
    }

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
    Root(
        environment = environment,
        timber = forest.timber.collectAsState().value,
        onDismiss = forest::dismiss,
    )
}

@Composable
fun Root(
    environment: Environment,
    timber: List<Tree>,
    onDismiss: (TreeIdentifier) -> Unit,
) = HnefataflMaterialTheme {
    ProvideStrings(settings = environment.settings) {
        NavigationRoot(environment) {
            SnackbarHost(timber, onDismiss)
        }
    }
}

@Composable
fun AppContentScreen(
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
                if (state.turnPlayerRole() is Role.Computer) {
                    makeBotPlay()
                }
            }
            // Disable back presses while playing to prevent accidentally
            // quitting the game. TODO: Turn into confirmation dialog?
            // FIXME: This seems to have no effect on Esc in desktop?
            val navigationState = rememberNavigationEventState(NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                isBackEnabled = state.winner != Winner.None,
                onBackCancelled = {},
                onBackCompleted = {},
            )
        }

        is GameState.State.FatalError -> Column(
            modifier = Modifier.safeDrawingPadding()
        ) {
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
private fun Title(
    turn: Player,
    winner: Winner,
    turnCount: UInt,
    textAlign: TextAlign,
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
        textAlign = textAlign,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.sp
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    state: GameState.State.Game,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.game
    Surface(
        color = HnefataflColors.light,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipIconButton(
                onClick = onQuit,
                painter = painterResource(Res.drawable.close),
                text = strings.quit,
            )
            val headerAlignment = getPlatform().headerAlignment
            Box(modifier = Modifier.weight(1F)) {
                Crossfade(
                    targetState = state.turn,
                    animationSpec = tween(durationMillis = 500)
                ) { turn ->
                    Title(
                        turn = turn,
                        winner = state.winner,
                        turnCount = state.turnCount,
                        textAlign = when (headerAlignment) {
                            HeaderAlignment.Left -> TextAlign.Left
                            HeaderAlignment.Center -> TextAlign.Center
                        },
                        modifier = Modifier.align(
                            when (headerAlignment) {
                                HeaderAlignment.Left -> Alignment.CenterStart
                                HeaderAlignment.Center -> Alignment.Center
                            }
                        )
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
            TooltipIconButton(
                onClick = onRestart,
                painter = painterResource(Res.drawable.restart),
                text = strings.restart,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val safeDrawing = WindowInsets.safeDrawing

    Column {
        Box(
            modifier = Modifier
                .background(HnefataflColors.light)
                .then(
                    safeDrawing.let { insets ->
                        val padding = insets.asPaddingValues()
                        val layoutDirection = LocalLayoutDirection.current
                        val excludingBottom = PaddingValues(
                            top = padding.calculateTopPadding(),
                            start = padding.calculateStartPadding(layoutDirection),
                            end = padding.calculateEndPadding(layoutDirection),
                        )
                        Modifier
                            .padding(excludingBottom)
                            .consumeWindowInsets(excludingBottom)
                    }
                ),
        ) {
            Header(
                state = state,
                onRestart = onRestart,
                onQuit = onQuit,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(safeDrawing.let {
                        insets ->
                    val padding = insets.asPaddingValues()
                    val layoutDirection = LocalLayoutDirection.current
                    val excludingBottom = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    )
                    Modifier
                        .padding(excludingBottom)
                        .consumeWindowInsets(excludingBottom)
                }),
            contentAlignment = Alignment.Center,
        ) {
            Board(
                board = state.board,
                boardState = boardState,
                plays = state.plays,
                dead = state.dead,
                makePlay = makePlay,
                previousPlay = state.previousPlay,
                isLoading = state.turnPlayerRole().isLoading
            )
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
            previousPlay = null,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}

@Composable
@Preview
private fun FatalErrorPreview() = PreviewSurface {
    AppContentScreen(
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
        AppContentScreen(
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
            previousPlay = null,
        ),
        makePlay = {},
        onRestart = {},
        onQuit = {},
    )
}
