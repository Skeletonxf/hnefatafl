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