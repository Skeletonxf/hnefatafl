package io.github.skeletonxf.ffi

import java.lang.foreign.MemoryAddress

interface TypedMemoryAddress {
    val address: MemoryAddress
}