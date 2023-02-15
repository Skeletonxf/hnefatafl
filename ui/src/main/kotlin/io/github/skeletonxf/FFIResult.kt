package io.github.skeletonxf

import java.lang.foreign.MemoryAddress

sealed class FFIResult<out T, out E> {
    data class Ok<T>(val ok: T) : FFIResult<T, Nothing>()
    data class Err<E>(val err: E) : FFIResult<Nothing, E>()

    fun okOrNull(): T? = when (this) {
        is Ok -> ok
        is Err -> null
    }

    fun <R> fold(ok: (T) -> R, err: (E) -> R) = when (this) {
        is Ok -> ok(this.ok)
        is Err -> err(this.err)
    }

    companion object {
        private const val FFI_RESULT_TYPE_OK: Byte = 0
        private const val FFI_RESULT_TYPE_ERROR: Byte = 1
        private const val FFI_RESULT_TYPE_NULL: Byte = 2

        fun <T, E> from(
            handle: MemoryAddress,
            get_type: (MemoryAddress) -> Byte,
            get_ok: (MemoryAddress) -> T,
            get_err: (MemoryAddress) -> E,
        ): FFIResult<T, FFIError<E?>> = when (get_type(handle)) {
            FFI_RESULT_TYPE_OK -> Ok(get_ok(handle))
            FFI_RESULT_TYPE_ERROR -> Err(FFIError("Data was error", get_err(handle)))
            FFI_RESULT_TYPE_NULL -> Err(FFIError("Data was null or corrupted", null))
            else -> Err(FFIError("Unrecognised FFI result type", null))
        }
    }
}