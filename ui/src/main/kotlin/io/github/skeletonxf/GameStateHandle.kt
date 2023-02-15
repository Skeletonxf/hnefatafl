package io.github.skeletonxf

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.board.BoardData
import io.github.skeletonxf.board.Tile
import java.lang.foreign.MemoryAddress
import java.lang.ref.Cleaner

class GameStateHandle: GameState {
    private val handle: MemoryAddress = bindings_h.game_state_handle_new()

    override val state: State<GameState.State> = mutableStateOf(getGameState())

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

    private fun getGameState(): GameState.State {
        val tiles = FFIResult.from(
            handle = bindings_h.game_state_handle_tiles(handle),
            is_ok = { bindings_h.result_tile_array_is_ok(it) },
            get_ok = { bindings_h.result_tile_array_get_ok(it) },
            get_err = { bindings_h.result_tile_array_get_error(it) },
        ).okOrNull() ?: return GameState.State.FatalError(
            IllegalStateException("Unable to query board tiles")
        )
        val length = bindings_h.tile_array_length(tiles).toInt()
        val copied = List(length) { i -> Tile.valueOf(bindings_h.tile_array_get(tiles, i.toLong())) }
        bindings_h.tile_array_destroy(tiles)
        val side = bindings_h.game_state_handle_grid_size(handle).toInt()
        return GameState.State.Game(
            board = BoardData(copied, side)
        )
    }

    companion object {
        private val bridgeCleaner: Cleaner = Cleaner.create()
    }
}

private data class BridgeHandleCleaner(private val handle: MemoryAddress): Runnable {
    override fun run() {
        // Because this class is private, and we only ever call it from the cleaner, and we never
        // give out any references to our `handle: MemoryAddress` to any other classes, this
        // runs exactly once after all references to GameStateHandle are dead and the cleaner
        // runs us. Hence, we can meet the requirement that the handle is not aliased, so the
        // Rust side can use it as an exclusive reference and reclaim the memory safely.
        bindings_h.game_state_handle_destroy(handle)
    }
}
