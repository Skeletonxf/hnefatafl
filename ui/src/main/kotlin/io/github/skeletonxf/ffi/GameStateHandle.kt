package io.github.skeletonxf.ffi

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.skeletonxf.bindings.FlatPlay
import io.github.skeletonxf.ui.GameState
import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.BoardData
import io.github.skeletonxf.data.GameStateUpdate
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Piece
import io.github.skeletonxf.data.Play
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.data.Position
import io.github.skeletonxf.data.Tile
import io.github.skeletonxf.data.Winner
import io.github.skeletonxf.functions.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.foreign.MemoryAddress
import java.lang.foreign.MemorySession
import java.lang.foreign.SegmentAllocator
import java.lang.ref.Cleaner

class GameStateHandle(
    private val coroutineScope: CoroutineScope,
    private val opponent: GameState.State.Game.Opponent
) : GameState {
    private val handle: MemoryAddress = bindings_h.game_state_handle_new()

    override val state: MutableState<GameState.State> = mutableStateOf(getGameState())

    init {
        // We must not use any inner classes or lambdas for the runnable object, to avoid capturing our
        // GameStateHandle instance, which would prevent the cleaner ever running.
        // We could hold onto the cleanable this method returns so that we can manually trigger it
        // with a `close()` method or such, but such an API can't stop us calling that method
        // while still holding references to the GameStateHandle, in which case we'd trigger
        // undefined behavior and likely reclaim the memory on the Rust side while we still
        // have other aliases to it that think it's still in use. Instead, the *only* way
        // to tell Rust it's time to call the destructor is when the cleaner determines there are
        // no more references to our GameStateHandle.
        bridgeCleaner.register(this, BridgeHandleCleaner(handle))
    }

    override fun debug() {
        bindings_h.game_state_handle_debug(handle)
    }

    override fun makePlay(play: Play) = coroutineScope.launchUnit {
        state.value = doPlay(
            KResult
                .from(
                    handle = bindings_h.game_state_handle_make_play(
                        handle,
                        play.from.x.toByte(),
                        play.from.y.toByte(),
                        play.to.x.toByte(),
                        play.to.y.toByte()
                    ),
                    getType = bindings_h::result_game_state_update_get_type,
                    getOk = bindings_h::result_game_state_update_get_ok,
                    getError = bindings_h::result_game_state_update_get_error,
                )
        )
    }

    override fun makeBotPlay() = coroutineScope.launchUnit {
        state.value = doPlay(
            KResult
                .from(
                    handle = bindings_h.game_state_handle_make_bot_play(handle),
                    getType = bindings_h::result_game_state_update_get_type,
                    getOk = bindings_h::result_game_state_update_get_ok,
                    getError = bindings_h::result_game_state_update_get_error,
                )
        )
    }

    private fun doPlay(result: KResult<Byte, FFIError<Unit?>>) = result
        .map(GameStateUpdate::valueOf)
        .fold(
            ok = { gameStateUpdate ->
                when (gameStateUpdate) {
                    // Might want to do something in particular when entering a win or capture state here?
                    GameStateUpdate.DefenderWin,
                    GameStateUpdate.AttackerWin,
                    GameStateUpdate.DefenderCapture,
                    GameStateUpdate.AttackerCapture,
                    GameStateUpdate.Nothing -> getGameState()
                }
            },
            error = { error ->
                GameState.State.FatalError(
                    "Failed to make play", error.toThrowable(), opponent
                )
            }
        )

    private fun getGameState(): GameState.State {
        val board = when (val result = getBoard()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query board tiles", result.err.toThrowable(), opponent
            )
        }
        val plays = when (val result = getAvailablePlays()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query available plays", result.err.toThrowable(), opponent
            )
        }
        val winner = when (val result = getWinner()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query winner", result.err.toThrowable(), opponent
            )
        }
        val turn = when (val result = getTurnPlayer()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query turn player", result.err.toThrowable(), opponent
            )
        }
        val dead = when (val result = getDead()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query dead pieces", result.err.toThrowable(), opponent
            )
        }
        val turnCount = when (val result = getTurnCount()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return GameState.State.FatalError(
                "Unable to query turn count", result.err.toThrowable(), opponent
            )
        }
        return GameState.State.Game(
            board = board,
            plays = plays,
            winner = winner,
            turn = turn,
            dead = dead,
            turnCount = turnCount,
            opponent = opponent,
        )
    }

    private fun getBoard(): KResult<BoardData, FFIError<String>> = TileArrayResult(
        bindings_h.game_state_handle_tiles(handle)
    )
        .toResult()
        .map { tileArrayAddress ->
            tileArrayToBoard(
                tileArrayAddress,
                bindings_h.game_state_handle_grid_size(handle).toInt()
            )
        }

    private fun getAvailablePlays(): KResult<List<Play>, FFIError<Unit?>> = KResult.from(
        handle = bindings_h.game_state_available_plays(handle),
        getType = { bindings_h.result_play_array_get_type(it) },
        getOk = { bindings_h.result_play_array_get_ok(it) },
        getError = { bindings_h.result_play_array_get_error(it) }
    ).map(destroy = bindings_h::play_array_destroy) { plays ->
        val length = bindings_h.play_array_length(plays).toInt()
        if (length == 0) {
            return@map listOf()
        }
        val bytesPerPlay = FlatPlay.sizeof()
        MemorySession.openConfined().use { memorySession ->
            val allocator = SegmentAllocator.newNativeArena((bytesPerPlay * length), memorySession)
            List(length) { i ->
                val memorySegment = bindings_h.play_array_get(allocator, plays, i.toLong())
                Play(
                    from = Position(
                        x = FlatPlay.`from_x$get`(memorySegment).toInt(),
                        y = FlatPlay.`from_y$get`(memorySegment).toInt()
                    ),
                    to = Position(
                        x = FlatPlay.`to_x$get`(memorySegment).toInt(),
                        y = FlatPlay.`to_y$get`(memorySegment).toInt()
                    )
                )
            }
        }
    }

    private fun getWinner(): KResult<Winner, FFIError<String>> = WinnerResult(
        bindings_h.game_state_handle_winner(handle)
    ).toResult()

    private fun getTurnPlayer(): KResult<Player, FFIError<String>> = PlayerResult(
        bindings_h.game_state_current_player(handle)
    ).toResult()

    private fun getDead(): KResult<List<Piece>, FFIError<String>> = TileArrayResult(
        bindings_h.game_state_handle_dead(handle)
    )
        .toResult()
        .map { tileArrayAddress ->
            tileArrayAddress.address.use(bindings_h::tile_array_destroy) { pieces ->
                val length = bindings_h.tile_array_length(pieces).toInt()
                val dead = List(length) { i ->
                    Piece.valueOf(
                        Tile.valueOf(
                            bindings_h.tile_array_get(pieces, i.toLong())
                        )
                    )
                }
                dead.filterNotNull()
            }
        }

    private fun getTurnCount(): KResult<UInt, FFIError<String>> = UIntResult(
        bindings_h.game_state_handle_turn_count(handle)
    ).toResult()

    companion object {
        private val bridgeCleaner: Cleaner = Cleaner.create()
    }
}

private data class BridgeHandleCleaner(private val handle: MemoryAddress) : Runnable {
    override fun run() {
        // Because this class is private, and we only ever call it from the cleaner, and we never
        // give out any references to our `handle: MemoryAddress` to any other classes, this
        // runs exactly once after all references to GameStateHandle are dead and the cleaner
        // runs us. Hence, we can meet the requirement that the handle is not aliased, so the
        // Rust side can use it as an exclusive reference and reclaim the memory safely.
        bindings_h.game_state_handle_destroy(handle)
    }
}
