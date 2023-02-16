package io.github.skeletonxf.ffi

data class FFIError<E>(val message: String, val error: E) {
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION") // Compiler doesn't realise this
    fun toThrowable() = FFIThrowable(message, error, if (error == null) Void::class else error!!::class)
}