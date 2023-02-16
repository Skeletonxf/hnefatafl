package io.github.skeletonxf.ffi

import kotlin.reflect.KClass

data class FFIThrowable(
    override val message: String,
    val error: Any?,
    val errorType: KClass<*>
) : Throwable(message)