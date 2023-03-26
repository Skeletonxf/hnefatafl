package io.github.skeletonxf.settings

import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.ffi.FFIError

interface Config {
    fun debug()

    fun getLocale(): KResult<String, FFIError<Unit?>>
}