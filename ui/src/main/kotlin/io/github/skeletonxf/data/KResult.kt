package io.github.skeletonxf.data

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

    fun <R> fold(ok: (T) -> R, err: (E) -> R) = when (this) {
        is Ok -> ok(this.ok)
        is Error -> err(this.err)
    }

    companion object
}