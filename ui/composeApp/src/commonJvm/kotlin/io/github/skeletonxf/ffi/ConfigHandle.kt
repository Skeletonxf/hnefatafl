package io.github.skeletonxf.ffi

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.settings.Config
import java.lang.foreign.MemorySegment
import java.lang.ref.Cleaner

class ConfigHandle private constructor(private val handle: ConfigHandleAddress) : Config {

    companion object {
        fun new(config: String): KResult<ConfigHandle, FFIError<String>> = ConfigHandleResult(
            withStringToUTF16Array(config) { memorySegment ->
                bindings_h.config_handle_new(memorySegment, config.length.toLong())
            }
        )
            .toResult()
            .map(::ConfigHandle)

        fun default(): ConfigHandle = ConfigHandle(ConfigHandleAddress(bindings_h.config_handle_default()))

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
        bridgeCleaner.register(this, ConfigHandleCleaner(handle.address))
    }

    override fun get(key: Config.StringKey): KResult<String, FFIError<String>> = UTF16ArrayHandleResult(
        bindings_h.config_handle_get_string_key(handle.address, key.value())
    ).toResult()

    override fun set(
        key: Config.StringKey,
        value: String
    ): KResult<Unit, FFIError<String>> = withStringToUTF16Array(value) { memorySegment ->
        VoidResult(bindings_h.config_handle_set_string_key(
            handle.address,
            bindings_h.Locale().toByte(),
            memorySegment,
            value.length.toLong())
        ).toResult()
    }

    override fun getAll(): KResult<String, FFIError<String>> = UTF16ArrayHandleResult(
        bindings_h.config_handle_get_file(handle.address)
    ).toResult()
}

private data class ConfigHandleCleaner(private val handle: MemorySegment) : Runnable {
    override fun run() {
        // Because this class is private, and we only ever call it from the cleaner, and we never
        // give out any references to our `handle: MemorySegment` to any other classes, this
        // runs exactly once after all references to ConfigHandle are dead and the cleaner
        // runs us. Hence, we can meet the requirement that the handle is not aliased, so the
        // Rust side can use it as an exclusive reference and reclaim the memory safely.
        bindings_h.config_handle_destroy(handle)
    }
}
