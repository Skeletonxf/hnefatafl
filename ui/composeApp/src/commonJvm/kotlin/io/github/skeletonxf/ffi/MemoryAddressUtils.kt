package io.github.skeletonxf.ffi

import io.github.skeletonxf.data.KResult
import java.lang.foreign.MemorySegment

/**
 * Do operations with a heap allocated object we need to clean up after use.
 *
 * Most likely this object is one we need to destroy with an exclusive reference, so
 * don't give out the memory address to other parts of the program.
 */
fun <R> MemorySegment.use(
    destroy: (MemorySegment) -> Unit,
    operation: (MemorySegment) -> R
): R {
    try {
        return operation(this)
    } finally {
        destroy(this)
    }
}

fun <T, E,> KResult<MemorySegment, E>.map(
    destroy: (MemorySegment) -> Unit,
    operation: (MemorySegment) -> T
): KResult<T, E> = map { address -> address.use(destroy, operation) }