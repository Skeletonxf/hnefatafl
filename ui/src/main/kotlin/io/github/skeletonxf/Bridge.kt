package io.github.skeletonxf

import io.github.skeletonxf.bindings.bindings_h
import java.io.Closeable
import java.lang.foreign.MemoryAddress

class Bridge {
    fun helloWorld() {
        println("Hello from Kotlin")
        GameStateHandle().use { it.debug() }
    }

    class GameStateHandle private constructor(private val handle: MemoryAddress): Closeable, GameState {
        constructor() : this(bindings_h.game_state_handle_new())

        override fun debug() {
            bindings_h.game_state_handle_debug(handle)
        }

        override fun close() {
            bindings_h.game_state_handle_destroy(handle)
        }
    }
}