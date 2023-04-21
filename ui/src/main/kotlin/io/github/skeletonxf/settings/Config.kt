package io.github.skeletonxf.settings

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.ffi.FFIError
import io.github.skeletonxf.ffi.KEnum

interface Config {
    fun debug()

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

    fun get(key: StringKey): KResult<String, FFIError<Unit?>>
    fun set(key: StringKey, value: String): KResult<Unit, FFIError<Unit?>>

    @Suppress("UNCHECKED_CAST")
    fun <V> get(key: ConfigKey<V>): KResult<V, FFIError<Unit?>> = when (key) {
        is StringKey -> get(key).map { it as V }
    }

    fun <V> set(key: ConfigKey<V>, value: V): KResult<Unit, FFIError<Unit?>> = when (key) {
        is StringKey -> set(key, value as String)
    }
}

sealed interface ConfigKey<V> : KEnum