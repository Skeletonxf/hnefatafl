package io.github.skeletonxf.ffi

import io.github.skeletonxf.bindings.bindings_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

@JvmInline
value class UTF16ArrayHandle(override val address: MemorySegment) : TypedMemorySegment

/**
 * Converts a memory address of a character array back to a string and deallocates
 * the character array memory.
 */
fun utf16ArrayToString(charsArrayHandle: UTF16ArrayHandle): String {
    val string = Arena.ofConfined().use { arena ->
        val chars = bindings_h.utf16_array_length(charsArrayHandle.address).toInt()
        if (chars == 0) {
            return ""
        }
        val bytes = chars * 2L
        val memorySegment = arena.allocate(bytes)
        bindings_h.utf16_array_copy_to(charsArrayHandle.address, memorySegment)
        String(CharArray(chars).apply { copyFrom(memorySegment, chars) })
    }
    bindings_h.utf16_array_destroy(charsArrayHandle.address)
    return string
}

/**
 * Performs an operation with a string converted to a character array passed as a memory segment
 * before the memory session is deallocated.
 * 
 * If the string is empty, the operation is called with null.
 */
fun <R> withStringToUTF16Array(
    string: String,
    operation: (MemorySegment?) -> R
): R = Arena.ofConfined().use { arena ->
    val chars = string.length
    if (chars == 0) {
        return@use operation(null)
    }
    val bytes = chars * 2L
    val charArray = string.toCharArray()
    val memorySegment = arena.allocate(bytes)
    memorySegment.copyFrom(charArray, chars)
    operation(memorySegment)
}

private fun MemorySegment.copyFrom(charArray: CharArray, chars: Int) = MemorySegment
    .copy(charArray, 0, this, ValueLayout.JAVA_CHAR, 0, chars)

private fun CharArray.copyFrom(memorySegment: MemorySegment, chars: Int) = MemorySegment
    .copy(memorySegment, ValueLayout.JAVA_CHAR, 0, this, 0, chars)