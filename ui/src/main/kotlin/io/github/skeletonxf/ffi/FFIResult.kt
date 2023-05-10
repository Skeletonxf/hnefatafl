package io.github.skeletonxf.ffi

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import java.lang.foreign.MemoryAddress

private val FFI_RESULT_TYPE_OK: Byte = bindings_h.Ok().toByte()
private val FFI_RESULT_TYPE_ERROR: Byte = bindings_h.Err().toByte()
private val FFI_RESULT_TYPE_NULL: Byte = bindings_h.Null().toByte()

fun <T, E> KResult.Companion.from(
    handle: MemoryAddress,
    getType: (MemoryAddress) -> Byte,
    getOk: (MemoryAddress) -> T,
    getError: (MemoryAddress) -> E,
): KResult<T, FFIError<E?>> = when (getType(handle)) {
    FFI_RESULT_TYPE_OK -> KResult.Ok(getOk(handle))
    FFI_RESULT_TYPE_ERROR -> KResult.Error(FFIError("Data was error", getError(handle)))
    FFI_RESULT_TYPE_NULL -> KResult.Error(FFIError("Data was null or corrupted", null))
    else -> KResult.Error(FFIError("Unrecognised FFI result type", null))
}

/**
 * A Kotlin memory address for a Rust type of the form `*mut FFIResult<T, *mut FFIError>` where the Ok type in Rust
 * will be mapped to a Kotlin type after calling `toResult`. The error description of the FFIError type will be
 * converted to a String.
 */
interface ResultError<T> : TypedMemoryAddress {
    /**
     * Converts from the memory address to Kotlin types. Note that in 99% of cases this consumes the Rust memory
     * that the `address` was referring to, so after calling `toResult` this instance of ResultError should no longer
     * be held in memory either.
     */
    fun toResult(): KResult<T, FFIError<String>>
}

private fun <T> KResult.Companion.from2(
    handle: MemoryAddress,
    getType: (MemoryAddress) -> Byte,
    getOk: (MemoryAddress) -> T,
    getError: (MemoryAddress) -> String,
): KResult<T, FFIError<String>> = when (getType(handle)) {
    FFI_RESULT_TYPE_OK -> KResult.Ok(getOk(handle))
    FFI_RESULT_TYPE_ERROR -> KResult.Error(FFIError("Data was error", getError(handle)))
    FFI_RESULT_TYPE_NULL -> KResult.Error(FFIError("Data was null or corrupted", ""))
    else -> KResult.Error(FFIError("Unrecognised FFI result type", ""))
}

private fun <T: TypedMemoryAddress> (() -> MemoryAddress).andWrap(
    typeAs: (MemoryAddress) -> T
): () -> T = { typeAs(this()) }

private fun <T: TypedMemoryAddress> ((MemoryAddress) -> MemoryAddress).andWrap(
    typeAs: (MemoryAddress) -> T
): (MemoryAddress) -> T = { typeAs(this(it)) }

private fun <T: TypedMemoryAddress, R> (() -> T).andThen(
    consumer: (T) -> R
): () -> R = { consumer(this()) }

private fun <T: TypedMemoryAddress, R> ((MemoryAddress) -> T).andThen(
    consumer: (T) -> R
): (MemoryAddress) -> R = { consumer(this(it)) }

@JvmInline
value class ConfigHandleAddress(override val address: MemoryAddress): TypedMemoryAddress

@JvmInline
value class ConfigHandleResult(
    override val address: MemoryAddress
): ResultError<ConfigHandleAddress> {
    override fun toResult(): KResult<ConfigHandleAddress, FFIError<String>> = KResult.from2(
        handle = address,
        getType = bindings_h::result_config_handle_get_type,
        getOk = bindings_h::result_config_handle_get_ok.andWrap(::ConfigHandleAddress),
        getError = bindings_h::result_config_handle_get_error
            .andWrap(::RustFFIError)
            .andThen(::consumeRustError),
    )
}

@JvmInline
value class UTF16ArrayHandleResult(
    override val address: MemoryAddress
): ResultError<String> {
    override fun toResult(): KResult<String, FFIError<String>> = KResult.from2(
        handle = address,
        getType = bindings_h::result_utf16_array_error_get_type,
        getOk = bindings_h::result_utf16_array_error_get_ok
            .andWrap(::UTF16ArrayHandle)
            .andThen(::utf16ArrayToString),
        getError = bindings_h::result_utf16_array_error_get_error
            .andWrap(::RustFFIError)
            .andThen(::consumeRustError),
    )
}

@JvmInline
value class VoidResult(
    override val address: MemoryAddress
): ResultError<Unit> {
    override fun toResult(): KResult<Unit, FFIError<String>> = KResult.from2(
        handle = address,
        getType = bindings_h::result_void_get_type,
        getOk = bindings_h::result_void_get_ok,
        getError = bindings_h::result_void_get_error
            .andWrap(::RustFFIError)
            .andThen(::consumeRustError),
    )
}

@JvmInline
value class TileArrayResult(
    override val address: MemoryAddress
): ResultError<TileArrayAddress> {
    override fun toResult(): KResult<TileArrayAddress, FFIError<String>> = KResult.from2(
        handle = address,
        getType = bindings_h::result_tile_array_error_get_type,
        getOk = bindings_h::result_tile_array_error_get_ok.andWrap(::TileArrayAddress),
        getError = bindings_h::result_tile_array_error_get_error
            .andWrap(::RustFFIError)
            .andThen(::consumeRustError),
    )
}

@JvmInline
value class RustFFIError(override val address: MemoryAddress) : TypedMemoryAddress

private fun consumeRustError(
    error: RustFFIError
): String = utf16ArrayToString(UTF16ArrayHandle(bindings_h.error_consume_info(error.address)))