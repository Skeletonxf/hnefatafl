package io.github.skeletonxf.settings

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
