package io.github.skeletonxf.settings

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.ffi.FFIError
import io.github.skeletonxf.ffi.KEnum

interface Config {
    enum class StringKey : ConfigKey<String> {
        Locale;

        override fun value(): Byte = when (this) {
            Locale -> bindings_h.Locale().toByte()
        }

        companion object {
            private val variants = StringKey.values().toList()
            fun valueOf(key: Byte) = KEnum.valueOf(key, variants, Locale)
        }
    }

    fun get(key: StringKey): KResult<String, FFIError<String>>
    fun set(key: StringKey, value: String): KResult<Unit, FFIError<String>>

    fun getAll(): KResult<String, FFIError<String>>

    @Suppress("UNCHECKED_CAST")
    fun <V> get(key: ConfigKey<V>): KResult<V, FFIError<String>> = when (key) {
        is StringKey -> get(key).map { it as V }
    }

    fun <V> set(key: ConfigKey<V>, value: V): KResult<Unit, FFIError<String>> = when (key) {
        is StringKey -> set(key, value as String)
    }
}

sealed interface ConfigKey<V> : KEnum