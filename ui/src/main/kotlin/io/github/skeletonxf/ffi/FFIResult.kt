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

interface ResultError<T> : TypedMemoryAddress {
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
            .andThen(::consumeRustError)
            .andThen(::utf16ArrayToString),
    )
}

@JvmInline
value class RustFFIError(override val address: MemoryAddress) : TypedMemoryAddress

private fun consumeRustError(
    error: RustFFIError
): UTF16ArrayHandle = UTF16ArrayHandle(bindings_h.error_consume_info(error.address))