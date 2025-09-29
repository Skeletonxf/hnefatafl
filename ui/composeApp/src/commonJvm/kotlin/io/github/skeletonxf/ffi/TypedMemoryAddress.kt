package io.github.skeletonxf.ffi

import java.lang.foreign.MemorySegment

interface TypedMemorySegment {
    val address: MemorySegment
}