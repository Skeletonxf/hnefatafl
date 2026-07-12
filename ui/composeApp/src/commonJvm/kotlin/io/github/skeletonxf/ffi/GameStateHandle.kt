package io.github.skeletonxf.ffi

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.skeletonxf.ui.game.GameState
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.game.Role
import io.github.skeletonxf.ui.game.RoleType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun uniffi.hnefatafl.FlatPlay.Companion.from(play: Play) = uniffi.hnefatafl.FlatPlay(
    fromX = play.from.x.toUByte(),
    fromY = play.from.y.toUByte(),
    toX = play.to.x.toUByte(),
    toY = play.to.y.toUByte(),
)

class GameStateHandle private constructor(
    private val coroutineScope: CoroutineScope,
    val configuration: Configuration,
    private val handle: uniffi.hnefatafl.GameStateHandle
) : GameState {

    private var ongoingPlay: Job? = null

    constructor(coroutineScope: CoroutineScope, configuration: Configuration) : this(
        coroutineScope,
        configuration,
        uniffi.hnefatafl.GameStateHandle()
    )

    override val state: MutableState<GameState.State> = mutableStateOf(
        getGameState(UIState.from(configuration, Role.Computer.Strategy.MinMax))
    )

    companion object {
        fun fromStartingConfiguration(
            coroutineScope: CoroutineScope,
            configuration: Configuration,
            strategy: Role.Computer.Strategy,
            tiles: List<Tile>,
            turn: Player,
            dead: List<Piece>,
        ): GameStateHandle {
            if (tiles.size != (11 * 11)) {
                throw IllegalArgumentException(
                    "Invalid number of tiles for starting state, expected ${11 * 11}, was ${tiles.size}"
                )
            }
            val handle = uniffi.hnefatafl.GameStateHandle.withStartingConfiguration(
                tiles.map { it.toTile() },
                turn.toTurnPlayer(),
                dead.map { it.toDead() }
            )
            return GameStateHandle(coroutineScope, configuration, handle).apply {
                state.value = getGameState(UIState.from(configuration, strategy))
            }
        }
    }

    private fun Configuration.fatalError(
        message: String,
        cause: Throwable,
    ) = GameState.State.FatalError(
        message = message,
        cause = cause,
        attackers = attackers,
        defenders = defenders,
    )

    private data class UIState(
        val attackers: Role,
        val defenders: Role,
        val previousPlay: Play?,
    ) {
        companion object {
            fun from(
                configuration: Configuration,
                strategy: Role.Computer.Strategy,
            ) = UIState(
                attackers = initialRoleState(configuration.attackers, strategy),
                defenders = initialRoleState(configuration.defenders, strategy),
                previousPlay = null,
            )

            private fun initialRoleState(
                type: RoleType,
                strategy: Role.Computer.Strategy,
            ): Role = when (type) {
                RoleType.Human -> Role.Human()
                RoleType.Computer -> Role.Computer(
                    isLoading = false,
                    strategy = strategy,
                )
            }
        }
    }

    private fun GameState.State.Game.uiState(): UIState = UIState(
        attackers,
        defenders,
        previousPlay,
    )

    override fun debug() = Log.debug(handle.debug())

    override fun makePlay(play: Play) = attemptPlay {
        val ui = when (val s = state.value) {
            is GameState.State.Game -> when (s.turnPlayerRole()) {
                is Role.Human -> s.uiState()
                is Role.Computer -> {
                    Log.error("Cannot make human play on a computer's turn")
                    return@attemptPlay
                }
            }
            is GameState.State.FatalError -> {
                Log.error("Cannot make play in a fatal error state")
                return@attemptPlay
            }
        }
        state.value = KResult.runCatching {
            handle.makePlay(uniffi.hnefatafl.FlatPlay.from(play))
        }.fold(
            ok = { getGameState(ui.copy(previousPlay = play)) },
            error = { error ->
                with(configuration) {
                    // FIXME: Errors seem to be getting misread by uniffi glue and not thrown
                    // as correct type due to a bug somewhere along the chain. Can reproduce by
                    // changing FlatPlay.from to use `from` in place of to which makes it easy to
                    // select an invalid move from the UI.
                    fatalError("Failed to make play", error)
                }
            }
        )
    }

    override fun makeBotPlay() = attemptPlay {
        val strategy: Role.Computer.Strategy
        val ui = when (val s = state.value) {
            is GameState.State.Game -> when (val r = s.turnPlayerRole()) {
                is Role.Computer -> {
                    val loading = when (s.turn) {
                        Player.Attacker -> s.copy(
                            attackers = s.attackers.enterLoading(),
                            defenders = s.defenders.exitLoading()
                        )

                        Player.Defender -> s.copy(
                            defenders = s.defenders.enterLoading(),
                            attackers = s.attackers.exitLoading()
                        )
                    }
                    strategy = r.strategy
                    state.value = loading
                    // UI state we'll enter after a play will be not loading again
                    loading.uiState().copy(
                        attackers = s.attackers.exitLoading(),
                        defenders = s.defenders.exitLoading(),
                    )
                }

                is Role.Human -> {
                    //Log.error("Cannot make bot play on a human's turn")
                    return@attemptPlay
                }
            }

            is GameState.State.FatalError -> {
                Log.error("Cannot make play in a fatal error state")
                ongoingPlay = null
                return@attemptPlay
            }
        }
        when (strategy) {
            Role.Computer.Strategy.MinMax -> {
                state.value = KResult.runCatching {
                    handle.makeBotPlay()
                }.fold(
                    ok = { botPlay ->
                        getGameState(ui.copy(previousPlay = Play.from(botPlay.play)))
                    },
                    error = { error ->
                        with (configuration) {
                            fatalError("Failed to make bot play", error)
                        }
                    }
                )
            }
            Role.Computer.Strategy.Random -> {
                val randomPlay = getAvailablePlays().randomOrNull()
                if (randomPlay != null) {
                    handle.makePlay(uniffi.hnefatafl.FlatPlay.from(randomPlay))
                    state.value = getGameState(ui.copy(previousPlay = randomPlay))
                } else {
                    state.value = with (configuration) {
                        fatalError(
                            "Failed to make bot play",
                            IllegalStateException("No plays available")
                        )
                    }
                }
            }
        }
    }

    /**
     * Critical section for the Job that will update the board state.
     * In some rare cases such as configuration changes on Android we
     * might expect multiple calls to the GameStateHandle to perform a
     * play, and we need to ignore them if one is in progress.
     */
    private fun attemptPlay(
        block: suspend CoroutineScope.() -> Unit
    ) = synchronized(this) {
        if (ongoingPlay == null) {
            try {
                ongoingPlay = coroutineScope.launch {
                    block()
                    ongoingPlay = null
                }
            } catch (error: CancellationException) {
                ongoingPlay = null
                throw error
            }
        } else {
            //Log.debug("Ignored repeat request to make bot play")
        }
    }

    private fun getGameState(ui: UIState): GameState.State = with (configuration) {
        GameState.State.Game(
            board = getBoard(),
            plays = getAvailablePlays(),
            winner = getWinner(),
            turn = getTurnPlayer(),
            dead = getDead(),
            turnCount = getTurnCount(),
            attackers = ui.attackers,
            defenders = ui.defenders,
            previousPlay = ui.previousPlay,
        )
    }

    private fun getBoard(): BoardData = BoardData(
        tiles = handle.tiles().map(Tile.Companion::from),
        length = handle.gridSize().toInt()
    )

    private fun getAvailablePlays(): List<Play> = handle
        .availablePlays()
        .map(Play.Companion::from)

    private fun getWinner(): Winner = handle.winner().let(Winner.Companion::from)

    private fun getTurnPlayer(): Player = handle.currentPlayer().let(Player.Companion::from)

    private fun getDead(): List<Piece> = handle.dead().map(Tile.Companion::from)

    private fun getTurnCount(): UInt = handle.turnCount()
}
