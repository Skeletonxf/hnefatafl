package io.github.skeletonxf.ffi

/**
 * A kotlin enum of some Rust FFI type which is represented as integer variants with no associated data.
 */
interface KEnum {
    fun value(): Byte

    companion object {
        /**
         * Given all the KEnum variants, parses some integer variant into the matching KEnum
         */
        fun <T: KEnum> valueOf(
            byte: Byte,
            variants: List<T>,
            catchall: T
        ): T = variants.firstOrNull { it.value() == byte } ?: catchall
    }
}
