package io.github.skeletonxf.ffi

import androidx.compose.runtime.mutableStateOf
import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.settings.Config
import java.lang.foreign.MemoryAddress
import java.lang.ref.Cleaner

class ConfigHandle private constructor(private val handle: MemoryAddress) : Config {

    companion object {
        fun new(config: String): KResult<ConfigHandle, FFIError<Unit?>> {
            if (config.isEmpty()) {
                return KResult.Error(FFIError(message = "Empty string input", Unit));
            }
            return KResult
                .from(
                    handle = withStringToUTF16Array(config) { memorySegment ->
                        bindings_h.config_handle_new(memorySegment, config.length.toLong())
                    },
                    getType = bindings_h::result_config_handle_get_type,
                    getOk = bindings_h::result_config_handle_get_ok,
                    getError = bindings_h::result_config_handle_get_error,
                )
                .map(::ConfigHandle)
        }

        private val bridgeCleaner: Cleaner = Cleaner.create()
    }

    init {
        // We must not use any inner classes or lambdas for the runnable object, to avoid capturing our
        // ConfigHandle instance, which would prevent the cleaner ever running.
        // We could hold onto the cleanable this method returns so that we can manually trigger it
        // with a `close()` method or such, but such an API can't stop us calling that method
        // while still holding references to the ConfigHandle, in which case we'd trigger
        // undefined behavior and likely reclaim the memory on the Rust side while we still
        // have other aliases to it that think it's still in use. Instead, the *only* way
        // to tell Rust it's time to call the destructor is when the cleaner determines there are
        // no more references to our ConfigHandle.
        bridgeCleaner.register(this, ConfigHandleCleaner(handle))
    }

    override val state = mutableStateOf(getConfigState())

    override fun debug() {
        bindings_h.config_handle_debug(handle)
    }

    private fun getConfigState(): Config.State {
        val locale = when (val result = getConfigLocale()) {
            is KResult.Ok -> result.ok
            is KResult.Error -> return Config.State.FatalError(
                "Unable to query locale", result.err.toThrowable()
            )
        }
        return Config.State.Config(
            locale = locale,
        )
    }

    private fun getConfigLocale(): KResult<String, FFIError<Unit?>> = KResult.from(
        handle = bindings_h.config_handle_locale(handle),
        getType = bindings_h::result_config_handle_get_type,
        getOk = bindings_h::result_config_handle_get_ok,
        getError = bindings_h::result_config_handle_get_error,
    ).map(::utf16ArrayToString)
}

private data class ConfigHandleCleaner(private val handle: MemoryAddress) : Runnable {
    override fun run() {
        // Because this class is private, and we only ever call it from the cleaner, and we never
        // give out any references to our `handle: MemoryAddress` to any other classes, this
        // runs exactly once after all references to ConfigHandle are dead and the cleaner
        // runs us. Hence, we can meet the requirement that the handle is not aliased, so the
        // Rust side can use it as an exclusive reference and reclaim the memory safely.
        bindings_h.config_handle_destroy(handle)
    }
}
