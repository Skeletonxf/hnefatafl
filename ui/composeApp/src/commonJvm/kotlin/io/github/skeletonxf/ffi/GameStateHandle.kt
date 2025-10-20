package io.github.skeletonxf.ffi

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.skeletonxf.ui.GameState
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.Configuration
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.functions.launchUnit
import io.github.skeletonxf.logging.Log
import io.github.skeletonxf.ui.Role
import io.github.skeletonxf.ui.RoleType
import kotlinx.coroutines.CoroutineScope

private fun uniffi.hnefatafl.FlatPlay.Companion.from(play: Play) = uniffi.hnefatafl.FlatPlay(
    fromX = play.from.x.toUByte(),
    fromY = play.from.y.toUByte(),
    toX = play.to.x.toUByte(),
    toY = play.to.y.toUByte(),
)

class GameStateHandle(
    private val coroutineScope: CoroutineScope,
    private val configuration: Configuration,
) : GameState {
    private val handle = uniffi.hnefatafl.GameStateHandle()

    override val state: MutableState<GameState.State> = mutableStateOf(
        getGameState(UIState.from(configuration))
    )

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
    ) {
        companion object {
            fun from(configuration: Configuration) = UIState(
                attackers = initialRoleState(configuration.attackers),
                defenders = initialRoleState(configuration.defenders),
            )

            private fun initialRoleState(type: RoleType): Role = when (type) {
                RoleType.Human -> Role.Human()
                RoleType.Computer -> Role.Computer(isLoading = false)
            }
        }
    }

    private fun GameState.State.Game.uiState(): UIState = UIState(attackers, defenders)

    override fun debug() = Log.debug(handle.debug())

    override fun makePlay(play: Play) = coroutineScope.launchUnit {
        val ui = when (val s = state.value) {
            is GameState.State.Game -> when (s.turnPlayerRole()) {
                is Role.Human -> s.uiState()
                is Role.Computer -> {
                    Log.error("Cannot make human play on a computer's turn")
                    return@launchUnit
                }
            }
            is GameState.State.FatalError -> {
                Log.error("Cannot make play in a fatal error state")
                return@launchUnit
            }
        }
        state.value = KResult.runCatching {
            handle.makePlay(uniffi.hnefatafl.FlatPlay.from(play))
        }.fold(
            ok = { getGameState(ui) },
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

    override fun makeBotPlay() = coroutineScope.launchUnit {
        val ui = when (val s = state.value) {
            is GameState.State.Game -> when (s.turnPlayerRole()) {
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
                    state.value = loading
                    // UI state we'll enter after a play will be not loading again
                    loading.uiState().copy(
                        attackers = s.attackers.exitLoading(),
                        defenders = s.defenders.exitLoading(),
                    )
                }

                is Role.Human -> {
                    Log.error("Cannot make bot play on a human's turn")
                    return@launchUnit
                }
            }

            is GameState.State.FatalError -> {
                Log.error("Cannot make play in a fatal error state")
                return@launchUnit
            }
        }
        state.value = KResult.runCatching {
            handle.makeBotPlay()
        }.fold(
            ok = { getGameState(ui) },
            error = { error ->
                with (configuration) {
                    fatalError("Failed to make bot play", error)
                }
            }
        )
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
