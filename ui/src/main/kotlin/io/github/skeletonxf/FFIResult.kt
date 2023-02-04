package io.github.skeletonxf

import java.lang.foreign.MemoryAddress

sealed class FFIResult<out T, out E> {
    data class Ok<T>(val ok: T) : FFIResult<T, Nothing>()
    data class Err<E>(val err: E) : FFIResult<Nothing, E>()

    fun okOrThrow(): T = when (this) {
        is Ok -> ok
        is Err -> throw IllegalStateException("Wasn't Ok")
    }

    companion object {
        fun <T, E> from(
            handle: MemoryAddress,
            is_ok: (MemoryAddress) -> Boolean,
            get_ok: (MemoryAddress) -> T,
            get_err: (MemoryAddress) -> E,
        ): FFIResult<T, E> = when (is_ok(handle)) {
            true -> Ok(get_ok(handle))
            false -> Err(get_err(handle))
        }
    }
}