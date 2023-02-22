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

    fun <R> fold(ok: (T) -> R, error: (E) -> R): R = when (this) {
        is Ok -> ok(this.ok)
        is Error -> error(this.err)
    }

    fun <T2> map(op: (T) -> T2): KResult<T2, E> = when (this) {
        is Ok -> Ok(op(this.ok))
        is Error -> this
    }

    companion object
}