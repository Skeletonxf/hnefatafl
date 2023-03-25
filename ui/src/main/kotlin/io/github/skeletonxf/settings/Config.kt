package io.github.skeletonxf.settings

import io.github.skeletonxf.ffi.FFIThrowable
import androidx.compose.runtime.State as ComposeState

interface Config {
    fun debug()

    val state: ComposeState<State>

    sealed class State {
        data class Config(
            val locale: String,
        ) : State()

        data class FatalError(
            val message: String,
            val cause: FFIThrowable,
        ) : State()
    }
}