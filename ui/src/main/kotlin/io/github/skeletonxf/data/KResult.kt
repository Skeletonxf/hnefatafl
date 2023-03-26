package io.github.skeletonxf.data

import io.github.skeletonxf.bindings.bindings_h.Err

/**
 * Kotliny version of Rust's Ok/Err result
 */
sealed class KResult<out T, out E> {
    data class Ok<T>(val ok: T) : KResult<T, Nothing>()
    data class Error<E>(val err: E) : KResult<Nothing, E>()

    fun okOrNull(): T? = when (this) {
        is Ok -> ok
        is Error -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Ok -> null
        is Error -> this.err
    }

    fun <R> fold(ok: (T) -> R, error: (E) -> R): R = when (this) {
        is Ok -> ok(this.ok)
        is Error -> error(this.err)
    }

    fun <T2> map(op: (T) -> T2): KResult<T2, E> = when (this) {
        is Ok -> Ok(op(this.ok))
        is Error -> this
    }

    fun <E2> mapError(op: (E) -> E2): KResult<T, E2> = when (this) {
        is Ok -> this
        is Error -> Error(op(this.err))
    }

    companion object {
        fun <T> runCatching(op: () -> T): KResult<T, Throwable> = try {
            Ok(op())
        } catch (error: Throwable) {
            Error(error)
        }
    }
}

fun <T, T2, E> KResult<T, E>.andThen(op: (T) -> KResult<T2, E>): KResult<T2, E> = when (this) {
    is KResult.Ok -> op(this.ok)
    is KResult.Error -> this
}