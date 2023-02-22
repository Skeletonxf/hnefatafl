package io.github.skeletonxf.ffi

import io.github.skeletonxf.data.KResult
import java.lang.foreign.MemoryAddress

/**
 * Do operations with a heap allocated object we need to clean up after use.
 *
 * Most likely this object is one we need to destroy with an exclusive reference, so
 * don't give out the memory address to other parts of the program.
 */
fun <R> MemoryAddress.use(
    destroy: (MemoryAddress) -> Unit,
    operation: (MemoryAddress) -> R
): R {
    try {
        return operation(this)
    } finally {
        destroy(this)
    }
}

fun <T, E,> KResult<MemoryAddress, E>.map(
    destroy: (MemoryAddress) -> Unit,
    operation: (MemoryAddress) -> T
): KResult<T, E> = map { address -> address.use(destroy, operation) }