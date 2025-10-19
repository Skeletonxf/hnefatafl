package io.github.skeletonxf.settings

import io.github.skeletonxf.bindings.bindings_h
import io.github.skeletonxf.data.KResult
import io.github.skeletonxf.data.Player
import io.github.skeletonxf.ffi.FFIError
import io.github.skeletonxf.ffi.KEnum

interface Config {
    enum class StringKey : ConfigKey<String> {
        Locale;
    }

    fun get(key: StringKey): String
    fun set(key: StringKey, value: String)

    fun getAll(): String

    @Suppress("UNCHECKED_CAST")
    fun <V> get(key: ConfigKey<V>): V = when (key) {
        is StringKey -> get(key) as V
    }

    fun <V> set(key: ConfigKey<V>, value: V): Unit = when (key) {
        is StringKey -> set(key, value as String)
    }
}

sealed interface ConfigKey<V>
